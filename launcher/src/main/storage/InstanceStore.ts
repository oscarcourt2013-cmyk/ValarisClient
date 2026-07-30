import { app } from 'electron'
import { mkdir, readFile, writeFile } from 'fs/promises'
import { join } from 'path'
import { DEFAULT_MINECRAFT_TARGET } from '../../shared/minecraft-targets'
import type { InstanceDatabase, StoredInstance } from './instance-types'

const PRIME_DEFAULT: StoredInstance = {
  id: 'prime-fabric',
  name: 'ValerisClient',
  minecraftVersion: DEFAULT_MINECRAFT_TARGET.mcVersion,
  loader: 'fabric',
  fabricLoaderVersion: DEFAULT_MINECRAFT_TARGET.fabricLoader,
  fabricApiVersion: DEFAULT_MINECRAFT_TARGET.fabricApi,
  includePrimeMod: true,
  ramMb: 4096,
  jvmArgs: ['-XX:+UseG1GC'],
  isDefault: true,
  createdAt: new Date().toISOString()
}

const DEFAULT_DB = (): InstanceDatabase => ({
  version: 1,
  instances: [PRIME_DEFAULT]
})

export class InstanceStore {
  private db: InstanceDatabase | null = null

  private get path(): string {
    return join(app.getPath('userData'), 'instances.json')
  }

  async load(): Promise<InstanceDatabase> {
    if (this.db) {
      return this.db
    }
    try {
      const raw = await readFile(this.path, 'utf8')
      this.db = JSON.parse(raw) as InstanceDatabase
      if (!this.db.instances?.length) {
        this.db = DEFAULT_DB()
        await this.save()
      }
    } catch {
      this.db = DEFAULT_DB()
      await this.save()
    }
    return this.db!
  }

  async save(): Promise<void> {
    if (!this.db) {
      return
    }
    await mkdir(app.getPath('userData'), { recursive: true })
    await writeFile(this.path, JSON.stringify(this.db, null, 2), 'utf8')
  }

  async mutate(fn: (db: InstanceDatabase) => void): Promise<InstanceDatabase> {
    const db = await this.load()
    fn(db)
    await this.save()
    return db
  }
}

export const instanceStore = new InstanceStore()
