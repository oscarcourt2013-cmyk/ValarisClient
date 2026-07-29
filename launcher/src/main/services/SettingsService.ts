import { BrowserWindow, dialog } from 'electron'
import type { LauncherSettings } from '../storage/SettingsStore'
import { settingsStore } from '../storage/SettingsStore'
import { getDefaultInstancesRoot } from '../minecraft/paths'
import { instanceService } from './InstanceService'
import { launcherBridgeService } from './LauncherBridgeService'
import { performanceService } from './PerformanceService'
import { launcherDiscordService } from './LauncherDiscordService'
import { validateJavaExecutable, type JavaInstallation } from '../minecraft/JavaService'

export type SettingsView = LauncherSettings & { defaultInstancesRoot: string }

export class SettingsService {
  async get(): Promise<SettingsView> {
    const settings = await settingsStore.load()
    return { ...settings, defaultInstancesRoot: getDefaultInstancesRoot() }
  }

  async update(partial: Partial<LauncherSettings>): Promise<{ settings: LauncherSettings; restartRequired?: boolean }> {
    const before = await settingsStore.load()
    const updated = await settingsStore.mutate((s) => {
      Object.assign(s, partial)
    })

    let restartRequired = false

    if (partial.discordRpc !== undefined) {
      await launcherDiscordService.setEnabled(partial.discordRpc)
    }

    if (partial.discordRpc !== undefined || partial.theme !== undefined) {
      const instances = await instanceService.list()
      await Promise.all(
        instances.filter((inst) => inst.includePrimeMod).map((inst) => launcherBridgeService.syncToInstance(inst.id))
      )
    }

    if (partial.performancePreset !== undefined) {
      await performanceService.applyPreset(partial.performancePreset)
    }

    if (partial.hardwareAccel !== undefined && partial.hardwareAccel !== before.hardwareAccel) {
      restartRequired = true
    }

    if (partial.developerMode !== undefined) {
      const win = BrowserWindow.getAllWindows()[0]
      if (win) {
        if (partial.developerMode) {
          win.webContents.openDevTools({ mode: 'detach' })
        } else if (win.webContents.isDevToolsOpened()) {
          win.webContents.closeDevTools()
        }
      }
    }

    return { settings: updated, restartRequired }
  }

  async browseJavaExecutable(): Promise<{ ok: boolean; install?: JavaInstallation; error?: string }> {
    const { canceled, filePaths } = await dialog.showOpenDialog({
      title: 'Select Java executable',
      properties: ['openFile'],
      filters:
        process.platform === 'win32'
          ? [{ name: 'Java executable', extensions: ['exe'] }]
          : [{ name: 'Java executable', extensions: ['*'] }]
    })

    if (canceled || !filePaths[0]) {
      return { ok: false, error: 'Cancelled.' }
    }

    const install = await validateJavaExecutable(filePaths[0])
    if (!install) {
      return { ok: false, error: 'Selected file is not a valid Java 21+ executable.' }
    }

    return { ok: true, install }
  }

  async addCustomJavaPath(javaPath: string): Promise<{ ok: boolean; install?: JavaInstallation; error?: string }> {
    const install = await validateJavaExecutable(javaPath)
    if (!install) {
      return { ok: false, error: 'Selected file is not a valid Java 21+ executable.' }
    }

    await settingsStore.mutate((settings) => {
      const paths = settings.customJavaPaths ?? []
      if (!paths.includes(install.path)) {
        settings.customJavaPaths = [...paths, install.path]
      }
      settings.defaultJavaPath = install.path
    })

    return { ok: true, install }
  }

  async browseWallpaper(): Promise<{ ok: boolean; path?: string; error?: string }> {
    const { canceled, filePaths } = await dialog.showOpenDialog({
      title: 'Select wallpaper',
      properties: ['openFile'],
      filters: [{ name: 'Images', extensions: ['png', 'jpg', 'jpeg', 'webp'] }]
    })
    if (canceled || !filePaths[0]) {
      return { ok: false, error: 'Cancelled.' }
    }
    await this.update({ wallpaperPath: filePaths[0] })
    return { ok: true, path: filePaths[0] }
  }

  async clearWallpaper(): Promise<{ settings: LauncherSettings; restartRequired?: boolean }> {
    return this.update({ wallpaperPath: null })
  }

  async browseInstancesRoot(): Promise<{ ok: boolean; path?: string; error?: string }> {
    const { canceled, filePaths } = await dialog.showOpenDialog({
      title: 'Select instances folder',
      properties: ['openDirectory', 'createDirectory']
    })
    if (canceled || !filePaths[0]) {
      return { ok: false, error: 'Cancelled.' }
    }
    await this.update({ instancesRoot: filePaths[0] })
    return { ok: true, path: filePaths[0] }
  }

  async getWallpaperDataUrl(): Promise<string | null> {
    const settings = await settingsStore.load()
    const path = settings.wallpaperPath
    if (!path) return null
    try {
      const { readFile } = await import('fs/promises')
      const { extname } = await import('path')
      const buf = await readFile(path)
      const ext = extname(path).toLowerCase().replace('.', '')
      const mime =
        ext === 'jpg' || ext === 'jpeg'
          ? 'image/jpeg'
          : ext === 'webp'
            ? 'image/webp'
            : 'image/png'
      return `data:${mime};base64,${buf.toString('base64')}`
    } catch {
      return null
    }
  }
}

export const settingsService = new SettingsService()
