/**
 * Tauri bridge — same `window.primeLauncher` surface as Electron preload.
 */
import { invoke } from '@tauri-apps/api/core'
import { listen } from '@tauri-apps/api/event'
import type { CreateInstanceDto, LaunchProgressDto, UpdateInstanceDto } from '../../../shared/ipc'

export function createTauriPrimeApi() {
  return {
    window: {
      minimize: (): void => {
        void invoke('window_minimize')
      },
      maximize: (): void => {
        void invoke('window_maximize')
      },
      close: (): void => {
        void invoke('window_close')
      }
    },
    app: {
      getVersion: (): Promise<string> => invoke('app_get_version'),
      getPlatform: (): Promise<string> => invoke('app_get_platform'),
      restart: (): Promise<void> => invoke('app_restart')
    },
    boot: {
      initialize: (): Promise<void> => invoke('boot_initialize')
    },
    account: {
      getPrime: () => invoke('account_get_prime'),
      getMinecraft: () => invoke('account_get_minecraft'),
      getActive: () => invoke('account_get_active'),
      setActive: (accountId: string) => invoke('account_set_active', { accountId }),
      loginMicrosoft: () => invoke('account_login_microsoft'),
      addOffline: (username: string) => invoke('account_add_offline', { username }),
      remove: (accountId: string) => invoke('account_remove', { accountId }),
      refreshMicrosoft: (accountId: string) => invoke('account_refresh_microsoft', { accountId }),
      syncPrime: () => invoke('account_sync_prime')
    },
    bridge: {
      syncToInstance: (instanceId: string) => invoke('bridge_sync', { instanceId })
    },
    dialog: {
      openFile: (opts?: { filters?: Array<{ name: string; extensions: string[] }> }) =>
        invoke<string | null>('dialog_open_file', { filters: opts?.filters ?? null })
    },
    launch: {
      game: (instanceId: string, serverAddress?: string) =>
        invoke('launch_game', { instanceId, serverAddress }),
      isRunning: (): Promise<boolean> => invoke('launch_is_running'),
      onProgress: (listener: (payload: LaunchProgressDto) => void): (() => void) => {
        let unlisten: (() => void) | undefined
        void listen<LaunchProgressDto>('launch:progress', (event) => {
          listener(event.payload)
        }).then((fn) => {
          unlisten = fn
        })
        return () => unlisten?.()
      },
      listLogs: () => invoke('launch_logs_list'),
      clearLogs: () => invoke('launch_logs_clear'),
      openLogFolder: () => invoke('launch_logs_open_folder'),
      openCrashReport: (filePath: string) => invoke('launch_crash_open', { filePath }),
      onLogAppend: (listener: (entry: unknown) => void): (() => void) => {
        let unlisten: (() => void) | undefined
        void listen('launch:log-append', (event) => {
          listener(event.payload)
        }).then((fn) => {
          unlisten = fn
        })
        return () => unlisten?.()
      },
      onLogReset: (listener: () => void): (() => void) => {
        let unlisten: (() => void) | undefined
        void listen('launch:log-reset', () => {
          listener()
        }).then((fn) => {
          unlisten = fn
        })
        return () => unlisten?.()
      }
    },
    profile: {
      getActive: () => invoke('profile_get_active'),
      getAll: () => invoke('profile_get_all'),
      setInstance: (instanceId: string) => invoke('profile_set_instance', { instanceId })
    },
    instance: {
      list: () => invoke('instance_list'),
      get: (id: string) => invoke('instance_get', { id }),
      getDefault: () => invoke('instance_get_default'),
      create: (input: CreateInstanceDto) => invoke('instance_create', { input }),
      update: (input: UpdateInstanceDto) => invoke('instance_update', { input }),
      remove: (id: string, deleteFiles?: boolean) =>
        invoke('instance_remove', { id, deleteFiles }),
      duplicate: (id: string) => invoke('instance_duplicate', { id }),
      setDefault: (id: string) => invoke('instance_set_default', { id }),
      openFolder: (id: string) => invoke('instance_open_folder', { id }),
      importDetect: () => invoke('instance_import_detect'),
      importList: (source: import('@shared/ipc').ImportLauncherId) =>
        invoke('instance_import_list', { source }),
      importRun: (source: import('@shared/ipc').ImportLauncherId, instanceIds: string[]) =>
        invoke('instance_import_run', { source, instanceIds })
    },
    minecraft: {
      getInstances: () => invoke('instance_list'),
      getFavoriteServers: () => invoke('servers_list')
    },
    mod: {
      list: () => invoke('content_mods_list', { instanceId: null })
    },
    content: {
      listMods: (instanceId?: string) => invoke('content_mods_list', { instanceId }),
      setModEnabled: (fileName: string, enabled: boolean, instanceId?: string) =>
        invoke('content_mods_set_enabled', { fileName, enabled, instanceId }),
      removeMod: (fileName: string, instanceId?: string) =>
        invoke('content_mods_remove', { fileName, instanceId }),
      importMod: (instanceId?: string) => invoke('content_import_mod', { instanceId }),
      installMod: (
        projectId: string,
        title: string,
        instanceId?: string,
        source?: 'modrinth' | 'curseforge',
        versionId?: string
      ) =>
        source === 'curseforge'
          ? invoke('content_install_curseforge', { projectId, title, instanceId, versionId })
          : invoke('content_install_modrinth', { projectId, title, instanceId, versionId }),
      listResourcePacks: (instanceId?: string) =>
        invoke('content_resource_packs_list', { instanceId }),
      setResourcePackActive: (fileName: string | null, instanceId?: string) =>
        invoke('content_resource_set_active', { fileName, instanceId }),
      removeResourcePack: (fileName: string, instanceId?: string) =>
        invoke('content_resource_remove', { fileName, instanceId }),
      importResourcePack: (instanceId?: string) =>
        invoke('content_resource_import', { instanceId }),
      installResourcePack: (
        projectId: string,
        title: string,
        instanceId?: string,
        source?: 'modrinth' | 'curseforge',
        versionId?: string
      ) =>
        source === 'curseforge'
          ? invoke('content_install_resource_curseforge', {
              projectId,
              title,
              instanceId,
              versionId
            })
          : invoke('content_install_resource_modrinth', {
              projectId,
              title,
              instanceId,
              versionId
            }),
      listShaders: (instanceId?: string) => invoke('content_shaders_list', { instanceId }),
      setShaderActive: (fileName: string | null, instanceId?: string) =>
        invoke('content_shader_set_active', { fileName, instanceId }),
      removeShader: (fileName: string, instanceId?: string) =>
        invoke('content_shader_remove', { fileName, instanceId }),
      importShader: (instanceId?: string) => invoke('content_shader_import', { instanceId }),
      installShader: (
        projectId: string,
        title: string,
        instanceId?: string,
        source?: 'modrinth' | 'curseforge',
        versionId?: string
      ) =>
        source === 'curseforge'
          ? invoke('content_install_shader_curseforge', {
              projectId,
              title,
              instanceId,
              versionId
            })
          : invoke('content_install_shader_modrinth', {
              projectId,
              title,
              instanceId,
              versionId
            }),
      searchModrinth: (query: string, type?: string, instanceId?: string) =>
        invoke('content_search_modrinth', { query, projectType: type ?? 'mod', instanceId }),
      searchCurseForge: (query: string, type?: string, instanceId?: string) =>
        invoke('content_search_curseforge', { query, projectType: type ?? 'mod', instanceId }),
      listVersions: (
        projectId: string,
        type: string,
        source: 'modrinth' | 'curseforge',
        instanceId?: string
      ) =>
        invoke('content_list_versions', {
          projectId,
          projectType: type,
          source,
          instanceId
        })
    },
    cloud: {
      getSyncStatus: async () => ({ lastSync: null, message: 'Local only' }),
      sync: () => invoke('account_sync_prime')
    },
    store: {
      catalog: () => invoke('store_catalog'),
      balance: () => invoke('store_balance'),
      purchase: (itemId: string) => invoke('store_purchase', { itemId }),
      history: async () => [],
      promos: async () => [],
      redeem: async () => ({ ok: false, error: 'Not available in Tauri build yet.' })
    },
    cosmetic: {
      list: () => invoke('cosmetic_list'),
      toggle: (cosmeticId: string) => invoke('cosmetic_toggle', { cosmeticId })
    },
    servers: {
      list: () => invoke('servers_list'),
      add: (name: string, address: string) => invoke('servers_add', { name, address }),
      remove: (serverId: string) => invoke('servers_remove', { serverId }),
      refresh: (serverId: string) => invoke('servers_refresh', { serverId }),
      refreshAll: () => invoke('servers_refresh_all')
    },
    media: {
      list: (instanceId?: string) => invoke('media_list', { instanceId }),
      openFolder: (instanceId?: string) => invoke('media_open_folder', { instanceId }),
      openFile: (filePath: string) => invoke('media_open_file', { filePath })
    },
    performance: {
      hardware: () => invoke('performance_hardware'),
      presets: () => invoke('performance_presets'),
      selected: () => invoke('performance_selected'),
      apply: (presetId: string, instanceId?: string) =>
        invoke('performance_apply', { presetId, instanceId })
    },
    downloads: {
      list: () => invoke('downloads_list'),
      clearCompleted: () => invoke('downloads_clear'),
      remove: (taskId: string) => invoke('downloads_remove', { taskId })
    },
    settings: {
      get: () => invoke('settings_get'),
      update: (patch: Record<string, unknown>) => invoke('settings_update', { patch }),
      listJava: () => invoke('settings_java_list'),
      browseJava: () => invoke('settings_java_browse'),
      addJavaPath: (javaPath: string) => invoke('settings_java_add', { javaPath }),
      browseWallpaper: () => invoke('settings_wallpaper_browse'),
      clearWallpaper: () => invoke('settings_wallpaper_clear'),
      wallpaperData: () => invoke('settings_wallpaper_data'),
      browseInstancesRoot: () => invoke('settings_browse_instances_root')
    },
    update: {
      check: (_force?: boolean) => invoke('update_check'),
      installLauncher: () => invoke('update_install_launcher'),
      installMod: async (statusOrInstance?: unknown) => {
        if (typeof statusOrInstance === 'string' || statusOrInstance == null) {
          const status = (await invoke('update_check')) as {
            mod?: { downloadUrl?: string; fileName?: string; updateAvailable?: boolean }
          }
          if (!status.mod?.updateAvailable || !status.mod.downloadUrl || !status.mod.fileName) {
            return { ok: false, errorKey: 'no_update' }
          }
          const inst = (await invoke('instance_get_default')) as { id?: string } | null
          const instanceId =
            typeof statusOrInstance === 'string' ? statusOrInstance : inst?.id
          if (!instanceId) return { ok: false, errorKey: 'no_instance' }
          return invoke('update_install_mod', {
            downloadUrl: status.mod.downloadUrl,
            fileName: status.mod.fileName,
            instanceId
          })
        }
        const status = statusOrInstance as {
          mod?: { downloadUrl?: string; fileName?: string }
        }
        const url = status?.mod?.downloadUrl
        const fileName = status?.mod?.fileName
        if (!url || !fileName) return { ok: false, error: 'No mod update' }
        const inst = (await invoke('instance_get_default')) as { id?: string } | null
        if (!inst?.id) return { ok: false, error: 'No instance' }
        return invoke('update_install_mod', {
          downloadUrl: url,
          fileName,
          instanceId: inst.id
        })
      },
      dismiss: () => invoke('update_dismiss'),
      onProgress: (listener: (payload: unknown) => void): (() => void) => {
        let unlisten: (() => void) | undefined
        void listen('update:progress', (event) => {
          listener(event.payload)
        }).then((fn) => {
          unlisten = fn
        })
        return () => unlisten?.()
      }
    }
  }
}

export function isTauriRuntime(): boolean {
  return typeof window !== 'undefined' && '__TAURI_INTERNALS__' in window
}
