import { useState } from 'react'
import { motion } from 'framer-motion'
import { Download, Package, Rocket } from 'lucide-react'
import { Button } from '@renderer/design-system/components'
import { useI18n } from '@renderer/context/I18nProvider'
import type { UpdateInstallResultDto, UpdateProgressDto, UpdateStatusDto } from '@shared/ipc'
import '../components/LoginModal.css'

interface UpdateModalProps {
  status: UpdateStatusDto
  onClose: () => void
  onUpdated: () => void
}

export function UpdateModal({ status, onClose, onUpdated }: UpdateModalProps) {
  const { t } = useI18n()
  const [busy, setBusy] = useState<'launcher' | 'mod' | null>(null)
  const [progress, setProgress] = useState<UpdateProgressDto | null>(null)
  const [error, setError] = useState<string | null>(null)

  async function handleLater() {
    await window.primeLauncher.update.dismiss()
    onClose()
  }

  async function install(target: 'launcher' | 'mod') {
    setBusy(target)
    setError(null)
    setProgress(null)

    const unsub = window.primeLauncher.update.onProgress((payload) => {
      if (payload.target === target) {
        setProgress(payload)
      }
    })

    let result: UpdateInstallResultDto
    if (target === 'launcher') {
      result = await window.primeLauncher.update.installLauncher()
    } else {
      result = await window.primeLauncher.update.installMod()
    }

    unsub()
    setBusy(null)

    if (!result.ok) {
      const key = result.errorKey ? `updates.errors.${result.errorKey}` : null
      setError(key ? t(key) : (result.error ?? t('updates.errors.unknown')))
      return
    }

    if (target === 'mod') {
      onUpdated()
      onClose()
    }
  }

  return (
    <motion.div
      className="modal-backdrop"
      initial={{ opacity: 0 }}
      animate={{ opacity: 1 }}
      exit={{ opacity: 0 }}
      onClick={() => void handleLater()}
    >
      <motion.div
        className="modal"
        style={{ width: 'min(520px, 100%)' }}
        initial={{ opacity: 0, scale: 0.95, y: 12 }}
        animate={{ opacity: 1, scale: 1, y: 0 }}
        exit={{ opacity: 0, scale: 0.95, y: 12 }}
        onClick={(e) => e.stopPropagation()}
      >
        <h2 className="modal__title">{t('updates.modal.title')}</h2>
        <p className="modal__subtitle">{t('updates.modal.subtitle')}</p>

        {status.launcher.updateAvailable && (
          <div style={{ marginBottom: 16, padding: '12px 14px', borderRadius: 'var(--radius-md)', background: 'var(--prime-surface-2)', border: '1px solid var(--prime-border)' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 6 }}>
              <Rocket size={16} />
              <strong>{t('updates.launcher.label')}</strong>
            </div>
            <p className="text-caption" style={{ color: 'var(--prime-muted)', marginBottom: 10 }}>
              {t('updates.versionLine', { current: status.launcher.current, latest: status.launcher.latest })}
            </p>
            <Button
              variant="primary"
              size="sm"
              icon={<Download size={14} />}
              disabled={busy !== null}
              onClick={() => void install('launcher')}
            >
              {busy === 'launcher' ? t('updates.installing') : t('updates.installLauncher')}
            </Button>
          </div>
        )}

        {status.mod.updateAvailable && (
          <div style={{ marginBottom: 16, padding: '12px 14px', borderRadius: 'var(--radius-md)', background: 'var(--prime-surface-2)', border: '1px solid var(--prime-border)' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 6 }}>
              <Package size={16} />
              <strong>{t('updates.mod.label')}</strong>
            </div>
            <p className="text-caption" style={{ color: 'var(--prime-muted)', marginBottom: 10 }}>
              {t('updates.versionLine', { current: status.mod.current, latest: status.mod.latest })}
            </p>
            <Button
              variant="secondary"
              size="sm"
              icon={<Download size={14} />}
              disabled={busy !== null}
              onClick={() => void install('mod')}
            >
              {busy === 'mod' ? t('updates.installing') : t('updates.installMod')}
            </Button>
          </div>
        )}

        {progress && (
          <p className="text-caption" style={{ color: 'var(--prime-muted)', marginBottom: 8 }}>
            {progress.detail ?? t(`updates.phase.${progress.phase}`)}
            {progress.percent > 0 ? ` · ${progress.percent}%` : ''}
          </p>
        )}

        {error && <div className="modal__error">{error}</div>}

        <div className="modal__footer">
          <Button variant="ghost" disabled={busy !== null} onClick={() => void handleLater()}>
            {t('updates.later')}
          </Button>
        </div>
      </motion.div>
    </motion.div>
  )
}
