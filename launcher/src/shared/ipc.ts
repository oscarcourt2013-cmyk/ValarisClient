/** IPC channel names — single source of truth for main ↔ renderer. */
export const IPC = {
  WINDOW_MINIMIZE: 'window:minimize',
  WINDOW_MAXIMIZE: 'window:maximize',
  WINDOW_CLOSE: 'window:close',
  APP_GET_VERSION: 'app:get-version',
  APP_GET_PLATFORM: 'app:get-platform',
  APP_RESTART: 'app:restart',
  BOOT_INITIALIZE: 'boot:initialize',

  ACCOUNT_GET_PRIME: 'account:get-prime',
  ACCOUNT_GET_MINECRAFT: 'account:get-minecraft',
  ACCOUNT_GET_ACTIVE: 'account:get-active',
  ACCOUNT_SET_ACTIVE: 'account:set-active',
  ACCOUNT_LOGIN_MICROSOFT: 'account:login-microsoft',
  ACCOUNT_ADD_OFFLINE: 'account:add-offline',
  ACCOUNT_REMOVE: 'account:remove',
  ACCOUNT_REFRESH_MICROSOFT: 'account:refresh-microsoft',
  ACCOUNT_SYNC_PRIME: 'account:sync-prime',
  LAUNCH_GAME: 'launch:game',
  BRIDGE_SYNC: 'bridge:sync',
  /** Whether the Minecraft process is currently alive. */
  LAUNCH_IS_RUNNING: 'launch:is-running',
  /** Main → renderer progress while downloading / launching. */
  LAUNCH_PROGRESS: 'launch:progress',
  LAUNCH_LOGS_LIST: 'launch:logs-list',
  LAUNCH_LOGS_CLEAR: 'launch:logs-clear',
  LAUNCH_LOGS_OPEN_FOLDER: 'launch:logs-open-folder',
  LAUNCH_CRASH_OPEN_REPORT: 'launch:crash-open-report',
  /** Main → renderer single log line. */
  LAUNCH_LOG_APPEND: 'launch:log-append',
  /** Main → renderer full log reset. */
  LAUNCH_LOG_RESET: 'launch:log-reset',

  INSTANCE_LIST: 'instance:list',
  INSTANCE_GET: 'instance:get',
  INSTANCE_GET_DEFAULT: 'instance:get-default',
  INSTANCE_CREATE: 'instance:create',
  INSTANCE_UPDATE: 'instance:update',
  INSTANCE_DELETE: 'instance:delete',
  INSTANCE_DUPLICATE: 'instance:duplicate',
  INSTANCE_SET_DEFAULT: 'instance:set-default',
  INSTANCE_OPEN_FOLDER: 'instance:open-folder',
  INSTANCE_IMPORT_DETECT: 'instance:import-detect',
  INSTANCE_IMPORT_LIST: 'instance:import-list',
  INSTANCE_IMPORT_RUN: 'instance:import-run',

  PROFILE_SET_INSTANCE: 'profile:set-instance',

  CONTENT_MODS_LIST: 'content:mods-list',
  CONTENT_MODS_SET_ENABLED: 'content:mods-set-enabled',
  CONTENT_MODS_REMOVE: 'content:mods-remove',
  CONTENT_MODS_IMPORT: 'content:mods-import',
  CONTENT_MODS_INSTALL_MODRINTH: 'content:mods-install-modrinth',
  CONTENT_MODS_INSTALL_CURSEFORGE: 'content:mods-install-curseforge',
  CONTENT_RESOURCE_LIST: 'content:resource-list',
  CONTENT_RESOURCE_SET_ACTIVE: 'content:resource-set-active',
  CONTENT_RESOURCE_REMOVE: 'content:resource-remove',
  CONTENT_RESOURCE_IMPORT: 'content:resource-import',
  CONTENT_RESOURCE_INSTALL_MODRINTH: 'content:resource-install-modrinth',
  CONTENT_RESOURCE_INSTALL_CURSEFORGE: 'content:resource-install-curseforge',
  CONTENT_SHADER_LIST: 'content:shader-list',
  CONTENT_SHADER_SET_ACTIVE: 'content:shader-set-active',
  CONTENT_SHADER_REMOVE: 'content:shader-remove',
  CONTENT_SHADER_IMPORT: 'content:shader-import',
  CONTENT_SHADER_INSTALL_MODRINTH: 'content:shader-install-modrinth',
  CONTENT_SHADER_INSTALL_CURSEFORGE: 'content:shader-install-curseforge',
  CONTENT_MODRINTH_SEARCH: 'content:modrinth-search',
  CONTENT_CURSEFORGE_SEARCH: 'content:curseforge-search',
  CONTENT_VERSIONS_LIST: 'content:versions-list',

  STORE_CATALOG: 'store:catalog',
  STORE_BALANCE: 'store:balance',
  STORE_PURCHASE: 'store:purchase',
  STORE_HISTORY: 'store:history',
  STORE_PROMOS: 'store:promos',
  STORE_REDEEM: 'store:redeem',
  STORE_SYNC_MODE: 'store:sync-mode',
  COSMETIC_LIST: 'cosmetic:list',
  COSMETIC_TOGGLE: 'cosmetic:toggle',
  SKIN_LIST: 'skin:list',
  SKIN_IMPORT: 'skin:import',
  SKIN_REMOVE: 'skin:remove',
  SKIN_SET_ACTIVE: 'skin:set-active',
  SKIN_ACTIVE_DATA: 'skin:active-data',
  FRIENDS_LIST: 'friends:list',
  FRIENDS_ADD: 'friends:add',
  FRIENDS_ACCEPT: 'friends:accept',
  FRIENDS_REMOVE: 'friends:remove',
  FRIENDS_UPDATE_NOTE: 'friends:update-note',
  FRIENDS_REFRESH_ALL: 'friends:refresh-all',
  FRIENDS_REFRESH: 'friends:refresh',
  CHAT_CONVERSATIONS: 'chat:conversations',
  CHAT_OPEN_DM: 'chat:open-dm',
  CHAT_MESSAGES: 'chat:messages',
  CHAT_SEND: 'chat:send',
  CHAT_UPLOAD: 'chat:upload',
  PARTY_GET: 'party:get',
  PARTY_CREATE: 'party:create',
  PARTY_INVITE: 'party:invite',
  PARTY_LEAVE: 'party:leave',
  PARTY_ACCEPT: 'party:accept',
  PARTY_DECLINE: 'party:decline',
  PARTY_SET_SERVER: 'party:set-server',
  SOCIAL_CONNECT: 'social:connect',
  SOCIAL_TYPING: 'social:typing',
  /** Main → renderer live social WebSocket events (party, presence, chat). */
  SOCIAL_EVENT: 'social:event',
  NEWS_LIST: 'news:list',
  SERVERS_LIST: 'servers:list',
  SERVERS_ADD: 'servers:add',
  SERVERS_REMOVE: 'servers:remove',
  SERVERS_REFRESH: 'servers:refresh',
  SERVERS_REFRESH_ALL: 'servers:refresh-all',
  MEDIA_LIST: 'media:list',
  MEDIA_OPEN_FOLDER: 'media:open-folder',
  MEDIA_OPEN_FILE: 'media:open-file',
  PERFORMANCE_HARDWARE: 'performance:hardware',
  PERFORMANCE_PRESETS: 'performance:presets',
  PERFORMANCE_SELECTED: 'performance:selected',
  PERFORMANCE_APPLY: 'performance:apply',
  DOWNLOADS_LIST: 'downloads:list',
  DOWNLOADS_CLEAR: 'downloads:clear',
  DOWNLOADS_REMOVE: 'downloads:remove',
  SETTINGS_GET: 'settings:get',
  SETTINGS_UPDATE: 'settings:update',
  SETTINGS_JAVA_LIST: 'settings:java-list',

  AI_CHAT: 'ai:chat',
  AI_CONFIRM_INSTALL: 'ai:confirm-install',
  AI_HAS_KEY: 'ai:has-key',
  AI_SET_KEY: 'ai:set-key',
  AI_CLEAR_KEY: 'ai:clear',
  AI_KEY_STATUS: 'ai:key-status',
  SETTINGS_JAVA_BROWSE: 'settings:java-browse',
  SETTINGS_JAVA_ADD: 'settings:java-add',
  SETTINGS_WALLPAPER_BROWSE: 'settings:wallpaper-browse',
  SETTINGS_WALLPAPER_CLEAR: 'settings:wallpaper-clear',
  SETTINGS_WALLPAPER_DATA: 'settings:wallpaper-data',
  SETTINGS_BROWSE_INSTANCES_ROOT: 'settings:browse-instances-root',
  UPDATE_CHECK: 'update:check',
  UPDATE_GET_STATUS: 'update:get-status',
  UPDATE_INSTALL_LAUNCHER: 'update:install-launcher',
  UPDATE_INSTALL_MOD: 'update:install-mod',
  UPDATE_DISMISS: 'update:dismiss',
  UPDATE_OPEN_RELEASE: 'update:open-release',
  /** Main → renderer download / install progress. */
  UPDATE_PROGRESS: 'update:progress'
} as const

