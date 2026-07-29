import { accountService } from './AccountService'
import { minecraftEngine } from '../minecraft/MinecraftEngine'
import { emitLaunchError, emitLaunchProgress, setLaunchServerAddress } from '../minecraft/launchProgress'
import { formatLaunchError } from '../minecraft/formatLaunchError'
import { launchLogService } from './LaunchLogService'
import { parseServerAddress } from './ServerService'
import { performanceService } from './PerformanceService'
import { launcherBridgeService } from './LauncherBridgeService'
import { analyticsService } from './AnalyticsService'
import { storeService } from './StoreService'
import { settingsStore } from '../storage/SettingsStore'
import type { LaunchResult } from '../storage/account-types'

export class LaunchService {
  async launch(instanceId: string, serverAddress?: string): Promise<LaunchResult> {
    if (minecraftEngine.isRunning()) {
      return { ok: false, message: 'Minecraft is already running.', error: 'ALREADY_RUNNING' }
    }

    const prep = await accountService.prepareLaunch(instanceId)
    if (!prep.ok) {
      return prep
    }

    try {
      setLaunchServerAddress(serverAddress)
      const settings = await settingsStore.load()
      launchLogService.append('info', `Launching instance ${instanceId}…`, 'start')

      await performanceService.applyPreset(settings.performancePreset, instanceId)
      const bridge = await launcherBridgeService.syncToInstance(instanceId)
      if (!bridge.ok) {
        launchLogService.append('warn', bridge.error ?? 'Bridge sync failed.', 'start')
      }

      const joinServer = serverAddress ? parseServerAddress(serverAddress) : undefined
      const result = await minecraftEngine.launchInstance(instanceId, joinServer)

      await storeService.grantLaunchReward()
      await analyticsService.track('game_launch', { instanceId, username: result.username })

      if (settings.closeOnLaunch) {
        const { BrowserWindow } = await import('electron')
        BrowserWindow.getAllWindows()[0]?.minimize()
      }

      const joinHint = serverAddress ? ` → ${serverAddress}` : ''
      return {
        ok: true,
        message: `Minecraft started as ${result.username}${joinHint}.`
      }
    } catch (err) {
      const message = formatLaunchError(err)
      emitLaunchError(message, err)
      launchLogService.append('error', message, 'error')
      await analyticsService.track('launch_failed', { instanceId, message })
      return { ok: false, message, error: 'LAUNCH_FAILED' }
    }
  }
}

export const launchService = new LaunchService()
