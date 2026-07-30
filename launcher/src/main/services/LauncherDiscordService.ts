import { app } from 'electron'
import { DiscordIpcClient } from '../discord/DiscordIpcClient'
import {
  DISCORD_APPLICATION_ID,
  type DiscordPresencePayload,
  presenceFingerprint
} from '../discord/types'
import type { LaunchProgressDto } from '../../shared/ipc'
import { settingsStore } from '../storage/SettingsStore'
import { accountService } from './AccountService'
import { minecraftEngine } from '../minecraft/MinecraftEngine'
import { launchLogService } from './LaunchLogService'

const APP_URL = `https://discord.com/applications/${DISCORD_APPLICATION_ID}`
const DISCORD_APP_URL = 'https://discord.com/app'

/** Launcher-side Discord Rich Presence (same app ID as the in-game mod). */
export class LauncherDiscordService {
  private readonly ipc = new DiscordIpcClient(DISCORD_APPLICATION_ID)
  private enabled = false
  private lastFingerprint = ''
  private sessionStart = Math.floor(Date.now() / 1000)
  private gameHandoff = false
  private retryTimer: ReturnType<typeof setInterval> | null = null

  async start(): Promise<void> {
    const settings = await settingsStore.load()
    this.enabled = settings.discordRpc
    if (!this.enabled) {
      return
    }
    this.startRetryLoop()
    await this.publishIdleWithDiagnostics('Launcher started')
  }

  async setEnabled(enabled: boolean): Promise<void> {
    this.enabled = enabled
    if (!enabled) {
      this.stopRetryLoop()
      this.lastFingerprint = ''
      this.ipc.shutdown()
      return
    }
    this.sessionStart = Math.floor(Date.now() / 1000)
    this.startRetryLoop()
    await this.publish(await this.idlePresence())
  }

  shutdown(): void {
    this.enabled = false
    this.lastFingerprint = ''
    this.stopRetryLoop()
    this.ipc.shutdown()
  }

  onLaunchProgress(payload: LaunchProgressDto): void {
    if (!this.enabled) {
      return
    }

    if (payload.phase === 'running') {
      this.gameHandoff = true
      this.lastFingerprint = ''
      void this.ipc.clearActivity()
      return
    }

    if (payload.phase === 'stopped' || payload.phase === 'crashed') {
      this.gameHandoff = false
      this.sessionStart = Math.floor(Date.now() / 1000)
      if (payload.phase === 'crashed') {
        void this.publish({
          details: 'Game crashed',
          state: payload.crash?.title ?? payload.detail ?? 'Minecraft crashed',
          largeImageKey: 'prime_logo',
          largeImageText: `ValerisClient Launcher v${app.getVersion()}`,
          smallImageKey: 'prime_logo',
          smallImageText: 'Crash detected',
          startTimestamp: this.sessionStart,
          buttons: defaultButtons()
        })
        return
      }
      void this.publish(this.idlePresence())
      return
    }

    if (payload.detail?.includes('Minecraft closed')) {
      this.gameHandoff = false
      this.sessionStart = Math.floor(Date.now() / 1000)
      void this.publish(this.idlePresence())
      return
    }

    if (this.gameHandoff || minecraftEngine.isRunning()) {
      return
    }

    if (payload.phase === 'error') {
      void this.publish({
        details: 'Launch failed',
        state: payload.detail ?? 'ValerisClient Launcher',
        largeImageKey: 'prime_logo',
        largeImageText: `ValerisClient Launcher v${app.getVersion()}`,
        smallImageKey: 'prime_logo',
        smallImageText: 'ValerisClient Launcher',
        startTimestamp: this.sessionStart,
        buttons: defaultButtons()
      })
      return
    }

    if (payload.phase === 'start' || payload.phase === 'download' || payload.phase === 'mods' || payload.phase === 'launch') {
      void this.publish({
        details: 'Launching Minecraft',
        state: payload.detail ?? 'Preparing…',
        largeImageKey: 'prime_logo',
        largeImageText: `ValerisClient Launcher v${app.getVersion()}`,
        smallImageKey: 'prime_logo',
        smallImageText: 'Launching',
        startTimestamp: this.sessionStart,
        buttons: defaultButtons()
      })
    }
  }

  private async idlePresence(): Promise<DiscordPresencePayload> {
    const account = await accountService.getStoredActiveAccount()
    const username = account?.username ?? 'Player'
    return {
      details: 'ValerisClient Launcher',
      state: `${username} • Ready to play`,
      largeImageKey: 'prime_logo',
      largeImageText: `ValerisClient Launcher v${app.getVersion()}`,
      smallImageKey: 'prime_logo',
      smallImageText: 'ValerisClient',
      startTimestamp: this.sessionStart,
      buttons: defaultButtons()
    }
  }

  private async publish(payload: DiscordPresencePayload | Promise<DiscordPresencePayload>): Promise<void> {
    if (!this.enabled) {
      return
    }
    const resolved = await payload
    const fingerprint = presenceFingerprint(resolved)
    if (fingerprint === this.lastFingerprint && this.ipc.isConnected()) {
      return
    }

    const ok = await this.ipc.setActivity(resolved)
    if (ok) {
      this.lastFingerprint = fingerprint
      launchLogService.append('info', 'Discord Rich Presence active', 'start')
    } else {
      const reason = this.ipc.lastError ?? 'Discord Desktop not running'
      launchLogService.append(
        'warn',
        `Discord RPC unavailable — ${reason} (App ID ${DISCORD_APPLICATION_ID})`,
        'start'
      )
    }
  }

  private async publishIdleWithDiagnostics(context: string): Promise<void> {
    await this.publish(this.idlePresence())
    if (!this.ipc.isConnected() && this.enabled) {
      const reason = this.ipc.lastError ?? ''
      const waiting =
        !reason ||
        reason.includes('not detected') ||
        reason.toLowerCase().includes('enoent') ||
        reason.toLowerCase().includes('econnrefused')
      launchLogService.append(
        'warn',
        waiting
          ? `${context}: waiting for Discord Desktop… (Application ID ${DISCORD_APPLICATION_ID})`
          : `${context}: Discord RPC failed — ${reason}`,
        'start'
      )
    }
  }

  private startRetryLoop(): void {
    this.stopRetryLoop()
    this.retryTimer = setInterval(() => {
      if (!this.enabled || this.gameHandoff) {
        return
      }
      if (this.ipc.isConnected()) {
        return
      }
      void this.publishIdleWithDiagnostics('Retry')
    }, 10_000)
  }

  private stopRetryLoop(): void {
    if (this.retryTimer) {
      clearInterval(this.retryTimer)
      this.retryTimer = null
    }
  }
}

function defaultButtons(): DiscordPresencePayload['buttons'] {
  return [
    { label: 'ValerisClient', url: APP_URL },
    { label: 'Discord', url: DISCORD_APP_URL }
  ]
}

export const launcherDiscordService = new LauncherDiscordService()
