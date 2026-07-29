import { useEffect, useLayoutEffect, useRef, useState } from 'react'
import { createPortal } from 'react-dom'
import { NavLink, Link, useLocation } from 'react-router-dom'
import {
  Home,
  UserCircle,
  Box,
  Puzzle,
  Image,
  Sun,
  ShoppingBag,
  Sparkles,
  Shirt,
  Library,
  Globe,
  Users,
  MessageCircle,
  Newspaper,
  Film,
  Gauge,
  Download,
  Terminal,
  IdCard,
  Settings,
  MoreHorizontal
} from 'lucide-react'
import type { LucideIcon } from 'lucide-react'
import { formatTier } from '@shared/format'
import { PRIMARY_NAV, SECONDARY_NAV, type NavSection } from '@shared/types'
import { Avatar, Badge } from '@renderer/design-system/components'
import { useI18n } from '@renderer/context/I18nProvider'
import './Sidebar.css'

const ICONS: Record<NavSection, LucideIcon> = {
  dashboard: Home,
  accounts: UserCircle,
  profile: IdCard,
  instances: Box,
  skins: Shirt,
  library: Library,
  mods: Puzzle,
  resources: Image,
  shaders: Sun,
  store: ShoppingBag,
  cosmetics: Sparkles,
  servers: Globe,
  friends: Users,
  chat: MessageCircle,
  news: Newspaper,
  media: Film,
  performance: Gauge,
  downloads: Download,
  console: Terminal,
  settings: Settings
}

function pathFor(id: NavSection): string {
  if (id === 'dashboard') return '/'
  return `/${id}`
}

interface SidebarProps {
  username: string
  tier: string
  uuid?: string
}

export function Sidebar({ username, tier, uuid }: SidebarProps) {
  const { t, locale } = useI18n()
  const location = useLocation()
  const [moreOpen, setMoreOpen] = useState(false)
  const [menuPos, setMenuPos] = useState<{ top: number; left: number } | null>(null)
  const moreRef = useRef<HTMLDivElement>(null)
  const buttonRef = useRef<HTMLButtonElement>(null)
  const menuRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    setMoreOpen(false)
  }, [location.pathname])

  useLayoutEffect(() => {
    if (!moreOpen || !buttonRef.current) {
      setMenuPos(null)
      return
    }
    const rect = buttonRef.current.getBoundingClientRect()
    setMenuPos({
      left: Math.round(rect.right + 10),
      top: Math.round(rect.bottom)
    })
  }, [moreOpen])

  useEffect(() => {
    if (!moreOpen) return

    function onPointerDown(e: PointerEvent) {
      const target = e.target as Node
      if (moreRef.current?.contains(target) || menuRef.current?.contains(target)) {
        return
      }
      setMoreOpen(false)
    }

    function onKeyDown(e: KeyboardEvent) {
      if (e.key === 'Escape') setMoreOpen(false)
    }

    function onReposition() {
      if (!buttonRef.current) return
      const rect = buttonRef.current.getBoundingClientRect()
      setMenuPos({
        left: Math.round(rect.right + 10),
        top: Math.round(rect.bottom)
      })
    }

    document.addEventListener('pointerdown', onPointerDown)
    document.addEventListener('keydown', onKeyDown)
    window.addEventListener('resize', onReposition)
    return () => {
      document.removeEventListener('pointerdown', onPointerDown)
      document.removeEventListener('keydown', onKeyDown)
      window.removeEventListener('resize', onReposition)
    }
  }, [moreOpen])

  const secondaryActive = SECONDARY_NAV.some((id) => location.pathname === pathFor(id))

  return (
    <aside className="sidebar">
      <nav className="sidebar__nav" aria-label={t('nav.primary')}>
        {PRIMARY_NAV.map((id) => {
          const Icon = ICONS[id]
          return (
            <NavLink
              key={id}
              to={pathFor(id)}
              title={t(`nav.${id}`)}
              className={({ isActive }) =>
                ['sidebar__link', isActive ? 'sidebar__link--active' : ''].filter(Boolean).join(' ')
              }
              end={id === 'dashboard'}
            >
              <Icon size={20} strokeWidth={1.75} />
              <span className="sidebar__tooltip">{t(`nav.${id}`)}</span>
            </NavLink>
          )
        })}
      </nav>

      <div className="sidebar__bottom">
        <div className="sidebar__more" ref={moreRef}>
          <button
            ref={buttonRef}
            type="button"
            className={[
              'sidebar__link',
              'sidebar__link--button',
              moreOpen ? 'is-open' : '',
              secondaryActive ? 'sidebar__link--active' : ''
            ]
              .filter(Boolean)
              .join(' ')}
            title={t('nav.more')}
            aria-expanded={moreOpen}
            aria-haspopup="menu"
            onClick={() => setMoreOpen((v) => !v)}
          >
            <MoreHorizontal size={20} strokeWidth={1.75} />
            <span className="sidebar__tooltip">{t('nav.more')}</span>
          </button>
        </div>

        <NavLink
          to="/settings"
          title={t('nav.settings')}
          className={({ isActive }) =>
            ['sidebar__link', isActive ? 'sidebar__link--active' : ''].filter(Boolean).join(' ')
          }
        >
          <Settings size={20} strokeWidth={1.75} />
          <span className="sidebar__tooltip">{t('nav.settings')}</span>
        </NavLink>

        <Link to="/accounts" className="sidebar__user" title={username}>
          <Avatar alt={username} uuid={uuid} size="sm" glow />
          <div className="sidebar__user-flyout">
            <div className="sidebar__username">{username}</div>
            <Badge variant="prime">{formatTier(tier, locale)}</Badge>
          </div>
        </Link>
      </div>

      {moreOpen &&
        menuPos &&
        createPortal(
          <div
            ref={menuRef}
            className="sidebar__more-menu"
            role="menu"
            aria-label={t('nav.more')}
            style={{
              position: 'fixed',
              left: menuPos.left,
              top: menuPos.top,
              transform: 'translateY(-100%)'
            }}
          >
            <div className="sidebar__more-heading">{t('nav.more')}</div>
            {SECONDARY_NAV.map((id) => {
              const Icon = ICONS[id]
              const to = pathFor(id)
              const active = location.pathname === to
              return (
                <Link
                  key={id}
                  to={to}
                  role="menuitem"
                  className={`sidebar__more-item${active ? ' is-active' : ''}`}
                  onClick={() => setMoreOpen(false)}
                >
                  <Icon size={16} />
                  {t(`nav.${id}`)}
                </Link>
              )
            })}
          </div>,
          document.body
        )}
    </aside>
  )
}
