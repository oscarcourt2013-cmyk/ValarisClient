import type { FavoriteServer, ServerSocialLinks } from './types'

/** Curated partner / featured servers — merged into favorites by address. */
export interface ServerCatalogEntry {
  address: string
  name: string
  description: string
  fullDescription: string
  bannerUrl?: string
  iconUrl?: string
  versionHint?: string
  social?: ServerSocialLinks
}

/**
 * Hard-coded catalog for known partners. Addresses are matched case-insensitively
 * without requiring an exact port (default 25565 implied).
 */
export const SERVER_CATALOG: ServerCatalogEntry[] = [
  {
    address: 'elysiasmp.fr',
    name: 'Elysia SMP',
    description: 'Survival multiplayer officiel — économie, claims, events.',
    fullDescription:
      'Elysia SMP est le serveur partenaire officiel de ValerisClient. Survival semi-vanilla, ' +
      'communauté active, boutique et votes. Rejoins avec le client Valeris pour le badge et les cosmétiques.',
    bannerUrl:
      'https://api.mcstatus.io/v2/widget/java/elysiasmp.fr?theme=dark&width=800&height=160',
    versionHint: '1.21+',
    social: {
      discord: 'https://discord.gg/elysia',
      website: 'https://elysiasmp.fr',
      store: 'https://elysiasmp.fr/store',
      vote: 'https://elysiasmp.fr/vote'
    }
  }
]

export function normalizeServerHost(address: string): string {
  let a = address.trim().toLowerCase()
  if (a.startsWith('minecraft://')) {
    a = a.slice('minecraft://'.length)
  }
  const slash = a.indexOf('/')
  if (slash >= 0) {
    a = a.slice(0, slash)
  }
  const colon = a.lastIndexOf(':')
  if (colon > 0 && /^\d+$/.test(a.slice(colon + 1))) {
    a = a.slice(0, colon)
  }
  return a
}

export function findCatalogEntry(address: string): ServerCatalogEntry | undefined {
  const host = normalizeServerHost(address)
  return SERVER_CATALOG.find((e) => normalizeServerHost(e.address) === host)
}

/** Apply catalog metadata onto a favorite without wiping live status fields. */
export function mergeCatalogMetadata(server: FavoriteServer): FavoriteServer {
  const entry = findCatalogEntry(server.address)
  if (!entry) {
    return server
  }
  return {
    ...server,
    name: server.name || entry.name,
    description: server.description || entry.description,
    fullDescription: server.fullDescription || entry.fullDescription,
    bannerUrl: server.bannerUrl || entry.bannerUrl,
    iconUrl: server.iconUrl || entry.iconUrl,
    version: server.version || entry.versionHint,
    social: { ...entry.social, ...server.social },
    partner: true
  }
}
