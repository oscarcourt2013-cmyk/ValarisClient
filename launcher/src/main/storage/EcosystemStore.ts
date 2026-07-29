import { app } from 'electron'
import { mkdir, readFile, writeFile } from 'fs/promises'
import { join } from 'path'
import type { FriendEntry } from '../../shared/content-types'
import type { FavoriteServer } from '../../shared/types'
import { DEFAULT_EQUIPPED_COSMETICS, DEFAULT_OWNED_STORE } from '../../shared/ecosystem-catalog'

export interface StorePurchaseRecord {
  id: string
  itemId: string
  itemName: string
  price: number
  purchasedAt: string
}

export interface EcosystemDatabase {
  version: 1
  primeCoins: number
  ownedStoreItems: string[]
  equippedCosmetics: string[]
  friends: FriendEntry[]
  favoriteServers?: FavoriteServer[]
  /** Local purchase ledger for the boutique history tab. */
  storeHistory?: StorePurchaseRecord[]
  /** Active promo codes already redeemed. */
  redeemedPromos?: string[]
}

const DEFAULT_DB = (): EcosystemDatabase => ({
  version: 1,
  primeCoins: 500,
  ownedStoreItems: [...DEFAULT_OWNED_STORE],
  equippedCosmetics: [...DEFAULT_EQUIPPED_COSMETICS],
  friends: []
})

export class EcosystemStore {
  private db: EcosystemDatabase | null = null

  private get path(): string {
    return join(app.getPath('userData'), 'ecosystem.json')
  }

  async load(): Promise<EcosystemDatabase> {
    if (this.db) {
      return this.db
    }
    try {
      const raw = await readFile(this.path, 'utf8')
      this.db = JSON.parse(raw) as EcosystemDatabase
    } catch {
      this.db = DEFAULT_DB()
      await this.save()
      return this.db!
    }
    // Ensure free defaults (new themes, free cosmetics) stay owned for existing installs.
    const owned = new Set(this.db.ownedStoreItems ?? [])
    let changed = false
    for (const id of DEFAULT_OWNED_STORE) {
      if (!owned.has(id)) {
        owned.add(id)
        changed = true
      }
    }
    if (changed) {
      this.db.ownedStoreItems = [...owned]
      await this.save()
    }
    return this.db!
  }

  async save(): Promise<void> {
    if (!this.db) {
      return
    }
    await mkdir(app.getPath('userData'), { recursive: true })
    await writeFile(this.path, JSON.stringify(this.db, null, 2), 'utf8')
  }

  async mutate(fn: (db: EcosystemDatabase) => void): Promise<EcosystemDatabase> {
    const db = await this.load()
    fn(db)
    await this.save()
    return db
  }
}

export const ecosystemStore = new EcosystemStore()
