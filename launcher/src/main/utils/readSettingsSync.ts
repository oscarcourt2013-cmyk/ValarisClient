import { existsSync, readFileSync } from 'fs'
import { join } from 'path'
import { app } from 'electron'

/** Reads settings.json synchronously — used before app.whenReady() for hardware acceleration. */
export function readSettingsSync(): { hardwareAccel: boolean } {
  try {
    const path = join(app.getPath('userData'), 'settings.json')
    if (!existsSync(path)) {
      return { hardwareAccel: true }
    }
    const raw = JSON.parse(readFileSync(path, 'utf8')) as { hardwareAccel?: boolean }
    return { hardwareAccel: raw.hardwareAccel !== false }
  } catch {
    return { hardwareAccel: true }
  }
}
