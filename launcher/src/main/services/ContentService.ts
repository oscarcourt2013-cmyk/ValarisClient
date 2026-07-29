import { dialog } from 'electron'
import { profileService } from './ProfileService'
import { instanceService } from './InstanceService'
import { downloadService } from './DownloadService'
import type { ModEntry, ResourcePackEntry, ShaderEntry } from '../../shared/content-types'
import type { ContentVersionDto } from '../../shared/ipc'
import type { ModrinthSearchHit } from '../content/ModrinthClient'
import { searchModrinth, listModrinthVersions } from '../content/ModrinthClient'
import type { CurseForgeSearchHit } from '../content/CurseForgeClient'
import { searchCurseForge, listCurseForgeFiles } from '../content/CurseForgeClient'
import * as ModManager from '../content/ModManager'
import * as ResourcePackManager from '../content/ResourcePackManager'
import * as ShaderManager from '../content/ShaderManager'

async function resolveInstanceId(instanceId?: string): Promise<string> {
  if (instanceId) {
    return instanceId
  }
  const profile = await profileService.getActiveProfile()
  if (profile.instanceId) {
    const inst = await instanceService.getStoredById(profile.instanceId)
    if (inst) {
      return profile.instanceId
    }
  }
  const fallback = await instanceService.getDefault()
  if (!fallback) {
    throw new Error('No instance available.')
  }
  return fallback.id
}

async function loaderForInstance(instanceId: string): Promise<'fabric' | 'forge' | 'quilt'> {
  const stored = await instanceService.getStoredById(instanceId)
  if (stored?.loader === 'fabric') {
    return 'fabric'
  }
  return 'fabric'
}

/** Mods, resource packs, shaders — local files + Modrinth public API. */
export class ContentService {
  async listMods(instanceId?: string): Promise<ModEntry[]> {
    return ModManager.listMods(await resolveInstanceId(instanceId))
  }

  async setModEnabled(fileName: string, enabled: boolean, instanceId?: string) {
    return ModManager.setModEnabled(await resolveInstanceId(instanceId), fileName, enabled)
  }

  async removeMod(fileName: string, instanceId?: string) {
    return ModManager.removeMod(await resolveInstanceId(instanceId), fileName)
  }

  async importMod(instanceId?: string) {
    const id = await resolveInstanceId(instanceId)
    const { canceled, filePaths } = await dialog.showOpenDialog({
      title: 'Import mod (.jar)',
      properties: ['openFile'],
      filters: [{ name: 'Fabric / Forge mods', extensions: ['jar'] }]
    })
    if (canceled || !filePaths[0]) {
      return { ok: false, error: 'Cancelled.' }
    }
    return ModManager.importModFile(id, filePaths[0])
  }

  async installModFromModrinth(projectId: string, title: string, instanceId?: string, versionId?: string) {
    try {
      const id = await resolveInstanceId(instanceId)
      const stored = await instanceService.getStoredById(id)
      if (!stored) {
        return { ok: false, error: 'Instance not found.' }
      }
      const result = await ModManager.installModFromModrinth(
        id,
        projectId,
        title,
        stored.minecraftVersion,
        await loaderForInstance(id),
        versionId
      )
      if (result.ok) {
        await downloadService.trackContentInstall(`Mod: ${title}`)
      }
      return result
    } catch (err) {
      return { ok: false, error: err instanceof Error ? err.message : 'Install failed.' }
    }
  }

  async installModFromCurseForge(projectId: string, title: string, instanceId?: string, fileId?: string) {
    try {
      const id = await resolveInstanceId(instanceId)
      const stored = await instanceService.getStoredById(id)
      if (!stored) {
        return { ok: false, error: 'Instance not found.' }
      }
      const result = await ModManager.installModFromCurseForge(
        id,
        projectId,
        title,
        stored.minecraftVersion,
        await loaderForInstance(id),
        fileId
      )
      if (result.ok) {
        await downloadService.trackContentInstall(`Mod: ${title}`)
      }
      return result
    } catch (err) {
      return { ok: false, error: err instanceof Error ? err.message : 'Install failed.' }
    }
  }

