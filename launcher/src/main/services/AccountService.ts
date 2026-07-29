import type { MinecraftAccount, PrimeAccount } from '../../shared/types'
import { microsoftAuth } from '../auth/MicrosoftAuth'
import { formatLaunchError } from '../minecraft/formatLaunchError'
import { newAccountId, offlineUuid, skinUrlForUuid, validateUsername } from '../auth/offline'
import type { AuthResult, LaunchResult, StoredMinecraftAccount, SyncResult } from '../storage/account-types'
import { accountStore } from '../storage/AccountStore'
import { instanceService } from './InstanceService'

function toPublic(account: StoredMinecraftAccount): MinecraftAccount {
  return {
    id: account.id,
    type: account.type,
    username: account.username,
    uuid: account.uuid,
    skinUrl: account.skinUrl ?? skinUrlForUuid(account.uuid),
    capeUrl: account.capeUrl
  }
}

function syncPrimeFromAccount(db: Awaited<ReturnType<typeof accountStore.getDb>>, account: StoredMinecraftAccount): void {
  db.primeAccount = {
    ...db.primeAccount,
    username: account.username,
    tier: account.type === 'microsoft' ? 'prime' : 'free',
    level: Math.max(1, Math.floor((db.profiles[0]?.playTimeMinutes ?? 0) / 60) + 1)
  }
}

/** Prime + Minecraft account management — Phase 3. */
export class AccountService {
  async getPrimeAccount(): Promise<PrimeAccount> {
    const db = await accountStore.getDb()
    return db.primeAccount
  }

  async getMinecraftAccounts(): Promise<MinecraftAccount[]> {
    const db = await accountStore.getDb()
    return db.accounts.map(toPublic)
  }

  async getActiveAccount(): Promise<MinecraftAccount | null> {
    const db = await accountStore.getDb()
    if (!db.activeAccountId) {
      return null
    }
    const found = db.accounts.find((a) => a.id === db.activeAccountId)
    return found ? toPublic(found) : null
  }

  async setActiveAccount(accountId: string): Promise<MinecraftAccount | null> {
    return accountStore.mutate((db) => {
      const account = db.accounts.find((a) => a.id === accountId)
      if (!account) {
        throw new Error('Account not found.')
      }
      db.activeAccountId = accountId
      account.lastUsedAt = new Date().toISOString()
      syncPrimeFromAccount(db, account)
      const profile = db.profiles.find((p) => p.id === db.activeProfileId)
      if (profile) {
        profile.minecraftAccountId = accountId
      }
    }).then(async () => this.getActiveAccount())
  }

  async loginMicrosoft(): Promise<AuthResult> {
    try {
      const stored = await microsoftAuth.login()
      await accountStore.mutate((db) => {
        db.accounts.push(stored)
        db.activeAccountId = stored.id
        syncPrimeFromAccount(db, stored)
        const profile = db.profiles.find((p) => p.id === db.activeProfileId)
        if (profile) {
          profile.minecraftAccountId = stored.id
        }
      })
      return { ok: true, accountId: stored.id }
    } catch (err) {
      const message = err instanceof Error ? err.message : 'Microsoft login failed.'
      return { ok: false, error: message }
    }
  }

  async addOffline(username: string): Promise<AuthResult> {
    const validation = validateUsername(username)
    if (validation) {
      return { ok: false, error: validation }
    }
    const trimmed = username.trim()
    const db = await accountStore.getDb()
    if (db.accounts.some((a) => a.username.toLowerCase() === trimmed.toLowerCase())) {
      return { ok: false, error: 'An account with this username already exists.' }
    }

    const uuid = offlineUuid(trimmed)
    const stored: StoredMinecraftAccount = {
      id: newAccountId(),
      type: 'offline',
      username: trimmed,
      uuid,
      skinUrl: skinUrlForUuid(uuid),
      addedAt: new Date().toISOString(),
      lastUsedAt: new Date().toISOString()
    }

    await accountStore.mutate((db) => {
      db.accounts.push(stored)
      db.activeAccountId = stored.id
      syncPrimeFromAccount(db, stored)
      const profile = db.profiles.find((p) => p.id === db.activeProfileId)
      if (profile) {
        profile.minecraftAccountId = stored.id
      }
    })
    return { ok: true, accountId: stored.id }
  }

