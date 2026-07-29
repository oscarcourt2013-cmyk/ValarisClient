import { useCallback, useEffect, useMemo, useState } from 'react'
import { createPortal } from 'react-dom'
import { AnimatePresence, motion } from 'framer-motion'
import {
  ExternalLink,
  Globe,
  Info,
  Plus,
  RefreshCw,
  Trash2,
  Users,
  Wifi
} from 'lucide-react'
import type { FavoriteServer, ServerSocialLinks } from '@shared/types'
import { PageShell } from '@renderer/pages/shared/PageShell'
import { Badge, Button } from '@renderer/design-system/components'
import { useAccounts } from '@renderer/context/AccountProvider'
import { useI18n } from '@renderer/context/I18nProvider'
import { LoginModal } from '@renderer/components/LoginModal'
import './ServersPage.css'

function pingColor(ping: number | undefined): string {
  if (ping === undefined) return 'var(--prime-muted)'
  if (ping < 50) return 'var(--prime-success)'
  if (ping < 100) return 'var(--prime-warning)'
  return 'var(--prime-error)'
}

const SOCIAL_ORDER: { key: keyof ServerSocialLinks; labelKey: string }[] = [
  { key: 'discord', labelKey: 'servers.social.discord' },
  { key: 'store', labelKey: 'servers.social.store' },
  { key: 'website', labelKey: 'servers.social.website' },
  { key: 'vote', labelKey: 'servers.social.vote' },
  { key: 'youtube', labelKey: 'servers.social.youtube' },
  { key: 'tiktok', labelKey: 'servers.social.tiktok' },
  { key: 'instagram', labelKey: 'servers.social.instagram' },
  { key: 'facebook', labelKey: 'servers.social.facebook' },
  { key: 'x', labelKey: 'servers.social.x' }
]

function ServerInfoModal({
  server,
  onClose,
  onJoin,
  busy
}: {
  server: FavoriteServer
  onClose: () => void
  onJoin: () => void
  busy: boolean
}) {
  const { t } = useI18n()
  const links = useMemo(
    () => SOCIAL_ORDER.filter((s) => Boolean(server.social?.[s.key])),
    [server.social]
  )

  return createPortal(
    <motion.div
      className="modal-backdrop"
      initial={{ opacity: 0 }}
      animate={{ opacity: 1 }}
      exit={{ opacity: 0 }}
      onClick={onClose}
    >
      <motion.div
        className="modal server-info"
        initial={{ opacity: 0, scale: 0.96, y: 16 }}
        animate={{ opacity: 1, scale: 1, y: 0 }}
        exit={{ opacity: 0, scale: 0.96, y: 16 }}
        onClick={(e) => e.stopPropagation()}
      >
        <div
          className="server-info__banner"
          style={
            server.bannerUrl
              ? { backgroundImage: `url(${server.bannerUrl})` }
              : undefined
          }
        >
          {!server.bannerUrl && <div className="server-info__banner-fallback" />}
        </div>
        <div className="server-info__head">
          {server.iconUrl ? (
            <img className="server-info__icon" src={server.iconUrl} alt="" />
          ) : (
            <div className="server-info__icon server-info__icon--empty">
              <Globe size={22} />
            </div>
          )}
          <div>
            <h2 className="modal__title" style={{ marginBottom: 4 }}>
              {server.name}
            </h2>
            <p className="modal__subtitle" style={{ margin: 0 }}>
              {server.address}
            </p>
          </div>
        </div>

        <p className="server-info__desc">
          {server.fullDescription || server.description || server.motd || t('servers.noDescription')}
        </p>

        <div className="server-info__stats">
          <span>
            <Users size={14} /> {server.players ?? '—'}
            {server.maxPlayers != null ? ` / ${server.maxPlayers}` : ''}
          </span>
          <span style={{ color: pingColor(server.ping) }}>
            <Wifi size={14} /> {server.ping != null ? `${server.ping} ms` : '—'}
          </span>
          <span>{server.version || t('servers.versionUnknown')}</span>
          <Badge variant={server.online ? 'success' : 'default'}>
            {server.online ? t('servers.online') : t('servers.offlineCached')}
          </Badge>
        </div>

        {links.length > 0 && (
          <div className="server-info__links">
            {links.map(({ key, labelKey }) => (
              <a
                key={key}
                className="server-info__link"
                href={server.social?.[key]}
                target="_blank"
                rel="noreferrer"
              >
                <ExternalLink size={14} />
                {t(labelKey)}
              </a>
            ))}
          </div>
        )}

        <div className="modal__footer">
          <Button variant="ghost" onClick={onClose}>
            {t('common.close')}
          </Button>
          <Button variant="primary" disabled={busy} onClick={onJoin}>
            {t('common.play')}
          </Button>
        </div>
      </motion.div>
    </motion.div>,
    document.body
  )
}

function partnerFirst(list: FavoriteServer[]): FavoriteServer[] {
  return [...list].sort((a, b) => Number(Boolean(b.partner)) - Number(Boolean(a.partner)))
}

