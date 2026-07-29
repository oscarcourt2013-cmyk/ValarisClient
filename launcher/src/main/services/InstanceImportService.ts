import { createHash } from 'crypto'
import { access, cp, mkdir, readdir, readFile, stat } from 'fs/promises'
import { basename, join } from 'path'
import { homedir } from 'os'
import type {
  DetectedImportInstanceDto,
  DetectedImportLauncherDto,
  ImportInstanceResultDto,
  ImportLauncherId
} from '../../shared/ipc'
import { getInstanceGameDir } from '../minecraft/paths'
import { settingsStore } from '../storage/SettingsStore'
import { instanceService } from './InstanceService'

const COPY_DIRS = ['mods', 'resourcepacks', 'screenshots'] as const
const COPY_FILES = ['options.txt'] as const

interface ParsedInstance {
  name: string
  minecraftVersion: string
  loader: 'vanilla' | 'fabric'
  fabricLoaderVersion?: string
  ramMb?: number
  gameDir: string
}

function appData(): string {
  return process.env.APPDATA || join(homedir(), 'AppData', 'Roaming')
}

function localAppData(): string {
  return process.env.LOCALAPPDATA || join(homedir(), 'AppData', 'Local')
}

function userProfile(): string {
  return process.env.USERPROFILE || homedir()
}

async function exists(path: string): Promise<boolean> {
  try {
    await access(path)
    return true
  } catch {
    return false
  }
}

async function isDir(path: string): Promise<boolean> {
  try {
    return (await stat(path)).isDirectory()
  } catch {
    return false
  }
}

function makeId(source: ImportLauncherId, root: string, name: string): string {
  return createHash('sha1').update(`${source}|${root}|${name}`).digest('hex').slice(0, 16)
}

function parseIni(raw: string): Record<string, string> {
  const out: Record<string, string> = {}
  for (const line of raw.split(/\r?\n/)) {
    const trimmed = line.trim()
    if (!trimmed || trimmed.startsWith('#') || trimmed.startsWith('[')) continue
    const eq = trimmed.indexOf('=')
    if (eq < 0) continue
    out[trimmed.slice(0, eq).trim()] = trimmed.slice(eq + 1).trim()
  }
  return out
}

async function resolveMinecraftGameDir(instanceDir: string): Promise<string> {
  const candidates = [join(instanceDir, '.minecraft'), join(instanceDir, 'minecraft'), instanceDir]
  for (const dir of candidates) {
    if (!(await isDir(dir))) continue
    if (
      (await exists(join(dir, 'mods'))) ||
      (await exists(join(dir, 'options.txt'))) ||
      (await exists(join(dir, 'saves'))) ||
      (await exists(join(dir, 'resourcepacks')))
    ) {
      return dir
    }
  }
  return (await isDir(join(instanceDir, '.minecraft')))
    ? join(instanceDir, '.minecraft')
    : instanceDir
}

async function folderFlags(gameDir: string): Promise<{
  hasMods: boolean
  hasResourcePacks: boolean
  hasScreenshots: boolean
  hasOptions: boolean
}> {
  return {
    hasMods: await exists(join(gameDir, 'mods')),
    hasResourcePacks: await exists(join(gameDir, 'resourcepacks')),
    hasScreenshots: await exists(join(gameDir, 'screenshots')),
    hasOptions: await exists(join(gameDir, 'options.txt'))
  }
}

async function parseMmcPack(instanceDir: string): Promise<{
  minecraftVersion: string
  loader: 'vanilla' | 'fabric'
  fabricLoaderVersion?: string
} | null> {
  try {
    const raw = await readFile(join(instanceDir, 'mmc-pack.json'), 'utf8')
    const pack = JSON.parse(raw) as {
      components?: Array<{ uid?: string; version?: string; cachedName?: string }>
    }
    const components = pack.components ?? []
    const mc = components.find((c) => c.uid === 'net.minecraft')
    const fabric = components.find(
      (c) =>
        c.uid === 'net.fabricmc.fabric-loader' ||
        c.uid === 'net.fabricmc.fabric-loader-intermediary' ||
        (c.cachedName || '').toLowerCase().includes('fabric loader')
    )
    const version = mc?.version?.trim()
    if (!version) return null
    return {
      minecraftVersion: version,
      loader: fabric?.version ? 'fabric' : 'vanilla',
      fabricLoaderVersion: fabric?.version
    }
  } catch {
    return null
  }
}

