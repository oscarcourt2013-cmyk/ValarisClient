import net from 'net'
import { readdirSync } from 'fs'
import type { DiscordPresencePayload } from './types'

const OP_HANDSHAKE = 0
const OP_FRAME = 1
const OP_CLOSE = 2

const CONNECT_TIMEOUT_MS = 3000
const READ_TIMEOUT_MS = 5000
const CONNECT_COOLDOWN_MS = 500

export type DiscordIpcErrorCode =
  | 'discord_not_running'
  | 'handshake_failed'
  | 'activity_rejected'
  | 'ipc_timeout'
  | 'ipc_closed'
  | 'unknown'

export class DiscordIpcError extends Error {
  constructor(
    message: string,
    readonly code: DiscordIpcErrorCode
  ) {
    super(message)
    this.name = 'DiscordIpcError'
  }
}

function trim(text: string, max: number): string {
  if (text.length <= max) {
    return text
  }
  return `${text.slice(0, Math.max(0, max - 1))}â€¦`
}

function sleep(ms: number): Promise<void> {
  return new Promise((resolve) => setTimeout(resolve, ms))
}

function isPipeMissingError(message: string): boolean {
  const lower = message.toLowerCase()
  return (
    lower.includes('enoent') ||
    lower.includes('econnrefused') ||
    lower.includes('connectex') ||
    lower.includes('no such file')
  )
}

/** Windows: scan named pipes; fallback to discord-ipc-0..9. */
function discordPipeCandidates(): string[] {
  if (process.platform === 'win32') {
    try {
      const names = readdirSync('\\\\.\\pipe\\')
        .filter((name) => /^discord(?:-(?:ptb|canary))?-ipc-\d+$/i.test(name))
        .sort((a, b) => a.localeCompare(b, undefined, { numeric: true }))
      if (names.length > 0) {
        return names.map((name) => `\\\\.\\pipe\\${name}`)
      }
    } catch {
      // ignore â€” fall through to defaults
    }
    return Array.from({ length: 10 }, (_, index) => `\\\\.\\pipe\\discord-ipc-${index}`)
  }

  const base = process.env.XDG_RUNTIME_DIR || '/tmp'
  return Array.from({ length: 10 }, (_, index) => `${base}/discord-ipc-${index}`)
}

/** Minimal Discord Rich Presence IPC â€” Windows named pipes + Unix sockets. */
export class DiscordIpcClient {
  private socket: net.Socket | null = null
  private connected = false
  private lastConnectAttempt = 0
  private recvBuffer = Buffer.alloc(0)
  private readonly queue: Array<() => Promise<void>> = []
  private draining = false
  lastError: string | null = null

  constructor(private readonly applicationId: string) {}

  isConnected(): boolean {
    return this.connected
  }

  async connect(): Promise<boolean> {
    if (this.connected) {
      return true
    }

    const waitMs = CONNECT_COOLDOWN_MS - (Date.now() - this.lastConnectAttempt)
    if (waitMs > 0) {
      await sleep(waitMs)
    }

    this.lastConnectAttempt = Date.now()
    this.close()

    const paths = discordPipeCandidates()
    const errors: string[] = []

    for (const pipePath of paths) {
      try {
        await this.openTransport(pipePath)
        await this.handshake()
        this.connected = true
        this.lastError = null
        return true
      } catch (err) {
        const message = err instanceof Error ? err.message : String(err)
        errors.push(`${pipePath}: ${message}`)
        this.lastError = message
        this.close()
      }
    }

    const allMissing = errors.length > 0 && errors.every((entry) => isPipeMissingError(entry))
    if (allMissing || errors.length === 0) {
      this.lastError =
        'Discord Desktop not detected â€” open Discord and restart the launcher.'
    } else if (errors.length > 0) {
      this.lastError = `${this.lastError} (tried ${paths.length} pipe(s))`
    }

    return false
  }

