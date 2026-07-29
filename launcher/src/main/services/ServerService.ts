import { randomUUID } from 'crypto'
import type { FavoriteServer } from '../../shared/types'
import { findCatalogEntry, mergeCatalogMetadata, SERVER_CATALOG } from '../../shared/server-catalog'
import { ecosystemStore } from '../storage/EcosystemStore'

export function parseServerAddress(address: string): { host: string; port: number } {
  const trimmed = address.trim()
  const colonIdx = trimmed.lastIndexOf(':')
  if (colonIdx > 0 && /^\d+$/.test(trimmed.slice(colonIdx + 1))) {
    return { host: trimmed.slice(0, colonIdx), port: Number(trimmed.slice(colonIdx + 1)) }
  }
  return { host: trimmed, port: 25565 }
}

function validateAddress(address: string): string | null {
  const { host } = parseServerAddress(address)
  if (!host || host.length > 255) {
    return 'Invalid server address.'
  }
  return null
}

function validateName(name: string): string | null {
  const trimmed = name.trim()
  if (trimmed.length < 1 || trimmed.length > 48) {
    return 'Name must be 1â€“48 characters.'
  }
  return null
}

type McStatusPayload = {
  online?: boolean
  version?: { name_clean?: string; name?: string }
  players?: { online?: number; max?: number }
  motd?: { clean?: string | string[]; html?: string }
  icon?: string | null
}

function motdText(motd: McStatusPayload['motd']): string | undefined {
  if (!motd?.clean) return undefined
  return Array.isArray(motd.clean) ? motd.clean.join(' ') : motd.clean
}

/**
 * Favorite servers â€” persisted locally with live status via mcstatus.io.
 * Last-known player counts are retained when a probe fails (anti-flicker).
 */
export class ServerService {
  async list(): Promise<FavoriteServer[]> {
    await this.ensurePartnersSeeded()
    const db = await ecosystemStore.load()
    const servers = (db.favoriteServers ?? []).map(mergeCatalogMetadata)
    // Partners always first (pinned).
    return [...servers].sort((a, b) => Number(Boolean(b.partner)) - Number(Boolean(a.partner)))
  }

  /** Seed partner catalog entries once and keep them pinned at the front of the store. */
  private async ensurePartnersSeeded(): Promise<void> {
    await ecosystemStore.mutate((db) => {
      if (!db.favoriteServers) {
        db.favoriteServers = []
      }
      for (const entry of SERVER_CATALOG) {
        const host = parseServerAddress(entry.address).host.toLowerCase()
        const existing = db.favoriteServers.find(
          (s) => parseServerAddress(s.address).host.toLowerCase() === host
        )
        if (!existing) {
          db.favoriteServers.push({
            id: randomUUID(),
            name: entry.name,
            address: entry.address,
            description: entry.description,
            fullDescription: entry.fullDescription,
            bannerUrl: entry.bannerUrl,
            iconUrl: entry.iconUrl,
            version: entry.versionHint,
            social: entry.social,
            partner: true
          })
        } else {
          existing.partner = true
        }
      }

      // Persist partner-first order (catalog sequence), don't leave partners at end.
      const partners: FavoriteServer[] = []
      const seen = new Set<string>()
      for (const entry of SERVER_CATALOG) {
        const host = parseServerAddress(entry.address).host.toLowerCase()
        const match = db.favoriteServers.find(
          (s) => parseServerAddress(s.address).host.toLowerCase() === host
        )
        if (match && !seen.has(match.id)) {
          partners.push(match)
          seen.add(match.id)
        }
      }
      const rest = db.favoriteServers.filter((s) => !seen.has(s.id))
      db.favoriteServers = [...partners, ...rest]
    })
  }

