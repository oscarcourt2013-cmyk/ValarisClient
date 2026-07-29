import { app } from 'electron'
import { mkdir, readFile, writeFile } from 'fs/promises'
import { join } from 'path'
import type { LauncherProfile, PrimeAccount } from '../../shared/types'
import type { StoredMinecraftAccount } from './account-types'

export interface AccountDatabase {
  version: 1
  activeAccountId: string | null
  activeProfileId: string
  accounts: StoredMinecraftAccount[]
  primeAccount: PrimeAccount
  profiles: LauncherProfile[]
}

const DEFAULT_DB = (): AccountDatabase => ({
  version: 1,
  activeAccountId: null,
  activeProfileId: 'default',
  accounts: [],
  primeAccount: {
    id: 'prime-local',
    username: 'Guest',
    tier: 'free',
    level: 1,
    createdAt: new Date().toISOString()
  },
  profiles: [
    {
      id: 'default',
      name: 'Default',
      minecraftAccountId: '',
      instanceId: 'prime-fabric',
      playTimeMinutes: 0
    }
  ]
})

export class AccountStore {
  private db: AccountDatabase | null = null

  private get path(): string {
    return join(app.getPath('userData'), 'accounts.json')
  }

  async load(): Promise<AccountDatabase> {
    if (this.db) {
      return this.db
    }
    try {
      const raw = await readFile(this.path, 'utf8')
      this.db = JSON.parse(raw) as AccountDatabase
    } catch {
      this.db = DEFAULT_DB()
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

  async getDb(): Promise<AccountDatabase> {
    return this.load()
  }

  async mutate(fn: (db: AccountDatabase) => void): Promise<AccountDatabase> {
    const db = await this.load()
    fn(db)
    await this.save()
    return db
  }
}

export const accountStore = new AccountStore()