  async listResourcePacks(instanceId?: string): Promise<ResourcePackEntry[]> {
    return ResourcePackManager.listResourcePacks(await resolveInstanceId(instanceId))
  }

  async setResourcePackActive(fileName: string | null, instanceId?: string) {
    return ResourcePackManager.setResourcePackActive(await resolveInstanceId(instanceId), fileName)
  }

  async importResourcePack(instanceId?: string) {
    const id = await resolveInstanceId(instanceId)
    const { canceled, filePaths } = await dialog.showOpenDialog({
      title: 'Import resource pack (.zip)',
      properties: ['openFile'],
      filters: [{ name: 'Resource packs', extensions: ['zip'] }]
    })
    if (canceled || !filePaths[0]) {
      return { ok: false, error: 'Cancelled.' }
    }
    return ResourcePackManager.importResourcePack(id, filePaths[0])
  }

  async removeResourcePack(fileName: string, instanceId?: string) {
    return ResourcePackManager.removeResourcePack(await resolveInstanceId(instanceId), fileName)
  }

  async installResourcePackFromModrinth(projectId: string, title: string, instanceId?: string, versionId?: string) {
    try {
      const id = await resolveInstanceId(instanceId)
      const stored = await instanceService.getStoredById(id)
      if (!stored) {
        return { ok: false, error: 'Instance not found.' }
      }
      const result = await ResourcePackManager.installResourcePackFromModrinth(
        id,
        projectId,
        title,
        stored.minecraftVersion,
        versionId
      )
      if (result.ok) {
        await downloadService.trackContentInstall(`Resource pack: ${title}`)
      }
      return result
    } catch (err) {
      return { ok: false, error: err instanceof Error ? err.message : 'Install failed.' }
    }
  }

  async installResourcePackFromCurseForge(projectId: string, title: string, instanceId?: string, fileId?: string) {
    try {
      const id = await resolveInstanceId(instanceId)
      const stored = await instanceService.getStoredById(id)
      if (!stored) {
        return { ok: false, error: 'Instance not found.' }
      }
      const result = await ResourcePackManager.installResourcePackFromCurseForge(
        id,
        projectId,
        title,
        stored.minecraftVersion,
        fileId
      )
      if (result.ok) {
        await downloadService.trackContentInstall(`Resource pack: ${title}`)
      }
      return result
    } catch (err) {
      return { ok: false, error: err instanceof Error ? err.message : 'Install failed.' }
    }
  }

  async listShaders(instanceId?: string): Promise<ShaderEntry[]> {
    return ShaderManager.listShaders(await resolveInstanceId(instanceId))
  }

  async setShaderActive(fileName: string | null, instanceId?: string) {
    return ShaderManager.setShaderActive(await resolveInstanceId(instanceId), fileName)
  }

  async importShader(instanceId?: string) {
    const id = await resolveInstanceId(instanceId)
    const { canceled, filePaths } = await dialog.showOpenDialog({
      title: 'Import shader pack (.zip)',
      properties: ['openFile'],
      filters: [{ name: 'Shader packs', extensions: ['zip'] }]
    })
    if (canceled || !filePaths[0]) {
      return { ok: false, error: 'Cancelled.' }
    }
    return ShaderManager.importShaderPack(id, filePaths[0])
  }

  async removeShader(fileName: string, instanceId?: string) {
    return ShaderManager.removeShaderPack(await resolveInstanceId(instanceId), fileName)
  }

  async installShaderFromModrinth(projectId: string, title: string, instanceId?: string, versionId?: string) {
    try {
      const id = await resolveInstanceId(instanceId)
      const stored = await instanceService.getStoredById(id)
      if (!stored) {
        return { ok: false, error: 'Instance not found.' }
      }
      const result = await ShaderManager.installShaderFromModrinth(
        id,
        projectId,
        title,
        stored.minecraftVersion,
        versionId
      )
      if (result.ok) {
        await downloadService.trackContentInstall(`Shader: ${title}`)
      }
      return result
    } catch (err) {
      return { ok: false, error: err instanceof Error ? err.message : 'Install failed.' }
    }
  }

