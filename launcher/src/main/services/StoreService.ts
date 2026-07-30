import { randomUUID } from 'crypto'
import type { StoreItem } from '../../shared/content-types'
import { DEFAULT_OWNED_STORE, STORE_CATALOG } from '../../shared/ecosystem-catalog'
import { ecosystemStore, type StorePurchaseRecord } from '../storage/EcosystemStore'
import { settingsStore } from '../storage/SettingsStore'
import {
  CLOUD_PROMO_CODES,
  CloudRequestError,
  CloudUnavailableError,
  storeCloudClient,
  type CloudStoreHistoryEntry
} from './StoreCloudClient'

/** Built-in local promo codes → bonus Prime Coins (offline fallback). */
const LOCAL_PROMO_CODES: Record<string, { coins: number; label: string }> = {
  PRIME2026: { coins: 100, label: 'Valeris 2026' },
  WELCOME: { coins: 50, label: 'Welcome bonus' },
  ELYSIA: { coins: 75, label: 'Elysia partner' }
}

export type StoreSyncMode = 'synced' | 'local'

export class StoreService {
  private syncMode: StoreSyncMode = 'local'

  getSyncMode(): StoreSyncMode {
    return this.syncMode
  }

  async getCatalog(): Promise<StoreItem[]> {
    try {
      const cloud = await storeCloudClient.getCatalog()
      const history = await storeCloudClient.getHistory().catch(() => [] as CloudStoreHistoryEntry[])
      await this.applyCloudSnapshot({
        balance: cloud.balance,
        owned: cloud.items.filter((i) => i.owned).map((i) => i.id),
        history
      })
      this.syncMode = 'synced'
      return cloud.items.map((item) => ({
        ...item,
        category: item.category,
        owned: !!item.owned
      }))
    } catch {
      this.syncMode = 'local'
      return this.getCatalogLocal()
    }
  }

  async getBalance(): Promise<number> {
    try {
      const balance = await storeCloudClient.getBalance()
      await ecosystemStore.mutate((d) => {
        d.primeCoins = balance
      })
      this.syncMode = 'synced'
      return balance
    } catch {
      const db = await ecosystemStore.load()
      return db.primeCoins
    }
  }

  async getHistory(): Promise<StorePurchaseRecord[]> {
    try {
      const history = await storeCloudClient.getHistory()
      const mapped = this.mapCloudHistory(history)
      await ecosystemStore.mutate((d) => {
        d.storeHistory = mapped
        d.redeemedPromos = this.redeemedCodesFromHistory(history)
      })
      this.syncMode = 'synced'
      return mapped
    } catch {
      const db = await ecosystemStore.load()
      return [...(db.storeHistory ?? [])].sort((a, b) => b.purchasedAt.localeCompare(a.purchasedAt))
    }
  }

  async listPromos(): Promise<{ code: string; label: string; coins: number; redeemed: boolean }[]> {
    try {
      const history = await storeCloudClient.getHistory()
      const redeemed = new Set(this.redeemedCodesFromHistory(history))
      await ecosystemStore.mutate((d) => {
        d.redeemedPromos = [...redeemed]
      })
      this.syncMode = 'synced'
      return Object.entries(CLOUD_PROMO_CODES).map(([code, meta]) => ({
        code,
        label: meta.label,
        coins: meta.coins,
        redeemed: redeemed.has(code)
      }))
    } catch {
      const db = await ecosystemStore.load()
      const redeemed = new Set(db.redeemedPromos ?? [])
      return Object.entries(LOCAL_PROMO_CODES).map(([code, meta]) => ({
        code,
        label: meta.label,
        coins: meta.coins,
        redeemed: redeemed.has(code.toUpperCase())
      }))
    }
  }

  async redeemPromo(codeRaw: string): Promise<{ ok: boolean; error?: string; coins?: number }> {
    try {
      const result = await storeCloudClient.redeem(codeRaw)
      await ecosystemStore.mutate((d) => {
        d.primeCoins = result.balance
        const code = result.code.toUpperCase()
        const prev = d.redeemedPromos ?? []
        if (!prev.includes(code)) {
          d.redeemedPromos = [...prev, code]
        }
      })
      this.syncMode = 'synced'
      return { ok: true, coins: result.coins }
    } catch (err) {
      if (err instanceof CloudRequestError) {
        return { ok: false, error: err.message }
      }
      if (!(err instanceof CloudUnavailableError)) {
        // Unexpected — still try local
      }
      return this.redeemPromoLocal(codeRaw)
    }
  }

