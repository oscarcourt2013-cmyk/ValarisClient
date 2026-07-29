import { mkdir, readdir, rename, rm, copyFile } from 'fs/promises'
import { join, basename } from 'path'
import type { ModEntry } from '../../shared/content-types'
import { getModsDir } from './paths'
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

const DISABLED_SUFFIX = '.disabled'

function displayNameFromFile(fileName: string): string {
  const base = fileName.replace(/\.jar\.disabled$/i, '').replace(/\.jar$/i, '')
  const cleaned = base.replace(/[-_+]/g, ' ').replace(/\s+/g, ' ').trim()
  return cleaned || fileName
}

function versionFromFile(fileName: string): string {
  const match = fileName.match(/(\d+\.\d+(?:\.\d+)?(?:[+][\w.]+)?)/)
  return match?.[1] ?? 'unknown'
}

function modIdFromFile(fileName: string): string {
  return encodeURIComponent(fileName)
}

function enabledFileName(fileName: string): string {
  return fileName.endsWith(DISABLED_SUFFIX) ? fileName.slice(0, -DISABLED_SUFFIX.length) : fileName
}

function disabledFileName(fileName: string): string {
  if (fileName.endsWith(DISABLED_SUFFIX)) {
    return fileName
  }
  return `${fileName}${DISABLED_SUFFIX}`
}

export async function listMods(instanceId: string): Promise<ModEntry[]> {
  const modsDir = getModsDir(instanceId)
  await mkdir(modsDir, { recursive: true })

  let files: string[] = []
  try {
    files = await readdir(modsDir)
  } catch {
    return []
  }

  const meta = await readContentMeta(instanceId)
  const jarFiles = files.filter((f) => f.endsWith('.jar') || f.endsWith('.jar.disabled'))

  return jarFiles.map((fileName) => {
    const enabled = fileName.endsWith('.jar') && !fileName.endsWith('.jar.disabled')
    const canonical = enabledFileName(fileName)
    const tracked = meta.mods[canonical]
    return {
      id: modIdFromFile(canonical),
      fileName: canonical,
      name: tracked?.title ?? displayNameFromFile(canonical),
      description: tracked ? `Installed from ${tracked.source === 'curseforge' ? 'CurseForge' : 'Modrinth'}` : 'Local mod file',
      version: tracked?.versionNumber ?? versionFromFile(canonical),
      author: tracked?.source === 'curseforge' ? 'CurseForge' : tracked?.source === 'modrinth' ? 'Modrinth' : 'Unknown',
      enabled,
      source: tracked?.source ?? 'local'
    }
  })
}

export async function setModEnabled(
  instanceId: string,
  fileName: string,
  enabled: boolean
): Promise<{ ok: boolean; error?: string }> {
  const modsDir = getModsDir(instanceId)
  const currentPath = join(modsDir, enabled ? disabledFileName(fileName) : fileName)
  const targetPath = join(modsDir, enabled ? fileName : disabledFileName(fileName))

  try {
    await rename(currentPath, targetPath)
    return { ok: true }
  } catch {
    return { ok: false, error: 'Mod file not found.' }
  }
}

export async function removeMod(instanceId: string, fileName: string): Promise<{ ok: boolean; error?: string }> {
  const modsDir = getModsDir(instanceId)
  for (const candidate of [fileName, disabledFileName(fileName)]) {
    try {
      await rm(join(modsDir, candidate), { force: true })
    } catch {
      // try next
    }
  }

  await patchContentMeta(instanceId, (meta) => {
    delete meta.mods[fileName]
  })

  return { ok: true }
}

export async function importModFile(instanceId: string, sourcePath: string): Promise<{ ok: boolean; error?: string }> {
  const name = basename(sourcePath)
  if (!name.endsWith('.jar')) {
    return { ok: false, error: 'Only .jar mod files are supported.' }
  }

  const modsDir = getModsDir(instanceId)
  await mkdir(modsDir, { recursive: true })
  await copyFile(sourcePath, join(modsDir, name))
  return { ok: true }
}

export async function installModFromModrinth(
  instanceId: string,
  projectId: string,
  title: string,
  minecraftVersion: string,
  loader: 'fabric' | 'forge' | 'quilt' = 'fabric',
  versionId?: string
): Promise<{ ok: boolean; error?: string; fileName?: string }> {
  try {
    const version = versionId
      ? await getModrinthVersionById(versionId)
      : await getModrinthVersion(projectId, minecraftVersion, loader)
    const file = version.files.find((f) => f.primary) ?? version.files[0]
    if (!file) {
      return { ok: false, error: 'No downloadable file for this project.' }
    }

    const modsDir = getModsDir(instanceId)
    await mkdir(modsDir, { recursive: true })
    const dest = join(modsDir, file.filename)

    const taskId = await downloadService.beginDownload(`Mod: ${title}`)
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
      meta.mods[file.filename] = {
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

export async function installModFromCurseForge(
  instanceId: string,
  modId: string,
  title: string,
  minecraftVersion: string,
  loader: 'fabric' | 'forge' | 'quilt' = 'fabric',
  fileId?: string
): Promise<{ ok: boolean; error?: string; fileName?: string }> {
  try {
    const file = fileId
      ? await getCurseForgeFileById(modId, Number.parseInt(fileId, 10))
      : await getCurseForgeFile(modId, minecraftVersion, loader)

    const modsDir = getModsDir(instanceId)
    await mkdir(modsDir, { recursive: true })
    const dest = join(modsDir, file.fileName)

    const taskId = await downloadService.beginDownload(`Mod: ${title}`)
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
      meta.mods[file.fileName] = {
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
