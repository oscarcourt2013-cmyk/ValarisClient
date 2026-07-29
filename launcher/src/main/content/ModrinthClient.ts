import { downloadService, formatBytes, type DownloadIntegrity } from '../services/DownloadService'

const MODRINTH_API = 'https://api.modrinth.com/v2'

export interface ModrinthSearchHit {
  project_id: string
  slug: string
  title: string
  description: string
  downloads: number
  icon_url?: string
  project_type: string
}

export interface ModrinthSearchResult {
  hits: ModrinthSearchHit[]
  offset: number
  limit: number
  total_hits: number
}

export interface ModrinthVersionFile {
  url: string
  filename: string
  primary: boolean
  hashes?: {
    sha1?: string
    sha512?: string
    sha256?: string
  }
}

export interface ModrinthVersion {
  id: string
  version_number: string
  game_versions: string[]
  loaders: string[]
  date_published: string
  files: ModrinthVersionFile[]
}

export async function searchModrinth(
  query: string,
  projectType: 'mod' | 'resourcepack' | 'shader',
  minecraftVersion: string,
  loader?: 'fabric' | 'forge' | 'quilt'
): Promise<ModrinthSearchHit[]> {
  const facets: string[][] = [[`project_type:${projectType}`], [`versions:${minecraftVersion}`]]
  if (loader && projectType === 'mod') {
    facets.push([`categories:${loader}`])
  }

  const params = new URLSearchParams({
    query,
    limit: '20',
    index: 'relevance',
    facets: JSON.stringify(facets)
  })

  const response = await fetch(`${MODRINTH_API}/search?${params.toString()}`)
  if (!response.ok) {
    throw new Error(`Modrinth search failed (${response.status}).`)
  }

  const data = (await response.json()) as ModrinthSearchResult
  return Array.isArray(data.hits) ? data.hits : []
}

export async function listModrinthVersions(
  projectId: string,
  minecraftVersion: string,
  loader?: 'fabric' | 'forge' | 'quilt'
): Promise<ModrinthVersion[]> {
  const params = new URLSearchParams({
    game_versions: JSON.stringify([minecraftVersion])
  })
  if (loader) {
    params.set('loaders', JSON.stringify([loader]))
  }

  const response = await fetch(`${MODRINTH_API}/project/${projectId}/version?${params.toString()}`)
  if (!response.ok) {
    throw new Error(`Modrinth version lookup failed (${response.status}).`)
  }

  return (await response.json()) as ModrinthVersion[]
}

export async function getModrinthVersionById(versionId: string): Promise<ModrinthVersion> {
  const response = await fetch(`${MODRINTH_API}/version/${versionId}`)
  if (!response.ok) {
    throw new Error(`Modrinth version lookup failed (${response.status}).`)
  }
  return (await response.json()) as ModrinthVersion
}

export async function getModrinthVersion(
  projectId: string,
  minecraftVersion: string,
  loader?: 'fabric' | 'forge' | 'quilt'
): Promise<ModrinthVersion> {
  const versions = await listModrinthVersions(projectId, minecraftVersion, loader)
  const version = versions[0]
  if (!version) {
    throw new Error(`No compatible version for Minecraft ${minecraftVersion}.`)
  }
  return version
}

function integrityFromModrinthFile(file: ModrinthVersionFile): DownloadIntegrity | undefined {
  if (file.hashes?.sha256) {
    return { algorithm: 'sha256', hash: file.hashes.sha256 }
  }
  if (file.hashes?.sha512) {
    return { algorithm: 'sha512', hash: file.hashes.sha512 }
  }
  if (file.hashes?.sha1) {
    return { algorithm: 'sha1', hash: file.hashes.sha1 }
  }
  return undefined
}

export async function downloadModrinthFile(
  url: string,
  destPath: string,
  onProgress?: (percent: number, speed: string) => void,
  integrity?: DownloadIntegrity
): Promise<void> {
  await downloadService.downloadFile({
    url,
    destPath,
    integrity,
    onProgress: (percent, speed, meta) => {
      if (!onProgress) {
        return
      }
      if (meta.total > 0) {
        const elapsedHint = meta.received > 0 ? speed : '—'
        onProgress(percent, `${elapsedHint} · ${formatBytes(meta.received)} / ${formatBytes(meta.total)}`)
      } else {
        onProgress(percent, speed)
      }
    }
  })
}

export { integrityFromModrinthFile }
