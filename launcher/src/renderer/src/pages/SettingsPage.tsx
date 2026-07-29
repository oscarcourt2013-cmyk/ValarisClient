import { useCallback, useEffect, useState, type CSSProperties } from 'react'
import { Link } from 'react-router-dom'
import { PageShell } from '@renderer/pages/shared/PageShell'
import { Toggle, Select, Button } from '@renderer/design-system/components'
import { useAccounts } from '@renderer/context/AccountProvider'
import { useI18n } from '@renderer/context/I18nProvider'
import { useTheme } from '@renderer/context/ThemeProvider'
import { LOCALES } from '@shared/i18n'
import type { PerformancePreset } from '@shared/content-types'
import type { StoreItem } from '@shared/content-types'
import type {
  UpdateProgressDto,
  UpdateStatusDto,
  JavaInstallationDto,
  SettingsUpdateDto,
  PrimeThemeId
} from '@shared/ipc'
import { isElevatedTheme } from '@shared/theme'
import './SettingsPage.css'

const SECTION_IDS = [
  'general',
  'appearance',
  'minecraft',
  'performance',
  'accounts',
  'privacy',
  'downloads',
  'updates',
  'advanced'
] as const

interface SettingsState {
  language: 'en' | 'fr'
  closeOnLaunch: boolean
  autoUpdate: boolean
  theme: PrimeThemeId
  backgroundNebula: boolean
  hardwareAccel: boolean
  defaultRamMb: number
  defaultJavaPath: string | null
  performancePreset: PerformancePreset
  analytics: boolean
  discordRpc: boolean
  concurrentDownloads: number
  developerMode: boolean
  jvmArgs: string
  gameWidth: number
  gameHeight: number
  gameDisplayMode: 'windowed' | 'borderless' | 'fullscreen'
  wallpaperPath: string | null
  accentColor: string | null
  uiSounds: boolean
}

