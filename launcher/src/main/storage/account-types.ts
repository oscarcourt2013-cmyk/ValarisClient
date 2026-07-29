/** Internal account record — tokens stay in main process only. */
export interface StoredMinecraftAccount {
  id: string
  type: 'microsoft' | 'offline'
  username: string
  uuid: string
  skinUrl?: string
  capeUrl?: string
  /** MSMC refresh token from Xbox.save() */
  msRefreshToken?: string
  /** Which OAuth stack issued the refresh token: live (Electron/msmc) or azure (Tauri/Prism). */
  msAuthProvider?: 'live' | 'azure'
  addedAt: string
  lastUsedAt?: string
}

export interface AuthResult {
  ok: boolean
  error?: string
  accountId?: string
}

export interface LaunchResult {
  ok: boolean
  message: string
  error?: string
}

export interface SyncResult {
  ok: boolean
  lastSync: string
  message: string
}