export type IpcChannel = (typeof IPC)[keyof typeof IPC]

export interface AuthResultDto {
  ok: boolean
  error?: string
  accountId?: string
}

export interface LaunchOptionsDto {
  instanceId: string
  serverAddress?: string
}

export interface LaunchResultDto {
  ok: boolean
  message: string
  error?: string
}

export interface SyncResultDto {
  ok: boolean
  lastSync: string
  message: string
}

export interface GameCrashAnalysisDto {
  source: 'crash_report' | 'exit_code' | 'latest_log' | 'launch_log'
  exitCode: number | null
  signal: string | null
  crashReportPath?: string
  title: string
  description?: string
  exceptionType?: string
  exceptionMessage?: string
  screen?: string
  primeInvolved: boolean
  primeLocation?: string
  modIds: string[]
  fixKey:
    | 'blurOnce'
    | 'outOfMemory'
    | 'primeMod'
    | 'modConflict'
    | 'loaderError'
    | 'unknown'
  sessionDurationSec: number
}

export interface GameExitInfoDto {
  reason: 'clean_quit' | 'launcher_kill'
  exitCode: number | null
  signal: string | null
  sessionDurationSec: number
}

export interface LaunchProgressDto {
  phase:
    | 'start'
    | 'fabric'
    | 'download'
    | 'mods'
    | 'launch'
    | 'running'
    | 'stopped'
    | 'crashed'
    | 'log'
    | 'error'
  detail: string
  percent?: number
  crash?: GameCrashAnalysisDto
  exit?: GameExitInfoDto
}

