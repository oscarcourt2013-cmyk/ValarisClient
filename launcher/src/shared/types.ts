export type AccountTier = 'free' | 'prime' | 'prime_plus'

export interface PrimeAccount {
  id: string
  username: string
  tier: AccountTier
  level: number
  avatarUrl?: string
  createdAt: string
}

export interface MinecraftAccount {
  id: string
  type: 'microsoft' | 'offline'
  username: string
  uuid: string
  skinUrl?: string
  capeUrl?: string
}

export interface LauncherProfile {
  id: string
  name: string
  minecraftAccountId: string
  instanceId: string
  lastPlayed?: string
  playTimeMinutes: number
}

export interface GameInstance {
  id: string
  name: string
  minecraftVersion: string
  loader: 'vanilla' | 'fabric' | 'forge' | 'neoforge'
  ramMb: number
  javaPath?: string
  jvmArgs: string[]
  modCount: number
  isDefault?: boolean
  includePrimeMod?: boolean
  fabricLoaderVersion?: string
  fabricApiVersion?: string
  createdAt?: string
  lastPlayed?: string
}

export interface NewsItem {
  id: string
  title: string
  summary: string
  date: string
  tag: 'update' | 'event' | 'announcement' | 'launcher'
}

/** Optional social / marketing links shown on the server info sheet. */
export interface ServerSocialLinks {
  discord?: string
  store?: string
  website?: string
  vote?: string
  youtube?: string
  tiktok?: string
  instagram?: string
  facebook?: string
  x?: string
}

export interface FavoriteServer {
  id: string
  name: string
  address: string
  /** Live / last-known player counts — kept when a refresh fails to avoid Offline flicker. */
  players?: number
  maxPlayers?: number
  ping?: number
  online?: boolean
  version?: string
  description?: string
  /** Short blurb from catalog or MOTD. */
  motd?: string
  /** Full description for the info sheet. */
  fullDescription?: string
  /** Server list icon (data URI or https). */
  iconUrl?: string
  /** Wide banner for cards / info sheet. */
  bannerUrl?: string
  social?: ServerSocialLinks
  /** Epoch ms of last successful online probe. */
  lastOnlineAt?: number
  /** True when this entry was seeded from the partner catalog. */
  partner?: boolean
}

export interface BootStep {
  id: string
  label: string
}

export const BOOT_STEPS: BootStep[] = [
  { id: 'core', label: 'Initializing Prime Core...' },
  { id: 'updates', label: 'Checking Updates...' },
  { id: 'minecraft', label: 'Loading Minecraft...' },
  { id: 'ready', label: 'Ready.' }
]

export type NavSection =
  | 'dashboard'
  | 'accounts'
  | 'profile'
  | 'instances'
  | 'skins'
  | 'library'
  | 'mods'
  | 'resources'
  | 'shaders'
  | 'store'
  | 'cosmetics'
  | 'servers'
  | 'friends'
  | 'chat'
  | 'news'
  | 'media'
  | 'performance'
  | 'downloads'
  | 'console'
  | 'settings'

export interface NavItem {
  id: NavSection
  label: string
  phase: number
}

/** Slim primary rail — keep this short so the launcher stays scannable. */
export const PRIMARY_NAV: NavSection[] = [
  'dashboard',
  'instances',
  'skins',
  'store',
  'library',
  'servers',
  'friends'
]

/** Overflow / “More” destinations — still routed, just not on the main rail. */
export const SECONDARY_NAV: NavSection[] = [
  'profile',
  'accounts',
  'chat',
  'media',
  'news',
  'downloads',
  'console',
  'performance'
]

/** @deprecated Prefer PRIMARY_NAV + SECONDARY_NAV — kept for any legacy consumers. */
export const NAV_ITEMS: NavItem[] = [
  { id: 'dashboard', label: 'Home', phase: 3 },
  { id: 'instances', label: 'Instances', phase: 3 },
  { id: 'skins', label: 'Skins', phase: 3 },
  { id: 'store', label: 'Prime Store', phase: 3 },
  { id: 'library', label: 'Library', phase: 3 },
  { id: 'servers', label: 'Servers', phase: 3 },
  { id: 'friends', label: 'Friends', phase: 3 },
  { id: 'profile', label: 'Profile', phase: 3 },
  { id: 'accounts', label: 'Accounts', phase: 3 },
  { id: 'mods', label: 'Mods', phase: 3 },
  { id: 'resources', label: 'Resource Packs', phase: 3 },
  { id: 'shaders', label: 'Shaders', phase: 3 },
  { id: 'cosmetics', label: 'Cosmetics', phase: 3 },
  { id: 'chat', label: 'Chat', phase: 3 },
  { id: 'news', label: 'News', phase: 3 },
  { id: 'media', label: 'Media', phase: 3 },
  { id: 'performance', label: 'Performance', phase: 3 },
  { id: 'downloads', label: 'Downloads', phase: 3 },
  { id: 'console', label: 'Console', phase: 3 },
  { id: 'settings', label: 'Settings', phase: 3 }
]
