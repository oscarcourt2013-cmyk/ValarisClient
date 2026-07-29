import { mkdir, readdir, copyFile, rm } from 'fs/promises'
import { join, basename } from 'path'
import type { ResourcePackEntry } from '../../shared/content-types'
import { getResourcePacksDir } from './paths'
import { getActiveResourcePackFile, setActiveResourcePack } from './options'
import { patchContentMeta, readContentMeta } from './contentMeta'
import {
  downloadModrinthFile,
  getModrinthVersion,
  getModrinthVersionById,
  integrityFromModrinthFile
} from './ModrinthClient'
import {
  downloadCurseForgeFile,
  getCurseForgeFile,
  getCurseForgeFileById,
  integrityFromCurseForgeFile
} from './CurseForgeClient'
import { downloadService } from '../services/DownloadService'

function packId(fileName: string): string {
  return encodeURIComponent(fileName)
}

function displayName(fileName: string, title?: string): string {
  return title ?? fileName.replace(/\.zip$/i, '').replace(/[-_+]/g, ' ').trim()
}

export async function listResourcePacks(instanceId: string): Promise<ResourcePackEntry[]> {
  const dir = getResourcePacksDir(instanceId)
  await mkdir(dir, { recursive: true })

  let files: string[] = []
  try {
    files = await readdir(dir)
  } catch {
    return []
  }

  const meta = await readContentMeta(instanceId)
  const activeFile = await getActiveResourcePackFile(instanceId)
  const packs = files.filter((f) => f.endsWith('.zip'))

  return packs.map((fileName) => {
    const tracked = meta.resourcePacks[fileName]
    return {
      id: packId(fileName),
      fileName,
      name: displayName(fileName, tracked?.title),
      description: tracked ? 'Installed from Modrinth' : 'Local resource pack',
      resolution: 'Mixed',
      active: activeFile === fileName
    }
  })
}

export async function setResourcePackActive(
  instanceId: string,
  fileName: string | null
): Promise<{ ok: boolean; error?: string }> {
  try {
    await setActiveResourcePack(instanceId, fileName)
    return { ok: true }
  } catch (err) {
    return { ok: false, error: err instanceof Error ? err.message : 'Could not update options.txt' }
  }
}

export async function importResourcePack(
  instanceId: string,
  sourcePath: string
): Promise<{ ok: boolean; error?: string }> {
  const name = basename(sourcePath)
  if (!name.endsWith('.zip')) {
    return { ok: false, error: 'Resource packs must be .zip files.' }
  }

  const dir = getResourcePacksDir(instanceId)
  await mkdir(dir, { recursive: true })
  await copyFile(sourcePath, join(dir, name))
  return { ok: true }
}

export async function removeResourcePack(
  instanceId: string,
  fileName: string
): Promise<{ ok: boolean; error?: string }> {
  const active = await getActiveResourcePackFile(instanceId)
  if (active === fileName) {
    await setActiveResourcePack(instanceId, null)
  }

  await rm(join(getResourcePacksDir(instanceId), fileName), { force: true })
  await patchContentMeta(instanceId, (meta) => {
    delete meta.resourcePacks[fileName]
  })
  return { ok: true }
}

export async function installResourcePackFromModrinth(
  instanceId: string,
  projectId: string,
  title: string,
  minecraftVersion: string,
  versionId?: string
): Promise<{ ok: boolean; error?: string; fileName?: string }> {
  try {
    const version = versionId
      ? await getModrinthVersionById(versionId)
      : await getModrinthVersion(projectId, minecraftVersion)
    const file = version.files.find((f) => f.primary) ?? version.files[0]
    if (!file) {
      return { ok: false, error: 'No downloadable file for this project.' }
    }

    const dir = getResourcePacksDir(instanceId)
    await mkdir(dir, { recursive: true })
    const dest = join(dir, file.filename)
    const taskId = await downloadService.beginDownload(`Resource pack: ${title}`)
    try {
      await downloadModrinthFile(
        file.url,
        dest,
        (percent, speed) => {
          void downloadService.updateDownload(taskId, percent, speed)
        },
        integrityFromModrinthFile(file)
      )
    } catch (err) {
      await downloadService.failDownload(taskId, err instanceof Error ? err.message : 'Download failed')
      throw err
    }

    await patchContentMeta(instanceId, (meta) => {
      meta.resourcePacks[file.filename] = {
        projectId,
        title,
        versionNumber: version.version_number,
        source: 'modrinth'
      }
    })

    return { ok: true, fileName: file.filename }
  } catch (err) {
    return { ok: false, error: err instanceof Error ? err.message : 'Install failed.' }
  }
}

export async function installResourcePackFromCurseForge(
  instanceId: string,
  modId: string,
  title: string,
  minecraftVersion: string,
  fileId?: string
): Promise<{ ok: boolean; error?: string; fileName?: string }> {
  try {
    const file = fileId
      ? await getCurseForgeFileById(modId, Number.parseInt(fileId, 10))
      : await getCurseForgeFile(modId, minecraftVersion)
    const dir = getResourcePacksDir(instanceId)
    await mkdir(dir, { recursive: true })
    const dest = join(dir, file.fileName)
    const taskId = await downloadService.beginDownload(`Resource pack: ${title}`)
    try {
      await downloadCurseForgeFile(
        modId,
        file.id,
        dest,
        (percent, speed) => {
          void downloadService.updateDownload(taskId, percent, speed)
        },
        integrityFromCurseForgeFile(file)
      )
    } catch (err) {
      await downloadService.failDownload(taskId, err instanceof Error ? err.message : 'Download failed')
      throw err
    }

    await patchContentMeta(instanceId, (meta) => {
      meta.resourcePacks[file.fileName] = {
        projectId: modId,
        title,
        versionNumber: file.fileName,
        source: 'curseforge'
      }
    })

    return { ok: true, fileName: file.fileName }
  } catch (err) {
    return { ok: false, error: err instanceof Error ? err.message : 'Install failed.' }
  }
}
