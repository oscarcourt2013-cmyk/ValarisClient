import { settingsStore } from '../storage/SettingsStore'
import { downloadService, formatBytes, type DownloadIntegrity } from '../services/DownloadService'

const CURSEFORGE_API = 'https://api.curseforge.com/v1'
const MINECRAFT_GAME_ID = 432

/** CurseForge class IDs for Minecraft content sections. */
const CLASS_IDS = {
  mod: 6,
  resourcepack: 12,
  shader: 6552
} as const

const LOADER_TYPES = {
  fabric: 4,
  forge: 1,
  quilt: 5
} as const

export interface CurseForgeSearchHit {
  project_id: string
  slug: string
  title: string
  description: string
  downloads: number
  icon_url?: string
  project_type: string
}

interface CurseForgeResponse<T> {
  data: T
}

interface CurseForgeMod {
  id: number
  slug: string
  name: string
  summary: string
  downloadCount: number
  logo?: { thumbnailUrl?: string; url?: string }
}

interface CurseForgeSearchResult {
  data: CurseForgeMod[]
  pagination: { totalCount: number }
}

interface CurseForgeFileHash {
  algo: number
  value: string
}

interface CurseForgeFile {
  id: number
  fileName: string
  downloadUrl: string
  gameVersions: string[]
  modLoaders?: { id: number; name: string }[]
  hashes?: CurseForgeFileHash[]
}

async function apiKey(): Promise<string> {
  const settings = await settingsStore.load()
  const key = settings.curseForgeApiKey?.trim()
  if (!key) {
    throw new Error('CurseForge API key is not configured in launcher settings.')
  }
  return key
}

async function curseForgeFetch<T>(path: string, params?: Record<string, string>): Promise<T> {
  const url = new URL(`${CURSEFORGE_API}${path}`)
  if (params) {
    for (const [key, value] of Object.entries(params)) {
      if (value) {
        url.searchParams.set(key, value)
      }
    }
  }

  const response = await fetch(url.toString(), {
    headers: {
      Accept: 'application/json',
      'x-api-key': await apiKey()
    }
  })

  if (!response.ok) {
    const body = await response.text().catch(() => '')
    throw new Error(`CurseForge API error (${response.status})${body ? `: ${body.slice(0, 120)}` : ''}`)
  }

  return (await response.json()) as T
}

function toHit(mod: CurseForgeMod, projectType: string): CurseForgeSearchHit {
  return {
    project_id: String(mod.id),
    slug: mod.slug,
    title: mod.name,
    description: mod.summary ?? '',
    downloads: mod.downloadCount ?? 0,
    icon_url: mod.logo?.thumbnailUrl ?? mod.logo?.url,
    project_type: projectType
  }
}

export async function searchCurseForge(
  query: string,
  projectType: 'mod' | 'resourcepack' | 'shader',
  minecraftVersion: string,
  loader?: 'fabric' | 'forge' | 'quilt'
): Promise<CurseForgeSearchHit[]> {
  const params: Record<string, string> = {
    gameId: String(MINECRAFT_GAME_ID),
    classId: String(CLASS_IDS[projectType]),
    searchFilter: query,
    gameVersion: minecraftVersion,
    sortField: '2',
    sortOrder: 'desc',
    pageSize: '20'
  }

  if (loader && projectType === 'mod') {
    params.modLoaderType = String(LOADER_TYPES[loader])
  }

  const result = await curseForgeFetch<CurseForgeSearchResult>('/mods/search', params)
  return (result.data ?? []).map((mod) => toHit(mod, projectType))
}

export async function listCurseForgeFiles(
  modId: string,
  minecraftVersion: string,
  loader?: 'fabric' | 'forge' | 'quilt'
): Promise<CurseForgeFile[]> {
  const params: Record<string, string> = {
    gameVersion: minecraftVersion,
    pageSize: '50',
    sortField: '2',
    sortOrder: 'desc'
  }
  if (loader) {
    params.modLoaderType = String(LOADER_TYPES[loader])
  }

  const result = await curseForgeFetch<CurseForgeResponse<CurseForgeFile[]>>(
    `/mods/${modId}/files`,
    params
  )
  return result.data ?? []
}

export async function getCurseForgeFileById(modId: string, fileId: number): Promise<CurseForgeFile> {
  const result = await curseForgeFetch<CurseForgeResponse<CurseForgeFile>>(`/mods/${modId}/files/${fileId}`)
  return result.data
}

export async function getCurseForgeFile(
  modId: string,
  minecraftVersion: string,
  loader?: 'fabric' | 'forge' | 'quilt'
): Promise<CurseForgeFile> {
  const files = await listCurseForgeFiles(modId, minecraftVersion, loader)
  const file = files[0]
  if (!file) {
    throw new Error(`No compatible CurseForge file for Minecraft ${minecraftVersion}.`)
  }
  return file
}

/** CurseForge hash algo: 1 = Sha1, 2 = Md5. Prefer Sha1 when present. */
export function integrityFromCurseForgeFile(file: CurseForgeFile): DownloadIntegrity | undefined {
  const sha1 = file.hashes?.find((h) => h.algo === 1)?.value
  if (sha1) {
    return { algorithm: 'sha1', hash: sha1 }
  }
  return undefined
}

export async function downloadCurseForgeFile(
  modId: string,
  fileId: number,
  destPath: string,
  onProgress?: (percent: number, speed: string) => void,
  integrity?: DownloadIntegrity
): Promise<void> {
  const result = await curseForgeFetch<CurseForgeResponse<string>>(
    `/mods/${modId}/files/${fileId}/download-url`
  )
  const url = result.data
  if (!url) {
    throw new Error('CurseForge did not return a download URL.')
  }

  let resolvedIntegrity = integrity
  if (!resolvedIntegrity) {
    try {
      const file = await getCurseForgeFileById(modId, fileId)
      resolvedIntegrity = integrityFromCurseForgeFile(file)
    } catch {
      resolvedIntegrity = undefined
    }
  }

  await downloadService.downloadFile({
    url,
    destPath,
    integrity: resolvedIntegrity,
    onProgress: (percent, speed, meta) => {
      if (!onProgress) {
        return
      }
      if (meta.total > 0) {
        onProgress(percent, `${speed} · ${formatBytes(meta.received)} / ${formatBytes(meta.total)}`)
      } else {
        onProgress(percent, speed)
      }
    }
  })
}
