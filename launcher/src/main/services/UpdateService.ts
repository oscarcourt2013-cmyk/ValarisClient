import { copyFile, mkdir, readdir, unlink, writeFile } from 'fs/promises'
import { app, shell } from 'electron'
import { join } from 'path'
import { spawn } from 'child_process'
import { GITHUB_RELEASES_URL } from '../../shared/github'
import {
  compareSemver,
  fetchLatestGitHubRelease,
  isPrimeModJarAsset,
  parseLauncherVersionFromAsset,
  parseModVersionFromAsset,
  pickPrimeModAsset,
  pickWindowsLauncherAsset,
  type GitHubRelease
} from '../../shared/githubRelease'
import { allStellarJarPrefixes, primeJarPrefix, resolveTarget } from '../../shared/minecraft-targets'
import type { UpdateInstallResultDto, UpdateProgressDto, UpdateStatusDto } from '../../shared/ipc'
import { getInstanceModsDir } from '../minecraft/paths'
import { minecraftEngine } from '../minecraft/MinecraftEngine'
import { instanceService } from './InstanceService'
import { settingsStore } from '../storage/SettingsStore'
import { emitUpdateProgress } from './updateProgress'
import { downloadService } from './DownloadService'

const CHECK_CACHE_MS = 60 * 60 * 1000

interface ModCacheManifest {
  tag: string
  fileName: string
  downloadedAt: string
  jarPrefix?: string
}

function modCacheDir(): string {
  return join(app.getPath('userData'), 'cache', 'prime-mod')
}

function modCacheManifestPath(): string {
  return join(modCacheDir(), 'manifest.json')
}

function offlineStatus(currentLauncher: string, currentMod: string, checkedAt: string): UpdateStatusDto {
  return {
    checkedAt,
    notes: 'Offline — could not check GitHub Releases.',
    releaseUrl: GITHUB_RELEASES_URL,
    launcher: { current: currentLauncher, latest: currentLauncher, updateAvailable: false },
    mod: { current: currentMod, latest: currentMod, updateAvailable: false },
    anyUpdateAvailable: false
  }
}

function buildDismissKey(status: UpdateStatusDto): string {
  return `launcher:${status.launcher.latest}|mod:${status.mod.latest}`
}

async function downloadWithProgress(
  url: string,
  dest: string,
  target: UpdateProgressDto['target'],
  label: string
): Promise<void> {
  await downloadService.downloadFile({
    url,
    destPath: dest,
    bypassCache: true,
    onProgress: (percent, _speed, meta) => {
      const reported = meta.total > 0 ? Math.min(99, percent) : percent
      emitUpdateProgress({
        target,
        phase: 'downloading',
        percent: reported,
        detail: label
      })
    }
  })
}

async function getInstalledModVersion(instanceId: string, jarPrefix: string): Promise<string> {
  const modsDir = getInstanceModsDir(instanceId)
  try {
    const files = await readdir(modsDir)
    let best: string | null = null
    for (const file of files) {
      if (!isPrimeModJarAsset(file, jarPrefix)) {
        continue
      }
      const version = parseModVersionFromAsset(file, jarPrefix)
      if (version && (!best || compareSemver(version, best) > 0)) {
        best = version
      }
    }
    return best ?? '0.0.0'
  } catch {
    return '0.0.0'
  }
}

function isAnyPrimeJar(fileName: string): boolean {
  return allStellarJarPrefixes().some((prefix) => isPrimeModJarAsset(fileName, prefix))
}

async function removeStalePrimeJars(modsDir: string, keepFileName: string): Promise<void> {
  let files: string[]
  try {
    files = await readdir(modsDir)
  } catch {
    return
  }

  await Promise.all(
    files
      .filter((f) => isAnyPrimeJar(f) && f !== keepFileName)
      .map((f) => unlink(join(modsDir, f)).catch(() => undefined))
  )
}

async function writeModCacheManifest(manifest: ModCacheManifest): Promise<void> {
  await mkdir(modCacheDir(), { recursive: true })
  await writeFile(modCacheManifestPath(), JSON.stringify(manifest, null, 2), 'utf8')
}

