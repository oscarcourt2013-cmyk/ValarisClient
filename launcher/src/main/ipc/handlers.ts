import { ipcMain, shell } from 'electron'
import { IPC } from '../../shared/ipc'
import { accountService } from '../services/AccountService'
import { profileService } from '../services/ProfileService'
import { instanceService } from '../services/InstanceService'
import { instanceImportService } from '../services/InstanceImportService'
import { serverService } from '../services/ServerService'
import { contentService } from '../services/ContentService'
import { cloudService } from '../services/CloudService'
import { cosmeticService } from '../services/CosmeticService'
import { skinLibraryService } from '../services/SkinLibraryService'
import { launchService } from '../services/LaunchService'
import { launchLogService } from '../services/LaunchLogService'
import { launcherBridgeService } from '../services/LauncherBridgeService'
import { storeService } from '../services/StoreService'
import { mediaService } from '../services/MediaService'
import { performanceService } from '../services/PerformanceService'
import { downloadService } from '../services/DownloadService'
import { bootService } from '../services/BootService'
import { listJavaInstallations } from '../minecraft/JavaService'
import { minecraftEngine } from '../minecraft/MinecraftEngine'
import { settingsService } from '../services/SettingsService'
import { updateService } from '../services/UpdateService'
import type { PerformancePreset } from '../../shared/content-types'
import type { LauncherSettings } from '../storage/SettingsStore'
import { settingsStore } from '../storage/SettingsStore'