  async add(name: string, address: string): Promise<{ ok: boolean; error?: string; server?: FavoriteServer }> {
    const nameErr = validateName(name)
    if (nameErr) {
      return { ok: false, error: nameErr }
    }
    const addrErr = validateAddress(address)
    if (addrErr) {
      return { ok: false, error: addrErr }
    }

    const catalog = findCatalogEntry(address)
    const server: FavoriteServer = mergeCatalogMetadata({
      id: randomUUID(),
      name: name.trim(),
      address: address.trim(),
      description: catalog?.description,
      fullDescription: catalog?.fullDescription,
      bannerUrl: catalog?.bannerUrl,
      iconUrl: catalog?.iconUrl,
      version: catalog?.versionHint,
      social: catalog?.social,
      partner: Boolean(catalog)
    })

    await ecosystemStore.mutate((db) => {
      if (!db.favoriteServers) {
        db.favoriteServers = []
      }
      db.favoriteServers.push(server)
    })

    return { ok: true, server }
  }

  async remove(serverId: string): Promise<{ ok: boolean; error?: string }> {
    const db = await ecosystemStore.load()
    const target = db.favoriteServers?.find((s) => s.id === serverId)
    if (!target) {
      return { ok: false, error: 'Server not found.' }
    }
    if (target.partner || findCatalogEntry(target.address)) {
      return { ok: false, error: 'Partner servers cannot be removed.' }
    }

    await ecosystemStore.mutate((db) => {
      db.favoriteServers = (db.favoriteServers ?? []).filter((s) => s.id !== serverId)
    })

    return { ok: true }
  }

  async updateMeta(
    serverId: string,
    patch: Partial<Pick<FavoriteServer, 'description' | 'fullDescription' | 'bannerUrl' | 'social'>>
  ): Promise<FavoriteServer | null> {
    let updated: FavoriteServer | null = null
    await ecosystemStore.mutate((db) => {
      const idx = db.favoriteServers?.findIndex((s) => s.id === serverId) ?? -1
      if (idx < 0 || !db.favoriteServers) {
        return
      }
      updated = mergeCatalogMetadata({ ...db.favoriteServers[idx], ...patch })
      db.favoriteServers[idx] = updated
    })
    return updated
  }

  async refreshStatus(serverId: string): Promise<FavoriteServer | null> {
    const db = await ecosystemStore.load()
    const server = db.favoriteServers?.find((s) => s.id === serverId)
    if (!server) {
      return null
    }

    const previous = mergeCatalogMetadata(server)
    const { host, port } = parseServerAddress(server.address)
    // Keep last-known counts by default â€” only overwrite on a successful online probe.
    let updated: FavoriteServer = { ...previous, online: false }

    try {
      const start = Date.now()
      const response = await fetch(`https://api.mcstatus.io/v2/status/java/${host}:${port}`, {
        headers: { Accept: 'application/json', 'User-Agent': 'stellar-client-launcher' }
      })
      const ping = Date.now() - start

      if (response.ok) {
        const data = (await response.json()) as McStatusPayload
        if (data.online) {
          updated = {
            ...previous,
            online: true,
            players: data.players?.online ?? previous.players ?? 0,
            maxPlayers: data.players?.max ?? previous.maxPlayers ?? 0,
            ping,
            version: data.version?.name_clean || data.version?.name || previous.version,
            motd: motdText(data.motd) || previous.motd,
            description: previous.description || motdText(data.motd),
            iconUrl: data.icon || previous.iconUrl,
            lastOnlineAt: Date.now()
          }
        } else {
          // Offline: keep cached players/max/ping so the UI does not flash 0/Offline.
          updated = {
            ...previous,
            online: false,
            ping: undefined
          }
        }
      }
    } catch {
      updated = { ...previous, online: previous.online ?? false, ping: undefined }
    }

    await ecosystemStore.mutate((db) => {
      const idx = db.favoriteServers?.findIndex((s) => s.id === serverId) ?? -1
      if (idx >= 0 && db.favoriteServers) {
        db.favoriteServers[idx] = updated
      }
    })

    return updated
  }

  async refreshAll(): Promise<FavoriteServer[]> {
    const servers = await this.list()
    if (servers.length === 0) {
      return []
    }
    const refreshed = await Promise.all(servers.map((s) => this.refreshStatus(s.id)))
    const list = refreshed.filter((s): s is FavoriteServer => s !== null).map(mergeCatalogMetadata)
    return [...list].sort((a, b) => Number(Boolean(b.partner)) - Number(Boolean(a.partner)))
  }
}

export const serverService = new ServerService()
