import { accountStore } from '../storage/AccountStore'
import { instanceService } from './InstanceService'
import type { LauncherProfile } from '../../shared/types'

/** Profile management tied to account store. */
export class ProfileService {
  async getProfiles(): Promise<LauncherProfile[]> {
    const db = await accountStore.getDb()
    return db.profiles
  }

  async getActiveProfile(): Promise<LauncherProfile> {
    const db = await accountStore.getDb()
    const profile = db.profiles.find((p) => p.id === db.activeProfileId) ?? db.profiles[0]
    if (!profile) {
      return {
        id: 'default',
        name: 'Default',
        minecraftAccountId: db.activeAccountId ?? '',
        instanceId: 'prime-fabric',
        playTimeMinutes: 0
      }
    }
    return {
      ...profile,
      minecraftAccountId: profile.minecraftAccountId || db.activeAccountId || ''
    }
  }

  async setActiveInstance(instanceId: string): Promise<{ ok: boolean; error?: string }> {
    const instance = await instanceService.getById(instanceId)
    if (!instance) {
      return { ok: false, error: 'Instance not found.' }
    }

    await accountStore.mutate((db) => {
      const profile = db.profiles.find((p) => p.id === db.activeProfileId) ?? db.profiles[0]
      if (profile) {
        profile.instanceId = instanceId
      }
    })

    return { ok: true }
  }
}

export const profileService = new ProfileService()