function statusFromRelease(
  release: GitHubRelease | null,
  currentLauncher: string,
  currentMod: string,
  checkedAt: string,
  jarPrefix: string,
  notes?: string
): UpdateStatusDto {
  if (!release) {
    return {
      checkedAt,
      notes: notes ?? 'No GitHub release yet. Check back after the first tag is published.',
      releaseUrl: GITHUB_RELEASES_URL,
      launcher: { current: currentLauncher, latest: currentLauncher, updateAvailable: false },
      mod: { current: currentMod, latest: currentMod, updateAvailable: false },
      anyUpdateAvailable: false
    }
  }

  const launcherAsset = pickWindowsLauncherAsset(release)
  const modAsset = pickPrimeModAsset(release, jarPrefix)

  const launcherLatest =
    (launcherAsset ? parseLauncherVersionFromAsset(launcherAsset.name) : null) ??
    release.tag_name.replace(/^v/i, '')
  const modLatest =
    (modAsset ? parseModVersionFromAsset(modAsset.name, jarPrefix) : null) ??
    release.tag_name.replace(/^v/i, '')

  const launcherUpdate = compareSemver(currentLauncher, launcherLatest) < 0
  const modUpdate = compareSemver(currentMod, modLatest) < 0

  const releaseNotes =
    notes ??
    (launcherUpdate || modUpdate
      ? release.body?.split('\n')[0]?.trim() || `Version ${release.tag_name} is available on GitHub.`
      : 'You are on the latest GitHub release.')

  return {
    checkedAt,
    notes: releaseNotes,
    releaseUrl: release.html_url,
    launcher: {
      current: currentLauncher,
      latest: launcherLatest,
      updateAvailable: launcherUpdate,
      downloadUrl: launcherAsset?.browser_download_url,
      fileName: launcherAsset?.name
    },
    mod: {
      current: currentMod,
      latest: modLatest,
      updateAvailable: modUpdate,
      downloadUrl: modAsset?.browser_download_url,
      fileName: modAsset?.name
    },
    anyUpdateAvailable: launcherUpdate || modUpdate
  }
}

/** Checks GitHub Releases for launcher + mod updates; installs in-app on Windows. */
export class UpdateService {
  private cachedStatus: UpdateStatusDto | null = null
  private cachedAt = 0
  private checkInFlight: Promise<UpdateStatusDto> | null = null

  getStatus(): UpdateStatusDto | null {
    return this.cachedStatus
  }

  async check(force = false): Promise<UpdateStatusDto> {
    const now = Date.now()
    if (!force && this.cachedStatus && now - this.cachedAt < CHECK_CACHE_MS) {
      return this.cachedStatus
    }

    if (this.checkInFlight && !force) {
      return this.checkInFlight
    }

    this.checkInFlight = this.fetchStatus(force)
    try {
      return await this.checkInFlight
    } finally {
      this.checkInFlight = null
    }
  }

  private async fetchStatus(force: boolean): Promise<UpdateStatusDto> {
    const currentLauncher = app.getVersion()
    const defaultInstance = await instanceService.getDefault()
    const jarPrefix = primeJarPrefix(defaultInstance?.minecraftVersion)
    const currentMod = defaultInstance
      ? await getInstalledModVersion(defaultInstance.id, jarPrefix)
      : '0.0.0'
    const checkedAt = new Date().toISOString()

    try {
      const release = await fetchLatestGitHubRelease()
      const status = statusFromRelease(release, currentLauncher, currentMod, checkedAt, jarPrefix)

      await settingsStore.mutate((s) => {
        s.lastUpdateCheck = checkedAt
      })

      this.cachedStatus = status
      this.cachedAt = Date.now()
      return status
    } catch {
      const status = offlineStatus(currentLauncher, currentMod, checkedAt)
      await settingsStore.mutate((s) => {
        s.lastUpdateCheck = checkedAt
      })
      if (force || !this.cachedStatus) {
        this.cachedStatus = status
        this.cachedAt = Date.now()
      }
      return this.cachedStatus ?? status
    }
  }

  async dismissBanner(): Promise<void> {
    const status = this.cachedStatus ?? (await this.check())
    await settingsStore.mutate((s) => {
      s.dismissedUpdateBanner = buildDismissKey(status)
    })
  }

  async shouldShowBanner(): Promise<boolean> {
    const settings = await settingsStore.load()
    const status = this.cachedStatus ?? (await this.check())
    if (!status.anyUpdateAvailable) {
      return false
    }
    return settings.dismissedUpdateBanner !== buildDismissKey(status)
  }

