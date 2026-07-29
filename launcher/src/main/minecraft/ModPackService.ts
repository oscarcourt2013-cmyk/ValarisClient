import { copyFile, mkdir, readdir, readFile, stat, unlink, writeFile } from 'fs/promises'
import { app } from 'electron'
import { basename, join } from 'path'
import {
  fetchLatestGitHubRelease,
  isPrimeModJarAsset,
  pickPrimeModAsset,
  type GitHubRelease
} from '../../shared/githubRelease'
import {
  allStellarJarPrefixes,
  parseStellarJarSemVer,
  resolveTarget,
  type MinecraftTarget
} from '../../shared/minecraft-targets'
import { getInstanceModsDir, getRepoRoot } from './paths'
import { emitLaunchProgress } from './launchProgress'
import type { InstanceLaunchConfig } from './constants'
import { downloadService } from '../services/DownloadService'

const FABRIC_API_PROJECT = 'P7dR8mSH'

interface ModCacheManifest {
  tag: string
  fileName: string
  downloadedAt: string
  lastGitHubCheckAt?: string
  jarPrefix?: string
}

/** Skip GitHub API + jar download if a local jar exists and we checked recently. */
const GITHUB_CHECK_INTERVAL_MS = 12 * 60 * 60 * 1000

type SemVer = [major: number, minor: number, patch: number]

interface JarCandidate {
  path: string
  version: SemVer
  source: string
}

async function downloadFile(url: string, dest: string): Promise<void> {
  await downloadService.downloadFile({
    url,
    destPath: dest
  })
}

function modCacheDir(): string {
  return join(app.getPath('userData'), 'cache', 'prime-mod')
}

function modCacheManifestPath(): string {
  return join(modCacheDir(), 'manifest.json')
}

function parseJarVersion(fileName: string, jarPrefix: string): SemVer | null {
  return parseStellarJarSemVer(fileName, jarPrefix)
}

function compareSemVer(a: SemVer, b: SemVer): number {
  for (let i = 0; i < 3; i++) {
    if (a[i] !== b[i]) {
      return a[i]! - b[i]!
    }
  }
  return 0
}

function pickNewestCandidate(candidates: JarCandidate[]): JarCandidate | null {
  let best: JarCandidate | null = null
  for (const candidate of candidates) {
    if (!best || compareSemVer(candidate.version, best.version) > 0) {
      best = candidate
    }
  }
  return best
}

async function readModCacheManifest(): Promise<ModCacheManifest | null> {
  try {
    const raw = await readFile(modCacheManifestPath(), 'utf8')
    return JSON.parse(raw) as ModCacheManifest
  } catch {
    return null
  }
}

async function writeModCacheManifest(manifest: ModCacheManifest): Promise<void> {
  await mkdir(modCacheDir(), { recursive: true })
  await writeFile(modCacheManifestPath(), JSON.stringify(manifest, null, 2), 'utf8')
}

function isGitHubCheckStale(manifest: ModCacheManifest | null, jarPrefix: string): boolean {
  if (!manifest?.lastGitHubCheckAt) {
    return true
  }
  if (manifest.jarPrefix && manifest.jarPrefix !== jarPrefix) {
    return true
  }
  const elapsed = Date.now() - new Date(manifest.lastGitHubCheckAt).getTime()
  return elapsed >= GITHUB_CHECK_INTERVAL_MS
}

async function touchGitHubCheck(manifest: ModCacheManifest | null, jarPrefix: string): Promise<void> {
  const base: ModCacheManifest = manifest ?? {
    tag: '',
    fileName: '',
    downloadedAt: new Date(0).toISOString()
  }
  await writeModCacheManifest({
    ...base,
    jarPrefix,
    lastGitHubCheckAt: new Date().toISOString()
  })
}

async function candidateFromPath(
  path: string,
  source: string,
  jarPrefix: string
): Promise<JarCandidate | null> {
  try {
    const info = await stat(path)
    if (!info.isFile() || info.size <= 0) {
      return null
    }
    const version = parseJarVersion(basename(path), jarPrefix)
    if (!version) {
      return null
    }
    return { path, version, source }
  } catch {
    return null
  }
}