  async setActivity(payload: DiscordPresencePayload): Promise<boolean> {
    if (!(await this.ensureConnected())) {
      return false
    }

    try {
      await this.sendActivity(payload, true)
      this.lastError = null
      return true
    } catch (err) {
      const message = err instanceof Error ? err.message : String(err)
      if (payload.buttons?.length) {
        try {
          await this.sendActivity({ ...payload, buttons: undefined }, false)
          this.lastError = null
          return true
        } catch (retryErr) {
          this.lastError = retryErr instanceof Error ? retryErr.message : String(retryErr)
        }
      } else {
        this.lastError = message
      }
      this.connected = false
      this.close()
      return false
    }
  }

  async clearActivity(): Promise<void> {
    if (!this.connected) {
      return
    }
    try {
      await this.sendFrame(this.buildClearActivity())
    } catch {
      this.connected = false
      this.close()
    }
  }

  shutdown(): void {
    void this.enqueue(async () => {
      if (this.connected) {
        try {
          await this.sendFrame(this.buildClearActivity())
        } catch {
          // ignore
        }
      }
      this.close()
    })
  }

  private async sendActivity(payload: DiscordPresencePayload, withButtons: boolean): Promise<void> {
    const json = withButtons ? this.buildSetActivity(payload) : this.buildSetActivity(payload, false)
    await this.sendFrame(json)
  }

  private async ensureConnected(): Promise<boolean> {
    if (this.connected) {
      return true
    }
    return this.connect()
  }

  private openTransport(pipePath: string): Promise<void> {
    return new Promise((resolve, reject) => {
      const socket = net.connect(pipePath)
      this.socket = socket
      this.recvBuffer = Buffer.alloc(0)

      const cleanup = (): void => {
        clearTimeout(timer)
        socket.off('error', onError)
        socket.off('connect', onConnect)
      }

      const onError = (err: Error): void => {
        cleanup()
        reject(err)
      }

      const onConnect = (): void => {
        cleanup()
        socket.on('data', (chunk: Buffer) => {
          this.recvBuffer = Buffer.concat([this.recvBuffer, chunk])
        })
        socket.on('error', () => {
          this.connected = false
        })
        socket.on('close', () => {
          this.connected = false
        })
        resolve()
      }

      const timer = setTimeout(() => {
        socket.destroy()
        reject(new DiscordIpcError(`Connection timeout (${pipePath})`, 'ipc_timeout'))
      }, CONNECT_TIMEOUT_MS)

      socket.once('error', onError)
      socket.once('connect', onConnect)
    })
  }

  private async handshake(): Promise<void> {
    const payload = JSON.stringify({ v: 1, client_id: this.applicationId })
    await this.writePacket(OP_HANDSHAKE, payload)
    const body = await this.readPacket(READ_TIMEOUT_MS)
    if (body) {
      this.assertNoError(body, 'handshake_failed')
    }
  }

  private buildSetActivity(snapshot: DiscordPresencePayload, includeButtons = true): string {
    const activity: Record<string, unknown> = { type: 0 }
    if (snapshot.details) {
      activity.details = trim(snapshot.details, 128)
    }
    if (snapshot.state) {
      activity.state = trim(snapshot.state, 128)
    }
    if (snapshot.startTimestamp) {
      activity.timestamps = { start: snapshot.startTimestamp }
    }

    activity.assets = {
      large_image: snapshot.largeImageKey ?? 'prime_logo',
      large_text: trim(snapshot.largeImageText ?? 'StellarClient', 128),
      small_image: snapshot.smallImageKey ?? 'prime_logo',
      ...(snapshot.smallImageText ? { small_text: trim(snapshot.smallImageText, 128) } : {})
    }

    if (includeButtons && snapshot.buttons?.length) {
      const buttons: Array<{ label: string; url: string }> = []
      for (const button of snapshot.buttons.slice(0, 2)) {
        buttons.push({
          label: trim(button.label, 32),
          url: button.url
        })
      }
      if (buttons.length > 0) {
        activity.buttons = buttons
      }
    }

    return JSON.stringify({
      cmd: 'SET_ACTIVITY',
      nonce: `${Date.now()}`,
      args: {
        pid: process.pid,
        activity
      }
    })
  }

  private buildClearActivity(): string {
    return JSON.stringify({
      cmd: 'SET_ACTIVITY',
      nonce: `${Date.now()}`,
      args: {
        pid: process.pid,
        activity: null
      }
    })
  }