export function SettingsPage() {
  const { t, setLocale } = useI18n()
  const { refreshTheme } = useTheme()
  const { accounts, activeAccount, loginMicrosoft } = useAccounts()
  const [section, setSection] = useState<(typeof SECTION_IDS)[number]>('general')
  const [settings, setSettings] = useState<SettingsState | null>(null)
  const [updateInfo, setUpdateInfo] = useState<UpdateStatusDto | null>(null)
  const [updateBusy, setUpdateBusy] = useState<'launcher' | 'mod' | 'check' | null>(null)
  const [updateProgress, setUpdateProgress] = useState<UpdateProgressDto | null>(null)
  const [updateError, setUpdateError] = useState<string | null>(null)
  const [saved, setSaved] = useState(false)
  const [ownsNebula, setOwnsNebula] = useState(false)
  const [javaInstalls, setJavaInstalls] = useState<JavaInstallationDto[]>([])
  const [restartRequired, setRestartRequired] = useState(false)
  const [groqStatus, setGroqStatus] = useState<{
    hasKey: boolean
    maskedKey: string | null
    viaProxy: boolean
  }>({
    hasKey: false,
    maskedKey: null,
    viaProxy: false
  })
  const [groqDraft, setGroqDraft] = useState('')

  const load = useCallback(async () => {
    const s = await window.primeLauncher.settings.get()
    const keyStatus = await window.primeLauncher.ai.keyStatus()
    setGroqStatus(keyStatus)
    setSettings({
      language: s.language,
      closeOnLaunch: s.closeOnLaunch,
      autoUpdate: s.autoUpdate,
      theme: s.theme,
      backgroundNebula: s.backgroundNebula,
      hardwareAccel: s.hardwareAccel,
      defaultRamMb: s.defaultRamMb,
      defaultJavaPath: s.defaultJavaPath,
      performancePreset: s.performancePreset,
      analytics: s.analytics,
      discordRpc: s.discordRpc,
      concurrentDownloads: s.concurrentDownloads,
      developerMode: s.developerMode,
      jvmArgs: s.jvmArgs.join('\n'),
      gameWidth: s.gameWidth,
      gameHeight: s.gameHeight,
      gameDisplayMode: s.gameDisplayMode,
      wallpaperPath: s.wallpaperPath ?? null,
      accentColor: s.accentColor ?? null,
      uiSounds: s.uiSounds !== false
    })
    const catalog = await window.primeLauncher.store.catalog()
    setOwnsNebula(catalog.some((item: StoreItem) => item.id === 'bg-nebula' && item.owned))
    setJavaInstalls(await window.primeLauncher.settings.listJava())
  }, [])

  useEffect(() => {
    void load()
  }, [load])

  async function patch(partial: Partial<SettingsState>) {
    if (!settings) {
      return
    }
    // Switching theme clears a custom accent so CSS theme tokens apply as labeled.
    const clearedAccent =
      partial.theme !== undefined && partial.theme !== settings.theme
        ? { accentColor: null as string | null }
        : {}
    const next = { ...settings, ...partial, ...clearedAccent }
    setSettings(next)

    if (partial.language) {
      setLocale(partial.language)
    }

    const result = (await window.primeLauncher.settings.update({
      language: next.language,
      closeOnLaunch: next.closeOnLaunch,
      autoUpdate: next.autoUpdate,
      theme: next.theme,
      backgroundNebula: next.backgroundNebula,
      hardwareAccel: next.hardwareAccel,
      defaultRamMb: next.defaultRamMb,
      defaultJavaPath: next.defaultJavaPath,
      performancePreset: next.performancePreset,
      analytics: next.analytics,
      discordRpc: next.discordRpc,
      concurrentDownloads: next.concurrentDownloads,
      developerMode: next.developerMode,
      jvmArgs: next.jvmArgs
        .split('\n')
        .map((l) => l.trim())
        .filter(Boolean),
      gameWidth: next.gameWidth,
      gameHeight: next.gameHeight,
      gameDisplayMode: next.gameDisplayMode,
      wallpaperPath: next.wallpaperPath,
      accentColor: next.accentColor,
      uiSounds: next.uiSounds
    })) as SettingsUpdateDto

    // Apply theme only after settings are persisted — otherwise get() still returns the old theme.
    if (
      partial.theme !== undefined ||
      partial.backgroundNebula !== undefined ||
      partial.wallpaperPath !== undefined ||
      partial.accentColor !== undefined ||
      clearedAccent.accentColor !== undefined ||
      partial.uiSounds !== undefined
    ) {
      await refreshTheme()
    }

    setRestartRequired(Boolean(result.restartRequired))
    setSaved(true)
    setTimeout(() => setSaved(false), 2000)
  }

  async function handleCheckUpdate(force = true) {
    setUpdateBusy('check')
    setUpdateError(null)
    const info = await window.primeLauncher.update.check(force)
    setUpdateInfo(info)
    setUpdateBusy(null)
  }

  useEffect(() => {
    if (section === 'updates' && !updateInfo) {
      void handleCheckUpdate(false)
    }
  }, [section])

  async function handleInstallUpdate(target: 'launcher' | 'mod') {
    setUpdateBusy(target)
    setUpdateError(null)
    setUpdateProgress(null)

    const unsub = window.primeLauncher.update.onProgress((payload) => {
      if (payload.target === target) {
        setUpdateProgress(payload)
      }
    })

    const result =
      target === 'launcher'
        ? await window.primeLauncher.update.installLauncher()
        : await window.primeLauncher.update.installMod()

    unsub()
    setUpdateBusy(null)

    if (!result.ok) {
      const key = result.errorKey ? `updates.errors.${result.errorKey}` : null
      setUpdateError(key ? t(key) : (result.error ?? t('updates.errors.unknown')))
      return
    }

    if (target === 'mod') {
      await handleCheckUpdate(true)
    }
  }

  if (!settings) {
    return null
  }

  return (
    <PageShell
      title={t('settings.title')}
      subtitle={t('settings.subtitle')}
      actions={saved ? <span className="text-caption">{t('common.saved')}</span> : undefined}
    >
      {restartRequired && (
        <p className="text-caption" style={{ marginBottom: 12, color: 'var(--prime-muted)' }}>
          {t('settings.restartRequired')}{' '}
          <button className="settings__input" style={{ cursor: 'pointer' }} onClick={() => void window.primeLauncher.app.restart()}>
            {t('settings.restartNow')}
          </button>
        </p>
      )}
      <div className="settings">
        <nav className="settings__nav">
          {SECTION_IDS.map((id) => (
            <button
              key={id}
              className={`settings__nav-item${section === id ? ' settings__nav-item--active' : ''}`}
              onClick={() => setSection(id)}
            >
              {t(`settings.sections.${id}`)}
            </button>
          ))}
        </nav>

        <div className="settings__panel">
          {section === 'general' && (
            <>
              <div className="settings__row">
                <div>
                  <div className="settings__label">{t('settings.language.label')}</div>
                  <div className="settings__hint">{t('settings.language.hint')}</div>
                </div>
                <Select
                  size="sm"
                  className="settings__select"
                  value={settings.language}
                  aria-label={t('settings.language.label')}
                  onChange={(v) => void patch({ language: v as 'en' | 'fr' })}
                  options={LOCALES.map((lang) => ({ value: lang.id, label: lang.label }))}
                />
              </div>
              <div className="settings__row">
                <div>
                  <div className="settings__label">{t('settings.closeOnLaunch.label')}</div>
                  <div className="settings__hint">{t('settings.closeOnLaunch.hint')}</div>
                </div>
                <Toggle
                  checked={settings.closeOnLaunch}
                  onChange={(v) => void patch({ closeOnLaunch: v })}
                  label={t('settings.closeOnLaunch.toggle')}
                />
              </div>
              <div className="settings__row">
                <div>
                  <div className="settings__label">{t('settings.autoUpdate.label')}</div>
                  <div className="settings__hint">{t('settings.autoUpdate.hint')}</div>
                </div>
                <Toggle
                  checked={settings.autoUpdate}
                  onChange={(v) => void patch({ autoUpdate: v })}
                  label={t('settings.autoUpdate.toggle')}
                />
              </div>
            </>
          )}

          {section === 'appearance' && (
            <>
              <div className="settings__row">
                <div>
                  <div className="settings__label">{t('settings.theme.label')}</div>
                  <div className="settings__hint">{t('settings.theme.hint')}</div>
                </div>
                <div className="settings__theme-picker">
                  {(
                    [
                      { id: 'prime-crimson' as const, swatch: '#e11d2e' },
                      { id: 'prime-midnight' as const, swatch: '#38bdf8' },
                      { id: 'prime-aurora' as const, swatch: '#34d399' },
                      { id: 'prime-obsidian' as const, swatch: '#f0d78c' },
                      { id: 'prime-ember' as const, swatch: '#fdba74' }
                    ] as const
                  ).map((opt) => {
                    const elevated = isElevatedTheme(opt.id)
                    return (
                      <button
                        key={opt.id}
                        type="button"
                        className={`settings__theme-swatch${settings.theme === opt.id ? ' settings__theme-swatch--active' : ''}${elevated ? ' settings__theme-swatch--elevated' : ''}`}
                        style={{ '--swatch': opt.swatch } as CSSProperties}
                        onClick={() => void patch({ theme: opt.id })}
                        title={t(`settings.theme.${opt.id.replace('prime-', '')}`)}
                      >
                        <span className="settings__theme-swatch-dot" />
                        <span>{t(`settings.theme.${opt.id.replace('prime-', '')}`)}</span>
                        {elevated && (
                          <span className="settings__theme-elevated">{t('settings.theme.elevated')}</span>
                        )}
                      </button>
                    )
                  })}
                </div>
              </div>
              {ownsNebula && (
                <div className="settings__row">
                  <div>
                    <div className="settings__label">{t('settings.backgroundNebula.label')}</div>
                    <div className="settings__hint">{t('settings.backgroundNebula.hint')}</div>
                  </div>
                  <Toggle
                    checked={settings.backgroundNebula}
                    onChange={(v) => void patch({ backgroundNebula: v })}
                    label={t('settings.backgroundNebula.toggle')}
                  />
                </div>
              )}
              <div className="settings__row">
                <div>
                  <div className="settings__label">{t('settings.wallpaper.label')}</div>
                  <div className="settings__hint">
                    {settings.wallpaperPath
                      ? settings.wallpaperPath
                      : t('settings.wallpaper.hint')}
                  </div>
                </div>
                <div className="settings__java-picker">
                  <button
                    type="button"
                    className="settings__java-browse"
                    onClick={() =>
                      void (async () => {
                        const result = await window.primeLauncher.settings.browseWallpaper()
                        if (result.ok) {
                          await patch({ wallpaperPath: result.path ?? null })
                          await refreshTheme()
                        }
                      })()
                    }
                  >
                    {t('settings.wallpaper.browse')}
                  </button>
                  {settings.wallpaperPath && (
                    <button
                      type="button"
                      className="settings__java-browse"
                      onClick={() =>
                        void (async () => {
                          await window.primeLauncher.settings.clearWallpaper()
                          await patch({ wallpaperPath: null })
                          await refreshTheme()
                        })()
                      }
                    >
                      {t('settings.wallpaper.clear')}
                    </button>
                  )}
                </div>
              </div>
              <div className="settings__row">
                <div>
                  <div className="settings__label">{t('settings.accent.label')}</div>
                  <div className="settings__hint">{t('settings.accent.hint')}</div>
                </div>
                <div className="settings__java-picker">
                  <input
                    type="color"
                    value={settings.accentColor ?? '#e11d2e'}
                    onChange={(e) => void patch({ accentColor: e.target.value })}
                    aria-label={t('settings.accent.label')}
                  />
                  {settings.accentColor && (
                    <button
                      type="button"
                      className="settings__java-browse"
                      onClick={() => void patch({ accentColor: null })}
                    >
                      {t('settings.accent.reset')}
                    </button>
                  )}
                </div>
              </div>
              <div className="settings__row">
                <div>
                  <div className="settings__label">{t('settings.uiSounds.label')}</div>
                  <div className="settings__hint">{t('settings.uiSounds.hint')}</div>
                </div>
                <Toggle
                  checked={settings.uiSounds}
                  onChange={(v) => void patch({ uiSounds: v })}
                  label={t('settings.uiSounds.toggle')}
                />
              </div>
              <div className="settings__row">
                <div>
                  <div className="settings__label">{t('settings.hardwareAccel.label')}</div>
                </div>
                <Toggle
                  checked={settings.hardwareAccel}
                  onChange={(v) => void patch({ hardwareAccel: v })}
                  label={t('settings.hardwareAccel.toggle')}
                />
              </div>
            </>
          )}

          {section === 'minecraft' && (
            <>
              <div className="settings__row">
                <div>
                  <div className="settings__label">{t('settings.javaPath.label')}</div>
                  <div className="settings__hint">{t('settings.javaPath.hint')}</div>
                </div>
                <div className="settings__java-picker">
                  <Select
                    size="sm"
                    className="settings__select"
                    value={settings.defaultJavaPath ?? 'auto'}
                    aria-label={t('settings.javaPath.label')}
                    onChange={(v) =>
                      void patch({
                        defaultJavaPath: v === 'auto' ? null : v
                      })
                    }
                    options={[
                      { value: 'auto', label: t('common.automatic') },
                      ...javaInstalls.map((java) => ({ value: java.path, label: java.label }))
                    ]}
                  />
                  <button
                    className="settings__input settings__java-browse"
                    style={{ cursor: 'pointer' }}
                    onClick={() =>
                      void (async () => {
                        const result = await window.primeLauncher.settings.browseJava()
                        if (!result.ok || !result.install) {
                          if (result.error && result.error !== 'Cancelled.') {
                            window.alert(result.error)
                          }
                          return
                        }
                        const added = await window.primeLauncher.settings.addJavaPath(result.install.path)
                        if (!added.ok || !added.install) {
                          window.alert(added.error ?? t('settings.javaPath.browseFailed'))
                          return
                        }
                        setJavaInstalls(await window.primeLauncher.settings.listJava())
                        await patch({ defaultJavaPath: added.install.path })
                      })()
                    }
                  >
                    {t('settings.javaPath.addPath')}
                  </button>
                </div>
              </div>
              <div className="settings__row">
                <div>
                  <div className="settings__label">{t('settings.defaultRam.label')}</div>
                </div>
                <Select
                  size="sm"
                  className="settings__select"
                  value={String(settings.defaultRamMb)}
                  aria-label={t('settings.defaultRam.label')}
                  onChange={(v) => void patch({ defaultRamMb: Number(v) })}
                  options={[
                    { value: '2048', label: '2048 MB' },
                    { value: '4096', label: '4096 MB' },
                    { value: '6144', label: '6144 MB' },
                    { value: '8192', label: '8192 MB' }
                  ]}
                />
              </div>
              <div className="settings__row">
                <div>
                  <div className="settings__label">{t('settings.gameResolution.label')}</div>
                  <div className="settings__hint">{t('settings.gameResolution.hint')}</div>
                </div>
                <div className="settings__resolution">
                  <input
                    type="number"
                    className="settings__input settings__resolution-input"
                    min={320}
                    max={7680}
                    value={settings.gameWidth}
                    onChange={(e) => void patch({ gameWidth: Number(e.target.value) || 854 })}
                  />
                  <span className="settings__resolution-sep">×</span>
                  <input
                    type="number"
                    className="settings__input settings__resolution-input"
                    min={240}
                    max={4320}
                    value={settings.gameHeight}
                    onChange={(e) => void patch({ gameHeight: Number(e.target.value) || 480 })}
                  />
                </div>
              </div>
              <div className="settings__row">
                <div>
                  <div className="settings__label">{t('settings.gameDisplayMode.label')}</div>
                  <div className="settings__hint">{t('settings.gameDisplayMode.hint')}</div>
                </div>
                <Select
                  size="sm"
                  className="settings__select"
                  value={settings.gameDisplayMode}
                  aria-label={t('settings.gameDisplayMode.label')}
                  onChange={(v) =>
                    void patch({
                      gameDisplayMode: v as SettingsState['gameDisplayMode']
                    })
                  }
                  options={[
                    { value: 'windowed', label: t('settings.gameDisplayMode.windowed') },
                    { value: 'borderless', label: t('settings.gameDisplayMode.borderless') },
                    { value: 'fullscreen', label: t('settings.gameDisplayMode.fullscreen') }
                  ]}
                />
              </div>
            </>
          )}

          {section === 'performance' && (
            <div className="settings__row">
              <div>
                <div className="settings__label">{t('settings.performancePreset.label')}</div>
              </div>
              <Select
                size="sm"
                className="settings__select"
                value={settings.performancePreset}
                aria-label={t('settings.performancePreset.label')}
                onChange={(v) => void patch({ performancePreset: v as PerformancePreset })}
                options={[
                  { value: 'low', label: t('settings.performancePreset.low') },
                  { value: 'balanced', label: t('settings.performancePreset.balanced') },
                  { value: 'performance', label: t('settings.performancePreset.performance') },
                  { value: 'ultra', label: t('settings.performancePreset.ultra') }
                ]}
              />
            </div>
          )}

          {section === 'accounts' && (
            <>
              <div className="settings__row">
                <div>
                  <div className="settings__label">{t('settings.activeAccount.label')}</div>
                  <div className="settings__hint">
                    {activeAccount
                      ? `${activeAccount.username} (${activeAccount.type})`
                      : t('settings.activeAccount.none')}
                  </div>
                </div>
                <Link to="/accounts">
                  <button className="settings__input" style={{ cursor: 'pointer' }}>
                    {t('common.manage')} ({accounts.length})
                  </button>
                </Link>
              </div>
              <div className="settings__row">
                <div>
                  <div className="settings__label">{t('settings.microsoftAccount.label')}</div>
                </div>
                <button className="settings__input" style={{ cursor: 'pointer' }} onClick={() => void loginMicrosoft()}>
                  {t('common.signIn')}
                </button>
              </div>
            </>
          )}

          {section === 'privacy' && (
            <>
              <div className="settings__row">
                <div>
                  <div className="settings__label">{t('settings.analytics.label')}</div>
                  <div className="settings__hint">{t('settings.analytics.hint')}</div>
                </div>
                <Toggle
                  checked={settings.analytics}
                  onChange={(v) => void patch({ analytics: v })}
                  label={t('settings.analytics.toggle')}
                />
              </div>
              <div className="settings__row">
                <div>
                  <div className="settings__label">{t('settings.discordRpc.label')}</div>
                  <div className="settings__hint">{t('settings.discordRpc.hint')}</div>
                </div>
                <Toggle
                  checked={settings.discordRpc}
                  onChange={(v) => void patch({ discordRpc: v })}
                  label={t('settings.discordRpc.toggle')}
                />
              </div>
            </>
          )}

          {section === 'downloads' && (
            <div className="settings__row">
              <div>
                <div className="settings__label">{t('settings.concurrentDownloads.label')}</div>
              </div>
              <Select
                size="sm"
                className="settings__select"
                value={String(settings.concurrentDownloads)}
                aria-label={t('settings.concurrentDownloads.label')}
                onChange={(v) => void patch({ concurrentDownloads: Number(v) })}
                options={[
                  { value: '1', label: '1' },
                  { value: '3', label: '3' },
                  { value: '5', label: '5' }
                ]}
              />
            </div>
          )}

          {section === 'updates' && (
            <>
              <div className="settings__row">
                <div>
                  <div className="settings__label">{t('settings.checkUpdates.label')}</div>
                  <div className="settings__hint">{t('settings.checkUpdates.hint')}</div>
                </div>
                <button
                  className="settings__input"
                  style={{ cursor: 'pointer' }}
                  disabled={updateBusy !== null}
                  onClick={() => void handleCheckUpdate(true)}
                >
                  {updateBusy === 'check' ? t('updates.checking') : t('common.checkNow')}
                </button>
              </div>

              {updateInfo && (
                <>
                  <div className="settings__row">
                    <div>
                      <div className="settings__label">{t('updates.launcher.label')}</div>
                      <div className="settings__hint">
                        {t('updates.versionLine', {
                          current: updateInfo.launcher.current,
                          latest: updateInfo.launcher.latest
                        })}
                      </div>
                    </div>
                    {updateInfo.launcher.updateAvailable ? (
                      <button
                        className="settings__input"
                        style={{ cursor: 'pointer' }}
                        disabled={updateBusy !== null}
                        onClick={() => void handleInstallUpdate('launcher')}
                      >
                        {updateBusy === 'launcher' ? t('updates.installing') : t('updates.installLauncher')}
                      </button>
                    ) : (
                      <span className="text-caption" style={{ color: 'var(--prime-success)', padding: '0 16px' }}>
                        {t('updates.upToDate')}
                      </span>
                    )}
                  </div>

                  <div className="settings__row">
                    <div>
                      <div className="settings__label">{t('updates.mod.label')}</div>
                      <div className="settings__hint">
                        {t('updates.versionLine', {
                          current: updateInfo.mod.current,
                          latest: updateInfo.mod.latest
                        })}
                      </div>
                    </div>
                    {updateInfo.mod.updateAvailable ? (
                      <button
                        className="settings__input"
                        style={{ cursor: 'pointer' }}
                        disabled={updateBusy !== null}
                        onClick={() => void handleInstallUpdate('mod')}
                      >
                        {updateBusy === 'mod' ? t('updates.installing') : t('updates.installMod')}
                      </button>
                    ) : (
                      <span className="text-caption" style={{ color: 'var(--prime-success)', padding: '0 16px' }}>
                        {t('updates.upToDate')}
                      </span>
                    )}
                  </div>

                  <div style={{ padding: '0 16px 16px' }}>
                    <p className="text-caption" style={{ color: 'var(--prime-muted)' }}>
                      {updateInfo.notes}
                    </p>
                    {updateProgress && (
                      <p className="text-caption" style={{ color: 'var(--prime-muted)', marginTop: 8 }}>
                        {updateProgress.detail ?? t(`updates.phase.${updateProgress.phase}`)}
                        {updateProgress.percent > 0 ? ` · ${updateProgress.percent}%` : ''}
                      </p>
                    )}
                    {updateError && (
                      <p className="text-caption" style={{ color: 'var(--prime-error)', marginTop: 8 }}>
                        {updateError}
                      </p>
                    )}
                  </div>
                </>
              )}
            </>
          )}

          {section === 'advanced' && (
            <>
              <div className="settings__row">
                <div>
                  <div className="settings__label">{t('settings.groqKey.label')}</div>
                  <div className="settings__hint">{t('settings.groqKey.hint')}</div>
                  <div className="settings__hint" style={{ marginTop: 6 }}>
                    {groqStatus.viaProxy
                      ? t('settings.groqKey.statusProxy')
                      : groqStatus.maskedKey
                        ? t('settings.groqKey.statusOk', { masked: groqStatus.maskedKey })
                        : groqStatus.hasKey
                          ? t('settings.groqKey.statusMissing')
                          : t('settings.groqKey.statusDown')}
                  </div>
                </div>
              </div>
              <div
                style={{
                  display: 'flex',
                  gap: 8,
                  margin: '0 16px 16px',
                  alignItems: 'center'
                }}
              >
                <input
                  className="settings__input"
                  style={{ flex: 1 }}
                  type="password"
                  value={groqDraft}
                  placeholder={t('settings.groqKey.placeholder')}
                  onChange={(e) => setGroqDraft(e.target.value)}
                />
                <Button
                  variant="secondary"
                  size="sm"
                  onClick={() => {
                    void window.primeLauncher.ai.setKey(groqDraft.trim()).then((status) => {
                      setGroqStatus(status)
                      setGroqDraft('')
                      setSaved(true)
                      setTimeout(() => setSaved(false), 2000)
                    })
                  }}
                >
                  {t('settings.groqKey.save')}
                </Button>
                <Button
                  variant="ghost"
                  size="sm"
                  onClick={() => {
                    void window.primeLauncher.ai.clearKey().then((status) => {
                      setGroqStatus(status)
                    })
                  }}
                >
                  {t('settings.groqKey.clear')}
                </Button>
              </div>
              <div className="settings__row">
                <div>
                  <div className="settings__label">{t('settings.jvmArgs.label')}</div>
                  <div className="settings__hint">{t('settings.jvmArgs.hint')}</div>
                </div>
              </div>
              <textarea
                className="settings__input"
                style={{ width: '100%', minHeight: 80, margin: '0 16px 16px', fontFamily: 'var(--font-mono)' }}
                value={settings.jvmArgs}
                onChange={(e) => void patch({ jvmArgs: e.target.value })}
              />
              <div className="settings__row">
                <div>
                  <div className="settings__label">{t('settings.developerMode.label')}</div>
                </div>
                <Toggle
                  checked={settings.developerMode}
                  onChange={(v) => void patch({ developerMode: v })}
                  label={t('settings.developerMode.toggle')}
                />
              </div>
            </>
          )}
        </div>
      </div>
    </PageShell>
  )
}