export interface LaunchLogEntryDto {
  id: string
  timestamp: string
  level: 'info' | 'warn' | 'error' | 'debug'
  phase?: LaunchProgressDto['phase']
  message: string
}

export interface CreateInstanceDto {
  name: string
  minecraftVersion: string
  loader: 'vanilla' | 'fabric'
  fabricLoaderVersion?: string
  fabricApiVersion?: string
  includePrimeMod?: boolean
  ramMb: number
  jvmArgs?: string[]
}

export interface UpdateInstanceDto {
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

export interface InstanceMutationDto {
  ok: boolean
  error?: string
}

export type ImportLauncherId = 'prism' | 'multimc' | 'lunar' | 'feather' | 'dawn'

export interface DetectedImportLauncherDto {
  id: ImportLauncherId
  label: string
  rootPath: string
  instanceCount: number
  available: boolean
}

export interface DetectedImportInstanceDto {
  id: string
  source: ImportLauncherId
  name: string
  minecraftVersion: string
  loader: 'vanilla' | 'fabric'
  fabricLoaderVersion?: string
  gameDir: string
  ramMb?: number
  hasMods: boolean
  hasResourcePacks: boolean
  hasScreenshots: boolean
  hasOptions: boolean
}

export interface ImportInstanceResultDto {
  ok: boolean
  error?: string
  instanceId?: string
}

export interface ModrinthSearchHitDto {
  project_id: string
  slug: string
  title: string
  description: string
  downloads: number
  icon_url?: string
  project_type: string
}

export interface ContentMutationDto {
  ok: boolean
  error?: string
  fileName?: string
}

export interface ContentVersionDto {
  id: string
  versionNumber: string
  gameVersions: string[]
  loaders: string[]
  fileName?: string
  recommended?: boolean
}

export interface JavaBrowseResultDto {
  ok: boolean
  install?: JavaInstallationDto
  error?: string
}

export interface BootStepDto {
  id: string
  labelKey: string
}

export interface JavaInstallationDto {
  path: string
  major: number
  label: string
}

export type { PrimeThemeId } from './theme'
export { normalizePrimeTheme, PRIME_THEMES, ELEVATED_PRIME_THEMES, isElevatedTheme, isPremiumTheme, storeIdForTheme, themeIdFromStoreId } from './theme'

export interface LauncherSettingsDto {
  language: 'en' | 'fr'
  closeOnLaunch: boolean
  autoUpdate: boolean
  theme: import('./theme').PrimeThemeId
  backgroundNebula: boolean
  hardwareAccel: boolean
  defaultRamMb: number
  performancePreset: string
  analytics: boolean
  discordRpc: boolean
  curseForgeApiKey?: string
  concurrentDownloads: number
  developerMode: boolean
  jvmArgs: string[]
  defaultJavaPath: string | null
  customJavaPaths: string[]
  gameWidth: number
  gameHeight: number
  gameDisplayMode: 'windowed' | 'borderless' | 'fullscreen'
  lastUpdateCheck?: string
  lastPrimeSync?: string
  dismissedUpdateBanner?: string
  lastServerAddress?: string
  wallpaperPath?: string | null
  accentColor?: string | null
  uiSounds: boolean
  onboardingDone: boolean
  instancesRoot: string | null
  /** Computed default when instancesRoot is null. */
  defaultInstancesRoot?: string
  lastSeenLauncherVersion?: string
  activeSkinId?: string | null
}

export interface SettingsUpdateDto {
  settings: LauncherSettingsDto
  restartRequired?: boolean
}

export interface UpdateComponentDto {
  current: string
  latest: string
  updateAvailable: boolean
  downloadUrl?: string
  fileName?: string
}

export interface UpdateStatusDto {
  checkedAt: string
  notes: string
  releaseUrl?: string
  launcher: UpdateComponentDto
  mod: UpdateComponentDto
  anyUpdateAvailable: boolean
}

/** @deprecated Use UpdateStatusDto — kept for gradual migration. */
export interface UpdateCheckDto extends UpdateStatusDto {
  current: string
  latest: string
  updateAvailable: boolean
  downloadUrl?: string
}

export interface UpdateInstallResultDto {
  ok: boolean
  error?: string
  errorKey?: string
  version?: string
}

export interface UpdateProgressDto {
  target: 'launcher' | 'mod'
  phase: 'downloading' | 'installing' | 'done' | 'error'
  percent: number
  detail?: string
}