async function findLocalBuildJar(target: MinecraftTarget): Promise<JarCandidate | null> {
  const libsDir = join(getRepoRoot(), target.localBuildDir, 'build', 'libs')
  try {
    const files = await readdir(libsDir)
    const matches = files.filter((f) => isPrimeModJarAsset(f, target.jarPrefix))
    let best: JarCandidate | null = null
    for (const file of matches) {
      const candidate = await candidateFromPath(join(libsDir, file), 'local-build', target.jarPrefix)
      if (candidate && (!best || compareSemVer(candidate.version, best.version) > 0)) {
        best = candidate
      }
    }
    return best
  } catch {
    return null
  }
}

async function findEnvOverrideJar(jarPrefix: string): Promise<JarCandidate | null> {
  if (!process.env.PRIME_CLIENT_JAR) {
    return null
  }
  return candidateFromPath(process.env.PRIME_CLIENT_JAR, 'env-override', jarPrefix)
}

async function findCachedGitHubJar(
  manifest: ModCacheManifest,
  jarPrefix: string
): Promise<JarCandidate | null> {
  if (manifest.jarPrefix && manifest.jarPrefix !== jarPrefix) {
    return null
  }
  if (!isPrimeModJarAsset(manifest.fileName, jarPrefix)) {
    return null
  }
  return candidateFromPath(join(modCacheDir(), manifest.fileName), 'github-cache', jarPrefix)
}

async function findInstalledInstanceJar(
  instanceId: string,
  jarPrefix: string
): Promise<JarCandidate | null> {
  const modsDir = getInstanceModsDir(instanceId)
  try {
    const files = await readdir(modsDir)
    let best: JarCandidate | null = null
    for (const file of files) {
      if (!isPrimeModJarAsset(file, jarPrefix)) {
        continue
      }
      const candidate = await candidateFromPath(join(modsDir, file), 'instance-mods', jarPrefix)
      if (candidate && (!best || compareSemVer(candidate.version, best.version) > 0)) {
        best = candidate
      }
    }
    return best
  } catch {
    return null
  }
}

async function downloadPrimeModFromRelease(
  release: GitHubRelease,
  jarPrefix: string
): Promise<JarCandidate | null> {
  const asset = pickPrimeModAsset(release, jarPrefix)
  if (!asset?.browser_download_url) {
    return null
  }

  await mkdir(modCacheDir(), { recursive: true })
  const dest = join(modCacheDir(), asset.name)

  emitLaunchProgress({
    phase: 'mods',
    detail: `Downloading StellarClient ${release.tag_name} from GitHubâ€¦`,
    percent: 38
  })

  await downloadFile(asset.browser_download_url, dest)
  await writeModCacheManifest({
    tag: release.tag_name,
    fileName: asset.name,
    jarPrefix,
    downloadedAt: new Date().toISOString()
  })
  return candidateFromPath(dest, 'github-download', jarPrefix)
}