  async installShaderFromCurseForge(projectId: string, title: string, instanceId?: string, fileId?: string) {
    try {
      const id = await resolveInstanceId(instanceId)
      const stored = await instanceService.getStoredById(id)
      if (!stored) {
        return { ok: false, error: 'Instance not found.' }
      }
      const result = await ShaderManager.installShaderFromCurseForge(
        id,
        projectId,
        title,
        stored.minecraftVersion,
        fileId
      )
      if (result.ok) {
        await downloadService.trackContentInstall(`Shader: ${title}`)
      }
      return result
    } catch (err) {
      return { ok: false, error: err instanceof Error ? err.message : 'Install failed.' }
    }
  }

  async searchModrinth(
    query: string,
    type: 'mod' | 'resourcepack' | 'shader',
    instanceId?: string
  ): Promise<ModrinthSearchHit[]> {
    try {
      const id = await resolveInstanceId(instanceId)
      const stored = await instanceService.getStoredById(id)
      if (!stored) {
        return []
      }
      const loader = type === 'mod' ? await loaderForInstance(id) : undefined
      return searchModrinth(query, type, stored.minecraftVersion, loader)
    } catch (err) {
      throw err instanceof Error ? err : new Error('Search failed.')
    }
  }

  async searchCurseForge(
    query: string,
    type: 'mod' | 'resourcepack' | 'shader',
    instanceId?: string
  ): Promise<CurseForgeSearchHit[]> {
    try {
      const id = await resolveInstanceId(instanceId)
      const stored = await instanceService.getStoredById(id)
      if (!stored) {
        return []
      }
      const loader = type === 'mod' ? await loaderForInstance(id) : undefined
      return searchCurseForge(query, type, stored.minecraftVersion, loader)
    } catch (err) {
      throw err instanceof Error ? err : new Error('Search failed.')
    }
  }

  async listContentVersions(
    projectId: string,
    type: 'mod' | 'resourcepack' | 'shader',
    source: 'modrinth' | 'curseforge',
    instanceId?: string
  ): Promise<ContentVersionDto[]> {
    try {
      const id = await resolveInstanceId(instanceId)
      const stored = await instanceService.getStoredById(id)
      if (!stored) {
        return []
      }

      const loader = type === 'mod' ? await loaderForInstance(id) : undefined

      if (source === 'modrinth') {
        const versions = await listModrinthVersions(projectId, stored.minecraftVersion, loader)
        return (Array.isArray(versions) ? versions : []).map((version, index) => {
          const files = Array.isArray(version.files) ? version.files : []
          const file = files.find((f) => f.primary) ?? files[0]
          return {
            id: version.id,
            versionNumber: version.version_number ?? version.id,
            gameVersions: Array.isArray(version.game_versions) ? version.game_versions : [],
            loaders: Array.isArray(version.loaders) ? version.loaders : [],
            fileName: file?.filename,
            recommended: index === 0
          }
        })
      }

      const files = await listCurseForgeFiles(projectId, stored.minecraftVersion, loader)
      return (Array.isArray(files) ? files : []).map((file, index) => ({
        id: String(file.id),
        versionNumber: (file.fileName ?? String(file.id)).replace(/\.jar$|\.zip$/i, ''),
        gameVersions: Array.isArray(file.gameVersions) ? file.gameVersions : [stored.minecraftVersion],
        loaders: file.modLoaders?.map((entry) => entry.name.toLowerCase()) ?? [],
        fileName: file.fileName,
        recommended: index === 0
      }))
    } catch (err) {
      throw err instanceof Error ? err : new Error('Could not load versions.')
    }
  }
}

export const contentService = new ContentService()
