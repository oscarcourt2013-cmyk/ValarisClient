import { accountService } from './AccountService'
import { settingsStore } from '../storage/SettingsStore'

/** Local profile sync — persists last sync time in settings. */
export class CloudService {
  async getSyncStatus(): Promise<{ lastSync: string | null; pending: boolean }> {
    const settings = await settingsStore.load()
    return { lastSync: settings.lastPrimeSync ?? null, pending: false }
  }

  async sync(): Promise<{ ok: boolean; lastSync: string; message: string }> {
    const result = await accountService.syncPrimeCloud()
    if (result.ok) {
      await settingsStore.mutate((s) => {
        s.lastPrimeSync = result.lastSync
      })
    }
    return result
  }
}

export const cloudService = new CloudService()
