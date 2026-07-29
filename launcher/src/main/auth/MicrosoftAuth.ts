import { randomUUID } from 'crypto'
import { Auth } from 'msmc'
import type { Authenticator } from 'minecraft-java-core'
import type { StoredMinecraftAccount } from '../storage/account-types'
import { newAccountId, skinUrlForUuid } from '../auth/offline'
import { formatLaunchError } from '../minecraft/formatLaunchError'

const SESSION_TTL_MS = 5 * 60 * 1000

interface CachedLaunchSession {
  authenticator: Authenticator
  fetchedAt: number
}

function normalizeAuthenticator(account: Authenticator): Authenticator {
  return {
    ...account,
    name: account.name || 'Player',
    uuid: String(account.uuid || '').replace(/-/g, ''),
    access_token: account.access_token || '',
    client_token: account.client_token || randomUUID(),
    user_properties:
      typeof account.user_properties === 'string'
        ? account.user_properties
        : JSON.stringify(account.user_properties ?? {}),
    meta: {
      ...account.meta,
      type: account.meta?.type || 'Xbox'
    }
  }
}

export class MicrosoftAuth {
  private launchSessions = new Map<string, CachedLaunchSession>()

  clearLaunchSession(accountId?: string): void {
    if (accountId) {
      this.launchSessions.delete(accountId)
      return
    }
    this.launchSessions.clear()
  }

  private cacheLaunchSession(accountId: string, authenticator: Authenticator): void {
    this.launchSessions.set(accountId, {
      authenticator,
      fetchedAt: Date.now()
    })
  }

  private getCachedLaunchSession(accountId: string): Authenticator | null {
    const cached = this.launchSessions.get(accountId)
    if (!cached) {
      return null
    }
    if (Date.now() - cached.fetchedAt > SESSION_TTL_MS) {
      this.launchSessions.delete(accountId)
      return null
    }
    return cached.authenticator
  }

  /** Returns a cached Minecraft access token when a recent launch/refresh session exists. */
  peekCachedAccessToken(accountId: string): string | null {
    const cached = this.getCachedLaunchSession(accountId)
    const token = cached?.access_token?.trim()
    return token || null
  }

  private buildAuthenticator(
    account: StoredMinecraftAccount,
    mc: { mclc: (refreshable?: boolean) => { name?: string; uuid: string; access_token: string; client_token?: string; user_properties?: unknown; meta?: { demo?: boolean } }; profile?: { name?: string } }
  ): Authenticator {
    const session = mc.mclc(true)
    return normalizeAuthenticator({
      name: session.name ?? mc.profile?.name ?? account.username,
      uuid: session.uuid,
      access_token: session.access_token,
      client_token: session.client_token ?? account.id,
      user_properties: JSON.stringify(session.user_properties ?? {}),
      meta: {
        type: 'Xbox',
        demo: session.meta?.demo
      }
    })
  }

  async getLaunchAuthenticator(account: StoredMinecraftAccount): Promise<Authenticator> {
    const cached = this.getCachedLaunchSession(account.id)
    if (cached) {
      return cached
    }

    const refreshed = await this.refresh(account)
    const fromRefresh = this.getCachedLaunchSession(refreshed.id)
    if (fromRefresh) {
      return fromRefresh
    }

    throw new Error('Microsoft session could not be prepared for launch.')
  }

  async login(): Promise<StoredMinecraftAccount> {
    const authManager = new Auth('select_account')
    const xbox = await authManager.launch('electron')
    const mc = await xbox.getMinecraft()
    const profile = mc.profile

    if (!profile?.id || !profile.name) {
      throw new Error('Microsoft account has no Minecraft profile. Purchase Minecraft or use offline mode.')
    }

    const activeSkin = profile.skins?.find((s) => s.state === 'ACTIVE')
    const activeCape = profile.capes?.find((c) => c.state === 'ACTIVE')

    const stored: StoredMinecraftAccount = {
      id: newAccountId(),
      type: 'microsoft',
      username: profile.name,
      uuid: profile.id,
      skinUrl: activeSkin?.url ?? skinUrlForUuid(profile.id),
      capeUrl: activeCape?.url,
      msRefreshToken: xbox.save(),
      msAuthProvider: 'live',
      addedAt: new Date().toISOString(),
      lastUsedAt: new Date().toISOString()
    }

    this.cacheLaunchSession(stored.id, this.buildAuthenticator(stored, mc))
    return stored
  }

  async refresh(account: StoredMinecraftAccount): Promise<StoredMinecraftAccount> {
    if (!account.msRefreshToken) {
      throw new Error('Missing Microsoft refresh token.')
    }

    try {
      const authManager = new Auth()
      const xbox = await authManager.refresh(account.msRefreshToken)
      const mc = await xbox.getMinecraft()
      const profile = mc.profile

      if (!profile?.id || !profile.name) {
        throw new Error('Could not refresh Minecraft profile.')
      }

      const activeSkin = profile.skins?.find((s) => s.state === 'ACTIVE')
      const activeCape = profile.capes?.find((c) => c.state === 'ACTIVE')

      const updated: StoredMinecraftAccount = {
        ...account,
        username: profile.name,
        uuid: profile.id,
        skinUrl: activeSkin?.url ?? skinUrlForUuid(profile.id),
        capeUrl: activeCape?.url,
        msRefreshToken: xbox.save(),
        msAuthProvider: 'live',
        lastUsedAt: new Date().toISOString()
      }

      this.cacheLaunchSession(updated.id, this.buildAuthenticator(updated, mc))
      return updated
    } catch (err) {
      this.clearLaunchSession(account.id)
      throw new Error(formatLaunchError(err))
    }
  }
}

export const microsoftAuth = new MicrosoftAuth()
