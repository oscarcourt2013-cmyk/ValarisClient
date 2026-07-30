import { useEffect, useState, type CSSProperties } from 'react'
import { motion, AnimatePresence } from 'framer-motion'
import {
  Check,
  FolderOpen,
  Globe,
  HardDrive,
  KeyRound,
  Palette,
  User
} from 'lucide-react'
import { Button } from '@renderer/design-system/components'
import { useAccounts } from '@renderer/context/AccountProvider'
import { useI18n } from '@renderer/context/I18nProvider'
import { useTheme } from '@renderer/context/ThemeProvider'
import { playUiSound } from '@renderer/lib/uiSounds'
import { LOCALES } from '@shared/i18n'
import { PRIME_THEMES, isElevatedTheme, type PrimeThemeId } from '@shared/theme'
import './OnboardingModal.css'

interface OnboardingModalProps {
  onDone: () => void
}

const STEPS = ['language', 'theme', 'ram', 'folder', 'account'] as const
type StepId = (typeof STEPS)[number]

const THEME_SWATCHES: Record<PrimeThemeId, string> = {
  'prime-crimson': '#e11d2e',
  'prime-midnight': '#38bdf8',
  'prime-aurora': '#34d399',
  'prime-obsidian': '#f0d78c',
  'prime-ember': '#fdba74'
}

const RAM_MIN = 2048
const RAM_MAX = 8192
const RAM_STEP = 512

