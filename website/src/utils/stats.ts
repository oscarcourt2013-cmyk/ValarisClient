/** Production Valeris backend (same host as launcher / SocialService). */
export const DEFAULT_API_BASE = 'http://194.9.172.102:26005'

/** Prefer Astro PUBLIC_API_URL at build time, else production default. */
export function apiBase(): string {
  const fromEnv =
    typeof import.meta !== 'undefined' &&
    import.meta.env &&
    typeof import.meta.env.PUBLIC_API_URL === 'string'
      ? import.meta.env.PUBLIC_API_URL.trim()
      : ''
  return (fromEnv || DEFAULT_API_BASE).replace(/\/$/, '')
}

export type PublicStats = {
  downloads: number
  launches: number
  updatedAt?: string
}

/** Fallback shown when the API is unreachable (last known marketing floor). */
export const FALLBACK_STATS: PublicStats = {
  downloads: 0,
  launches: 0
}

export function formatCount(n: number): string {
  try {
    return new Intl.NumberFormat('fr-FR', { notation: n >= 10000 ? 'compact' : 'standard' }).format(
      Math.max(0, Math.floor(n))
    )
  } catch {
    return String(Math.max(0, Math.floor(n)))
  }
}

export async function fetchPublicStats(signal?: AbortSignal): Promise<PublicStats> {
  const res = await fetch(`${apiBase()}/v1/stats`, {
    method: 'GET',
    headers: { Accept: 'application/json' },
    signal
  })
  if (!res.ok) throw new Error(`stats HTTP ${res.status}`)
  const data = (await res.json()) as Partial<PublicStats>
  return {
    downloads: Number(data.downloads) || 0,
    launches: Number(data.launches) || 0,
    updatedAt: typeof data.updatedAt === 'string' ? data.updatedAt : undefined
  }
}

/** Fire-and-forget download increment (survives navigation via keepalive). */
export function trackDownload(): void {
  const url = `${apiBase()}/v1/stats/download`
  const body = JSON.stringify({})
  try {
    if (typeof navigator !== 'undefined' && typeof navigator.sendBeacon === 'function') {
      const blob = new Blob([body], { type: 'application/json' })
      if (navigator.sendBeacon(url, blob)) return
    }
  } catch {
    /* fall through */
  }
  void fetch(url, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body,
    keepalive: true,
    mode: 'cors'
  }).catch(() => {})
}
