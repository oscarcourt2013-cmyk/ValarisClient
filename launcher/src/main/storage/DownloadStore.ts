import { app } from 'electron'
import { mkdir, readFile, writeFile } from 'fs/promises'
import { join } from 'path'
import type { DownloadTask } from '../../shared/content-types'

export interface DownloadDatabase {
  version: 1
  tasks: DownloadTask[]
}

const DEFAULT_DB = (): DownloadDatabase => ({
  version: 1,
  tasks: []
})

export class DownloadStore {
  private db: DownloadDatabase | null = null

  private get path(): string {
    return join(app.getPath('userData'), 'downloads.json')
  }

  async load(): Promise<DownloadDatabase> {
    if (this.db) {
      return this.db
    }
    try {
      const raw = await readFile(this.path, 'utf8')
      this.db = JSON.parse(raw) as DownloadDatabase
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

  async mutate(fn: (db: DownloadDatabase) => void): Promise<DownloadDatabase> {
    const db = await this.load()
    fn(db)
    await this.save()
    return db
  }
}

export const downloadStore = new DownloadStore()
