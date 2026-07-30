import { randomUUID } from 'crypto'
import type { StoreItem } from '../../shared/content-types'
import { STORE_CATALOG } from '../../shared/ecosystem-catalog'
import { ecosystemStore, type StorePurchaseRecord } from '../storage/EcosystemStore'
import { settingsStore } from '../storage/SettingsStore'

/**
 * Local-only store. Balance, ownership, history and promo redemptions all live in
 * {@code ecosystem.json} on disk; there is no remote catalog or sync.
 */

/** Built-in promo codes -> bonus Coins. */
const PROMO_CODES: Record<string, { coins: number; label: string }> = {
  PRIME2026: { coins: 100, label: 'Valeris 2026' },
  WELCOME: { coins: 50, label: 'Welcome bonus' },
  ELYSIA: { coins: 75, label: 'Elysia partner' }
}

export class StoreService {
  async getCatalog(): Promise<StoreItem[]> {
    const db = await ecosystemStore.load()
    return STORE_CATALOG.map((item) => ({
      ...item,
      owned: db.ownedStoreItems.includes(item.id)
    }))
  }

  async getBalance(): Promise<number> {
    const db = await ecosystemStore.load()
    return db.primeCoins
  }

  async getHistory(): Promise<StorePurchaseRecord[]> {
    const db = await ecosystemStore.load()
    return [...(db.storeHistory ?? [])].sort((a, b) => b.purchasedAt.localeCompare(a.purchasedAt))
  }

  async listPromos(): Promise<{ code: string; label: string; coins: number; redeemed: boolean }[]> {
    const db = await ecosystemStore.load()
    const redeemed = new Set(db.redeemedPromos ?? [])
    return Object.entries(PROMO_CODES).map(([code, meta]) => ({
      code,
      label: meta.label,
      coins: meta.coins,
      redeemed: redeemed.has(code.toUpperCase())
    }))
  }

  async redeemPromo(codeRaw: string): Promise<{ ok: boolean; error?: string; coins?: number }> {
    const code = codeRaw.trim().toUpperCase()
    const promo = PROMO_CODES[code]
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

  async purchase(itemId: string): Promise<{ ok: boolean; error?: string }> {
    const item = STORE_CATALOG.find((i) => i.id === itemId)
    if (!item) {
      return { ok: false, error: 'Item not found.' }
    }

    const db = await ecosystemStore.load()
    if (db.ownedStoreItems.includes(itemId)) {
      return { ok: false, error: 'Already owned.' }
    }

    if (item.price > 0 && db.primeCoins < item.price) {
      return { ok: false, error: `Need ${item.price} Coins (you have ${db.primeCoins}).` }
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

  async grantLaunchReward(): Promise<void> {
    await ecosystemStore.mutate((db) => {
      db.primeCoins += 10
    })
  }

  private async applyNebulaUnlock(itemId: string): Promise<void> {
    if (itemId !== 'bg-nebula') return
    await settingsStore.mutate((s) => {
      s.backgroundNebula = true
    })
  }
}

export const storeService = new StoreService()
