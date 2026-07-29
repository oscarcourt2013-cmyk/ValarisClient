import { Minus, Square, X } from 'lucide-react'
import { PrimeLogo } from '@renderer/design-system/components'
import { useI18n } from '@renderer/context/I18nProvider'
import './TitleBar.css'

export function TitleBar() {
  const { t } = useI18n()

  return (
    <header className="titlebar">
      <div className="titlebar__left">
        <div className="titlebar__brand">
          <PrimeLogo size={28} compact />
          <span className="titlebar__name">{t('app.name')}</span>
        </div>
      </div>
      <div className="titlebar__controls">
        <button className="titlebar__btn" onClick={() => window.primeLauncher.window.minimize()} aria-label="Minimize">
          <Minus size={16} />
        </button>
        <button className="titlebar__btn" onClick={() => window.primeLauncher.window.maximize()} aria-label="Maximize">
          <Square size={14} />
        </button>
        <button
          className="titlebar__btn titlebar__btn--close"
          onClick={() => window.primeLauncher.window.close()}
          aria-label="Close"
        >
          <X size={16} />
        </button>
      </div>
    </header>
  )
}
