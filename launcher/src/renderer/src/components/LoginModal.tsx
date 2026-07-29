import { useState } from 'react'
import { motion } from 'framer-motion'
import { KeyRound, User } from 'lucide-react'
import { Button } from '@renderer/design-system/components'
import { useAccounts } from '@renderer/context/AccountProvider'
import { useI18n } from '@renderer/context/I18nProvider'
import './LoginModal.css'

interface LoginModalProps {
  onClose: () => void
}

export function LoginModal({ onClose }: LoginModalProps) {
  const { t } = useI18n()
  const { loginMicrosoft, addOffline } = useAccounts()
  const [username, setUsername] = useState('')
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)

  async function handleMicrosoft() {
    setBusy(true)
    setError(null)
    const result = await loginMicrosoft()
    setBusy(false)
    if (result.ok) {
      onClose()
    } else {
      setError(result.error ?? t('modals.login.loginFailed'))
    }
  }

  async function handleOffline() {
    setBusy(true)
    setError(null)
    const result = await addOffline(username)
    setBusy(false)
    if (result.ok) {
      onClose()
    } else {
      setError(result.error ?? t('modals.login.offlineFailed'))
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
        className="modal"
        initial={{ opacity: 0, scale: 0.95, y: 12 }}
        animate={{ opacity: 1, scale: 1, y: 0 }}
        exit={{ opacity: 0, scale: 0.95, y: 12 }}
        onClick={(e) => e.stopPropagation()}
      >
        <h2 className="modal__title">{t('modals.login.title')}</h2>
        <p className="modal__subtitle">{t('modals.login.subtitle')}</p>

        <div className="modal__actions">
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
        </div>

        <p className="text-caption" style={{ textAlign: 'center', margin: '20px 0 12px' }}>
          {t('modals.login.offlineDivider')}
        </p>

        <input
          className="modal__field"
          placeholder={t('modals.login.offlinePlaceholder')}
          value={username}
          maxLength={16}
          disabled={busy}
          onChange={(e) => setUsername(e.target.value)}
          onKeyDown={(e) => e.key === 'Enter' && void handleOffline()}
        />
        <Button
          variant="secondary"
          size="md"
          block
          icon={<User size={16} />}
          disabled={busy || username.trim().length < 3}
          onClick={() => void handleOffline()}
        >
          {t('modals.login.offlineContinue')}
        </Button>

        {error && <div className="modal__error">{error}</div>}

        <div className="modal__footer">
          <Button variant="ghost" size="sm" onClick={onClose} disabled={busy}>
            {t('modals.login.cancel')}
          </Button>
        </div>
      </motion.div>
    </motion.div>
  )
}
