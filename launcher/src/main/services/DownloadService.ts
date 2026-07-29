import { createHash, randomUUID } from 'crypto'
import { createReadStream } from 'fs'
import { copyFile, mkdir, open, readFile, rename, rm, stat, unlink, writeFile } from 'fs/promises'
import { dirname, join } from 'path'
import { app } from 'electron'
import type { DownloadTask } from '../../shared/content-types'
import type { LaunchProgressDto } from '../../shared/ipc'
import { downloadStore } from '../storage/DownloadStore'
import { downloadQueue } from '../utils/DownloadQueue'

const ACTIVE_LAUNCH_ID = 'active-minecraft-launch'
const CACHE_INDEX_VERSION = 1 as const

export type DownloadHashAlgorithm = 'sha256' | 'sha1' | 'sha512'

export interface DownloadIntegrity {
  algorithm: DownloadHashAlgorithm
  hash: string
}

export interface DownloadFileOptions {
  url: string
  destPath: string
  /** Hex SHA-256 digest; verified after download when set. */
  sha256?: string
  /** Explicit integrity check (used when algorithm is not sha256, e.g. Modrinth sha512). */
  integrity?: DownloadIntegrity
  /** Extra request headers (API keys, auth, etc.). */
  headers?: Record<string, string>
  /** Skip disk cache lookup and store. */
  bypassCache?: boolean
  onProgress?: (percent: number, speed: string, meta: { received: number; total: number }) => void
}

export interface DownloadFileResult {
  path: string
  fromCache: boolean
  bytes: number
  sha256?: string
}

interface CacheIndexEntry {
  key: string
  url: string
  fileName: string
  size: number
  sha256?: string
  integrity?: DownloadIntegrity
  completedAt: string
}

interface CacheIndex {
  version: typeof CACHE_INDEX_VERSION
  entries: Record<string, CacheIndexEntry>
}

function normalizeHex(value: string): string {
  return value.trim().toLowerCase().replace(/^0x/, '')
}

function resolveIntegrity(options: DownloadFileOptions): DownloadIntegrity | undefined {
  if (options.sha256?.trim()) {
    return { algorithm: 'sha256', hash: normalizeHex(options.sha256) }
  }
  if (options.integrity?.hash?.trim()) {
    return {
      algorithm: options.integrity.algorithm,
      hash: normalizeHex(options.integrity.hash)
    }
  }
  return undefined
}

function cacheKeyFor(url: string, integrity?: DownloadIntegrity): string {
  if (integrity) {
    return createHash('sha256').update(`${integrity.algorithm}:${integrity.hash}`, 'utf8').digest('hex')
  }
  return createHash('sha256').update(url, 'utf8').digest('hex')
}