  async purchase(itemId: string): Promise<{ ok: boolean; error?: string }> {
    try {
      const result = await storeCloudClient.purchase(itemId)
      const item = STORE_CATALOG.find((i) => i.id === itemId)
      await ecosystemStore.mutate((d) => {
        d.primeCoins = result.balance
        d.ownedStoreItems = [...new Set([...DEFAULT_OWNED_STORE, ...result.owned])]
        const already = (d.storeHistory ?? []).some((h) => h.itemId === itemId)
        if (!already && item) {
          d.storeHistory = [
            ...(d.storeHistory ?? []),
            {
              id: randomUUID(),
              itemId: item.id,
              itemName: item.name,
              price: result.price,
              purchasedAt: new Date().toISOString()
            }
          ]
        }
      })
      await this.applyNebulaUnlock(itemId)
      this.syncMode = 'synced'
      return { ok: true }
    } catch (err) {
      if (err instanceof CloudRequestError) {
        return { ok: false, error: err.message }
      }
      return this.purchaseLocal(itemId)
    }
  }

  async grantLaunchReward(): Promise<void> {
    await ecosystemStore.mutate((db) => {
      db.primeCoins += 10
    })
  }

  private async getCatalogLocal(): Promise<StoreItem[]> {
    const db = await ecosystemStore.load()
    return STORE_CATALOG.map((item) => ({
      ...item,
      owned: db.ownedStoreItems.includes(item.id)
    }))
  }

  private async redeemPromoLocal(
    codeRaw: string
  ): Promise<{ ok: boolean; error?: string; coins?: number }> {
    this.syncMode = 'local'
    const code = codeRaw.trim().toUpperCase()
    const promo = LOCAL_PROMO_CODES[code]
    if (!promo) {
      return { ok: false, error: 'Invalid promo code.' }
    }
    const db = await ecosystemStore.load()
    if ((db.redeemedPromos ?? []).includes(code)) {
      return { ok: false, error: 'Promo already redeemed.' }
    }
    await ecosystemStore.mutate((d) => {
      d.primeCoins += promo.coins
      d.redeemedPromos = [...(d.redeemedPromos ?? []), code]
    })
    return { ok: true, coins: promo.coins }
  }

  private async purchaseLocal(itemId: string): Promise<{ ok: boolean; error?: string }> {
    this.syncMode = 'local'
    const item = STORE_CATALOG.find((i) => i.id === itemId)
    if (!item) {
      return { ok: false, error: 'Item not found.' }
    }

    const db = await ecosystemStore.load()
    if (db.ownedStoreItems.includes(itemId)) {
      return { ok: false, error: 'Already owned.' }
    }

    if (item.price > 0 && db.primeCoins < item.price) {
      return { ok: false, error: `Need ${item.price} Prime Coins (you have ${db.primeCoins}).` }
    }

    await ecosystemStore.mutate((d) => {
      if (item.price > 0) {
        d.primeCoins -= item.price
      }
      d.ownedStoreItems.push(itemId)
      d.storeHistory = [
        ...(d.storeHistory ?? []),
        {
          id: randomUUID(),
          itemId: item.id,
          itemName: item.name,
          price: item.price,
          purchasedAt: new Date().toISOString()
        }
      ]
    })

    await this.applyNebulaUnlock(itemId)
    return { ok: true }
  }

  private async applyNebulaUnlock(itemId: string): Promise<void> {
    if (itemId !== 'bg-nebula') return
    await settingsStore.mutate((s) => {
      s.backgroundNebula = true
    })
  }

  private async applyCloudSnapshot(snapshot: {
    balance: number
    owned: string[]
    history: CloudStoreHistoryEntry[]
  }): Promise<void> {
    const mapped = this.mapCloudHistory(snapshot.history)
    const redeemed = this.redeemedCodesFromHistory(snapshot.history)
    await ecosystemStore.mutate((d) => {
      d.primeCoins = snapshot.balance
      d.ownedStoreItems = [...new Set([...DEFAULT_OWNED_STORE, ...snapshot.owned])]
      d.storeHistory = mapped
      d.redeemedPromos = redeemed
    })
  }

  private mapCloudHistory(history: CloudStoreHistoryEntry[]): StorePurchaseRecord[] {
    return history
      .filter((h) => h.kind === 'purchase' && h.itemId)
      .map((h) => {
        const item = STORE_CATALOG.find((i) => i.id === h.itemId)
        return {
          id: h.id,
          itemId: h.itemId!,
          itemName: item?.name ?? h.itemId!,
          price: Math.abs(Number(h.amount) || item?.price || 0),
          purchasedAt: h.createdAt
        }
      })
      .sort((a, b) => b.purchasedAt.localeCompare(a.purchasedAt))
  }

  private redeemedCodesFromHistory(history: CloudStoreHistoryEntry[]): string[] {
    return [
      ...new Set(
        history
          .filter((h) => h.kind === 'redeem' && h.code)
          .map((h) => String(h.code).toUpperCase())
      )
    ]
  }
}

export const storeService = new StoreService()
