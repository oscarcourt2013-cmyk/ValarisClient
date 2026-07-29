import { motion } from 'framer-motion'
import { Sparkles, X } from 'lucide-react'
import { Button } from '@renderer/design-system/components'
import { useI18n } from '@renderer/context/I18nProvider'
import './WhatsNewModal.css'

export interface WhatsNewEntry {
  version: string
  items: string[]
}

interface WhatsNewModalProps {
  entry: WhatsNewEntry
  onClose: () => void
}

export function WhatsNewModal({ entry, onClose }: WhatsNewModalProps) {
  const { t } = useI18n()

  async function dismiss() {
    await window.primeLauncher.settings.update({ lastSeenLauncherVersion: entry.version })
    onClose()
  }

  return (
    <div className="whatsnew-modal">
      <motion.div
        className="whatsnew-modal__card"
        initial={{ opacity: 0, scale: 0.96 }}
        animate={{ opacity: 1, scale: 1 }}
      >
        <button type="button" className="whatsnew-modal__close" onClick={() => void dismiss()} aria-label="Close">
          <X size={16} />
        </button>
        <div className="whatsnew-modal__head">
          <Sparkles size={22} />
          <div>
            <p className="whatsnew-modal__eyebrow">{t('whatsNew.eyebrow')}</p>
            <h2>
              {t('whatsNew.title')} · v{entry.version}
            </h2>
          </div>
        </div>
        <ul className="whatsnew-modal__list">
          {entry.items.map((item) => (
            <li key={item}>{item}</li>
          ))}
        </ul>
        <div className="whatsnew-modal__actions">
          <Button variant="primary" onClick={() => void dismiss()}>
            {t('whatsNew.gotIt')}
          </Button>
          <Button
            variant="ghost"
            onClick={() => {
              void window.primeLauncher.update.openRelease()
              void dismiss()
            }}
          >
            {t('whatsNew.changelog')}
          </Button>
        </div>
      </motion.div>
    </div>
  )
}

/** In-app release notes keyed by launcher version. */
export const WHATS_NEW_BY_VERSION: Record<string, string[]> = {
  '0.9.16': [
    'Premium home play hub — account, instance & server in one place',
    'Pro 3D skin viewer with cape, fullscreen & camera reset',
    'Local skin library import + store 3D preview',
    'Wallpaper / accent customization, UI sounds & onboarding'
  ]
}

export function resolveWhatsNew(version: string): WhatsNewEntry | null {
  const exact = WHATS_NEW_BY_VERSION[version]
  if (exact?.length) return { version, items: exact }
  const latestKey = Object.keys(WHATS_NEW_BY_VERSION).sort().at(-1)
  if (!latestKey) return null
  const items = WHATS_NEW_BY_VERSION[latestKey]
  if (!items?.length) return null
  return { version, items }
}