  private async sendFrame(json: string): Promise<void> {
    await this.writePacket(OP_FRAME, json)

    const deadline = Date.now() + READ_TIMEOUT_MS
    while (Date.now() < deadline) {
      const body = await this.readPacket(Math.max(250, deadline - Date.now()))
      if (!body) {
        continue
      }

      try {
        const parsed = JSON.parse(body) as { cmd?: string; evt?: string }
        if (parsed.evt === 'ERROR') {
          this.assertNoError(body, 'activity_rejected')
        }
        if (parsed.cmd === 'SET_ACTIVITY') {
          return
        }
        // Ignore unrelated DISPATCH frames.
      } catch (err) {
        if (err instanceof DiscordIpcError) {
          throw err
        }
      }
    }

    throw new DiscordIpcError('Discord IPC read timeout', 'ipc_timeout')
  }

  private assertNoError(body: string, code: DiscordIpcErrorCode): void {
    try {
      const parsed = JSON.parse(body) as { evt?: string; data?: { message?: string; code?: number } }
      if (parsed.evt === 'ERROR') {
        const detail = parsed.data?.message ?? `Discord error ${parsed.data?.code ?? ''}`.trim()
        throw new DiscordIpcError(detail || 'Discord rejected Rich Presence', code)
      }
    } catch (err) {
      if (err instanceof DiscordIpcError) {
        throw err
      }
      // Non-JSON frame (DISPATCH) â€” success
    }
  }

  private writePacket(opcode: number, json: string): Promise<void> {
    const body = Buffer.from(json, 'utf8')
    const header = Buffer.alloc(8)
    header.writeInt32LE(opcode, 0)
    header.writeInt32LE(body.length, 4)
    return this.write(Buffer.concat([header, body]))
  }

  private write(data: Buffer): Promise<void> {
    return new Promise((resolve, reject) => {
      if (!this.socket) {
        reject(new DiscordIpcError('Discord IPC not connected', 'discord_not_running'))
        return
      }
      this.socket.write(data, (err) => {
        if (err) {
          this.connected = false
          reject(err)
        } else {
          resolve()
        }
      })
    })
  }

  private readPacket(timeoutMs = READ_TIMEOUT_MS): Promise<string | null> {
    return new Promise((resolve, reject) => {
      const tryRead = (): void => {
        if (this.recvBuffer.length < 8) {
          return
        }
        const opcode = this.recvBuffer.readInt32LE(0)
        const length = this.recvBuffer.readInt32LE(4)
        if (this.recvBuffer.length < 8 + length) {
          return
        }
        const body =
          length > 0 ? this.recvBuffer.subarray(8, 8 + length).toString('utf8') : ''
        this.recvBuffer = this.recvBuffer.subarray(8 + length)
        if (opcode === OP_CLOSE) {
          this.connected = false
          reject(new DiscordIpcError('Discord IPC closed connection', 'ipc_closed'))
          return
        }
        cleanup()
        resolve(body || null)
      }

      const onData = (): void => {
        tryRead()
      }

      const cleanup = (): void => {
        this.socket?.off('data', onData)
        clearTimeout(timer)
      }

      const timer = setTimeout(() => {
        cleanup()
        reject(new DiscordIpcError('Discord IPC read timeout', 'ipc_timeout'))
      }, timeoutMs)

      this.socket?.on('data', onData)
      tryRead()
    })
  }

  private enqueue(task: () => Promise<void>): Promise<void> {
    return new Promise((resolve, reject) => {
      this.queue.push(async () => {
        try {
          await task()
          resolve()
        } catch (err) {
          reject(err)
        }
      })
      void this.drainQueue()
    })
  }

  private async drainQueue(): Promise<void> {
    if (this.draining) {
      return
    }
    this.draining = true
    while (this.queue.length > 0) {
      const task = this.queue.shift()
      if (task) {
        try {
          await task()
        } catch {
          this.connected = false
          this.close()
        }
      }
    }
    this.draining = false
  }

  private close(): void {
    this.connected = false
    this.recvBuffer = Buffer.alloc(0)
    if (this.socket) {
      try {
        this.socket.destroy()
      } catch {
        // ignore
      }
      this.socket = null
    }
  }
}