export function OnboardingModal({ onDone }: OnboardingModalProps) {
  const { t, setLocale } = useI18n()
  const { refreshTheme } = useTheme()
  const { activeAccount, loginMicrosoft, addOffline, refresh } = useAccounts()
  const [step, setStep] = useState(0)
  const [language, setLanguage] = useState<'en' | 'fr'>('en')
  const [theme, setTheme] = useState<PrimeThemeId>('prime-crimson')
  const [ramMb, setRamMb] = useState(4096)
  const [instancesRoot, setInstancesRoot] = useState<string | null>(null)
  const [defaultRoot, setDefaultRoot] = useState('')
  const [offlineName, setOfflineName] = useState('')
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [ready, setReady] = useState(false)

  useEffect(() => {
    void (async () => {
      const settings = await window.primeLauncher.settings.get()
      setLanguage(settings.language)
      setTheme(settings.theme)
      setRamMb(settings.defaultRamMb)
      setInstancesRoot(settings.instancesRoot ?? null)
      setDefaultRoot(settings.defaultInstancesRoot ?? '')
      setReady(true)
    })()
  }, [])

  useEffect(() => {
    if (ready) playUiSound('click')
  }, [step, ready])

  async function persist(partial: Record<string, unknown>) {
    await window.primeLauncher.settings.update(partial)
  }

  async function applyLanguage(next: 'en' | 'fr') {
    setLanguage(next)
    setLocale(next)
    await persist({ language: next })
  }

  async function applyTheme(next: PrimeThemeId) {
    setTheme(next)
    await persist({ theme: next, accentColor: null })
    await refreshTheme()
  }

  async function applyRam(next: number) {
    const clamped = Math.min(RAM_MAX, Math.max(RAM_MIN, Math.round(next / RAM_STEP) * RAM_STEP))
    setRamMb(clamped)
    await persist({ defaultRamMb: clamped })
  }

  async function browseFolder() {
    const result = await window.primeLauncher.settings.browseInstancesRoot()
    if (!result.ok || !result.path) return
    setInstancesRoot(result.path)
  }

  async function useDefaultFolder() {
    setInstancesRoot(null)
    await persist({ instancesRoot: null })
  }

  async function handleMicrosoft() {
    setBusy(true)
    setError(null)
    const result = await loginMicrosoft()
    setBusy(false)
    if (!result.ok) {
      setError(result.error ?? t('modals.login.loginFailed'))
      return
    }
    await refresh()
    playUiSound('success')
  }

  async function handleOffline() {
    setBusy(true)
    setError(null)
    const result = await addOffline(offlineName)
    setBusy(false)
    if (!result.ok) {
      setError(result.error ?? t('modals.login.offlineFailed'))
      return
    }
    await refresh()
    playUiSound('success')
  }

  async function finish() {
    setBusy(true)
    await persist({
      language,
      theme,
      defaultRamMb: ramMb,
      instancesRoot,
      onboardingDone: true
    })
    await refreshTheme()
    playUiSound('success')
    setBusy(false)
    onDone()
  }

  async function next() {
    if (step === STEPS.indexOf('folder') && instancesRoot !== undefined) {
      await persist({ instancesRoot })
    }
    if (step < STEPS.length - 1) {
      setStep((s) => s + 1)
      return
    }
    await finish()
  }

  const id: StepId = STEPS[step]!
  const displayRoot = instancesRoot || defaultRoot

  if (!ready) {
    return <div className="onboard-wizard" aria-busy="true" />
  }

  return (
    <div className="onboard-wizard">
      <div className="onboard-wizard__glow" aria-hidden />
      <motion.div
        className="onboard-wizard__panel"
        initial={{ opacity: 0, y: 18 }}
        animate={{ opacity: 1, y: 0 }}
      >
        <header className="onboard-wizard__header">
          <p className="onboard-wizard__eyebrow">{t('onboarding.eyebrow')}</p>
          <h1 className="onboard-wizard__brand">Valeris</h1>
          <p className="onboard-wizard__subtitle">{t('onboarding.subtitle')}</p>
        </header>

        <div className="onboard-wizard__progress" role="tablist" aria-label={t('onboarding.eyebrow')}>
          {STEPS.map((stepId, i) => (
            <button
              key={stepId}
              type="button"
              role="tab"
              aria-selected={i === step}
              className={
                i === step ? 'is-active' : i < step ? 'is-done' : ''
              }
              onClick={() => i <= step && setStep(i)}
            >
              <span>{i + 1}</span>
              {t(`onboarding.steps.${stepId}.short`)}
            </button>
          ))}
        </div>

        <AnimatePresence mode="wait">
          <motion.div
            key={id}
            className="onboard-wizard__body"
            initial={{ opacity: 0, x: 16 }}
            animate={{ opacity: 1, x: 0 }}
            exit={{ opacity: 0, x: -12 }}
            transition={{ duration: 0.18 }}
          >
            <div className="onboard-wizard__step-head">
              {id === 'language' && <Globe size={22} />}
              {id === 'theme' && <Palette size={22} />}
              {id === 'ram' && <HardDrive size={22} />}
              {id === 'folder' && <FolderOpen size={22} />}
              {id === 'account' && <KeyRound size={22} />}
              <div>
                <h2>{t(`onboarding.steps.${id}.title`)}</h2>
                <p>{t(`onboarding.steps.${id}.body`)}</p>
              </div>
            </div>

            {id === 'language' && (
              <div className="onboard-wizard__choices">
                {LOCALES.map((loc) => (
                  <button
                    key={loc.id}
                    type="button"
                    className={`onboard-wizard__choice${language === loc.id ? ' is-active' : ''}`}
                    onClick={() => void applyLanguage(loc.id)}
                  >
                    <strong>{loc.label}</strong>
                    <span>{loc.id.toUpperCase()}</span>
                  </button>
                ))}
              </div>
            )}

            {id === 'theme' && (
              <div className="onboard-wizard__themes">
                {PRIME_THEMES.map((themeId) => (
                  <button
                    key={themeId}
                    type="button"
                    className={`onboard-wizard__theme${theme === themeId ? ' is-active' : ''}`}
                    style={{ '--swatch': THEME_SWATCHES[themeId] } as CSSProperties}
                    onClick={() => void applyTheme(themeId)}
                  >
                    <span className="onboard-wizard__theme-dot" />
                    <span>{t(`settings.theme.${themeId.replace('prime-', '')}`)}</span>
                    {isElevatedTheme(themeId) && (
                      <em>{t('settings.theme.elevated')}</em>
                    )}
                  </button>
                ))}
              </div>
            )}

            {id === 'ram' && (
              <div className="onboard-wizard__ram">
                <div className="onboard-wizard__ram-value">
                  <strong>{ramMb}</strong>
                  <span>MB</span>
                </div>
                <input
                  type="range"
                  min={RAM_MIN}
                  max={RAM_MAX}
                  step={RAM_STEP}
                  value={ramMb}
                  aria-label={t('settings.defaultRam.label')}
                  onChange={(e) => void applyRam(Number(e.target.value))}
                />
                <div className="onboard-wizard__ram-scale">
                  <span>{RAM_MIN} MB</span>
                  <span>{RAM_MAX} MB</span>
                </div>
              </div>
            )}

            {id === 'folder' && (
              <div className="onboard-wizard__folder">
                <code className="onboard-wizard__path">{displayRoot || '…'}</code>
                <div className="onboard-wizard__folder-actions">
                  <Button variant="secondary" icon={<FolderOpen size={16} />} onClick={() => void browseFolder()}>
                    {t('onboarding.browseFolder')}
                  </Button>
                  {instancesRoot && (
                    <Button variant="ghost" onClick={() => void useDefaultFolder()}>
                      {t('onboarding.useDefaultFolder')}
                    </Button>
                  )}
                </div>
                <p className="onboard-wizard__hint">{t('onboarding.folderHint')}</p>
              </div>
            )}

            {id === 'account' && (
              <div className="onboard-wizard__account">
                {activeAccount ? (
                  <div className="onboard-wizard__signed-in">
                    <User size={18} />
                    <div>
                      <strong>{activeAccount.username}</strong>
                      <span>{t('onboarding.signedIn')}</span>
                    </div>
                    <Check size={18} className="onboard-wizard__check" />
                  </div>
                ) : (
                  <>
                    <Button
                      variant="primary"
                      size="lg"
                      block
                      icon={<KeyRound size={18} />}
                      disabled={busy}
                      onClick={() => void handleMicrosoft()}
                    >
                      {t('modals.login.microsoft')}
                    </Button>
                    <p className="onboard-wizard__divider">{t('modals.login.offlineDivider')}</p>
                    <input
                      className="onboard-wizard__field"
                      placeholder={t('modals.login.offlinePlaceholder')}
                      value={offlineName}
                      maxLength={16}
                      disabled={busy}
                      onChange={(e) => setOfflineName(e.target.value)}
                      onKeyDown={(e) => e.key === 'Enter' && void handleOffline()}
                    />
                    <Button
                      variant="secondary"
                      block
                      icon={<User size={16} />}
                      disabled={busy || offlineName.trim().length < 3}
                      onClick={() => void handleOffline()}
                    >
                      {t('modals.login.offlineContinue')}
                    </Button>
                  </>
                )}
                {error && <div className="onboard-wizard__error">{error}</div>}
              </div>
            )}
          </motion.div>
        </AnimatePresence>

        <footer className="onboard-wizard__footer">
          <button type="button" className="onboard-wizard__skip" disabled={busy} onClick={() => void finish()}>
            {t('onboarding.skip')}
          </button>
          <div className="onboard-wizard__nav">
            {step > 0 && (
              <Button variant="ghost" disabled={busy} onClick={() => setStep((s) => s - 1)}>
                {t('onboarding.back')}
              </Button>
            )}
            <Button
              variant="primary"
              icon={step === STEPS.length - 1 ? <Check size={16} /> : undefined}
              disabled={busy}
              onClick={() => void next()}
            >
              {step === STEPS.length - 1 ? t('onboarding.finish') : t('onboarding.next')}
            </Button>
          </div>
        </footer>
      </motion.div>
    </div>
  )
}