async function parsePrismLikeInstance(instanceDir: string): Promise<ParsedInstance | null> {
  const cfgPath = join(instanceDir, 'instance.cfg')
  if (!(await exists(cfgPath)) && !(await exists(join(instanceDir, 'mmc-pack.json')))) {
    return null
  }

  let name = basename(instanceDir)
  let intendedVersion = ''
  let ramMb: number | undefined

  if (await exists(cfgPath)) {
    try {
      const cfg = parseIni(await readFile(cfgPath, 'utf8'))
      name = cfg.name || cfg.InstanceName || name
      intendedVersion = cfg.IntendedVersion || cfg.MinecraftVersion || ''
      const maxMem = Number(cfg.MaxMemAlloc || cfg.MaxMemory || '')
      if (Number.isFinite(maxMem) && maxMem >= 512) {
        ramMb = maxMem
      }
    } catch {
      // keep defaults
    }
  }

  const pack = await parseMmcPack(instanceDir)
  const minecraftVersion = pack?.minecraftVersion || intendedVersion || '1.21.11'
  const gameDir = await resolveMinecraftGameDir(instanceDir)

  return {
    name: name.slice(0, 32),
    minecraftVersion,
    loader: pack?.loader ?? 'vanilla',
    fabricLoaderVersion: pack?.fabricLoaderVersion,
    ramMb,
    gameDir
  }
}

async function listPrismLike(root: string): Promise<ParsedInstance[]> {
  if (!(await isDir(root))) return []
  const entries = await readdir(root, { withFileTypes: true })
  const out: ParsedInstance[] = []
  for (const entry of entries) {
    if (!entry.isDirectory() || entry.name.startsWith('.')) continue
    const parsed = await parsePrismLikeInstance(join(root, entry.name))
    if (parsed) out.push(parsed)
  }
  return out
}

async function firstExisting(paths: string[]): Promise<string | null> {
  for (const path of paths) {
    if (await isDir(path)) return path
  }
  return null
}

const LAUNCHER_ROOTS: Record<ImportLauncherId, () => string[]> = {
  prism: () => [
    join(appData(), 'PrismLauncher', 'instances'),
    join(localAppData(), 'PrismLauncher', 'instances'),
    join(userProfile(), 'PrismLauncher', 'instances')
  ],
  multimc: () => [
    join(appData(), 'MultiMC', 'instances'),
    join(appData(), 'multimc', 'instances'),
    join(localAppData(), 'MultiMC', 'instances'),
    join(userProfile(), 'MultiMC', 'instances')
  ],
  lunar: () => [
    join(userProfile(), '.lunarclient'),
    join(appData(), 'lunarclient'),
    join(localAppData(), 'lunarclient')
  ],
  feather: () => [
    join(appData(), 'Feather', 'instances'),
    join(appData(), 'Feather Launcher', 'instances'),
    join(appData(), 'feather', 'instances'),
    join(localAppData(), 'Feather', 'instances'),
    join(userProfile(), '.feather', 'instances')
  ],
  dawn: () => [
    join(appData(), 'DawnClient', 'instances'),
    join(appData(), 'Dawn', 'instances'),
    join(appData(), 'dawnclient', 'instances'),
    join(localAppData(), 'DawnClient', 'instances'),
    join(userProfile(), '.dawnclient', 'instances')
  ]
}

const LAUNCHER_LABELS: Record<ImportLauncherId, string> = {
  prism: 'Prism Launcher',
  multimc: 'MultiMC',
  lunar: 'Lunar Client',
  feather: 'Feather',
  dawn: 'Dawn Client'
}

async function listLunarInstances(root: string): Promise<ParsedInstance[]> {
  const out: ParsedInstance[] = []
  const offlineRoots = [
    join(root, 'offline', 'multiver'),
    join(root, 'offline'),
    join(root, 'game-cache')
  ]

  for (const offline of offlineRoots) {
    if (!(await isDir(offline))) continue
    const entries = await readdir(offline, { withFileTypes: true })
    for (const entry of entries) {
      if (!entry.isDirectory()) continue
      const dir = join(offline, entry.name)
      const versionGuess = entry.name.replace(/^v/i, '')
      const looksLikeVersion = /^\d+(\.\d+){1,3}/.test(versionGuess)
      const gameDir =
        (await exists(join(dir, 'options.txt'))) || (await exists(join(dir, 'mods')))
          ? dir
          : (await resolveMinecraftGameDir(dir))

      if (!looksLikeVersion && !(await exists(join(gameDir, 'options.txt'))) && !(await exists(join(gameDir, 'mods')))) {
        continue
      }

      out.push({
        name: `Lunar ${looksLikeVersion ? versionGuess : entry.name}`.slice(0, 32),
        minecraftVersion: looksLikeVersion ? versionGuess : '1.21.11',
        loader: 'vanilla',
        gameDir
      })
    }
  }

  // Shared settings folder as a last-resort profile
  const sharedGame = join(root, 'settings', 'game')
  if ((await isDir(sharedGame)) && out.length === 0) {
    out.push({
      name: 'Lunar Shared',
      minecraftVersion: '1.21.11',
      loader: 'vanilla',
      gameDir: sharedGame
    })
  }

  // Dedupe by gameDir
  const seen = new Set<string>()
  return out.filter((item) => {
    if (seen.has(item.gameDir)) return false
    seen.add(item.gameDir)
    return true
  })
}