export function ServersPage() {
  const { t } = useI18n()
  const { activeAccount, launch, profile } = useAccounts()
  const [servers, setServers] = useState<FavoriteServer[]>([])
  const [name, setName] = useState('')
  const [address, setAddress] = useState('')
  const [message, setMessage] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)
  const [showLogin, setShowLogin] = useState(false)
  const [infoServer, setInfoServer] = useState<FavoriteServer | null>(null)

  const refresh = useCallback(async () => {
    setBusy(true)
    try {
      setServers(partnerFirst(await window.primeLauncher.servers.refreshAll()))
    } finally {
      setBusy(false)
    }
  }, [])

  useEffect(() => {
    void (async () => {
      setServers(partnerFirst(await window.primeLauncher.servers.list()))
      setServers(partnerFirst(await window.primeLauncher.servers.refreshAll()))
    })()
  }, [])

  async function handleAdd() {
    setBusy(true)
    setMessage(null)
    const result = await window.primeLauncher.servers.add(name, address)
    setBusy(false)
    if (result.ok) {
      setName('')
      setAddress('')
      setMessage(t('servers.added'))
      await refresh()
    } else {
      setMessage(result.error ?? t('servers.addFailed'))
    }
  }

  async function handleRemove(serverId: string) {
    const result = await window.primeLauncher.servers.remove(serverId)
    if (!result.ok) {
      setMessage(result.error ?? t('servers.removeFailed'))
      return
    }
    await refresh()
  }

  async function handleJoin(server: FavoriteServer) {
    if (!activeAccount) {
      setShowLogin(true)
      return
    }
    const instanceId = profile?.instanceId
    if (!instanceId) {
      setMessage(t('servers.joinNeedsAccount'))
      return
    }
    setBusy(true)
    await window.primeLauncher.profile.setInstance(instanceId)
    await launch(instanceId, server.address)
    setBusy(false)
  }

  return (
    <PageShell
      title={t('pages.servers.title')}
      subtitle={t('pages.servers.subtitle')}
      actions={
        <Button
          variant="secondary"
          size="sm"
          icon={<RefreshCw size={14} />}
          disabled={busy}
          onClick={() => void refresh()}
        >
          {t('servers.refresh')}
        </Button>
      }
    >
      <div className="servers-add">
        <input
          className="modal__field"
          placeholder={t('servers.serverName')}
          value={name}
          onChange={(e) => setName(e.target.value)}
        />
        <input
          className="modal__field"
          placeholder={t('servers.serverAddress')}
          value={address}
          onChange={(e) => setAddress(e.target.value)}
        />
        <Button
          variant="primary"
          icon={<Plus size={16} />}
          disabled={busy || name.trim().length < 1 || address.trim().length < 3}
          onClick={() => void handleAdd()}
        >
          {t('servers.addServer')}
        </Button>
      </div>

      {message && <p className="servers-message">{message}</p>}

      {servers.length === 0 ? (
        <p className="text-caption">{t('servers.empty')}</p>
      ) : (
        <div className="servers-grid">
          {servers.map((server) => (
            <article key={server.id} className="server-card">
              <div
                className="server-card__banner"
                style={
                  server.bannerUrl
                    ? { backgroundImage: `url(${server.bannerUrl})` }
                    : undefined
                }
              >
                {server.partner && (
                  <span className="server-card__partner">
                    <Badge variant="prime">{t('servers.partner')}</Badge>
                  </span>
                )}
              </div>
              <div className="server-card__body">
                <div className="server-card__title-row">
                  {server.iconUrl ? (
                    <img className="server-card__icon" src={server.iconUrl} alt="" />
                  ) : (
                    <div className="server-card__icon server-card__icon--empty">
                      <Globe size={18} />
                    </div>
                  )}
                  <div>
                    <h3>{server.name}</h3>
                    <p>{server.address}</p>
                  </div>
                </div>
                <p className="server-card__desc">
                  {server.description || server.motd || t('servers.noDescription')}
                </p>
                <div className="server-card__meta">
                  <span>
                    <Users size={13} /> {server.players ?? '—'}
                    {server.maxPlayers != null ? `/${server.maxPlayers}` : ''}
                  </span>
                  <span style={{ color: pingColor(server.ping) }}>
                    {server.ping != null ? `${server.ping}ms` : '—'}
                  </span>
                  <span>{server.version || '—'}</span>
                  <Badge variant={server.online ? 'success' : 'default'}>
                    {server.online ? t('servers.online') : t('servers.offlineCached')}
                  </Badge>
                </div>
                <div className="server-card__actions">
                  <Button
                    variant="primary"
                    size="sm"
                    disabled={busy}
                    onClick={() => void handleJoin(server)}
                  >
                    {t('common.play')}
                  </Button>
                  <Button
                    variant="secondary"
                    size="sm"
                    icon={<Info size={14} />}
                    onClick={() => setInfoServer(server)}
                  >
                    {t('servers.info')}
                  </Button>
                  {!server.partner && (
                    <Button
                      variant="ghost"
                      size="sm"
                      icon={<Trash2 size={14} />}
                      onClick={() => void handleRemove(server.id)}
                    />
                  )}
                </div>
              </div>
            </article>
          ))}
        </div>
      )}

      <AnimatePresence>
        {showLogin && <LoginModal onClose={() => setShowLogin(false)} />}
        {infoServer && (
          <ServerInfoModal
            server={infoServer}
            busy={busy}
            onClose={() => setInfoServer(null)}
            onJoin={() => void handleJoin(infoServer)}
          />
        )}
      </AnimatePresence>
    </PageShell>
  )
}
