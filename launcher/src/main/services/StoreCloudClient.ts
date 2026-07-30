import type { StoreItem } from '../../shared/content-types'
import { socialService } from './SocialService'

const REQUEST_TIMEOUT_MS = 8_000

/** Cloud promo codes — mirrors backend/lib/storeCatalog.js. */
export const CLOUD_PROMO_CODES: Record<string, { coins: number; label: string }> = {
  WELCOME100: { coins: 100, label: 'Welcome bonus' },
  PRIME500: { coins: 500, label: 'Valeris starter pack' },
  ELYSIA250: { coins: 250, label: 'Elysia promo' },
  FOUNDER1000: { coins: 1000, label: 'Founder gift' }
}

export interface CloudStoreHistoryEntry {
  id: string
  kind: string
  itemId: string | null
  amount: number
  code: string | null
  createdAt: string
}

export interface CloudCatalogResponse {
  items: StoreItem[]
  balance: number
}

export interface CloudPurchaseResult {
  itemId: string
  price: number
  balance: number
  owned: string[]
}

export interface CloudRedeemResult {
  code: string
  coins: number
  label: string
  balance: number
}

/** Thrown when the cloud store cannot be reached (offline / no session / timeout). */
export class CloudUnavailableError extends Error {
  constructor(message = 'Cloud store unavailable') {
    super(message)
    this.name = 'CloudUnavailableError'
  }
}

/** Thrown when the cloud rejects a request with a business error (keep offline fallback off). */
export class CloudRequestError extends Error {
  constructor(message: string) {
    super(message)
    this.name = 'CloudRequestError'
  }
}

function withTimeout(init?: RequestInit): RequestInit {
  const signal =
    typeof AbortSignal !== 'undefined' && 'timeout' in AbortSignal
      ? AbortSignal.timeout(REQUEST_TIMEOUT_MS)
      : undefined
  return signal ? { ...init, signal } : { ...init }
}

async function readError(res: Response): Promise<string> {
  const data = (await res.json().catch(() => null)) as { error?: string } | null
  return data?.error || `Request failed (${res.status})`
}

/**
 * Thin client for GET/POST /v1/store/* using the social Bearer session
 * and the same API base as friends (DEFAULT_API_BASE / PRIME_API_BASE).
 */
export class StoreCloudClient {
  async getCatalog(): Promise<CloudCatalogResponse> {
    const res = await this.fetch('/v1/store/catalog')
    if (!res.ok) throw new CloudUnavailableError(await readError(res))
    const data = (await res.json()) as CloudCatalogResponse
    return {
      items: Array.isArray(data.items) ? data.items : [],
      balance: Number(data.balance) || 0
    }
  }

  async getBalance(): Promise<number> {
    const res = await this.fetch('/v1/store/balance')
    if (!res.ok) throw new CloudUnavailableError(await readError(res))
    const data = (await res.json()) as { balance?: number }
    return Number(data.balance) || 0
  }

  async getHistory(limit = 50): Promise<CloudStoreHistoryEntry[]> {
    const res = await this.fetch(`/v1/store/history?limit=${limit}`)
    if (!res.ok) throw new CloudUnavailableError(await readError(res))
    const data = (await res.json()) as { history?: CloudStoreHistoryEntry[] }
    return Array.isArray(data.history) ? data.history : []
  }

  async purchase(itemId: string): Promise<CloudPurchaseResult> {
    const res = await this.fetch('/v1/store/purchase', {
      method: 'POST',
      body: JSON.stringify({ itemId })
    })
    if (res.status >= 500 || res.status === 0) {
      throw new CloudUnavailableError(await readError(res))
    }
    if (!res.ok) {
      throw new CloudRequestError(await readError(res))
    }
    const data = (await res.json()) as CloudPurchaseResult & { ok?: boolean }
    return {
      itemId: data.itemId,
      price: Number(data.price) || 0,
      balance: Number(data.balance) || 0,
      owned: Array.isArray(data.owned) ? data.owned : []
    }
  }

  async redeem(code: string): Promise<CloudRedeemResult> {
    const res = await this.fetch('/v1/store/redeem', {
      method: 'POST',
      body: JSON.stringify({ code })
    })
    if (res.status >= 500 || res.status === 0) {
      throw new CloudUnavailableError(await readError(res))
    }
    if (!res.ok) {
      throw new CloudRequestError(await readError(res))
    }
    const data = (await res.json()) as CloudRedeemResult & { ok?: boolean }
    return {
      code: data.code,
      coins: Number(data.coins) || 0,
      label: data.label || data.code,
      balance: Number(data.balance) || 0
    }
  }

  private async fetch(path: string, init?: RequestInit): Promise<Response> {
    try {
      return await socialService.apiFetch(path, withTimeout(init))
    } catch (err) {
      const message = err instanceof Error ? err.message : 'Cloud store unavailable'
      throw new CloudUnavailableError(message)
    }
  }
}

export const storeCloudClient = new StoreCloudClient()
