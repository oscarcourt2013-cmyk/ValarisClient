import { app, dialog } from 'electron'
import { copyFile, mkdir, readdir, readFile, unlink, writeFile } from 'fs/promises'
import { basename, extname, join } from 'path'
import { randomUUID } from 'crypto'
import { settingsStore } from '../storage/SettingsStore'

export interface LocalSkinDto {
  id: string
  name: string
  fileName: string
  /** data: URL for the skinview3d canvas */
  dataUrl: string
  createdAt: string
}

interface SkinManifest {
  version: 1
  skins: Array<{ id: string; name: string; fileName: string; createdAt: string }>
}

function skinsDir(): string {
  return join(app.getPath('userData'), 'skins')
}

function manifestPath(): string {
  return join(skinsDir(), 'manifest.json')
}

async function ensureDir(): Promise<void> {
  await mkdir(skinsDir(), { recursive: true })
}

async function loadManifest(): Promise<SkinManifest> {
  await ensureDir()
  try {
    const raw = await readFile(manifestPath(), 'utf8')
    return JSON.parse(raw) as SkinManifest
  } catch {
    return { version: 1, skins: [] }
  }
}

async function saveManifest(manifest: SkinManifest): Promise<void> {
  await ensureDir()
  await writeFile(manifestPath(), JSON.stringify(manifest, null, 2), 'utf8')
}

async function toDataUrl(filePath: string): Promise<string> {
  const buf = await readFile(filePath)
  return `data:image/png;base64,${buf.toString('base64')}`
}

export class SkinLibraryService {
  async list(): Promise<LocalSkinDto[]> {
    const manifest = await loadManifest()
    const out: LocalSkinDto[] = []
    for (const entry of manifest.skins) {
      try {
        const dataUrl = await toDataUrl(join(skinsDir(), entry.fileName))
        out.push({ ...entry, dataUrl })
      } catch {
        // skip missing files
      }
    }
    return out
  }

  async importPng(): Promise<{ ok: boolean; skin?: LocalSkinDto; error?: string }> {
    const result = await dialog.showOpenDialog({
      title: 'Import Minecraft skin',
      filters: [{ name: 'PNG Skin', extensions: ['png'] }],
      properties: ['openFile']
    })
    if (result.canceled || !result.filePaths[0]) {
      return { ok: false, error: 'Cancelled.' }
    }

    const source = result.filePaths[0]!
    const id = randomUUID()
    const fileName = `${id}.png`
    await ensureDir()
    await copyFile(source, join(skinsDir(), fileName))

    const name = basename(source, extname(source)).slice(0, 32) || 'Custom Skin'
    const createdAt = new Date().toISOString()
    const manifest = await loadManifest()
    manifest.skins.unshift({ id, name, fileName, createdAt })
    await saveManifest(manifest)

    const dataUrl = await toDataUrl(join(skinsDir(), fileName))
    return { ok: true, skin: { id, name, fileName, dataUrl, createdAt } }
  }

  async remove(id: string): Promise<{ ok: boolean; error?: string }> {
    const manifest = await loadManifest()
    const entry = manifest.skins.find((s) => s.id === id)
    if (!entry) {
      return { ok: false, error: 'Skin not found.' }
    }
    manifest.skins = manifest.skins.filter((s) => s.id !== id)
    await saveManifest(manifest)
    await unlink(join(skinsDir(), entry.fileName)).catch(() => undefined)

    const settings = await settingsStore.load()
    if (settings.activeSkinId === id) {
      await settingsStore.mutate((s) => {
        s.activeSkinId = null
      })
    }
    return { ok: true }
  }

  async setActive(id: string | null): Promise<{ ok: boolean }> {
    if (id) {
      const manifest = await loadManifest()
      if (!manifest.skins.some((s) => s.id === id)) {
        return { ok: false }
      }
    }
    await settingsStore.mutate((s) => {
      s.activeSkinId = id
    })
    return { ok: true }
  }

  async getActiveDataUrl(): Promise<string | null> {
    const settings = await settingsStore.load()
    if (!settings.activeSkinId) {
      return null
    }
    const list = await this.list()
    return list.find((s) => s.id === settings.activeSkinId)?.dataUrl ?? null
  }

  /** Dev helper — scan loose pngs not in manifest (noop for now). */
  async reconcile(): Promise<void> {
    await ensureDir()
    const files = await readdir(skinsDir())
    const pngs = files.filter((f) => f.endsWith('.png'))
    const manifest = await loadManifest()
    const known = new Set(manifest.skins.map((s) => s.fileName))
    for (const file of pngs) {
      if (known.has(file)) continue
      const id = file.replace(/\.png$/i, '')
      manifest.skins.push({
        id,
        name: id.slice(0, 8),
        fileName: file,
        createdAt: new Date().toISOString()
      })
    }
    await saveManifest(manifest)
  }
}

export const skinLibraryService = new SkinLibraryService()