/** Resolves the newest StellarClient jar across dev build, instance, cache and GitHub. */
async function resolveStellarClientSource(
  instanceId: string,
  target: MinecraftTarget
): Promise<JarCandidate | null> {
  const { jarPrefix } = target
  const candidates: JarCandidate[] = []

  const envJar = await findEnvOverrideJar(jarPrefix)
  if (envJar) {
    candidates.push(envJar)
  }

  const localJar = await findLocalBuildJar(target)
  if (localJar) {
    candidates.push(localJar)
  }

  const installedJar = await findInstalledInstanceJar(instanceId, jarPrefix)
  if (installedJar) {
    candidates.push(installedJar)
  }

  const manifest = await readModCacheManifest()
  if (manifest) {
    const cached = await findCachedGitHubJar(manifest, jarPrefix)
    if (cached) {
      candidates.push(cached)
    }
  }

  let best = pickNewestCandidate(candidates)

  const shouldCheckGitHub = !best || isGitHubCheckStale(manifest, jarPrefix)
  if (shouldCheckGitHub) {
    emitLaunchProgress({
      phase: 'mods',
      detail: 'Checking GitHub Releases for StellarClient modâ€¦',
      percent: 36
    })

    const release = await fetchLatestGitHubRelease()
    await touchGitHubCheck(manifest, jarPrefix)

    if (release) {
      const asset = pickPrimeModAsset(release, jarPrefix)
      const remoteVersion = asset ? parseJarVersion(asset.name, jarPrefix) : null

      const shouldDownload =
        asset?.browser_download_url &&
        remoteVersion &&
        (!best || compareSemVer(remoteVersion, best.version) > 0)

      if (shouldDownload) {
        const downloaded = await downloadPrimeModFromRelease(release, jarPrefix)
        if (downloaded && (!best || compareSemVer(downloaded.version, best.version) > 0)) {
          best = downloaded
        }
      } else if (asset && remoteVersion && !best) {
        const downloaded = await downloadPrimeModFromRelease(release, jarPrefix)
        if (downloaded) {
          best = downloaded
        }
      }
    }
  } else if (best) {
    emitLaunchProgress({
      phase: 'mods',
      detail: `Using StellarClient ${best.version.join('.')} (cached, no remote check)`,
      percent: 38
    })
  }

  if (best) {
    emitLaunchProgress({
      phase: 'mods',
      detail: `Using StellarClient ${best.version.join('.')} (${best.source})`,
      percent: 39
    })
  }

  return best
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

async function ensureFabricApi(config: InstanceLaunchConfig, modsDir: string): Promise<string> {
  const destName = `fabric-api-${config.fabricApiModrinthVersion}.jar`
  const dest = join(modsDir, destName)
  try {
    const info = await stat(dest)
    if (info.isFile() && info.size > 0) {
      return dest
    }
  } catch {
    // download below
  }

  emitLaunchProgress({
    phase: 'mods',
    detail: 'Downloading Fabric API from Modrinthâ€¦',
    percent: 35
  })

  const query = new URLSearchParams({
    game_versions: JSON.stringify([config.minecraftVersion]),
    loaders: JSON.stringify(['fabric'])
  })
  const response = await fetch(
    `https://api.modrinth.com/v2/project/${FABRIC_API_PROJECT}/version?${query.toString()}`
  )
  if (!response.ok) {
    throw new Error(`Modrinth API error (${response.status}) while fetching Fabric API.`)
  }

  const versions = (await response.json()) as Array<{
    version_number: string
    files: Array<{ url: string; primary: boolean; filename: string }>
  }>

  const exact = versions.find((v) => v.version_number === config.fabricApiModrinthVersion)
  const chosen = exact ?? versions[0]
  if (!chosen) {
    throw new Error(`No Fabric API build found for Minecraft ${config.minecraftVersion}.`)
  }

  const file = chosen.files.find((f) => f.primary) ?? chosen.files[0]
  if (!file?.url) {
    throw new Error('Fabric API version has no downloadable file.')
  }

  await downloadFile(file.url, dest)
  return dest
}

/** Installs StellarClient + Fabric API when includePrimeMod is enabled. */
export async function installInstanceMods(
  instanceId: string,
  config: InstanceLaunchConfig
): Promise<{ primeJar: string | null; fabricApiJar: string | null }> {
  if (!config.includePrimeMod) {
    return { primeJar: null, fabricApiJar: null }
  }

  const target = resolveTarget(config.minecraftVersion)
  const modsDir = getInstanceModsDir(instanceId)
  await mkdir(modsDir, { recursive: true })

  emitLaunchProgress({
    phase: 'mods',
    detail: `Preparing mods for Minecraft ${target.mcVersion}â€¦`,
    percent: 30
  })

  const fabricApiJar = await ensureFabricApi(config, modsDir)

  const primeCandidate = await resolveStellarClientSource(instanceId, target)
  if (!primeCandidate) {
    return { primeJar: null, fabricApiJar }
  }

  const primeDestName = basename(primeCandidate.path)
  await removeStalePrimeJars(modsDir, primeDestName)

  const primeDest = join(modsDir, primeDestName)
  try {
    const [srcInfo, destInfo] = await Promise.all([stat(primeCandidate.path), stat(primeDest)])
    if (srcInfo.size === destInfo.size && srcInfo.mtimeMs <= destInfo.mtimeMs) {
      return { primeJar: primeDest, fabricApiJar }
    }
  } catch {
    // copy below
  }

  emitLaunchProgress({
    phase: 'mods',
    detail: `Installing StellarClient mod ${primeCandidate.version.join('.')}â€¦`,
    percent: 40
  })
  await copyFile(primeCandidate.path, primeDest)
  return { primeJar: primeDest, fabricApiJar }
}

export function StellarClientBuildHint(minecraftVersion?: string): string {
  const target = resolveTarget(minecraftVersion)
  return (
    `Build the mod with \`.\\gradlew :${target.localBuildDir}:build\`, set PRIME_CLIENT_JAR, ` +
    `or publish a GitHub Release with a ${target.jarPrefix}*.jar asset.`
  )
}