  async removeAccount(accountId: string): Promise<AuthResult> {
    const db = await accountStore.getDb()
    if (db.accounts.length <= 1 && db.accounts[0]?.id === accountId) {
      // allow removing last account
    }
    const exists = db.accounts.some((a) => a.id === accountId)
    if (!exists) {
      return { ok: false, error: 'Account not found.' }
    }

    await accountStore.mutate((db) => {
      db.accounts = db.accounts.filter((a) => a.id !== accountId)
      if (db.activeAccountId === accountId) {
        db.activeAccountId = db.accounts[0]?.id ?? null
        if (db.activeAccountId) {
          const next = db.accounts.find((a) => a.id === db.activeAccountId)!
          syncPrimeFromAccount(db, next)
        } else {
          db.primeAccount = { ...db.primeAccount, username: 'Guest', tier: 'free' }
        }
      }
    })
    microsoftAuth.clearLaunchSession(accountId)
    return { ok: true }
  }

  async refreshMicrosoftAccount(accountId: string): Promise<AuthResult> {
    const db = await accountStore.getDb()
    const account = db.accounts.find((a) => a.id === accountId && a.type === 'microsoft')
    if (!account) {
      return { ok: false, error: 'Microsoft account not found.' }
    }
    try {
      const refreshed = await microsoftAuth.refresh(account)
      await accountStore.mutate((db) => {
        const idx = db.accounts.findIndex((a) => a.id === accountId)
        if (idx >= 0) {
          db.accounts[idx] = refreshed
          syncPrimeFromAccount(db, refreshed)
        }
      })
      return { ok: true, accountId }
    } catch (err) {
      const message = err instanceof Error ? err.message : 'Token refresh failed.'
      return { ok: false, error: message }
    }
  }

  async syncPrimeCloud(): Promise<SyncResult> {
    const db = await accountStore.getDb()
    const active = db.activeAccountId
      ? db.accounts.find((a) => a.id === db.activeAccountId)
      : undefined

    if (!active) {
      return { ok: false, lastSync: '', message: 'Sign in to sync your Prime profile.' }
    }

    if (active.type === 'microsoft' && active.msRefreshToken) {
      await this.refreshMicrosoftAccount(active.id)
    }

    const now = new Date().toISOString()
    await accountStore.mutate((db) => {
      db.primeAccount = {
        ...db.primeAccount,
        username: active.username,
        tier: active.type === 'microsoft' ? 'prime' : 'free'
      }
    })

    return {
      ok: true,
      lastSync: now,
      message: 'Prime profile synced locally (no cloud server — data stays on this PC).'
    }
  }

  async prepareLaunch(instanceId: string): Promise<LaunchResult> {
    const instance = await instanceService.getStoredById(instanceId)
    if (!instance) {
      return { ok: false, message: 'Instance not found.', error: 'NO_INSTANCE' }
    }

    const db = await accountStore.getDb()
    const accountId = db.activeAccountId
    if (!accountId) {
      return { ok: false, message: 'No account selected.', error: 'NO_ACCOUNT' }
    }

    let account = db.accounts.find((a) => a.id === accountId)
    if (!account) {
      return { ok: false, message: 'Active account missing.', error: 'NO_ACCOUNT' }
    }

    if (account.type === 'microsoft' && account.msRefreshToken) {
      try {
        account = await microsoftAuth.refresh(account)
        await accountStore.mutate((db) => {
          const idx = db.accounts.findIndex((a) => a.id === accountId)
          if (idx >= 0) {
            db.accounts[idx] = account!
          }
          account!.lastUsedAt = new Date().toISOString()
        })
      } catch (err) {
        return {
          ok: false,
          message: formatLaunchError(err),
          error: 'TOKEN_EXPIRED'
        }
      }
    }

    await accountStore.mutate((db) => {
      const profile = db.profiles.find((p) => p.id === db.activeProfileId)
      if (profile) {
        profile.lastPlayed = new Date().toISOString()
        profile.instanceId = instanceId
        profile.playTimeMinutes += 1
      }
      syncPrimeFromAccount(db, account!)
    })

    return {
      ok: true,
      message: `Launching as ${account.username}…`
    }
  }

  /** Internal — launch pipeline uses this in Phase 4. */
  async getStoredActiveAccount(): Promise<StoredMinecraftAccount | null> {
    const db = await accountStore.getDb()
    if (!db.activeAccountId) {
      return null
    }
    return db.accounts.find((a) => a.id === db.activeAccountId) ?? null
  }
}

export const accountService = new AccountService()
