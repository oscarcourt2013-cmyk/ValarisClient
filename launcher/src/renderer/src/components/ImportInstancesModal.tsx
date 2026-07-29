import { useEffect, useState } from 'react'
import { motion } from 'framer-motion'
import { Download, Import } from 'lucide-react'
import { Button } from '@renderer/design-system/components'
import { useI18n } from '@renderer/context/I18nProvider'
import { playUiSound } from '@renderer/lib/uiSounds'
import type {
  DetectedImportInstanceDto,
  DetectedImportLauncherDto,
  ImportLauncherId
} from '@shared/ipc'
import './ImportInstancesModal.css'

interface ImportInstancesModalProps {
  onClose: () => void
  onImported: () => void
}

export function ImportInstancesModal({ onClose, onImported }: ImportInstancesModalProps) {
  const { t } = useI18n()
  const [launchers, setLaunchers] = useState<DetectedImportLauncherDto[]>([])
  const [source, setSource] = useState<ImportLauncherId | null>(null)
  const [instances, setInstances] = useState<DetectedImportInstanceDto[]>([])
  const [selected, setSelected] = useState<Set<string>>(new Set())
  const [loading, setLoading] = useState(true)
  const [listing, setListing] = useState(false)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [status, setStatus] = useState<string | null>(null)

  useEffect(() => {
    void (async () => {
      try {
        const detected = await window.primeLauncher.instance.importDetect()
        setLaunchers(detected)
      } catch (err) {
        setError(err instanceof Error ? err.message : t('import.errorDetect'))
      } finally {
        setLoading(false)
      }
    })()
  }, [t])

  async function pickSource(id: ImportLauncherId) {
    setSource(id)
    setSelected(new Set())
    setError(null)
    setStatus(null)
    setListing(true)
    try {
      const list = await window.primeLauncher.instance.importList(id)
      setInstances(list)
    } catch (err) {
      setInstances([])
      setError(err instanceof Error ? err.message : t('import.errorList'))
    } finally {
      setListing(false)
    }
  }

  function toggle(id: string) {
    setSelected((prev) => {
      const next = new Set(prev)
      if (next.has(id)) next.delete(id)
      else next.add(id)
      return next
    })
  }

  function toggleAll() {
    if (selected.size === instances.length) {
      setSelected(new Set())
      return
    }
    setSelected(new Set(instances.map((i) => i.id)))
  }

  async function runImport() {
    if (!source || selected.size === 0) return
    setBusy(true)
    setError(null)
    setStatus(null)
    const result = await window.primeLauncher.instance.importRun(source, [...selected])
    setBusy(false)
    if (!result.ok) {
      setError(result.error ?? t('import.errorImport'))
      return
    }
    const okCount = result.imported.filter((r) => r.ok).length
    playUiSound('success')
    setStatus(t('import.success', { count: okCount }))
    onImported()
    if (okCount === selected.size) {
      setTimeout(onClose, 700)
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
        className="modal import-modal"
        initial={{ opacity: 0, scale: 0.96, y: 10 }}
        animate={{ opacity: 1, scale: 1, y: 0 }}
        exit={{ opacity: 0, scale: 0.96, y: 10 }}
        onClick={(e) => e.stopPropagation()}
      >
        <h2 className="modal__title">{t('import.title')}</h2>
        <p className="modal__subtitle">{t('import.subtitle')}</p>

        {loading ? (
          <p className="import-modal__muted">{t('import.scanning')}</p>
        ) : (
          <>
            <div className="import-modal__sources">
              {launchers.map((launcher) => (
                <button
                  key={launcher.id}
                  type="button"
                  className={`import-modal__source${source === launcher.id ? ' is-active' : ''}${
                    launcher.available ? '' : ' is-empty'
                  }`}
                  onClick={() => void pickSource(launcher.id)}
                >
                  <strong>{launcher.label}</strong>
                  <span>
                    {launcher.available
                      ? t('import.foundCount', { count: launcher.instanceCount })
                      : t('import.notFound')}
                  </span>
                </button>
              ))}
            </div>

            {source && (
              <div className="import-modal__list-wrap">
                <div className="import-modal__list-head">
                  <span>{t('import.selectInstances')}</span>
                  {instances.length > 0 && (
                    <button type="button" onClick={toggleAll}>
                      {selected.size === instances.length ? t('import.deselectAll') : t('import.selectAll')}
                    </button>
                  )}
                </div>

                {listing ? (
                  <p className="import-modal__muted">{t('import.loadingInstances')}</p>
                ) : instances.length === 0 ? (
                  <p className="import-modal__muted">{t('import.emptySource')}</p>
                ) : (
                  <ul className="import-modal__list">
                    {instances.map((inst) => (
                      <li key={inst.id}>
                        <label className={selected.has(inst.id) ? 'is-selected' : ''}>
                          <input
                            type="checkbox"
                            checked={selected.has(inst.id)}
                            onChange={() => toggle(inst.id)}
                          />
                          <div>
                            <strong>{inst.name}</strong>
                            <span>
                              MC {inst.minecraftVersion} · {inst.loader}
                              {inst.hasMods ? ` · ${t('import.tagMods')}` : ''}
                              {inst.hasResourcePacks ? ` · ${t('import.tagPacks')}` : ''}
                            </span>
                          </div>
                        </label>
                      </li>
                    ))}
                  </ul>
                )}
              </div>
            )}
          </>
        )}

        {error && <div className="modal__error">{error}</div>}
        {status && <div className="import-modal__status">{status}</div>}

        <div className="modal__footer import-modal__footer">
          <Button variant="ghost" size="sm" onClick={onClose} disabled={busy}>
            {t('actions.cancel')}
          </Button>
          <Button
            variant="primary"
            size="sm"
            icon={busy ? <Download size={14} /> : <Import size={14} />}
            disabled={busy || !source || selected.size === 0}
            onClick={() => void runImport()}
          >
            {busy ? t('import.importing') : t('import.importSelected', { count: selected.size })}
          </Button>
        </div>
      </motion.div>
    </motion.div>
  )
}