function formatBytes(bytes: number): string {
  if (!Number.isFinite(bytes) || bytes < 0) {
    return '—'
  }
  if (bytes < 1024) {
    return `${bytes} B`
  }
  if (bytes < 1024 * 1024) {
    return `${(bytes / 1024).toFixed(1)} KB`
  }
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`
}

function formatSpeed(bytesPerSec: number): string {
  if (!Number.isFinite(bytesPerSec) || bytesPerSec <= 0) {
    return '—'
  }
  if (bytesPerSec < 1024) {
    return `${bytesPerSec.toFixed(0)} B/s`
  }
  if (bytesPerSec < 1024 * 1024) {
    return `${(bytesPerSec / 1024).toFixed(1)} KB/s`
  }
  return `${(bytesPerSec / (1024 * 1024)).toFixed(1)} MB/s`
}

function formatEta(remainingBytes: number, bytesPerSec: number): string {
  if (!Number.isFinite(remainingBytes) || remainingBytes <= 0 || bytesPerSec <= 0) {
    return '…'
  }
  const seconds = Math.ceil(remainingBytes / bytesPerSec)
  if (seconds < 60) {
    return `${seconds}s`
  }
  const minutes = Math.floor(seconds / 60)
  const rem = seconds % 60
  return `${minutes}m ${rem}s`
}

async function hashFile(filePath: string, algorithm: DownloadHashAlgorithm): Promise<string> {
  return await new Promise<string>((resolve, reject) => {
    const hash = createHash(algorithm)
    const stream = createReadStream(filePath)
    stream.on('data', (chunk) => hash.update(chunk))
    stream.on('error', reject)
    stream.on('end', () => resolve(hash.digest('hex')))
  })
}

async function ensureParentDir(filePath: string): Promise<void> {
  await mkdir(dirname(filePath), { recursive: true })
}

async function atomicReplace(tmpPath: string, destPath: string): Promise<void> {
  await ensureParentDir(destPath)
  try {
    await unlink(destPath)
  } catch {
    // dest may not exist yet
  }
  await rename(tmpPath, destPath)
}

async function copyAtomically(sourcePath: string, destPath: string): Promise<void> {
  const tmpPath = `${destPath}.tmp`
  await ensureParentDir(destPath)
  await copyFile(sourcePath, tmpPath)
  await atomicReplace(tmpPath, destPath)
}

/** Persists download / launch progress locally for the Download Center. */
export class DownloadService {
  private cacheIndex: CacheIndex | null = null

  private get cacheDir(): string {
    return join(app.getPath('userData'), 'cache', 'http-downloads')
  }

  private get cacheFilesDir(): string {
    return join(this.cacheDir, 'files')
  }

  private get cacheIndexPath(): string {
    return join(this.cacheDir, 'index.json')
  }

  private async loadCacheIndex(): Promise<CacheIndex> {
    if (this.cacheIndex) {
      return this.cacheIndex
    }
    try {
      const raw = await readFile(this.cacheIndexPath, 'utf8')
      const parsed = JSON.parse(raw) as CacheIndex
      if (parsed?.version === CACHE_INDEX_VERSION && parsed.entries && typeof parsed.entries === 'object') {
        this.cacheIndex = parsed
        return this.cacheIndex
      }
    } catch {
      // rebuild below
    }
    this.cacheIndex = { version: CACHE_INDEX_VERSION, entries: {} }
    await this.saveCacheIndex()
    return this.cacheIndex
  }

  private async saveCacheIndex(): Promise<void> {
    if (!this.cacheIndex) {
      return
    }
    await mkdir(this.cacheDir, { recursive: true })
    await writeFile(this.cacheIndexPath, JSON.stringify(this.cacheIndex, null, 2), 'utf8')
  }

  async list(): Promise<DownloadTask[]> {
    const db = await downloadStore.load()
    return db.tasks.sort((a, b) => {
      if (a.status === 'downloading' && b.status !== 'downloading') {
        return -1
      }
      if (b.status === 'downloading' && a.status !== 'downloading') {
        return 1
      }
      return 0
    })
  }

  async clearCompleted(): Promise<void> {
    await downloadStore.mutate((db) => {
      db.tasks = db.tasks.filter((t) => t.status !== 'completed')
    })
  }

  async remove(taskId: string): Promise<void> {
    await downloadStore.mutate((db) => {
      db.tasks = db.tasks.filter((t) => t.id !== taskId)
    })
  }

  async trackContentInstall(name: string): Promise<void> {
    await downloadStore.mutate((db) => {
      db.tasks.unshift({
        id: randomUUID(),
        name: name.slice(0, 80),
        progress: 100,
        speed: 'Modrinth',
        size: '—',
        eta: '—',
        status: 'completed'
      })
    })
  }

  async beginDownload(name: string): Promise<string> {
    const id = randomUUID()
    await downloadStore.mutate((db) => {
      db.tasks.unshift({
        id,
        name: name.slice(0, 80),
        progress: 0,
        speed: '—',
        size: '—',
        eta: '…',
        status: 'downloading'
      })
    })
    return id
  }

  async updateDownload(
    taskId: string,
    progress: number,
    speed: string,
    meta?: { size?: string; eta?: string }
  ): Promise<void> {
    await downloadStore.mutate((db) => {
      const task = db.tasks.find((t) => t.id === taskId)
      if (task) {
        task.progress = progress
        task.speed = speed
        task.status = progress >= 100 ? 'completed' : 'downloading'
        task.eta = meta?.eta ?? (progress >= 100 ? '—' : '…')
        if (meta?.size) {
          task.size = meta.size
        }
      }
    })
  }

  async failDownload(taskId: string, reason?: string): Promise<void> {
    await downloadStore.mutate((db) => {
      const task = db.tasks.find((t) => t.id === taskId)
      if (task) {
        task.status = 'failed'
        task.speed = '—'
        task.eta = '—'
        if (reason) {
          task.size = reason.slice(0, 80)
        }
      }
    })
  }

  /**
   * Robust HTTP download with concurrency limits, Range resume, optional hash
   * verification, disk cache, and atomic `.tmp` → dest rename.
   */
  async downloadFile(options: DownloadFileOptions): Promise<DownloadFileResult> {
    return await downloadQueue.run(() => this.downloadFileUnlocked(options))
  }

  private async downloadFileUnlocked(options: DownloadFileOptions): Promise<DownloadFileResult> {
    const integrity = resolveIntegrity(options)
    const key = cacheKeyFor(options.url, integrity)

    if (!options.bypassCache) {
      const cached = await this.tryReadFromCache(key, options.destPath, integrity)
      if (cached) {
        options.onProgress?.(100, 'Cache', { received: cached.bytes, total: cached.bytes })
        return cached
      }
    }

    const tmpPath = `${options.destPath}.tmp`
    await ensureParentDir(options.destPath)

    const result = await this.transferWithResume({
      url: options.url,
      tmpPath,
      headers: options.headers,
      onProgress: options.onProgress
    })

    if (integrity) {
      const digest = await hashFile(tmpPath, integrity.algorithm)
      if (digest !== integrity.hash) {
        await unlink(tmpPath).catch(() => undefined)
        throw new Error(
          `Integrity check failed (${integrity.algorithm}): expected ${integrity.hash.slice(0, 12)}…, got ${digest.slice(0, 12)}…`
        )
      }
    }

    const sha256 = integrity?.algorithm === 'sha256' ? integrity.hash : await hashFile(tmpPath, 'sha256')
    await atomicReplace(tmpPath, options.destPath)

    if (!options.bypassCache) {
      await this.writeToCache(key, options.url, options.destPath, sha256, integrity, result.bytes)
    }

    options.onProgress?.(100, 'Done', { received: result.bytes, total: result.bytes })
    return {
      path: options.destPath,
      fromCache: false,
      bytes: result.bytes,
      sha256
    }
  }

  private async tryReadFromCache(
    key: string,
    destPath: string,
    integrity?: DownloadIntegrity
  ): Promise<DownloadFileResult | null> {
    const index = await this.loadCacheIndex()
    const entry = index.entries[key]
    if (!entry) {
      return null
    }

    const cachedPath = join(this.cacheFilesDir, entry.fileName)
    try {
      const info = await stat(cachedPath)
      if (!info.isFile() || info.size <= 0) {
        delete index.entries[key]
        await this.saveCacheIndex()
        return null
      }

      if (integrity) {
        const digest = await hashFile(cachedPath, integrity.algorithm)
        if (digest !== integrity.hash) {
          await rm(cachedPath, { force: true }).catch(() => undefined)
          delete index.entries[key]
          await this.saveCacheIndex()
          return null
        }
      }

      // Already at destination with matching size — treat as cache hit.
      try {
        const destInfo = await stat(destPath)
        if (destInfo.isFile() && destInfo.size === info.size) {
          if (integrity) {
            const destDigest = await hashFile(destPath, integrity.algorithm)
            if (destDigest === integrity.hash) {
              return {
                path: destPath,
                fromCache: true,
                bytes: info.size,
                sha256: entry.sha256
              }
            }
          } else {
            return {
              path: destPath,
              fromCache: true,
              bytes: info.size,
              sha256: entry.sha256
            }
          }
        }
      } catch {
        // dest missing — copy from cache
      }

      await copyAtomically(cachedPath, destPath)
      return {
        path: destPath,
        fromCache: true,
        bytes: info.size,
        sha256: entry.sha256
      }
    } catch {
      delete index.entries[key]
      await this.saveCacheIndex()
      return null
    }
  }

  private async writeToCache(
    key: string,
    url: string,
    sourcePath: string,
    sha256: string,
    integrity: DownloadIntegrity | undefined,
    size: number
  ): Promise<void> {
    try {
      await mkdir(this.cacheFilesDir, { recursive: true })
      const fileName = key
      const cachedPath = join(this.cacheFilesDir, fileName)
      await copyAtomically(sourcePath, cachedPath)

      const index = await this.loadCacheIndex()
      index.entries[key] = {
        key,
        url,
        fileName,
        size,
        sha256,
        integrity,
        completedAt: new Date().toISOString()
      }
      await this.saveCacheIndex()
    } catch {
      // Cache is best-effort; download already succeeded.
    }
  }

  private async transferWithResume(params: {
    url: string
    tmpPath: string
    headers?: Record<string, string>
    onProgress?: DownloadFileOptions['onProgress']
  }): Promise<{ bytes: number }> {
    let existing = 0
    try {
      const info = await stat(params.tmpPath)
      if (info.isFile() && info.size > 0) {
        existing = info.size
      } else {
        await unlink(params.tmpPath).catch(() => undefined)
      }
    } catch {
      existing = 0
    }

    const headers: Record<string, string> = { ...(params.headers ?? {}) }
    if (existing > 0) {
      headers.Range = `bytes=${existing}-`
    }

    const response = await fetch(params.url, { headers })

    if (existing > 0 && response.status === 416) {
      // Temp file may already be complete — keep it and let caller verify.
      return { bytes: existing }
    }

    if (existing > 0 && response.status === 200) {
      // Server ignored Range — restart from scratch.
      await unlink(params.tmpPath).catch(() => undefined)
      existing = 0
    }

    if (!response.ok && response.status !== 206) {
      throw new Error(`Download failed (${response.status}).`)
    }

    if (!response.body) {
      throw new Error('Download failed: empty response body.')
    }

    const contentLength = Number(response.headers.get('content-length') || 0)
    const total =
      response.status === 206
        ? existing + (contentLength > 0 ? contentLength : 0)
        : contentLength > 0
          ? contentLength
          : 0

    const append = existing > 0 && response.status === 206
    const handle = await open(params.tmpPath, append ? 'a' : 'w')
    const start = Date.now()
    let received = existing

    try {
      const reader = response.body.getReader()
      while (true) {
        const { done, value } = await reader.read()
        if (done) {
          break
        }
        if (!value || value.byteLength === 0) {
          continue
        }
        await handle.write(Buffer.from(value))
        received += value.byteLength

        if (params.onProgress) {
          const elapsed = Math.max(0.001, (Date.now() - start) / 1000)
          const sessionBytes = received - existing
          const bytesPerSec = sessionBytes / elapsed
          const percent = total > 0 ? Math.min(99, Math.round((received / total) * 100)) : 0
          params.onProgress(percent, formatSpeed(bytesPerSec), { received, total })
        }
      }
    } finally {
      await handle.close()
    }

    return { bytes: received }
  }

  onLaunchProgress(payload: LaunchProgressDto): void {
    void this.trackProgress(payload)
  }

  private async trackProgress(payload: LaunchProgressDto): Promise<void> {
    if (payload.phase === 'log') {
      return
    }

    const progress = payload.percent ?? (payload.phase === 'running' ? 100 : 0)
    const status: DownloadTask['status'] =
      payload.phase === 'running' || payload.phase === 'stopped' || payload.phase === 'crashed'
        ? 'completed'
        : payload.phase === 'start'
          ? 'queued'
          : 'downloading'

    await downloadStore.mutate((db) => {
      let task = db.tasks.find((t) => t.id === ACTIVE_LAUNCH_ID)
      if (!task) {
        task = {
          id: ACTIVE_LAUNCH_ID,
          name: 'Minecraft launch',
          progress: 0,
          speed: '—',
          size: '—',
          eta: '—',
          status: 'queued'
        }
        db.tasks.unshift(task)
      }

      task.name = payload.detail.slice(0, 80) || 'Minecraft launch'
      task.progress = progress
      task.status = status
      task.speed = status === 'downloading' ? 'CDN' : '—'
      task.eta = status === 'completed' ? '—' : status === 'downloading' ? '…' : '—'

      if (status === 'completed') {
        task.id = randomUUID()
        task.name = 'Last launch completed'
      }
    })
  }
}

export const downloadService = new DownloadService()

/** @deprecated Prefer downloadService.downloadFile — kept for call-site clarity. */
export async function managedDownload(options: DownloadFileOptions): Promise<DownloadFileResult> {
  return await downloadService.downloadFile(options)
}

export { formatBytes, formatSpeed, formatEta }
