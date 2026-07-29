import { execFile } from 'child_process'
import { existsSync, mkdirSync, readdirSync, rmSync, statSync } from 'fs'
import { rename, rm, unlink } from 'fs/promises'
import { join } from 'path'
import { promisify } from 'util'
import { app } from 'electron'
import AdmZip from 'adm-zip'
import { emitLaunchProgress } from './launchProgress'
import { downloadService } from '../services/DownloadService'

const execFileAsync = promisify(execFile)
const SUPPORTED_VERSIONS = [21] as const

function getRuntimeDir(version = 21): string {
  return join(app.getPath('userData'), 'runtime', `jre-${version}`)
}

export function getBundledJavaPath(version = 21): string | null {
  const dir = getRuntimeDir(version)
  const exe = process.platform === 'win32' ? 'java.exe' : 'java'
  const javaPath = join(dir, 'bin', exe)
  return existsSync(javaPath) ? javaPath : null
}

function getAdoptiumDownloadUrl(version = 21): string {
  const osMap: Record<string, string> = { win32: 'windows', linux: 'linux', darwin: 'mac' }
  const archMap: Record<string, string> = { x64: 'x64', ia32: 'x86', arm64: 'aarch64' }

  const adoptiumOs = osMap[process.platform]
  const adoptiumArch = archMap[process.arch]
  if (!adoptiumOs || !adoptiumArch) {
    throw new Error(`Unsupported platform for bundled Java: ${process.platform}/${process.arch}`)
  }

  return `https://api.adoptium.net/v3/binary/latest/${version}/ga/${adoptiumOs}/${adoptiumArch}/jre/hotspot/normal/eclipse`
}

async function downloadJreArchive(version: number, onProgress?: (received: number, total: number) => void): Promise<string> {
  const runtimeDir = getRuntimeDir(version)
  const tempFile = join(join(runtimeDir, '..'), `jre-${version}.download`)

  await downloadService.downloadFile({
    url: getAdoptiumDownloadUrl(version),
    destPath: tempFile,
    bypassCache: true,
    onProgress: (_percent, _speed, meta) => {
      onProgress?.(meta.received, meta.total)
    }
  })

  return tempFile
}

async function extractJreArchive(version: number, archivePath: string): Promise<void> {
  const runtimeDir = getRuntimeDir(version)
  const tmpExtract = `${runtimeDir}-extract-tmp`

  if (existsSync(tmpExtract)) {
    await rm(tmpExtract, { recursive: true, force: true })
  }
  mkdirSync(tmpExtract, { recursive: true })

  if (process.platform === 'win32') {
    new AdmZip(archivePath).extractAllTo(tmpExtract, true)
  } else {
    await execFileAsync('tar', ['-xzf', archivePath, '-C', tmpExtract])
  }

  const entries = readdirSync(tmpExtract).filter((entry) => statSync(join(tmpExtract, entry)).isDirectory())
  if (entries.length === 0) {
    throw new Error('Bundled JRE archive was empty or had an unexpected layout.')
  }

  const extractedDir = join(tmpExtract, entries[0]!)
  if (existsSync(runtimeDir)) {
    await rm(runtimeDir, { recursive: true, force: true })
  }
  await rename(extractedDir, runtimeDir)
  await rm(tmpExtract, { recursive: true, force: true })
}

export async function ensureBundledJava(version = 21): Promise<string> {
  if (!SUPPORTED_VERSIONS.includes(version as (typeof SUPPORTED_VERSIONS)[number])) {
    throw new Error(`Unsupported bundled Java version: ${version}`)
  }

  const existing = getBundledJavaPath(version)
  if (existing) {
    return existing
  }

  mkdirSync(join(getRuntimeDir(version), '..'), { recursive: true })
  emitLaunchProgress({
    phase: 'launch',
    detail: `Downloading Java ${version} (Adoptium)…`,
    percent: 72
  })

  const tempFile = await downloadJreArchive(version, (received, total) => {
    if (total <= 0) {
      return
    }
    emitLaunchProgress({
      phase: 'launch',
      detail: `Downloading Java ${version}… ${Math.round((received / total) * 100)}%`,
      percent: 72 + Math.round((received / total) * 6)
    })
  })

  try {
    emitLaunchProgress({ phase: 'launch', detail: `Extracting Java ${version}…`, percent: 79 })
    await extractJreArchive(version, tempFile)
    await unlink(tempFile).catch(() => undefined)
    const installed = getBundledJavaPath(version)
    if (!installed) {
      throw new Error('Java installation finished but java.exe was not found.')
    }
    return installed
  } catch (err) {
    if (existsSync(tempFile)) {
      rmSync(tempFile, { force: true })
    }
    throw err
  }
}
