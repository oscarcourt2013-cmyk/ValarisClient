import { mkdir, readFile, writeFile } from 'fs/promises'
import { dirname } from 'path'
import { getContentMetaPath } from './paths'

export interface ModMeta {
  projectId: string
  title: string
  versionNumber: string
  source: 'modrinth' | 'curseforge'
}

export interface PackMeta {
  projectId: string
  title: string
  versionNumber: string
  source: 'modrinth' | 'curseforge'
}

export interface ContentMetaFile {
  version: 1
  mods: Record<string, ModMeta>
  resourcePacks: Record<string, PackMeta>
  shaders: Record<string, PackMeta>
}

const EMPTY: ContentMetaFile = {
  version: 1,
  mods: {},
  resourcePacks: {},
  shaders: {}
}

export async function readContentMeta(instanceId: string): Promise<ContentMetaFile> {
  try {
    const raw = await readFile(getContentMetaPath(instanceId), 'utf8')
    return { ...EMPTY, ...(JSON.parse(raw) as ContentMetaFile) }
  } catch {
    return { ...EMPTY }
  }
}

export async function writeContentMeta(instanceId: string, meta: ContentMetaFile): Promise<void> {
  const path = getContentMetaPath(instanceId)
  await mkdir(dirname(path), { recursive: true })
  await writeFile(path, JSON.stringify(meta, null, 2), 'utf8')
}

export async function patchContentMeta(
  instanceId: string,
  fn: (meta: ContentMetaFile) => void
): Promise<ContentMetaFile> {
  const meta = await readContentMeta(instanceId)
  fn(meta)
  await writeContentMeta(instanceId, meta)
  return meta
}
