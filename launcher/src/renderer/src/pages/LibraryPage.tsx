import { Link } from 'react-router-dom'
import { Puzzle, Image, Sun, Download, Terminal, Gauge } from 'lucide-react'
import { PageShell } from '@renderer/pages/shared/PageShell'
import { useI18n } from '@renderer/context/I18nProvider'
import './LibraryPage.css'

const LINKS = [
  { to: '/mods', icon: Puzzle, titleKey: 'pages.mods.title', descKey: 'pages.mods.subtitle' },
  { to: '/resources', icon: Image, titleKey: 'pages.resources.title', descKey: 'pages.resources.subtitle' },
  { to: '/shaders', icon: Sun, titleKey: 'pages.shaders.title', descKey: 'pages.shaders.subtitle' },
  { to: '/downloads', icon: Download, titleKey: 'pages.downloads.title', descKey: 'pages.downloads.subtitle' },
  { to: '/console', icon: Terminal, titleKey: 'pages.console.title', descKey: 'pages.console.subtitle' },
  { to: '/performance', icon: Gauge, titleKey: 'pages.performance.title', descKey: 'pages.performance.subtitle' }
] as const

export function LibraryPage() {
  const { t } = useI18n()

  return (
    <PageShell title={t('pages.library.title')} subtitle={t('pages.library.subtitle')}>
      <div className="library-grid">
        {LINKS.map(({ to, icon: Icon, titleKey, descKey }) => (
          <Link key={to} to={to} className="library-card">
            <div className="library-card__icon">
              <Icon size={22} />
            </div>
            <div>
              <div className="library-card__title">{t(titleKey)}</div>
              <p className="library-card__desc">{t(descKey)}</p>
            </div>
          </Link>
        ))}
      </div>
    </PageShell>
  )
}
