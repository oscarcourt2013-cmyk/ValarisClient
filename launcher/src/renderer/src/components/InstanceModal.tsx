import { useEffect, useMemo, useState } from 'react'
import { motion } from 'framer-motion'
import { Button, Select } from '@renderer/design-system/components'
import { useI18n } from '@renderer/context/I18nProvider'
import type { GameInstance } from '@shared/types'
import type { JavaInstallationDto } from '@shared/ipc'
import {
  DEFAULT_MINECRAFT_TARGET,
  MINECRAFT_TARGETS,
  resolveTarget,
  type MinecraftTarget
} from '@shared/minecraft-targets'
import '@renderer/components/LoginModal.css'
import '@renderer/components/InstanceModal.css'

export type InstancePreset = 'prime' | 'fabric' | 'vanilla'

interface InstanceModalProps {
  mode: 'create' | 'edit'
  preset?: InstancePreset
  /** Pre-select MC version when opening create (e.g. "26.2"). */
  initialMcVersion?: string
  instance?: GameInstance
  onClose: () => void
  onSaved: () => void
}

type InstanceKind = 'prime' | 'fabric' | 'vanilla'

function kindFromInstance(inst: GameInstance): InstanceKind {
  if (inst.loader === 'vanilla') return 'vanilla'
  if (inst.includePrimeMod) return 'prime'
  return 'fabric'
}

function defaultNameFor(kind: InstanceKind, target: MinecraftTarget): string {
  if (kind === 'prime') return `ValerisClient ${target.mcVersion}`
  if (kind === 'fabric') return `Fabric ${target.mcVersion}`
  return `Vanilla ${target.mcVersion}`
}

