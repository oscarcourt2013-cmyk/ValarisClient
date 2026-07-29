import { contextBridge, ipcRenderer, type IpcRendererEvent } from 'electron'
import { IPC, type CreateInstanceDto, type LaunchProgressDto, type UpdateInstanceDto } from '../shared/ipc'

const api = {
  window: {
    minimize: (): void => ipcRenderer.send(IPC.WINDOW_MINIMIZE),
    maximize: (): void => ipcRenderer.send(IPC.WINDOW_MAXIMIZE),
    close: (): void => ipcRenderer.send(IPC.WINDOW_CLOSE)
  },
  app: {
    getVersion: (): Promise<string> => ipcRenderer.invoke(IPC.APP_GET_VERSION),
    getPlatform: (): Promise<string> => ipcRenderer.invoke(IPC.APP_GET_PLATFORM),
    restart: (): Promise<void> => ipcRenderer.invoke(IPC.APP_RESTART)
  },
  boot: {
    initialize: (): Promise<void> => ipcRenderer.invoke(IPC.BOOT_INITIALIZE)
  },
  account: {
    getPrime: () => ipcRenderer.invoke(IPC.ACCOUNT_GET_PRIME),
    getMinecraft: () => ipcRenderer.invoke(IPC.ACCOUNT_GET_MINECRAFT),
    getActive: () => ipcRenderer.invoke(IPC.ACCOUNT_GET_ACTIVE),
    setActive: (accountId: string) => ipcRenderer.invoke(IPC.ACCOUNT_SET_ACTIVE, accountId),
    loginMicrosoft: () => ipcRenderer.invoke(IPC.ACCOUNT_LOGIN_MICROSOFT),
    addOffline: (username: string) => ipcRenderer.invoke(IPC.ACCOUNT_ADD_OFFLINE, username),
    remove: (accountId: string) => ipcRenderer.invoke(IPC.ACCOUNT_REMOVE, accountId),
    refreshMicrosoft: (accountId: string) => ipcRenderer.invoke(IPC.ACCOUNT_REFRESH_MICROSOFT, accountId),
    syncPrime: () => ipcRenderer.invoke(IPC.ACCOUNT_SYNC_PRIME)
  },
  bridge: {
    syncToInstance: (instanceId: string) => ipcRenderer.invoke(IPC.BRIDGE_SYNC, instanceId)
  },
  launch: {
    game: (instanceId: string, serverAddress?: string) => ipcRenderer.invoke(IPC.LAUNCH_GAME, instanceId, serverAddress),
    isRunning: (): Promise<boolean> => ipcRenderer.invoke(IPC.LAUNCH_IS_RUNNING),
    onProgress: (listener: (payload: LaunchProgressDto) => void): (() => void) => {
      const handler = (_event: IpcRendererEvent, payload: LaunchProgressDto): void => {
        listener(payload)
      }
      ipcRenderer.on(IPC.LAUNCH_PROGRESS, handler)
      return () => ipcRenderer.removeListener(IPC.LAUNCH_PROGRESS, handler)
    },
    listLogs: () => ipcRenderer.invoke(IPC.LAUNCH_LOGS_LIST),
    clearLogs: () => ipcRenderer.invoke(IPC.LAUNCH_LOGS_CLEAR),
    openLogFolder: () => ipcRenderer.invoke(IPC.LAUNCH_LOGS_OPEN_FOLDER),
    openCrashReport: (filePath: string) => ipcRenderer.invoke(IPC.LAUNCH_CRASH_OPEN_REPORT, filePath),
    onLogAppend: (listener: (entry: import('../shared/ipc').LaunchLogEntryDto) => void): (() => void) => {
      const handler = (_event: IpcRendererEvent, entry: import('../shared/ipc').LaunchLogEntryDto): void => {
        listener(entry)
      }
      ipcRenderer.on(IPC.LAUNCH_LOG_APPEND, handler)
      return () => ipcRenderer.removeListener(IPC.LAUNCH_LOG_APPEND, handler)
    },
    onLogReset: (listener: () => void): (() => void) => {
      const handler = (): void => listener()
      ipcRenderer.on(IPC.LAUNCH_LOG_RESET, handler)
      return () => ipcRenderer.removeListener(IPC.LAUNCH_LOG_RESET, handler)
    }
  },
  profile: {
    getActive: () => ipcRenderer.invoke('profile:get-active'),
    getAll: () => ipcRenderer.invoke('profile:get-all'),
    setInstance: (instanceId: string) => ipcRenderer.invoke(IPC.PROFILE_SET_INSTANCE, instanceId)
  },
  instance: {
    list: () => ipcRenderer.invoke(IPC.INSTANCE_LIST),
    get: (id: string) => ipcRenderer.invoke(IPC.INSTANCE_GET, id),
    getDefault: () => ipcRenderer.invoke(IPC.INSTANCE_GET_DEFAULT),
    create: (input: CreateInstanceDto) => ipcRenderer.invoke(IPC.INSTANCE_CREATE, input),
    update: (input: UpdateInstanceDto) => ipcRenderer.invoke(IPC.INSTANCE_UPDATE, input),
    remove: (id: string, deleteFiles?: boolean) =>
      ipcRenderer.invoke(IPC.INSTANCE_DELETE, id, deleteFiles),
    duplicate: (id: string) => ipcRenderer.invoke(IPC.INSTANCE_DUPLICATE, id),
    setDefault: (id: string) => ipcRenderer.invoke(IPC.INSTANCE_SET_DEFAULT, id),
    openFolder: (id: string) => ipcRenderer.invoke(IPC.INSTANCE_OPEN_FOLDER, id),
    importDetect: () =>
      ipcRenderer.invoke(IPC.INSTANCE_IMPORT_DETECT) as Promise<
        import('../shared/ipc').DetectedImportLauncherDto[]
      >,
    importList: (source: import('../shared/ipc').ImportLauncherId) =>
      ipcRenderer.invoke(IPC.INSTANCE_IMPORT_LIST, source) as Promise<
        import('../shared/ipc').DetectedImportInstanceDto[]
      >,
    importRun: (source: import('../shared/ipc').ImportLauncherId, instanceIds: string[]) =>
      ipcRenderer.invoke(IPC.INSTANCE_IMPORT_RUN, source, instanceIds) as Promise<{
        ok: boolean
        imported: import('../shared/ipc').ImportInstanceResultDto[]
        error?: string
      }>
  },
  minecraft: {
    getInstances: () => ipcRenderer.invoke('minecraft:get-instances'),
    getNews: () => ipcRenderer.invoke('minecraft:get-news'),
    getFavoriteServers: () => ipcRenderer.invoke('minecraft:get-favorite-servers')
  },
  mod: {
    list: () => ipcRenderer.invoke('mod:list')
  },
  content: {
    listMods: (instanceId?: string) => ipcRenderer.invoke(IPC.CONTENT_MODS_LIST, instanceId),
    setModEnabled: (fileName: string, enabled: boolean, instanceId?: string) =>
      ipcRenderer.invoke(IPC.CONTENT_MODS_SET_ENABLED, fileName, enabled, instanceId),
    removeMod: (fileName: string, instanceId?: string) =>
      ipcRenderer.invoke(IPC.CONTENT_MODS_REMOVE, fileName, instanceId),
    importMod: (instanceId?: string) => ipcRenderer.invoke(IPC.CONTENT_MODS_IMPORT, instanceId),
    installMod: (
      projectId: string,
      title: string,
      instanceId?: string,
      source?: 'modrinth' | 'curseforge',
      versionId?: string
    ) =>
      ipcRenderer.invoke(
        source === 'curseforge' ? IPC.CONTENT_MODS_INSTALL_CURSEFORGE : IPC.CONTENT_MODS_INSTALL_MODRINTH,
        projectId,
        title,
        instanceId,
        versionId
      ),
    listResourcePacks: (instanceId?: string) =>
      ipcRenderer.invoke(IPC.CONTENT_RESOURCE_LIST, instanceId),
    setResourcePackActive: (fileName: string | null, instanceId?: string) =>
      ipcRenderer.invoke(IPC.CONTENT_RESOURCE_SET_ACTIVE, fileName, instanceId),
    removeResourcePack: (fileName: string, instanceId?: string) =>
      ipcRenderer.invoke(IPC.CONTENT_RESOURCE_REMOVE, fileName, instanceId),
    importResourcePack: (instanceId?: string) =>
      ipcRenderer.invoke(IPC.CONTENT_RESOURCE_IMPORT, instanceId),
    installResourcePack: (
      projectId: string,
      title: string,
      instanceId?: string,
      source?: 'modrinth' | 'curseforge',
      versionId?: string
    ) =>
      ipcRenderer.invoke(
        source === 'curseforge' ? IPC.CONTENT_RESOURCE_INSTALL_CURSEFORGE : IPC.CONTENT_RESOURCE_INSTALL_MODRINTH,
        projectId,
        title,
        instanceId,
        versionId
      ),
    listShaders: (instanceId?: string) => ipcRenderer.invoke(IPC.CONTENT_SHADER_LIST, instanceId),
    setShaderActive: (fileName: string | null, instanceId?: string) =>
      ipcRenderer.invoke(IPC.CONTENT_SHADER_SET_ACTIVE, fileName, instanceId),
    removeShader: (fileName: string, instanceId?: string) =>
      ipcRenderer.invoke(IPC.CONTENT_SHADER_REMOVE, fileName, instanceId),
    importShader: (instanceId?: string) => ipcRenderer.invoke(IPC.CONTENT_SHADER_IMPORT, instanceId),
    installShader: (
      projectId: string,
      title: string,
      instanceId?: string,
      source?: 'modrinth' | 'curseforge',
      versionId?: string
    ) =>
      ipcRenderer.invoke(
        source === 'curseforge' ? IPC.CONTENT_SHADER_INSTALL_CURSEFORGE : IPC.CONTENT_SHADER_INSTALL_MODRINTH,
        projectId,
        title,
        instanceId,
        versionId
      ),
    searchModrinth: (
      query: string,
      type: 'mod' | 'resourcepack' | 'shader',
      instanceId?: string
    ) => ipcRenderer.invoke(IPC.CONTENT_MODRINTH_SEARCH, query, type, instanceId),
    searchCurseForge: (
      query: string,
      type: 'mod' | 'resourcepack' | 'shader',
      instanceId?: string
    ) => ipcRenderer.invoke(IPC.CONTENT_CURSEFORGE_SEARCH, query, type, instanceId),
    listVersions: (
      projectId: string,
      type: 'mod' | 'resourcepack' | 'shader',
      source: 'modrinth' | 'curseforge',
      instanceId?: string
    ) => ipcRenderer.invoke(IPC.CONTENT_VERSIONS_LIST, projectId, type, source, instanceId)
  },
  cloud: {
    getSyncStatus: () => ipcRenderer.invoke('cloud:sync-status'),
    sync: () => ipcRenderer.invoke('cloud:sync')
  },
  store: {
    catalog: () => ipcRenderer.invoke(IPC.STORE_CATALOG),
    balance: () => ipcRenderer.invoke(IPC.STORE_BALANCE),
    purchase: (itemId: string) => ipcRenderer.invoke(IPC.STORE_PURCHASE, itemId),
    history: () => ipcRenderer.invoke(IPC.STORE_HISTORY),
    promos: () => ipcRenderer.invoke(IPC.STORE_PROMOS),
    redeem: (code: string) => ipcRenderer.invoke(IPC.STORE_REDEEM, code),
    syncMode: () => ipcRenderer.invoke(IPC.STORE_SYNC_MODE) as Promise<'synced' | 'local'>
  },
  cosmetic: {
    list: () => ipcRenderer.invoke(IPC.COSMETIC_LIST),
    toggle: (cosmeticId: string) => ipcRenderer.invoke(IPC.COSMETIC_TOGGLE, cosmeticId)
  },
  skins: {
    list: () => ipcRenderer.invoke(IPC.SKIN_LIST),
    import: () => ipcRenderer.invoke(IPC.SKIN_IMPORT),
    remove: (id: string) => ipcRenderer.invoke(IPC.SKIN_REMOVE, id),
    setActive: (id: string | null) => ipcRenderer.invoke(IPC.SKIN_SET_ACTIVE, id),
    activeData: () => ipcRenderer.invoke(IPC.SKIN_ACTIVE_DATA)
  },
  friends: {
    list: () => ipcRenderer.invoke(IPC.FRIENDS_LIST),
    add: (username: string, note?: string) => ipcRenderer.invoke(IPC.FRIENDS_ADD, username, note),
    accept: (friendId: string) => ipcRenderer.invoke(IPC.FRIENDS_ACCEPT, friendId),
    remove: (friendId: string) => ipcRenderer.invoke(IPC.FRIENDS_REMOVE, friendId),
    updateNote: (friendId: string, note: string) =>
      ipcRenderer.invoke(IPC.FRIENDS_UPDATE_NOTE, friendId, note),
    refreshAll: () => ipcRenderer.invoke(IPC.FRIENDS_REFRESH_ALL),
    refresh: (friendId: string) => ipcRenderer.invoke(IPC.FRIENDS_REFRESH, friendId)
  },
  chat: {
    connect: () => ipcRenderer.invoke(IPC.SOCIAL_CONNECT),
    conversations: () => ipcRenderer.invoke(IPC.CHAT_CONVERSATIONS),
    openDm: (uuid: string) => ipcRenderer.invoke(IPC.CHAT_OPEN_DM, uuid),
    messages: (conversationId: string) => ipcRenderer.invoke(IPC.CHAT_MESSAGES, conversationId),
    send: (conversationId: string, text: string, imageUrl?: string | null) =>
      ipcRenderer.invoke(IPC.CHAT_SEND, conversationId, text, imageUrl),
    upload: (filePath: string) => ipcRenderer.invoke(IPC.CHAT_UPLOAD, filePath)
  },
  social: {
    connect: () => ipcRenderer.invoke(IPC.SOCIAL_CONNECT),
    sendTyping: (conversationId: string) => ipcRenderer.invoke(IPC.SOCIAL_TYPING, conversationId),
    onEvent: (listener: (event: Record<string, unknown>) => void): (() => void) => {
      const handler = (_event: IpcRendererEvent, payload: Record<string, unknown>): void => {
        listener(payload)
      }
      ipcRenderer.on(IPC.SOCIAL_EVENT, handler)
      return () => ipcRenderer.removeListener(IPC.SOCIAL_EVENT, handler)
    }
  },
  party: {
    get: () => ipcRenderer.invoke(IPC.PARTY_GET),
    create: () => ipcRenderer.invoke(IPC.PARTY_CREATE),
    invite: (uuid: string) => ipcRenderer.invoke(IPC.PARTY_INVITE, uuid),
    leave: () => ipcRenderer.invoke(IPC.PARTY_LEAVE),
    accept: (inviteId: string) => ipcRenderer.invoke(IPC.PARTY_ACCEPT, inviteId),
    decline: (inviteId: string) => ipcRenderer.invoke(IPC.PARTY_DECLINE, inviteId),
    setServer: (serverAddress: string) => ipcRenderer.invoke(IPC.PARTY_SET_SERVER, serverAddress)
  },
  news: {
    list: () => ipcRenderer.invoke(IPC.NEWS_LIST)
  },
  servers: {
    list: () => ipcRenderer.invoke(IPC.SERVERS_LIST),
    add: (name: string, address: string) => ipcRenderer.invoke(IPC.SERVERS_ADD, name, address),
    remove: (serverId: string) => ipcRenderer.invoke(IPC.SERVERS_REMOVE, serverId),
    refresh: (serverId: string) => ipcRenderer.invoke(IPC.SERVERS_REFRESH, serverId),
    refreshAll: () => ipcRenderer.invoke(IPC.SERVERS_REFRESH_ALL)
  },
  media: {
    list: (instanceId?: string) => ipcRenderer.invoke(IPC.MEDIA_LIST, instanceId),
    openFolder: (instanceId?: string) => ipcRenderer.invoke(IPC.MEDIA_OPEN_FOLDER, instanceId),
    openFile: (filePath: string) => ipcRenderer.invoke(IPC.MEDIA_OPEN_FILE, filePath)
  },
  performance: {
    hardware: () => ipcRenderer.invoke(IPC.PERFORMANCE_HARDWARE),
    presets: () => ipcRenderer.invoke(IPC.PERFORMANCE_PRESETS),
    selected: () => ipcRenderer.invoke(IPC.PERFORMANCE_SELECTED),
    apply: (presetId: string, instanceId?: string) =>
      ipcRenderer.invoke(IPC.PERFORMANCE_APPLY, presetId, instanceId)
  },
  downloads: {
    list: () => ipcRenderer.invoke(IPC.DOWNLOADS_LIST),
    clearCompleted: () => ipcRenderer.invoke(IPC.DOWNLOADS_CLEAR),
    remove: (taskId: string) => ipcRenderer.invoke(IPC.DOWNLOADS_REMOVE, taskId)
  },
  settings: {
    get: () => ipcRenderer.invoke(IPC.SETTINGS_GET),
    update: (partial: Record<string, unknown>) => ipcRenderer.invoke(IPC.SETTINGS_UPDATE, partial),
    listJava: () => ipcRenderer.invoke(IPC.SETTINGS_JAVA_LIST),
    browseJava: () => ipcRenderer.invoke(IPC.SETTINGS_JAVA_BROWSE),
    addJavaPath: (javaPath: string) => ipcRenderer.invoke(IPC.SETTINGS_JAVA_ADD, javaPath),
    browseWallpaper: () => ipcRenderer.invoke(IPC.SETTINGS_WALLPAPER_BROWSE),
    clearWallpaper: () => ipcRenderer.invoke(IPC.SETTINGS_WALLPAPER_CLEAR),
    wallpaperData: () => ipcRenderer.invoke(IPC.SETTINGS_WALLPAPER_DATA),
    browseInstancesRoot: () =>
      ipcRenderer.invoke(IPC.SETTINGS_BROWSE_INSTANCES_ROOT) as Promise<{
        ok: boolean
        path?: string
        error?: string
      }>
  },
  update: {
    check: (force?: boolean) => ipcRenderer.invoke(IPC.UPDATE_CHECK, force),
    getStatus: () => ipcRenderer.invoke(IPC.UPDATE_GET_STATUS),
    installLauncher: () => ipcRenderer.invoke(IPC.UPDATE_INSTALL_LAUNCHER),
    installMod: (instanceId?: string) => ipcRenderer.invoke(IPC.UPDATE_INSTALL_MOD, instanceId),
    dismiss: () => ipcRenderer.invoke(IPC.UPDATE_DISMISS),
    openRelease: (url?: string) => ipcRenderer.invoke(IPC.UPDATE_OPEN_RELEASE, url),
    onProgress: (listener: (payload: import('../shared/ipc').UpdateProgressDto) => void): (() => void) => {
      const handler = (_event: IpcRendererEvent, payload: import('../shared/ipc').UpdateProgressDto): void => {
        listener(payload)
      }
      ipcRenderer.on(IPC.UPDATE_PROGRESS, handler)
      return () => ipcRenderer.removeListener(IPC.UPDATE_PROGRESS, handler)
    }
  },
  ai: {
    keyStatus: () => ipcRenderer.invoke(IPC.AI_KEY_STATUS) as Promise<{
      hasKey: boolean
      maskedKey: string | null
      viaProxy: boolean
    }>,
    hasKey: () => ipcRenderer.invoke(IPC.AI_HAS_KEY) as Promise<boolean>,
    setKey: (key: string) =>
      ipcRenderer.invoke(IPC.AI_SET_KEY, key) as Promise<{
        hasKey: boolean
        maskedKey: string | null
        viaProxy: boolean
      }>,
    clearKey: () =>
      ipcRenderer.invoke(IPC.AI_CLEAR_KEY) as Promise<{
        hasKey: boolean
        maskedKey: string | null
        viaProxy: boolean
      }>,
    chat: (payload: {
      message: string
      instanceId?: string
      history?: Array<{ role: 'user' | 'assistant'; content: string }>
    }) =>
      ipcRenderer.invoke(IPC.AI_CHAT, payload) as Promise<{
        ok: boolean
        reply: string
        proposals: Array<{
          projectId: string
          title: string
          source: 'modrinth' | 'curseforge'
          type: 'mod' | 'resourcepack' | 'shader'
          description?: string
          downloads?: number
          iconUrl?: string
          slug?: string
        }>
        error?: string
        hasKey: boolean
      }>,
    confirmInstall: (payload: {
      projectId: string
      title: string
      source: 'modrinth' | 'curseforge'
      type: 'mod' | 'resourcepack' | 'shader'
      instanceId?: string
      versionId?: string
    }) =>
      ipcRenderer.invoke(IPC.AI_CONFIRM_INSTALL, payload) as Promise<{
        ok: boolean
        error?: string
        fileName?: string
      }>
  }
}

export type PrimeLauncherApi = typeof api

contextBridge.exposeInMainWorld('primeLauncher', api)
