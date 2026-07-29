/** Persisted instance record — lives in instances.json (local only). */
export interface StoredInstance {
  id: string
  name: string
  minecraftVersion: string
  loader: 'vanilla' | 'fabric'
  fabricLoaderVersion?: string
  fabricApiVersion?: string
  includePrimeMod: boolean
  ramMb: number
  javaPath?: string
  jvmArgs: string[]
  isDefault: boolean
  createdAt: string
  lastPlayed?: string
}

export interface InstanceDatabase {
  version: 1
  instances: StoredInstance[]
}

export interface CreateInstanceInput {
  name: string
  minecraftVersion: string
  loader: 'vanilla' | 'fabric'
  fabricLoaderVersion?: string
  fabricApiVersion?: string
  includePrimeMod?: boolean
  ramMb: number
  jvmArgs?: string[]
}

export interface UpdateInstanceInput {
  id: string
  name?: string
  minecraftVersion?: string
  loader?: 'vanilla' | 'fabric'
  fabricLoaderVersion?: string
  fabricApiVersion?: string
  includePrimeMod?: boolean
  ramMb?: number
  javaPath?: string
  jvmArgs?: string[]
}

export interface InstanceMutationResult {
  ok: boolean
  error?: string
  instance?: StoredInstance
}
