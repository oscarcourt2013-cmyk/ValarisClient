import { randomUUID } from 'crypto'
import { readdir, rm } from 'fs/promises'
import { shell } from 'electron'
import { join } from 'path'
import type { GameInstance } from '../../shared/types'
import { DEFAULT_MINECRAFT_TARGET, resolveTarget } from '../../shared/minecraft-targets'
import { instanceStore } from '../storage/InstanceStore'
import type {
  CreateInstanceInput,
  InstanceMutationResult,
  StoredInstance,
  UpdateInstanceInput
} from '../storage/instance-types'
import type { InstanceLaunchConfig } from '../minecraft/constants'
import { getInstanceGameDir } from '../minecraft/paths'
import { settingsStore } from '../storage/SettingsStore'

const DEFAULT_MC_VERSION = DEFAULT_MINECRAFT_TARGET.mcVersion

function normalizeName(name: string): string | null {
  const trimmed = name.trim()
  if (trimmed.length < 1 || trimmed.length > 32) {
    return null
  }
  return trimmed
}

function clampRam(ramMb: number): number {
  return Math.min(16384, Math.max(512, Math.round(ramMb)))
}

async function countJarMods(instanceId: string): Promise<number> {
  const modsDir = join(getInstanceGameDir(instanceId), 'mods')
  try {
    const files = await readdir(modsDir)
    return files.filter((f) => f.endsWith('.jar')).length
  } catch {
    return 0
  }
}

function toGameInstance(stored: StoredInstance, modCount: number): GameInstance {
  return {
    id: stored.id,
    name: stored.name,
    minecraftVersion: stored.minecraftVersion,
    loader: stored.loader,
    ramMb: stored.ramMb,
    javaPath: stored.javaPath,
    jvmArgs: stored.jvmArgs,
    modCount,
    isDefault: stored.isDefault,
    includePrimeMod: stored.includePrimeMod,
    fabricLoaderVersion: stored.fabricLoaderVersion,
    fabricApiVersion: stored.fabricApiVersion,
    createdAt: stored.createdAt,
    lastPlayed: stored.lastPlayed
  }
}

export class InstanceService {
  async list(): Promise<GameInstance[]> {
    const db = await instanceStore.load()
    return Promise.all(
      db.instances.map(async (stored) => toGameInstance(stored, await countJarMods(stored.id)))
    )
  }

  async getById(id: string): Promise<GameInstance | null> {
    const db = await instanceStore.load()
    const stored = db.instances.find((i) => i.id === id)
    if (!stored) {
      return null
    }
    return toGameInstance(stored, await countJarMods(id))
  }

  async getStoredById(id: string): Promise<StoredInstance | null> {
    const db = await instanceStore.load()
    return db.instances.find((i) => i.id === id) ?? null
  }

  async getDefault(): Promise<GameInstance | null> {
    const db = await instanceStore.load()
    const stored = db.instances.find((i) => i.isDefault) ?? db.instances[0]
    if (!stored) {
      return null
    }
    return toGameInstance(stored, await countJarMods(stored.id))
  }

  toLaunchConfig(stored: StoredInstance): InstanceLaunchConfig {
    const target = resolveTarget(stored.minecraftVersion)
    return {
      id: stored.id,
      name: stored.name,
      minecraftVersion: stored.minecraftVersion,
      loader: stored.loader,
      fabricLoaderVersion: stored.fabricLoaderVersion ?? target.fabricLoader,
      fabricApiModrinthVersion: stored.fabricApiVersion ?? target.fabricApi,
      includePrimeMod: stored.includePrimeMod,
      ramMb: stored.ramMb,
      javaPath: stored.javaPath,
      jvmArgs: stored.jvmArgs
    }
  }

  async create(input: CreateInstanceInput): Promise<InstanceMutationResult> {
    const name = normalizeName(input.name)
    if (!name) {
      return { ok: false, error: 'Name must be 1–32 characters.' }
    }

    const loader = input.loader
    if (loader !== 'vanilla' && loader !== 'fabric') {
      return { ok: false, error: 'Only Vanilla and Fabric are supported locally.' }
    }

    const settings = await settingsStore.load()
    const mcVersion = (input.minecraftVersion || DEFAULT_MC_VERSION).trim()
    const target = resolveTarget(mcVersion)
    const includePrimeMod = Boolean(input.includePrimeMod)
    const stored: StoredInstance = {
      id: randomUUID(),
      name,
      minecraftVersion: mcVersion,
      loader,
      fabricLoaderVersion:
        loader === 'fabric' ? input.fabricLoaderVersion ?? target.fabricLoader : undefined,
      fabricApiVersion:
        loader === 'fabric' && includePrimeMod
          ? input.fabricApiVersion ?? target.fabricApi
          : input.fabricApiVersion,
      includePrimeMod,
      ramMb: clampRam(input.ramMb || settings.defaultRamMb),
      jvmArgs: input.jvmArgs ?? (loader === 'fabric' ? ['-XX:+UseG1GC'] : []),
      isDefault: false,
      createdAt: new Date().toISOString()
    }

    await instanceStore.mutate((db) => {
      db.instances.push(stored)
    })

    return { ok: true, instance: stored }
  }

