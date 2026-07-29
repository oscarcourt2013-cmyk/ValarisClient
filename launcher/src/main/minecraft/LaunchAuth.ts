import type { Authenticator } from 'minecraft-java-core'
import type { StoredMinecraftAccount } from '../storage/account-types'
import { microsoftAuth } from '../auth/MicrosoftAuth'
import { offlineUuid } from '../auth/offline'

function offlineAuthenticator(account: StoredMinecraftAccount): Authenticator {
  const uuid = (account.uuid || offlineUuid(account.username)).replace(/-/g, '')
  return {
    name: account.username,
    uuid,
    access_token: 'prime_offline_access_token',
    client_token: account.id,
    user_properties: '{}',
    meta: {
      type: 'offline',
      online: false
    }
  }
}

export async function resolveLaunchAuthenticator(
  account: StoredMinecraftAccount
): Promise<Authenticator> {
  if (account.type === 'offline') {
    return offlineAuthenticator(account)
  }

  if (!account.msRefreshToken) {
    throw new Error('Microsoft session missing. Sign in again from Accounts.')
  }

  return microsoftAuth.getLaunchAuthenticator(account)
}
