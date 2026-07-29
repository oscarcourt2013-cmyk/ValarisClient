import { access } from 'fs/promises'
import { constants } from 'fs'
import { readdir, stat } from 'fs/promises'
import { join } from 'path'
import { shell } from 'electron'
import type { MediaItem } from '../../shared/content-types'
import { getInstanceGameDir } from '../minecraft/paths'
import { allowInstanceMedia, toMediaUrl } from '../protocol/mediaProtocol'
import { profileService } from './ProfileService'
import { instanceService } from './InstanceService'

function formatSize(bytes: number): string {
  if (bytes < 1024 * 1024) {
    return `${(bytes / 1024).toFixed(1)} KB`
  }
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`
}

async function fileExists(path: string): Promise<boolean> {
  try {
    await access(path, constants.F_OK)
    return true
  } catch {
    return false
  }
}

async function clipThumbnail(dir: string, stem: string): Promise<string | undefined> {
  for (const ext of ['.jpg', '.png', '.jpeg']) {
    const candidate = join(dir, `${stem}${ext}`)
    if (await fileExists(candidate)) {
      return toMediaUrl(candidate)
    }
  }
  return undefined
}

async function resolveInstanceId(instanceId?: string): Promise<string | null> {
  if (instanceId) {
    return instanceId
  }
  const profile = await profileService.getActiveProfile()
  if (profile.instanceId && (await instanceService.getStoredById(profile.instanceId))) {
    return profile.instanceId
  }
  const fallback = await instanceService.getDefault()
  return fallback?.id ?? null
}

async function scanDir(
  dir: string,
  type: MediaItem['type'],
  extensions: RegExp
): Promise<MediaItem[]> {
  const items: MediaItem[] = []

  try {
    const files = await readdir(dir)
    for (const file of files) {
      if (!extensions.test(file)) {
        continue
      }
      const filePath = join(dir, file)
      const info = await stat(filePath)
      const stem = file.replace(extensions, '')
      const thumbnailUrl =
        type === 'screenshot'
          ? toMediaUrl(filePath)
          : type === 'clip'
            ? await clipThumbnail(dir, stem)
            : undefined

      items.push({
        id: `${type}-${encodeURIComponent(file)}`,
        type,
        title: stem,
        date: info.mtime.toISOString().slice(0, 10),
        size: formatSize(info.size),
        filePath,
        thumbnailUrl,
        mediaUrl: type === 'clip' ? toMediaUrl(filePath) : undefined
      })
    }
  } catch {
    // directory missing
  }

  return items
}

/** Scans screenshots, StellarClient replays, and exported clips. */
export class MediaService {
  async list(instanceId?: string): Promise<MediaItem[]> {
    const id = await resolveInstanceId(instanceId)
    if (!id) {
      return []
    }

    allowInstanceMedia(id)
    const gameDir = getInstanceGameDir(id)

    const [screenshots, replays, clips] = await Promise.all([
      scanDir(join(gameDir, 'screenshots'), 'screenshot', /\.(png|jpg|jpeg)$/i),
      scanDir(join(gameDir, 'config', 'StellarClient', 'replays'), 'replay', /\.json$/i),
      scanDir(join(gameDir, 'config', 'StellarClient', 'clips'), 'clip', /\.(mp4|webm|mkv)$/i)
    ])

    return [...screenshots, ...replays, ...clips].sort((a, b) => b.date.localeCompare(a.date))
  }

  async openFolder(instanceId?: string): Promise<void> {
    const id = await resolveInstanceId(instanceId)
    if (!id) {
      return
    }
    const dir = join(getInstanceGameDir(id), 'config', 'StellarClient', 'clips')
    await shell.openPath(dir)
  }

  async openFile(filePath: string): Promise<void> {
    await shell.openPath(filePath)
  }
}

export const mediaService = new MediaService()
