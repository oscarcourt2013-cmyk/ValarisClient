import { accountStore } from '../storage/AccountStore'
import { instanceStore } from '../storage/InstanceStore'
import { ecosystemStore } from '../storage/EcosystemStore'
import { settingsStore } from '../storage/SettingsStore'
import { downloadStore } from '../storage/DownloadStore'
import { updateService } from './UpdateService'

/** Real boot tasks — stores and optional update check. */
export class BootService {
  async initialize(): Promise<void> {
    await Promise.all([
      accountStore.load(),
      instanceStore.load(),
      ecosystemStore.load(),
      settingsStore.load(),
      downloadStore.load()
    ])

    const settings = await settingsStore.load()
    if (settings.autoUpdate) {
      try {
        await updateService.check()
      } catch {
        // offline — continue boot
      }
    }
  }
}

export const bootService = new BootService()
