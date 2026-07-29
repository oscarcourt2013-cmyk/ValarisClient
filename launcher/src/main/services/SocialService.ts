import { WebSocket } from 'ws'
import type { FriendEntry } from '../../shared/content-types'
import { microsoftAuth } from '../auth/MicrosoftAuth'
import { accountService } from './AccountService'
import { accountStore } from '../storage/AccountStore'
import { ecosystemStore } from '../storage/EcosystemStore'

/** Production default — same host/port as voice; path differs. */
export const DEFAULT_API_BASE = 'http://194.9.172.102:26005'

export interface SocialFriend {
  friendshipId: string
  status: 'pending' | 'accepted' | 'blocked'
  incoming: boolean
  uuid: string
  username: string
  note?: string
  serverAddress?: string | null
  presence: { status: string; activity: string; serverAddress: string | null }
}

export interface ChatMessage {
  id: string
  conversationId: string
  senderUuid: string
  senderUsername?: string
  text: string
  imageUrl: string | null
  createdAt: string
}

export interface Conversation {
  id: string
  type: string
  participantUuids: string[]
  participants: { uuid: string; username: string }[]
  updatedAt: string
}

type Listener = (event: Record<string, unknown>) => void

function apiBase(): string {
  return (process.env.PRIME_API_BASE || DEFAULT_API_BASE).replace(/\/$/, '')
}

function socialWsUrl(token: string): string {
  const base = apiBase().replace(/^http/, 'ws')
  return `${base}/social?token=${encodeURIComponent(token)}`
}

/** Remote Prime social API + live WebSocket (friends, chat, party, presence). */
export class SocialService {
  private token: string | null = null
  private uuid: string | null = null
  private ws: WebSocket | null = null
  private listeners = new Set<Listener>()
  private connecting: Promise<void> | null = null
  private reconnectFailures = 0
  private reconnectTimer: ReturnType<typeof setTimeout> | null = null
  private pingTimer: ReturnType<typeof setInterval> | null = null

  onEvent(listener: Listener): () => void {
    this.listeners.add(listener)
    return () => this.listeners.delete(listener)
  }

  /** Clears cached session so the next call re-authenticates. */
  clearSession(): void {
    this.token = null
    this.uuid = null
    this.reconnectFailures = 0
    if (this.reconnectTimer) {
      clearTimeout(this.reconnectTimer)
      this.reconnectTimer = null
    }
    this.stopPing()
    try {
      this.ws?.close()
    } catch {
      // ignore
    }
    this.ws = null
    this.connecting = null
  }

  private stopPing(): void {
    if (this.pingTimer) {
      clearInterval(this.pingTimer)
      this.pingTimer = null
    }
  }

  private startPing(): void {
    this.stopPing()
    this.pingTimer = setInterval(() => {
      if (!this.ws || this.ws.readyState !== WebSocket.OPEN) return
      try {
        this.ws.send(JSON.stringify({ t: 'ping', ts: Date.now() }))
      } catch {
        // ignore
      }
    }, 25_000)
  }

  private scheduleReconnect(): void {
    if (!this.token) return
    if (this.reconnectTimer) return
    // Exponential backoff: 3s → 6s → 12s → 24s → 30s cap.
    const delayMs = Math.min(30_000, 3_000 * 2 ** Math.min(this.reconnectFailures, 3))
    this.reconnectFailures++
    this.reconnectTimer = setTimeout(() => {
      this.reconnectTimer = null
      if (this.token) void this.connectWs()
    }, delayMs)
  }

  private emit(event: Record<string, unknown>): void {
    for (const listener of this.listeners) {
      try {
        listener(event)
      } catch {
        // ignore
      }
    }
  }

