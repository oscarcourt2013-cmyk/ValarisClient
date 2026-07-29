import { appendFile, mkdir } from 'fs/promises'
import { join } from 'path'
import { app } from 'electron'
import { settingsStore } from '../storage/SettingsStore'

/** Opt-in local analytics log — no network, stored in userData when enabled. */
export class AnalyticsService {
  private get logPath(): string {
    return join(app.getPath('userData'), 'analytics.log')
  }

  async track(event: string, detail?: Record<string, string | number | boolean>): Promise<void> {
    const settings = await settingsStore.load()
    if (!settings.analytics) {
      return
    }

    const line = JSON.stringify({
      ts: new Date().toISOString(),
      event,
      ...detail
    })

    await mkdir(app.getPath('userData'), { recursive: true })
    await appendFile(this.logPath, `${line}\n`, 'utf8')
  }
}

export const analyticsService = new AnalyticsService()
