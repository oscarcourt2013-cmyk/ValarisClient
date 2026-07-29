import type { AccountTier, NewsItem } from './types'
import type { Locale } from './i18n'

export function formatTier(tier: AccountTier | string, locale: Locale = 'en'): string {
  const labels: Record<Locale, Record<string, string>> = {
    en: {
      free: 'Free',
      prime: 'Prime',
      prime_plus: 'Prime Plus'
    },
    fr: {
      free: 'Gratuit',
      prime: 'Prime',
      prime_plus: 'Prime Plus'
    }
  }
  return labels[locale][tier] ?? tier
}

export function formatNewsTag(tag: NewsItem['tag']): string {
  const labels: Record<NewsItem['tag'], string> = {
    update: 'Update',
    event: 'Event',
    announcement: 'Announcement',
    launcher: 'Launcher'
  }
  return labels[tag]
}

export function formatLoader(loader: string): string {
  if (loader === 'neoforge') return 'NeoForge'
  return loader.charAt(0).toUpperCase() + loader.slice(1)
}

export function playerHeadUrl(uuid: string | undefined, username: string, pixelSize: number): string {
  const id = uuid ? uuid.replace(/-/g, '') : encodeURIComponent(username)
  return `https://mc-heads.net/avatar/${id}/${pixelSize}`
}

export function playerSkinUrl(uuid: string | undefined, username: string): string {
  const id = uuid ? uuid.replace(/-/g, '') : encodeURIComponent(username)
  return `https://mc-heads.net/skin/${id}`
}

/** Official / Mojang cape texture when available; falls back to mc-heads. */
export function playerCapeUrl(uuid: string | undefined, username: string, capeUrl?: string | null): string | null {
  if (capeUrl && capeUrl.trim()) return capeUrl.trim()
  if (!uuid && !username) return null
  const id = uuid ? uuid.replace(/-/g, '') : encodeURIComponent(username)
  return `https://mc-heads.net/cape/${id}`
}

/** Full-body skin render for the Skins hub preview. */
export function playerBodyUrl(uuid: string | undefined, username: string, size = 280): string {
  const id = uuid ? uuid.replace(/-/g, '') : encodeURIComponent(username)
  return `https://mc-heads.net/body/${id}/${size}`
}

export function isSkinTextureUrl(url: string): boolean {
  return url.includes('textures.minecraft.net') || url.includes('/texture/')
}
