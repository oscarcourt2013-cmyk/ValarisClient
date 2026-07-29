import { BrowserWindow } from 'electron'
import { IPC } from '../../shared/ipc'
import type { LaunchProgressDto } from '../../shared/ipc'
import { downloadService } from '../services/DownloadService'
import { launchLogService } from '../services/LaunchLogService'
import { launcherDiscordService } from '../services/LauncherDiscordService'
import { socialService } from '../services/SocialService'

let launchServerAddress: string | null = null

/** Remember the server address for in-game presence while a launch is active. */
export function setLaunchServerAddress(serverAddress?: string | null): void {
  launchServerAddress = serverAddress?.trim() || null
}

function syncPresenceFromLaunchProgress(payload: LaunchProgressDto): void {
  if (payload.phase === 'running') {
    socialService.setPresence('in-game', 'Playing', launchServerAddress)
    return
  }
  if (payload.phase === 'stopped' || payload.phase === 'crashed') {
    socialService.setPresence('online', 'In launcher')
    launchServerAddress = null
  }
}

export function emitLaunchProgress(payload: LaunchProgressDto): void {
  downloadService.onLaunchProgress(payload)
  launchLogService.fromProgress(payload)
  launcherDiscordService.onLaunchProgress(payload)
  syncPresenceFromLaunchProgress(payload)

  for (const win of BrowserWindow.getAllWindows()) {
    win.webContents.send(IPC.LAUNCH_PROGRESS, payload)
  }
}

export function emitLaunchError(message: string, err?: unknown): void {
  emitLaunchProgress({ phase: 'error', detail: message })
  if (err instanceof Error && err.stack) {
    launchLogService.append('error', err.stack, 'log')
  }
}
