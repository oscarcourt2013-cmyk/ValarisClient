export interface ModEntry {
  id: string
  fileName: string
  name: string
  description: string
  version: string
  author: string
  enabled: boolean
  source: 'modrinth' | 'curseforge' | 'local'
}

export interface ResourcePackEntry {
  id: string
  fileName: string
  name: string
  description: string
  resolution: string
  active: boolean
}

export interface ShaderEntry {
  id: string
  fileName: string
  name: string
  description: string
  backend: 'iris' | 'optifine'
  active: boolean
}

export interface StoreItem {
  id: string
  name: string
  description: string
  price: number
  category: 'cosmetic' | 'theme' | 'background' | 'badge'
  owned: boolean
}

export interface CosmeticItem {
  id: string
  name: string
  type: 'cape' | 'wings' | 'pet' | 'emote' | 'badge'
  equipped: boolean
  rarity: 'common' | 'rare' | 'epic' | 'legendary'
}

export interface FriendEntry {
  id: string
  username: string
  status: 'online' | 'away' | 'offline' | 'in-game'
  activity?: string
  /** Multiplayer host:port when the friend is in-game on a server. */
  serverAddress?: string | null
  note?: string
  pending?: boolean
  incoming?: boolean
}

export interface MediaItem {
  id: string
  type: 'screenshot' | 'replay' | 'clip'
  title: string
  date: string
  size: string
  filePath?: string
  thumbnailUrl?: string
  mediaUrl?: string
}

export interface DownloadTask {
  id: string
  name: string
  progress: number
  speed: string
  size: string
  eta: string
  status: 'downloading' | 'paused' | 'completed' | 'queued' | 'failed'
}

export interface HardwareProfile {
  cpu: string
  gpu: string
  ramGb: number
}

export type PerformancePreset = 'low' | 'balanced' | 'performance' | 'ultra'

export interface PerformancePresetInfo {
  id: PerformancePreset
  label: string
  ramMb: number
  renderDistance: number
  description: string
}