  async openReleasePage(url?: string): Promise<void> {
    await shell.openExternal(url ?? GITHUB_RELEASES_URL)
  }

  async installLauncher(): Promise<UpdateInstallResultDto> {
    if (!app.isPackaged) {
      return { ok: false, errorKey: 'dev_mode' }
    }

    if (process.platform !== 'win32') {
      return { ok: false, errorKey: 'unsupported_platform' }
    }

    const status = await this.check(true)
    if (!status.launcher.updateAvailable || !status.launcher.downloadUrl) {
      return { ok: false, errorKey: 'no_update' }
    }

    const fileName = status.launcher.fileName ?? `stellar-client-launcher-Setup-${status.launcher.latest}.exe`
    const dest = join(app.getPath('temp'), fileName)

    try {
      emitUpdateProgress({
        target: 'launcher',
        phase: 'downloading',
        percent: 0,
        detail: fileName
      })

      await downloadWithProgress(status.launcher.downloadUrl, dest, 'launcher', fileName)

      emitUpdateProgress({ target: 'launcher', phase: 'installing', percent: 100 })

      const installDir = join(app.getPath('exe'), '..')
      const args = ['/S', `/D=${installDir}`]

      spawn(dest, args, { detached: true, stdio: 'ignore' }).unref()

      setTimeout(() => {
        app.quit()
      }, 500)

      return { ok: true, version: status.launcher.latest }
    } catch (err) {
      emitUpdateProgress({
        target: 'launcher',
        phase: 'error',
        percent: 0,
        detail: err instanceof Error ? err.message : 'Install failed'
      })
      return { ok: false, error: err instanceof Error ? err.message : 'Install failed' }
    }
  }

  async installMod(instanceId?: string): Promise<UpdateInstallResultDto> {
    if (minecraftEngine.isRunning()) {
      return { ok: false, errorKey: 'game_running' }
    }

    const instance = instanceId
      ? await instanceService.getById(instanceId)
      : await instanceService.getDefault()

    if (!instance) {
      return { ok: false, errorKey: 'no_instance' }
    }

    if (!instance.includePrimeMod) {
      return { ok: false, errorKey: 'prime_mod_disabled' }
    }

    const target = resolveTarget(instance.minecraftVersion)
    const jarPrefix = target.jarPrefix

    const release = await fetchLatestGitHubRelease()
    const modAsset = release ? pickPrimeModAsset(release, jarPrefix) : undefined
    if (!modAsset?.browser_download_url || !modAsset.name) {
      return { ok: false, errorKey: 'no_update' }
    }

    const currentMod = await getInstalledModVersion(instance.id, jarPrefix)
    const latest =
      parseModVersionFromAsset(modAsset.name, jarPrefix) ?? release!.tag_name.replace(/^v/i, '')
    if (compareSemver(currentMod, latest) >= 0) {
      return { ok: false, errorKey: 'no_update' }
    }

    const modsDir = getInstanceModsDir(instance.id)
    await mkdir(modsDir, { recursive: true })

    const cachePath = join(modCacheDir(), modAsset.name)
    const destPath = join(modsDir, modAsset.name)

    try {
      emitUpdateProgress({
        target: 'mod',
        phase: 'downloading',
        percent: 0,
        detail: modAsset.name
      })

      await downloadWithProgress(modAsset.browser_download_url, cachePath, 'mod', modAsset.name)

      emitUpdateProgress({ target: 'mod', phase: 'installing', percent: 100 })

      await removeStalePrimeJars(modsDir, modAsset.name)
      await copyFile(cachePath, destPath)

      await writeModCacheManifest({
        tag: latest,
        fileName: modAsset.name,
        jarPrefix,
        downloadedAt: new Date().toISOString()
      })

      emitUpdateProgress({ target: 'mod', phase: 'done', percent: 100 })

      const refreshed = await this.check(true)
      this.cachedStatus = refreshed

      return { ok: true, version: latest }
    } catch (err) {
      emitUpdateProgress({
        target: 'mod',
        phase: 'error',
        percent: 0,
        detail: err instanceof Error ? err.message : 'Install failed'
      })
      return { ok: false, error: err instanceof Error ? err.message : 'Install failed' }
    }
  }
}

export const updateService = new UpdateService()