export function InstanceModal({
  mode,
  preset = 'prime',
  initialMcVersion,
  instance,
  onClose,
  onSaved
}: InstanceModalProps) {
  const { t } = useI18n()
  const initialTarget = resolveTarget(
    mode === 'edit' && instance ? instance.minecraftVersion : initialMcVersion ?? DEFAULT_MINECRAFT_TARGET.mcVersion
  )
  const [kind, setKind] = useState<InstanceKind>(
    mode === 'edit' && instance ? kindFromInstance(instance) : preset
  )
  const [targetId, setTargetId] = useState(initialTarget.id)
  const [name, setName] = useState(
    mode === 'edit' && instance ? instance.name : defaultNameFor(preset, initialTarget)
  )
  const [ramMb, setRamMb] = useState(
    mode === 'edit' && instance ? instance.ramMb : preset === 'vanilla' ? 2048 : 4096
  )
  const [showAdvanced, setShowAdvanced] = useState(false)
  const [jvmArgsText, setJvmArgsText] = useState(
    (instance?.jvmArgs ?? (preset === 'vanilla' ? [] : ['-XX:+UseG1GC'])).join('\n')
  )
  const [javaPath, setJavaPath] = useState(instance?.javaPath ?? '')
  const [javaInstalls, setJavaInstalls] = useState<JavaInstallationDto[]>([])
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [nameTouched, setNameTouched] = useState(mode === 'edit')

  const target = useMemo(
    () => MINECRAFT_TARGETS.find((t) => t.id === targetId) ?? DEFAULT_MINECRAFT_TARGET,
    [targetId]
  )

  useEffect(() => {
    void window.primeLauncher.settings.listJava().then(setJavaInstalls)
  }, [])

  useEffect(() => {
    if (mode !== 'create') return
    const tTarget = resolveTarget(initialMcVersion ?? DEFAULT_MINECRAFT_TARGET.mcVersion)
    setKind(preset)
    setTargetId(tTarget.id)
    setName(defaultNameFor(preset, tTarget))
    setNameTouched(false)
    setRamMb(preset === 'vanilla' ? 2048 : 4096)
    setJvmArgsText(preset === 'vanilla' ? '' : '-XX:+UseG1GC')
    setJavaPath('')
    setShowAdvanced(false)
    setError(null)
  }, [mode, preset, initialMcVersion])

  useEffect(() => {
    if (!nameTouched && mode === 'create') {
      setName(defaultNameFor(kind, target))
    }
  }, [kind, target, nameTouched, mode])

  async function handleSubmit() {
    setBusy(true)
    setError(null)

    const jvmArgs = jvmArgsText
      .split('\n')
      .map((line) => line.trim())
      .filter(Boolean)

    const loader = kind === 'vanilla' ? 'vanilla' : 'fabric'
    const includePrimeMod = kind === 'prime'
    const payload = {
      name,
      minecraftVersion: target.mcVersion,
      loader: loader as 'vanilla' | 'fabric',
      fabricLoaderVersion: loader === 'fabric' ? target.fabricLoader : undefined,
      fabricApiVersion: includePrimeMod ? target.fabricApi : undefined,
      includePrimeMod,
      ramMb,
      jvmArgs
    }

    if (mode === 'create') {
      const result = await window.primeLauncher.instance.create(payload)
      setBusy(false)
      if (result.ok) {
        onSaved()
        onClose()
      } else {
        setError(result.error ?? t('modals.instance.createFailed'))
      }
      return
    }

    if (!instance) {
      setBusy(false)
      return
    }

    const result = await window.primeLauncher.instance.update({
      id: instance.id,
      ...payload,
      javaPath: javaPath || undefined
    })
    setBusy(false)
    if (result.ok) {
      onSaved()
      onClose()
    } else {
      setError(result.error ?? t('modals.instance.saveFailed'))
    }
  }

  return (
    <motion.div
      className="modal-backdrop"
      initial={{ opacity: 0 }}
      animate={{ opacity: 1 }}
      exit={{ opacity: 0 }}
      onClick={onClose}
    >
      <motion.div
        className="modal instance-modal"
        initial={{ opacity: 0, scale: 0.95, y: 12 }}
        animate={{ opacity: 1, scale: 1, y: 0 }}
        exit={{ opacity: 0, scale: 0.95, y: 12 }}
        onClick={(e) => e.stopPropagation()}
      >
        <h2 className="modal__title">
          {mode === 'create' ? t('modals.instance.createTitle') : t('modals.instance.editTitle')}
        </h2>
        <p className="modal__subtitle">{t('modals.instance.subtitle')}</p>

        <label className="text-caption">{t('modals.instance.kind')}</label>
        <div className="instance-modal__cards">
          {(
            [
              ['prime', t('instances.ValerisClient')],
              ['fabric', t('instances.fabric')],
              ['vanilla', t('instances.vanilla')]
            ] as const
          ).map(([id, label]) => (
            <button
              key={id}
              type="button"
              className={`instance-modal__card${kind === id ? ' is-active' : ''}`}
              onClick={() => setKind(id)}
            >
              <span className="instance-modal__card-title">{label}</span>
              <span className="instance-modal__card-desc">
                {id === 'prime'
                  ? t('modals.instance.kindPrimeHint')
                  : id === 'fabric'
                    ? t('modals.instance.kindFabricHint')
                    : t('modals.instance.kindVanillaHint')}
              </span>
            </button>
          ))}
        </div>

        <label className="text-caption">{t('modals.instance.minecraftVersion')}</label>
        <div className="instance-modal__cards instance-modal__cards--versions">
          {MINECRAFT_TARGETS.map((opt) => (
            <button
              key={opt.id}
              type="button"
              className={`instance-modal__card${targetId === opt.id ? ' is-active' : ''}`}
              onClick={() => setTargetId(opt.id)}
            >
              <span className="instance-modal__card-title">
                {opt.mcVersion}
                {opt.recommended ? (
                  <span className="instance-modal__badge">{t('modals.instance.recommended')}</span>
                ) : null}
              </span>
              <span className="instance-modal__card-desc">
                {kind === 'prime'
                  ? t('modals.instance.primeJarHint', { prefix: opt.jarPrefix })
                  : t('modals.instance.javaHint', { major: opt.javaMajor })}
              </span>
            </button>
          ))}
        </div>

        {kind === 'prime' && (
          <p className="text-caption instance-modal__note">{t('modals.instance.primeAutoNote')}</p>
        )}

        <label className="text-caption">{t('modals.instance.name')}</label>
        <input
          className="modal__field"
          value={name}
          onChange={(e) => {
            setNameTouched(true)
            setName(e.target.value)
          }}
        />

        <label className="text-caption">{t('modals.instance.ram')}</label>
        <input
          className="modal__field"
          type="number"
          min={512}
          max={16384}
          step={256}
          value={ramMb}
          onChange={(e) => setRamMb(Number(e.target.value))}
        />

        <button
          type="button"
          className="instance-modal__advanced-toggle"
          onClick={() => setShowAdvanced((v) => !v)}
        >
          {showAdvanced ? t('modals.instance.hideAdvanced') : t('modals.instance.showAdvanced')}
        </button>

        {showAdvanced && (
          <>
            <label className="text-caption">{t('modals.instance.jvmArgs')}</label>
            <textarea
              className="modal__field"
              style={{ height: 72, padding: '10px 12px', resize: 'vertical' }}
              value={jvmArgsText}
              onChange={(e) => setJvmArgsText(e.target.value)}
            />

            {mode === 'edit' && (
              <>
                <label className="text-caption">{t('modals.instance.javaPath')}</label>
                <p className="text-caption" style={{ margin: '0 0 8px', color: 'var(--prime-muted)' }}>
                  {t('modals.instance.javaPathHint')}
                </p>
                <div style={{ display: 'flex', gap: 8, alignItems: 'flex-start' }}>
                  <Select
                    className="modal__select"
                    value={javaPath || 'auto'}
                    aria-label={t('modals.instance.javaPath')}
                    onChange={(v) => setJavaPath(v === 'auto' ? '' : v)}
                    options={[
                      { value: 'auto', label: t('common.automatic') },
                      ...javaInstalls.map((java) => ({ value: java.path, label: java.label }))
                    ]}
                  />
                  <Button
                    variant="ghost"
                    onClick={() =>
                      void (async () => {
                        const result = await window.primeLauncher.settings.browseJava()
                        if (!result.ok || !result.install) {
                          if (result.error && result.error !== 'Cancelled.') {
                            setError(result.error)
                          }
                          return
                        }
                        await window.primeLauncher.settings.addJavaPath(result.install.path)
                        setJavaPath(result.install.path)
                        setJavaInstalls(await window.primeLauncher.settings.listJava())
                      })()
                    }
                  >
                    {t('settings.javaPath.addPath')}
                  </Button>
                </div>
              </>
            )}
          </>
        )}

        {error && <div className="modal__error">{error}</div>}

        <div className="modal__footer">
          <Button variant="ghost" onClick={onClose} disabled={busy}>
            {t('actions.cancel')}
          </Button>
          <Button variant="primary" disabled={busy} onClick={() => void handleSubmit()}>
            {busy
              ? t('modals.instance.saving')
              : mode === 'create'
                ? t('modals.instance.create')
                : t('actions.save')}
          </Button>
        </div>
      </motion.div>
    </motion.div>
  )
}