async function listSourceInstances(source: ImportLauncherId, root: string): Promise<ParsedInstance[]> {
  if (source === 'lunar') {
    return listLunarInstances(root)
  }
  return listPrismLike(root)
}

async function toDto(
  source: ImportLauncherId,
  root: string,
  parsed: ParsedInstance
): Promise<DetectedImportInstanceDto> {
  const flags = await folderFlags(parsed.gameDir)
  return {
    id: makeId(source, root, `${parsed.name}|${parsed.gameDir}`),
    source,
    name: parsed.name,
    minecraftVersion: parsed.minecraftVersion,
    loader: parsed.loader,
    fabricLoaderVersion: parsed.fabricLoaderVersion,
    gameDir: parsed.gameDir,
    ramMb: parsed.ramMb,
    ...flags
  }
}

async function copyContent(sourceGameDir: string, destGameDir: string): Promise<void> {
  await mkdir(destGameDir, { recursive: true })

  for (const dir of COPY_DIRS) {
    const from = join(sourceGameDir, dir)
    if (!(await isDir(from))) continue
    await cp(from, join(destGameDir, dir), { recursive: true, force: true })
  }

  for (const file of COPY_FILES) {
    const from = join(sourceGameDir, file)
    if (!(await exists(from))) continue
    await cp(from, join(destGameDir, file), { force: true })
  }
}

export class InstanceImportService {
  async detect(): Promise<DetectedImportLauncherDto[]> {
    const sources = Object.keys(LAUNCHER_ROOTS) as ImportLauncherId[]
    const results: DetectedImportLauncherDto[] = []

    for (const id of sources) {
      const root = await firstExisting(LAUNCHER_ROOTS[id]())
      if (!root) {
        results.push({
          id,
          label: LAUNCHER_LABELS[id],
          rootPath: LAUNCHER_ROOTS[id]()[0]!,
          instanceCount: 0,
          available: false
        })
        continue
      }
      const instances = await listSourceInstances(id, root)
      results.push({
        id,
        label: LAUNCHER_LABELS[id],
        rootPath: root,
        instanceCount: instances.length,
        available: instances.length > 0
      })
    }

    return results
  }

  async list(source: ImportLauncherId): Promise<DetectedImportInstanceDto[]> {
    const root = await firstExisting(LAUNCHER_ROOTS[source]())
    if (!root) return []
    const parsed = await listSourceInstances(source, root)
    return Promise.all(parsed.map((item) => toDto(source, root, item)))
  }

  async importOne(instanceId: string, source: ImportLauncherId): Promise<ImportInstanceResultDto> {
    const candidates = await this.list(source)
    const found = candidates.find((item) => item.id === instanceId)
    if (!found) {
      return { ok: false, error: 'Instance not found for import.' }
    }

    const settings = await settingsStore.load()
    const created = await instanceService.create({
      name: found.name,
      minecraftVersion: found.minecraftVersion,
      loader: found.loader,
      fabricLoaderVersion: found.fabricLoaderVersion,
      includePrimeMod: false,
      ramMb: found.ramMb || settings.defaultRamMb
    })

    if (!created.ok || !created.instance) {
      return { ok: false, error: created.error ?? 'Failed to create instance.' }
    }

    try {
      await copyContent(found.gameDir, getInstanceGameDir(created.instance.id))
    } catch (err) {
      return {
        ok: false,
        error: err instanceof Error ? err.message : 'Failed to copy instance files.',
        instanceId: created.instance.id
      }
    }

    return { ok: true, instanceId: created.instance.id }
  }

  async importMany(
    source: ImportLauncherId,
    instanceIds: string[]
  ): Promise<{ ok: boolean; imported: ImportInstanceResultDto[]; error?: string }> {
    if (!instanceIds.length) {
      return { ok: false, imported: [], error: 'No instances selected.' }
    }
    const imported: ImportInstanceResultDto[] = []
    for (const id of instanceIds) {
      imported.push(await this.importOne(id, source))
    }
    const anyOk = imported.some((r) => r.ok)
    return {
      ok: anyOk,
      imported,
      error: anyOk ? undefined : imported[0]?.error ?? 'Import failed.'
    }
  }
}

export const instanceImportService = new InstanceImportService()