  async update(input: UpdateInstanceInput): Promise<InstanceMutationResult> {
    const db = await instanceStore.load()
    const current = db.instances.find((i) => i.id === input.id)
    if (!current) {
      return { ok: false, error: 'Instance not found.' }
    }

    if (input.name !== undefined) {
      const name = normalizeName(input.name)
      if (!name) {
        return { ok: false, error: 'Name must be 1–32 characters.' }
      }
    }

    let updated: StoredInstance | undefined

    await instanceStore.mutate((db) => {
      const idx = db.instances.findIndex((i) => i.id === input.id)
      if (idx < 0) {
        return
      }

      const inst = db.instances[idx]!
      if (input.name !== undefined) {
        inst.name = normalizeName(input.name)!
      }
      if (input.minecraftVersion !== undefined) {
        inst.minecraftVersion = input.minecraftVersion.trim()
        if (inst.includePrimeMod && input.fabricApiVersion === undefined) {
          const target = resolveTarget(inst.minecraftVersion)
          inst.fabricApiVersion = target.fabricApi
          inst.fabricLoaderVersion = inst.fabricLoaderVersion ?? target.fabricLoader
        }
      }
      if (input.loader !== undefined) {
        inst.loader = input.loader
        if (input.loader === 'vanilla') {
          inst.fabricLoaderVersion = undefined
          inst.fabricApiVersion = undefined
          inst.includePrimeMod = false
        }
      }
      if (input.fabricLoaderVersion !== undefined) {
        inst.fabricLoaderVersion = input.fabricLoaderVersion
      }
      if (input.fabricApiVersion !== undefined) {
        inst.fabricApiVersion = input.fabricApiVersion
      }
      if (input.includePrimeMod !== undefined) {
        inst.includePrimeMod = input.includePrimeMod
      }
      if (input.ramMb !== undefined) {
        inst.ramMb = clampRam(input.ramMb)
      }
      if (input.javaPath !== undefined) {
        inst.javaPath = input.javaPath.trim() || undefined
      }
      if (input.jvmArgs !== undefined) {
        inst.jvmArgs = input.jvmArgs
      }

      updated = { ...inst }
    })

    return { ok: true, instance: updated! }
  }

  async duplicate(id: string): Promise<InstanceMutationResult> {
    const db = await instanceStore.load()
    const source = db.instances.find((i) => i.id === id)
    if (!source) {
      return { ok: false, error: 'Instance not found.' }
    }

    const copy: StoredInstance = {
      ...source,
      id: randomUUID(),
      name: `${source.name} (Copy)`,
      isDefault: false,
      createdAt: new Date().toISOString(),
      lastPlayed: undefined
    }

    await instanceStore.mutate((db) => {
      db.instances.push(copy)
    })

    return { ok: true, instance: copy }
  }

  async remove(id: string, deleteFiles = false): Promise<InstanceMutationResult> {
    const db = await instanceStore.load()
    if (db.instances.length <= 1) {
      return { ok: false, error: 'Cannot delete the last instance.' }
    }

    const target = db.instances.find((i) => i.id === id)
    if (!target) {
      return { ok: false, error: 'Instance not found.' }
    }

    await instanceStore.mutate((db) => {
      db.instances = db.instances.filter((i) => i.id !== id)
      if (target.isDefault && db.instances[0]) {
        db.instances[0]!.isDefault = true
      }
    })

    if (deleteFiles) {
      try {
        await rm(join(getInstanceGameDir(id), '..'), { recursive: true, force: true })
      } catch {
        // non-fatal
      }
    }

    return { ok: true }
  }

  async setDefault(id: string): Promise<InstanceMutationResult> {
    let updated: StoredInstance | undefined

    await instanceStore.mutate((db) => {
      const target = db.instances.find((i) => i.id === id)
      if (!target) {
        return
      }
      for (const inst of db.instances) {
        inst.isDefault = inst.id === id
      }
      updated = { ...target, isDefault: true }
    })

    if (!updated) {
      return { ok: false, error: 'Instance not found.' }
    }

    return { ok: true, instance: updated }
  }

  async touchLastPlayed(id: string): Promise<void> {
    await instanceStore.mutate((db) => {
      const inst = db.instances.find((i) => i.id === id)
      if (inst) {
        inst.lastPlayed = new Date().toISOString()
      }
    })
  }

  async openFolder(id: string): Promise<InstanceMutationResult> {
    const stored = await this.getStoredById(id)
    if (!stored) {
      return { ok: false, error: 'Instance not found.' }
    }
    const folder = getInstanceGameDir(id)
    await shell.openPath(folder)
    return { ok: true, instance: stored }
  }
}

export const instanceService = new InstanceService()