  async ensureSession(): Promise<{ token: string; uuid: string }> {
    const db = await accountStore.getDb()
    const account = db.accounts.find((a) => a.id === db.activeAccountId) || db.accounts[0]
    if (!account) {
      throw new Error('No Minecraft account. Log in first.')
    }
    if (this.token && this.uuid === account.uuid) {
      return { token: this.token, uuid: this.uuid }
    }
    const accessToken =
      account.type === 'microsoft' ? microsoftAuth.peekCachedAccessToken(account.id) : null
    const res = await fetch(`${apiBase()}/v1/auth/session`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', Accept: 'application/json' },
      body: JSON.stringify({
        uuid: account.uuid,
        username: account.username,
        offline: account.type === 'offline',
        client: 'launcher',
        ...(accessToken ? { accessToken } : {})
      })
    })
    if (!res.ok) {
      const err = (await res.json().catch(() => null)) as { error?: string } | null
      throw new Error(err?.error || `Auth failed (${res.status})`)
    }
    const data = (await res.json()) as { token: string }
    this.token = data.token
    this.uuid = account.uuid
    await this.connectWs()
    return { token: this.token, uuid: this.uuid }
  }

  private async connectWs(): Promise<void> {
    if (!this.token) return
    if (this.connecting) return this.connecting
    this.connecting = new Promise((resolve) => {
      try {
        this.ws?.close()
      } catch {
        // ignore
      }
      const ws = new WebSocket(socialWsUrl(this.token!))
      this.ws = ws
      ws.on('open', () => {
        this.connecting = null
        this.reconnectFailures = 0
        this.startPing()
        this.setPresence('online', 'In launcher')
        resolve()
      })
      ws.on('message', (raw) => {
        try {
          const msg = JSON.parse(raw.toString()) as Record<string, unknown>
          if (msg.t === 'pong') return
          this.emit(msg)
        } catch {
          // ignore
        }
      })
      ws.on('close', () => {
        this.stopPing()
        if (this.ws === ws) {
          this.ws = null
        }
        this.connecting = null
        this.scheduleReconnect()
      })
      ws.on('error', () => {
        this.stopPing()
        this.connecting = null
        try {
          ws.close()
        } catch {
          // ignore — close handler schedules reconnect
        }
        if (this.ws === ws) {
          this.ws = null
        }
        resolve()
      })
    })
    return this.connecting
  }

  private async authHeaders(): Promise<Record<string, string>> {
    const { token } = await this.ensureSession()
    return {
      Authorization: `Bearer ${token}`,
      Accept: 'application/json',
      'Content-Type': 'application/json'
    }
  }

  /** On 401, drop the session so the next request re-logs in. */
  private async fetchAuthed(path: string, init?: RequestInit): Promise<Response> {
    const headers = await this.authHeaders()
    const res = await fetch(`${apiBase()}${path}`, {
      ...init,
      headers: { ...headers, ...(init?.headers as Record<string, string> | undefined) }
    })
    if (res.status === 401) {
      this.clearSession()
      const headers2 = await this.authHeaders()
      return fetch(`${apiBase()}${path}`, {
        ...init,
        headers: { ...headers2, ...(init?.headers as Record<string, string> | undefined) }
      })
    }
    return res
  }

  /** Authenticated HTTP against the Prime API (same Bearer session as friends/social). */
  async apiFetch(path: string, init?: RequestInit): Promise<Response> {
    return this.fetchAuthed(path, init)
  }

  async listFriends(): Promise<FriendEntry[]> {
    const res = await this.fetchAuthed('/v1/friends')
    if (!res.ok) throw new Error('Failed to load friends')
    const data = (await res.json()) as { friends: SocialFriend[] }
    return data.friends
      .filter((f) => f.status === 'accepted' || f.status === 'pending' || f.incoming)
      .map((f) => {
        const serverAddress = f.presence?.serverAddress || f.serverAddress || null
        const note = f.note || ''
        return {
          id: f.uuid,
          username: f.username,
          status: mapStatus(f.presence?.status),
          activity:
            note ||
            (f.status === 'pending'
              ? 'Pending friend request'
              : f.presence?.activity ||
                (serverAddress ? `Playing ${serverAddress}` : 'Offline')),
          serverAddress,
          note,
          pending: f.status === 'pending',
          incoming: f.incoming
        }
      })
  }

  async requestFriend(username: string, note?: string): Promise<{ ok: boolean; error?: string }> {
    try {
      const res = await this.fetchAuthed('/v1/friends/request', {
        method: 'POST',
        body: JSON.stringify({ username: username.trim() })
      })
      const data = (await res.json().catch(() => ({}))) as { error?: string; friendship?: { toUuid?: string } }
      if (!res.ok) return { ok: false, error: data.error || 'Request failed' }
      if (note?.trim() && data.friendship?.toUuid) {
        await this.saveNote(data.friendship.toUuid, note.trim())
      }
      return { ok: true }
    } catch (err) {
      return { ok: false, error: err instanceof Error ? err.message : 'Request failed' }
    }
  }

  async acceptFriend(uuid: string): Promise<{ ok: boolean; error?: string }> {
    try {
      const res = await this.fetchAuthed('/v1/friends/accept', {
        method: 'POST',
        body: JSON.stringify({ uuid })
      })
      if (!res.ok) {
        const data = (await res.json().catch(() => ({}))) as { error?: string }
        return { ok: false, error: data.error || 'Accept failed' }
      }
      return { ok: true }
    } catch (err) {
      return { ok: false, error: err instanceof Error ? err.message : 'Accept failed' }
    }
  }

  async removeFriend(uuid: string): Promise<{ ok: boolean; error?: string }> {
    try {
      const res = await this.fetchAuthed(`/v1/friends/${encodeURIComponent(uuid)}`, {
        method: 'DELETE'
      })
      if (!res.ok) {
        const data = (await res.json().catch(() => ({}))) as { error?: string }
        return { ok: false, error: data.error || 'Remove failed' }
      }
      return { ok: true }
    } catch (err) {
      return { ok: false, error: err instanceof Error ? err.message : 'Remove failed' }
    }
  }

  async listConversations(): Promise<Conversation[]> {
    const res = await this.fetchAuthed('/v1/conversations')
    if (!res.ok) throw new Error('Failed to load conversations')
    const data = (await res.json()) as { conversations: Conversation[] }
    return data.conversations
  }

  async openDm(uuid: string): Promise<Conversation> {
    const res = await this.fetchAuthed('/v1/conversations/dm', {
      method: 'POST',
      body: JSON.stringify({ uuid })
    })
    if (!res.ok) {
      const data = (await res.json().catch(() => ({}))) as { error?: string }
      throw new Error(data.error || 'Failed to open DM')
    }
    const data = (await res.json()) as { conversation: Conversation }
    return data.conversation
  }

  async listMessages(conversationId: string): Promise<ChatMessage[]> {
    const res = await this.fetchAuthed(
      `/v1/conversations/${encodeURIComponent(conversationId)}/messages`
    )
    if (!res.ok) throw new Error('Failed to load messages')
    const data = (await res.json()) as { messages: ChatMessage[] }
    return data.messages.map((m) => ({
      ...m,
      imageUrl: m.imageUrl ? absolutize(m.imageUrl) : null
    }))
  }

  async sendMessage(
    conversationId: string,
    text: string,
    imageUrl?: string | null
  ): Promise<ChatMessage> {
    const res = await this.fetchAuthed(
      `/v1/conversations/${encodeURIComponent(conversationId)}/messages`,
      {
        method: 'POST',
        body: JSON.stringify({ text, imageUrl: imageUrl || null })
      }
    )
    if (!res.ok) {
      const data = (await res.json().catch(() => ({}))) as { error?: string }
      throw new Error(data.error || 'Send failed')
    }
    const data = (await res.json()) as { message: ChatMessage }
    return {
      ...data.message,
      imageUrl: data.message.imageUrl ? absolutize(data.message.imageUrl) : null
    }
  }

  async uploadImage(filePath: string): Promise<string> {
    const { readFile } = await import('fs/promises')
    const { basename } = await import('path')
    const buf = await readFile(filePath)
    const { token } = await this.ensureSession()
    const boundary = '----PrimeUpload' + Date.now()
    const filename = basename(filePath)
    const ext = filename.toLowerCase()
    const contentType = ext.endsWith('.png')
      ? 'image/png'
      : ext.endsWith('.webp')
        ? 'image/webp'
        : ext.endsWith('.gif')
          ? 'image/gif'
          : 'image/jpeg'
    const preamble = Buffer.from(
      `--${boundary}\r\nContent-Disposition: form-data; name="file"; filename="${filename}"\r\nContent-Type: ${contentType}\r\n\r\n`
    )
    const closing = Buffer.from(`\r\n--${boundary}--\r\n`)
    const body = Buffer.concat([preamble, buf, closing])
    const res = await fetch(`${apiBase()}/v1/upload`, {
      method: 'POST',
      headers: {
        Authorization: `Bearer ${token}`,
        'Content-Type': `multipart/form-data; boundary=${boundary}`
      },
      body
    })
    if (!res.ok) {
      const data = (await res.json().catch(() => ({}))) as { error?: string }
      throw new Error(data.error || 'Upload failed')
    }
    const data = (await res.json()) as { url: string }
    return absolutize(data.url)
  }

  async createParty(): Promise<unknown> {
    const res = await this.fetchAuthed('/v1/party', { method: 'POST' })
    if (!res.ok) throw new Error('Failed to create party')
    return res.json()
  }

  async inviteToParty(uuid: string): Promise<unknown> {
    const res = await this.fetchAuthed('/v1/party/invite', {
      method: 'POST',
      body: JSON.stringify({ uuid })
    })
    if (!res.ok) {
      const data = (await res.json().catch(() => ({}))) as { error?: string }
      throw new Error(data.error || 'Invite failed')
    }
    return res.json()
  }

  async leaveParty(): Promise<void> {
    await this.fetchAuthed('/v1/party/leave', { method: 'POST' })
  }

  async acceptPartyInvite(inviteId: string): Promise<unknown> {
    const res = await this.fetchAuthed('/v1/party/accept', {
      method: 'POST',
      body: JSON.stringify({ inviteId })
    })
    if (!res.ok) {
      const data = (await res.json().catch(() => ({}))) as { error?: string }
      throw new Error(data.error || 'Accept invite failed')
    }
    return res.json()
  }

  async declinePartyInvite(inviteId: string): Promise<void> {
    const res = await this.fetchAuthed('/v1/party/decline', {
      method: 'POST',
      body: JSON.stringify({ inviteId })
    })
    if (!res.ok) {
      const data = (await res.json().catch(() => ({}))) as { error?: string }
      throw new Error(data.error || 'Decline invite failed')
    }
  }

  async getParty(): Promise<unknown> {
    const res = await this.fetchAuthed('/v1/party')
    if (!res.ok) throw new Error('Failed to load party')
    return res.json()
  }

  async setPartyServer(serverAddress: string): Promise<{ ok: boolean; error?: string }> {
    try {
      const res = await this.fetchAuthed('/v1/party/server', {
        method: 'POST',
        body: JSON.stringify({ serverAddress: serverAddress.trim() })
      })
      if (!res.ok) {
        const data = (await res.json().catch(() => ({}))) as { error?: string }
        return { ok: false, error: data.error || 'Only the party leader can share a server.' }
      }
      return { ok: true }
    } catch (err) {
      return { ok: false, error: err instanceof Error ? err.message : 'Failed to share server' }
    }
  }

  /** Push presence to the social WebSocket (`{ t: 'presence', status, activity, serverAddress }`). */
  setPresence(status: string, activity: string, serverAddress?: string | null): void {
    if (!this.ws || this.ws.readyState !== WebSocket.OPEN) {
      return
    }
    try {
      this.ws.send(
        JSON.stringify({
          t: 'presence',
          status,
          activity,
          serverAddress: serverAddress ?? null
        })
      )
    } catch {
      // ignore send failures — reconnect loop will restore session
    }
  }

  sendTyping(conversationId: string): void {
    if (!conversationId || !this.ws || this.ws.readyState !== WebSocket.OPEN) return
    try {
      this.ws.send(JSON.stringify({ t: 'typing', conversationId }))
    } catch {
      // ignore
    }
  }

  async updateNote(friendId: string, note: string): Promise<void> {
    const trimmed = note.trim()
    const res = await this.fetchAuthed(`/v1/friends/${encodeURIComponent(friendId)}/note`, {
      method: 'PUT',
      body: JSON.stringify({ note: trimmed })
    })
    if (!res.ok) {
      const data = (await res.json().catch(() => ({}))) as { error?: string }
      throw new Error(data.error || 'Failed to save note')
    }
    await this.saveNote(friendId, trimmed)
  }

  private async localNotes(): Promise<Record<string, string>> {
    const db = await ecosystemStore.load()
    const notes: Record<string, string> = {}
    for (const f of db.friends) {
      if (f.activity && !isPresenceActivity(f.activity)) {
        notes[f.id] = f.activity
      }
    }
    return notes
  }

  private async saveNote(uuid: string, note: string): Promise<void> {
    await ecosystemStore.mutate((db) => {
      const existing = db.friends.find((f) => f.id === uuid)
      if (existing) {
        existing.activity = note
      } else {
        db.friends.push({ id: uuid, username: uuid, status: 'offline', activity: note })
      }
    })
  }
}

function mapStatus(status?: string): FriendEntry['status'] {
  if (status === 'in-game') return 'in-game'
  if (status === 'away') return 'away'
  if (status === 'online') return 'online'
  return 'offline'
}

function isPresenceActivity(text: string): boolean {
  return (
    text.startsWith('Playing') ||
    text === 'Offline' ||
    text === 'In launcher' ||
    text === 'In game' ||
    text === 'Pending friend request'
  )
}

function absolutize(url: string): string {
  if (url.startsWith('http')) return url
  return `${apiBase()}${url.startsWith('/') ? '' : '/'}${url}`
}

export const socialService = new SocialService()

// Touch accountService so tree-shaking keeps the import used for session context
void accountService