/** Registers IPC handlers for all launcher services. */
export function registerServiceHandlers(): void {
  ipcMain.handle(IPC.ACCOUNT_GET_PRIME, () => accountService.getPrimeAccount())
  ipcMain.handle(IPC.ACCOUNT_GET_MINECRAFT, () => accountService.getMinecraftAccounts())
  ipcMain.handle(IPC.ACCOUNT_GET_ACTIVE, () => accountService.getActiveAccount())
  ipcMain.handle(IPC.ACCOUNT_SET_ACTIVE, (_e, accountId: string) =>
    accountService.setActiveAccount(accountId)
  )
  ipcMain.handle(IPC.ACCOUNT_LOGIN_MICROSOFT, () => accountService.loginMicrosoft())
  ipcMain.handle(IPC.ACCOUNT_ADD_OFFLINE, (_e, username: string) =>
    accountService.addOffline(username)
  )
  ipcMain.handle(IPC.ACCOUNT_REMOVE, (_e, accountId: string) =>
    accountService.removeAccount(accountId)
  )
  ipcMain.handle(IPC.ACCOUNT_REFRESH_MICROSOFT, (_e, accountId: string) =>
    accountService.refreshMicrosoftAccount(accountId)
  )
  ipcMain.handle(IPC.ACCOUNT_SYNC_PRIME, () => accountService.syncPrimeCloud())

  ipcMain.handle(IPC.LAUNCH_GAME, (_e, instanceId: string, serverAddress?: string) =>
    launchService.launch(instanceId, serverAddress)
  )
  ipcMain.handle(IPC.LAUNCH_IS_RUNNING, () => minecraftEngine.isRunning())
  ipcMain.handle(IPC.LAUNCH_LOGS_LIST, () => launchLogService.list())
  ipcMain.handle(IPC.LAUNCH_LOGS_CLEAR, () => launchLogService.clear())
  ipcMain.handle(IPC.LAUNCH_LOGS_OPEN_FOLDER, () => launchLogService.openFolder())
  ipcMain.handle(IPC.LAUNCH_CRASH_OPEN_REPORT, (_e, filePath: string) => {
    if (!filePath || typeof filePath !== 'string') {
      return
    }
    return shell.openPath(filePath)
  })

  ipcMain.handle(IPC.BRIDGE_SYNC, (_e, instanceId?: string) => launcherBridgeService.syncToInstance(instanceId!))

  ipcMain.handle(IPC.INSTANCE_LIST, () => instanceService.list())
  ipcMain.handle(IPC.INSTANCE_GET, (_e, id: string) => instanceService.getById(id))
  ipcMain.handle(IPC.INSTANCE_GET_DEFAULT, () => instanceService.getDefault())
  ipcMain.handle(IPC.INSTANCE_CREATE, (_e, input) => instanceService.create(input))
  ipcMain.handle(IPC.INSTANCE_UPDATE, (_e, input) => instanceService.update(input))
  ipcMain.handle(IPC.INSTANCE_DELETE, (_e, id: string, deleteFiles?: boolean) =>
    instanceService.remove(id, deleteFiles)
  )
  ipcMain.handle(IPC.INSTANCE_DUPLICATE, (_e, id: string) => instanceService.duplicate(id))
  ipcMain.handle(IPC.INSTANCE_SET_DEFAULT, (_e, id: string) => instanceService.setDefault(id))
  ipcMain.handle(IPC.INSTANCE_OPEN_FOLDER, (_e, id: string) => instanceService.openFolder(id))
  ipcMain.handle(IPC.INSTANCE_IMPORT_DETECT, () => instanceImportService.detect())
  ipcMain.handle(IPC.INSTANCE_IMPORT_LIST, (_e, source: import('../../shared/ipc').ImportLauncherId) =>
    instanceImportService.list(source)
  )
  ipcMain.handle(
    IPC.INSTANCE_IMPORT_RUN,
    (_e, source: import('../../shared/ipc').ImportLauncherId, instanceIds: string[]) =>
      instanceImportService.importMany(source, instanceIds)
  )
  ipcMain.handle(IPC.PROFILE_SET_INSTANCE, (_e, instanceId: string) =>
    profileService.setActiveInstance(instanceId)
  )

  ipcMain.handle('profile:get-active', () => profileService.getActiveProfile())
  ipcMain.handle('profile:get-all', () => profileService.getProfiles())
  ipcMain.handle('minecraft:get-instances', () => instanceService.list())
  ipcMain.handle('minecraft:get-favorite-servers', () => serverService.list())

  ipcMain.handle(IPC.SERVERS_LIST, () => serverService.list())
  ipcMain.handle(IPC.SERVERS_ADD, (_e, name: string, address: string) => serverService.add(name, address))
  ipcMain.handle(IPC.SERVERS_REMOVE, (_e, serverId: string) => serverService.remove(serverId))
  ipcMain.handle(IPC.SERVERS_REFRESH, (_e, serverId: string) => serverService.refreshStatus(serverId))
  ipcMain.handle(IPC.SERVERS_REFRESH_ALL, () => serverService.refreshAll())

  ipcMain.handle(IPC.CONTENT_MODS_LIST, (_e, instanceId?: string) => contentService.listMods(instanceId))
  ipcMain.handle(IPC.CONTENT_MODS_SET_ENABLED, (_e, fileName: string, enabled: boolean, instanceId?: string) =>
    contentService.setModEnabled(fileName, enabled, instanceId)
  )
  ipcMain.handle(IPC.CONTENT_MODS_REMOVE, (_e, fileName: string, instanceId?: string) =>
    contentService.removeMod(fileName, instanceId)
  )
  ipcMain.handle(IPC.CONTENT_MODS_IMPORT, (_e, instanceId?: string) => contentService.importMod(instanceId))
  ipcMain.handle(
    IPC.CONTENT_MODS_INSTALL_MODRINTH,
    (_e, projectId: string, title: string, instanceId?: string, versionId?: string) =>
      contentService.installModFromModrinth(projectId, title, instanceId, versionId)
  )
  ipcMain.handle(
    IPC.CONTENT_MODS_INSTALL_CURSEFORGE,
    (_e, projectId: string, title: string, instanceId?: string, fileId?: string) =>
      contentService.installModFromCurseForge(projectId, title, instanceId, fileId)
  )

  ipcMain.handle(IPC.CONTENT_RESOURCE_LIST, (_e, instanceId?: string) =>
    contentService.listResourcePacks(instanceId)
  )
  ipcMain.handle(IPC.CONTENT_RESOURCE_SET_ACTIVE, (_e, fileName: string | null, instanceId?: string) =>
    contentService.setResourcePackActive(fileName, instanceId)
  )
  ipcMain.handle(IPC.CONTENT_RESOURCE_REMOVE, (_e, fileName: string, instanceId?: string) =>
    contentService.removeResourcePack(fileName, instanceId)
  )
  ipcMain.handle(IPC.CONTENT_RESOURCE_IMPORT, (_e, instanceId?: string) =>
    contentService.importResourcePack(instanceId)
  )
  ipcMain.handle(
    IPC.CONTENT_RESOURCE_INSTALL_MODRINTH,
    (_e, projectId: string, title: string, instanceId?: string, versionId?: string) =>
      contentService.installResourcePackFromModrinth(projectId, title, instanceId, versionId)
  )
  ipcMain.handle(
    IPC.CONTENT_RESOURCE_INSTALL_CURSEFORGE,
    (_e, projectId: string, title: string, instanceId?: string, fileId?: string) =>
      contentService.installResourcePackFromCurseForge(projectId, title, instanceId, fileId)
  )

  ipcMain.handle(IPC.CONTENT_SHADER_LIST, (_e, instanceId?: string) => contentService.listShaders(instanceId))
  ipcMain.handle(IPC.CONTENT_SHADER_SET_ACTIVE, (_e, fileName: string | null, instanceId?: string) =>
    contentService.setShaderActive(fileName, instanceId)
  )
  ipcMain.handle(IPC.CONTENT_SHADER_REMOVE, (_e, fileName: string, instanceId?: string) =>
    contentService.removeShader(fileName, instanceId)
  )
  ipcMain.handle(IPC.CONTENT_SHADER_IMPORT, (_e, instanceId?: string) => contentService.importShader(instanceId))
  ipcMain.handle(
    IPC.CONTENT_SHADER_INSTALL_MODRINTH,
    (_e, projectId: string, title: string, instanceId?: string, versionId?: string) =>
      contentService.installShaderFromModrinth(projectId, title, instanceId, versionId)
  )
  ipcMain.handle(
    IPC.CONTENT_SHADER_INSTALL_CURSEFORGE,
    (_e, projectId: string, title: string, instanceId?: string, fileId?: string) =>
      contentService.installShaderFromCurseForge(projectId, title, instanceId, fileId)
  )

  ipcMain.handle(IPC.CONTENT_MODRINTH_SEARCH, (_e, query: string, type: 'mod' | 'resourcepack' | 'shader', instanceId?: string) =>
    contentService.searchModrinth(query, type, instanceId)
  )
  ipcMain.handle(IPC.CONTENT_CURSEFORGE_SEARCH, (_e, query: string, type: 'mod' | 'resourcepack' | 'shader', instanceId?: string) =>
    contentService.searchCurseForge(query, type, instanceId)
  )
  ipcMain.handle(
    IPC.CONTENT_VERSIONS_LIST,
    (
      _e,
      projectId: string,
      type: 'mod' | 'resourcepack' | 'shader',
      source: 'modrinth' | 'curseforge',
      instanceId?: string
    ) => contentService.listContentVersions(projectId, type, source, instanceId)
  )

  ipcMain.handle('mod:list', () => contentService.listMods())
  ipcMain.handle('cloud:sync-status', () => cloudService.getSyncStatus())
  ipcMain.handle('cloud:sync', () => cloudService.sync())

  ipcMain.handle(IPC.STORE_CATALOG, () => storeService.getCatalog())
  ipcMain.handle(IPC.STORE_BALANCE, () => storeService.getBalance())
  ipcMain.handle(IPC.STORE_PURCHASE, (_e, itemId: string) => storeService.purchase(itemId))
  ipcMain.handle(IPC.STORE_HISTORY, () => storeService.getHistory())
  ipcMain.handle(IPC.STORE_PROMOS, () => storeService.listPromos())
  ipcMain.handle(IPC.STORE_REDEEM, (_e, code: string) => storeService.redeemPromo(code))

  ipcMain.handle(IPC.COSMETIC_LIST, () => cosmeticService.list())
  ipcMain.handle(IPC.COSMETIC_TOGGLE, (_e, cosmeticId: string) => cosmeticService.toggleEquip(cosmeticId))

  ipcMain.handle(IPC.SKIN_LIST, () => skinLibraryService.list())
  ipcMain.handle(IPC.SKIN_IMPORT, () => skinLibraryService.importPng())
  ipcMain.handle(IPC.SKIN_REMOVE, (_e, id: string) => skinLibraryService.remove(id))
  ipcMain.handle(IPC.SKIN_SET_ACTIVE, (_e, id: string | null) => skinLibraryService.setActive(id))
  ipcMain.handle(IPC.SKIN_ACTIVE_DATA, () => skinLibraryService.getActiveDataUrl())

  ipcMain.handle(IPC.BOOT_INITIALIZE, () => bootService.initialize())
  ipcMain.handle(IPC.SETTINGS_JAVA_LIST, async () => {
    const settings = await settingsStore.load()
    return listJavaInstallations(settings.customJavaPaths ?? [])
  })
  ipcMain.handle(IPC.SETTINGS_JAVA_BROWSE, () => settingsService.browseJavaExecutable())
  ipcMain.handle(IPC.SETTINGS_JAVA_ADD, (_e, javaPath: string) => settingsService.addCustomJavaPath(javaPath))

  ipcMain.handle(IPC.MEDIA_LIST, (_e, instanceId?: string) => mediaService.list(instanceId))
  ipcMain.handle(IPC.MEDIA_OPEN_FOLDER, (_e, instanceId?: string) => mediaService.openFolder(instanceId))
  ipcMain.handle(IPC.MEDIA_OPEN_FILE, (_e, filePath: string) => mediaService.openFile(filePath))

  ipcMain.handle(IPC.PERFORMANCE_HARDWARE, () => performanceService.getHardware())
  ipcMain.handle(IPC.PERFORMANCE_PRESETS, () => performanceService.getPresets())
  ipcMain.handle(IPC.PERFORMANCE_SELECTED, () => performanceService.getSelectedPreset())
  ipcMain.handle(IPC.PERFORMANCE_APPLY, (_e, presetId: PerformancePreset, instanceId?: string) =>
    performanceService.applyPreset(presetId, instanceId)
  )

  ipcMain.handle(IPC.DOWNLOADS_LIST, () => downloadService.list())
  ipcMain.handle(IPC.DOWNLOADS_CLEAR, () => downloadService.clearCompleted())
  ipcMain.handle(IPC.DOWNLOADS_REMOVE, (_e, taskId: string) => downloadService.remove(taskId))

  ipcMain.handle(IPC.SETTINGS_GET, () => settingsService.get())
  ipcMain.handle(IPC.SETTINGS_UPDATE, (_e, partial: Partial<LauncherSettings>) => settingsService.update(partial))
  ipcMain.handle(IPC.SETTINGS_WALLPAPER_BROWSE, () => settingsService.browseWallpaper())
  ipcMain.handle(IPC.SETTINGS_WALLPAPER_CLEAR, () => settingsService.clearWallpaper())
  ipcMain.handle(IPC.SETTINGS_WALLPAPER_DATA, () => settingsService.getWallpaperDataUrl())
  ipcMain.handle(IPC.SETTINGS_BROWSE_INSTANCES_ROOT, () => settingsService.browseInstancesRoot())

  ipcMain.handle(IPC.UPDATE_CHECK, (_e, force?: boolean) => updateService.check(Boolean(force)))
  ipcMain.handle(IPC.UPDATE_GET_STATUS, () => updateService.getStatus())
  ipcMain.handle(IPC.UPDATE_INSTALL_LAUNCHER, () => updateService.installLauncher())
  ipcMain.handle(IPC.UPDATE_INSTALL_MOD, (_e, instanceId?: string) => updateService.installMod(instanceId))
  ipcMain.handle(IPC.UPDATE_DISMISS, () => updateService.dismissBanner())
  ipcMain.handle(IPC.UPDATE_OPEN_RELEASE, (_e, url?: string) => updateService.openReleasePage(url))
}
