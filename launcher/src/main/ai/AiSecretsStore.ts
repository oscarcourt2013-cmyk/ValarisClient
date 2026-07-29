import { app } from 'electron'
import { mkdir, readFile, writeFile } from 'fs/promises'
import { join } from 'path'

interface SecretsFile {
  groqApiKey?: string
}

/** Local Groq API key — never synced with settings.json. */
export class AiSecretsStore {
  private cache: SecretsFile | null = null

  private path(): string {
    return join(app.getPath('userData'), 'secrets.json')
  }

  async load(): Promise<SecretsFile> {
    if (this.cache) {
      return this.cache
    }
    try {
      const raw = await readFile(this.path(), 'utf8')
      this.cache = JSON.parse(raw) as SecretsFile
    } catch {
      this.cache = {}
    }
    return this.cache
  }

  async groqApiKey(): Promise<string> {
    const file = await this.load()
    const fromFile = file.groqApiKey?.trim() ?? ''
    if (fromFile) {
      return fromFile
    }
    return process.env.GROQ_API_KEY?.trim() ?? ''
  }

  async hasKey(): Promise<boolean> {
    return Boolean(await this.groqApiKey())
  }

  async setGroqApiKey(key: string): Promise<void> {
    const next: SecretsFile = { ...(await this.load()), groqApiKey: key.trim() }
    this.cache = next
    await mkdir(app.getPath('userData'), { recursive: true })
    await writeFile(this.path(), JSON.stringify(next, null, 2), 'utf8')
  }

  async clearGroqApiKey(): Promise<void> {
    await this.setGroqApiKey('')
  }
}

export const aiSecretsStore = new AiSecretsStore()
