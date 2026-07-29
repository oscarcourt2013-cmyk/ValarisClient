import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { motion, AnimatePresence } from 'framer-motion'
import { Play, UserCog, Box, Shirt, ChevronRight, Globe, Users } from 'lucide-react'
import { Badge, Button, Select } from '@renderer/design-system/components'
import { useAccounts } from '@renderer/context/AccountProvider'
import { useI18n } from '@renderer/context/I18nProvider'
import { LoginModal } from '@renderer/components/LoginModal'
import { UpdateModal } from '@renderer/components/UpdateModal'
import { CrashReportPanel } from '@renderer/components/CrashReportPanel'
import { LaunchStrip, Skeleton } from '@renderer/components/Skeleton'
import { EmptyState } from '@renderer/components/EmptyState'
import type { UpdateStatusDto } from '@shared/ipc'
import { formatLoader, formatTier, playerCapeUrl } from '@shared/format'
import type { FavoriteServer, GameInstance, NewsItem } from '@shared/types'
import type { FriendEntry } from '@shared/content-types'
import { SkinViewer3D } from '@renderer/components/SkinViewer3D'
import { playUiSound } from '@renderer/lib/uiSounds'
import './DashboardPage.css'

interface DashboardPageProps {
  news: NewsItem[]
  servers: FavoriteServer[]
}

export function DashboardPage({ news, servers }: DashboardPageProps) {
  const { t, locale } = useI18n()
  const {
    prime,
    accounts,
    activeAccount,
    profile,
    launch,
    launchMessage,
    launchProgress,
    gameRunning,
    clearLaunchMessage,
    setActive
  } = useAccounts()
  const [showLogin, setShowLogin] = useState(false)
  const [launching, setLaunching] = useState(false)
  const [instances, setInstances] = useState<GameInstance[]>([])
  const [instance, setInstance] = useState<GameInstance | null>(null)
  const [selectedServer, setSelectedServer] = useState('')
  const [skinDataUrl, setSkinDataUrl] = useState<string | null>(null)
  const [crashDismissed, setCrashDismissed] = useState(false)
  const [updateStatus, setUpdateStatus] = useState<UpdateStatusDto | null>(null)
  const [showUpdate, setShowUpdate] = useState(false)
  const [friends, setFriends] = useState<FriendEntry[]>([])

  useEffect(() => {
    void window.primeLauncher.friends.list().then(setFriends).catch(() => setFriends([]))
  }, [])

  useEffect(() => {
    void (async () => {
      const status = await window.primeLauncher.update.check()
      setUpdateStatus(status)
      if (status.anyUpdateAvailable) {
        const settings = await window.primeLauncher.settings.get()
        const key = `launcher:${status.launcher.latest}|mod:${status.mod.latest}`
        if (settings.dismissedUpdateBanner !== key) {
          setShowUpdate(true)
        }
      }
    })()
  }, [])

  useEffect(() => {
    if (launchProgress?.phase === 'crashed') {
      setCrashDismissed(false)
    }
  }, [launchProgress?.phase, launchProgress?.crash?.title])

  useEffect(() => {
    void (async () => {
      const list = (await window.primeLauncher.instance.list()) as GameInstance[]
      setInstances(list)
      let current: GameInstance | null = null
      if (profile?.instanceId) {
        current = list.find((i: GameInstance) => i.id === profile.instanceId) ?? null
      }
      if (!current) {
        current = (await window.primeLauncher.instance.getDefault()) ?? list[0] ?? null
      }
      setInstance(current)
    })()
  }, [profile?.instanceId])

  useEffect(() => {
    void (async () => {
      const settings = await window.primeLauncher.settings.get()
      const last =
        typeof settings.lastServerAddress === 'string' ? settings.lastServerAddress.trim() : ''
      if (last) {
        setSelectedServer(last)
        return
      }
      if (servers[0]?.address) setSelectedServer(servers[0].address)
    })()
  }, [servers])

  useEffect(() => {
    void window.primeLauncher.skins.activeData().then(setSkinDataUrl)
  }, [activeAccount?.id])

  const mcUsername = activeAccount?.username ?? t('common.guest')
  const capeUrl = playerCapeUrl(activeAccount?.uuid, mcUsername, activeAccount?.capeUrl)

  async function handlePlay() {
    if (!activeAccount || !instance) {
      setShowLogin(true)
      return
    }
    setLaunching(true)
    playUiSound('click')
    clearLaunchMessage()
    await window.primeLauncher.profile.setInstance(instance.id)
    const server = selectedServer.trim() || undefined
    if (server) {
      await window.primeLauncher.settings.update({ lastServerAddress: server })
    }
    await launch(instance.id, server)
    setLaunching(false)
  }

  async function handleJoinServer(address: string) {
    if (!activeAccount || !instance) {
      setShowLogin(true)
      return
    }
    setSelectedServer(address)
    setLaunching(true)
    playUiSound('click')
    clearLaunchMessage()
    await window.primeLauncher.profile.setInstance(instance.id)
    await window.primeLauncher.settings.update({ lastServerAddress: address })
    await launch(instance.id, address)
    setLaunching(false)
  }

  async function handleInstanceChange(id: string) {
    const next = instances.find((i) => i.id === id)
    if (!next) return
    setInstance(next)
    await window.primeLauncher.profile.setInstance(id)
    playUiSound('click')
  }

  async function handleAccountChange(id: string) {
    await setActive(id)
    playUiSound('click')
  }

  if (!instance) {
    return (
      <div className="home home--loading">
        <Skeleton width={280} height={360} radius={16} />
        <div style={{ display: 'flex', flexDirection: 'column', gap: 12, flex: 1 }}>
          <Skeleton width="40%" height={14} />
          <Skeleton width="70%" height={36} />
          <Skeleton width="55%" height={18} />
          <Skeleton width={180} height={48} radius={12} />
        </div>
      </div>
    )
  }

  const topNews = news.slice(0, 3)
  const topServers = servers.slice(0, 4)
  const activeFriends = friends
    .filter((f) => f.status === 'online' || f.status === 'in-game' || f.status === 'away')
    .slice(0, 5)
  const preparing =
    launching ||
    (launchProgress != null &&
      ['start', 'fabric', 'download', 'mods', 'launch'].includes(launchProgress.phase))
  const showLaunchStrip =
    preparing ||
    gameRunning ||
    (launchProgress?.phase === 'error' && Boolean(launchMessage || launchProgress.detail))

  function launchStatusLabel(): string {
    if (gameRunning) return t('dashboard.launchStatus.running')
    if (launchProgress?.phase === 'error') {
      return launchMessage || launchProgress.detail || t('dashboard.launchStatus.error')
    }
    switch (launchProgress?.phase) {
      case 'download':
        return t('dashboard.launchStatus.download')
      case 'mods':
      case 'fabric':
        return t('dashboard.launchStatus.mods')
      case 'launch':
        return t('dashboard.launchStatus.starting')
      case 'start':
      default:
        return t('dashboard.launchStatus.preparing')
    }
  }

  return (
    <motion.div
      className="home"
      initial={{ opacity: 0 }}
      animate={{ opacity: 1 }}
      transition={{ duration: 0.35 }}
    >
      {!activeAccount && (
        <div className="home__alert">
          <div>
            <strong>{t('dashboard.signInTitle')}</strong>
            <p>{t('dashboard.signInHint')}</p>
          </div>
          <Button variant="primary" onClick={() => setShowLogin(true)}>
            {t('dashboard.addAccount')}
          </Button>
        </div>
      )}

      {(launchProgress?.phase === 'crashed' || showLaunchStrip) && (
        <div className="home__status">
          {launchProgress?.phase === 'crashed' && launchProgress.crash && !crashDismissed && (
            <CrashReportPanel crash={launchProgress.crash} onDismiss={() => setCrashDismissed(true)} />
          )}
          {showLaunchStrip && launchProgress?.phase !== 'crashed' && (
            <LaunchStrip
              label={launchStatusLabel()}
              percent={launchProgress?.percent}
              phase={launchProgress?.phase}
              running={gameRunning}
            />
          )}
        </div>
      )}

      <section className="home__stage">
        <div className="home__character">
          <div className="home__character-glow" />
          <SkinViewer3D
            className="home__viewer"
            uuid={activeAccount?.uuid}
            username={mcUsername}
            skinUrl={skinDataUrl}
            capeUrl={capeUrl}
            pose="idle"
            width={300}
            height={400}
            showControls
          />
        </div>

        <div className="home__copy">
          <p className="home__eyebrow">{t('dashboard.welcomeBack')}</p>
          <h1 className="home__name">{mcUsername}</h1>
          <p className="home__meta">
            {instance.name}
            <span>·</span>
            MC {instance.minecraftVersion}
            <span>·</span>
            {formatLoader(instance.loader)}
          </p>

          <div className="home__chips">
            <Badge variant="prime">{formatTier(prime?.tier ?? 'free', locale)}</Badge>
            <span className="home__chip">
              {instance.ramMb} MB · {instance.modCount} mods
            </span>
          </div>

          <div className="home__playhub">
            <label className="home__field">
              <span>{t('dashboard.playHub.account')}</span>
              <Select
                value={activeAccount?.id ?? ''}
                disabled={accounts.length === 0}
                aria-label={t('dashboard.playHub.account')}
                onChange={(id) => void handleAccountChange(id)}
                options={
                  accounts.length === 0
                    ? [{ value: '', label: t('common.guest') }]
                    : accounts.map((a) => ({ value: a.id, label: a.username }))
                }
              />
            </label>
            <label className="home__field">
              <span>{t('dashboard.playHub.instance')}</span>
              <Select
                value={instance.id}
                aria-label={t('dashboard.playHub.instance')}
                onChange={(id) => void handleInstanceChange(id)}
                options={instances.map((i) => ({
                  value: i.id,
                  label: `${i.name} · ${i.minecraftVersion}`
                }))}
              />
            </label>
            <label className="home__field">
              <span>{t('dashboard.playHub.server')}</span>
              <Select
                value={selectedServer}
                aria-label={t('dashboard.playHub.server')}
                onChange={(v) => {
                  setSelectedServer(v)
                  playUiSound('click')
                }}
                options={[
                  { value: '', label: t('dashboard.playHub.singleplayer') },
                  ...servers.map((s) => ({ value: s.address, label: s.name }))
                ]}
              />
            </label>
          </div>

          <div className="home__cta">
            <Button
              variant="primary"
              size="xl"
              icon={<Play size={22} fill="currentColor" />}
              disabled={launching || gameRunning}
              onClick={() => void handlePlay()}
            >
              {gameRunning
                ? t('dashboard.launchStatus.running')
                : launching
                  ? t('common.launching')
                  : t('common.play')}
            </Button>
            <div className="home__links">
              <Link to="/instances" className="home__link">
                <Box size={15} />
                {t('dashboard.instance')}
              </Link>
              <Link to="/skins" className="home__link">
                <Shirt size={15} />
                {t('nav.skins')}
              </Link>
              <Link to="/profile" className="home__link">
                <UserCog size={15} />
                {t('dashboard.profile')}
              </Link>
            </div>
          </div>
        </div>
      </section>

      <section className="home__rail">
        <div className="home__panel">
          <div className="home__panel-head">
            <h2>{t('dashboard.favoriteServers')}</h2>
            <Link to="/servers" className="home__panel-more">
              <ChevronRight size={16} />
            </Link>
          </div>
          {topServers.length === 0 ? (
            <EmptyState
              icon={<Globe size={20} />}
              title={t('dashboard.noServers')}
              description={t('dashboard.noServersHint')}
              action={
                <Link to="/servers">
                  <Button variant="secondary" size="sm">
                    {t('dashboard.addServer')}
                  </Button>
                </Link>
              }
            />
          ) : (
            <ul className="home__list">
              {topServers.map((s) => (
                <li key={s.id} className="home__row">
                  <div className="home__row-icon">
                    <Globe size={16} />
                  </div>
                  <div className="home__row-text">
                    <strong>{s.name}</strong>
                    <span>{s.address}</span>
                  </div>
                  <Button
                    variant="secondary"
                    size="sm"
                    disabled={launching || gameRunning}
                    onClick={() => void handleJoinServer(s.address)}
                  >
                    {gameRunning ? t('dashboard.launchStatus.inGame') : t('common.join')}
                  </Button>
                </li>
              ))}
            </ul>
          )}
        </div>

        <div className="home__panel">
          <div className="home__panel-head">
            <h2>{t('dashboard.friendsActivity')}</h2>
            <Link to="/friends" className="home__panel-more">
              <ChevronRight size={16} />
            </Link>
          </div>
          {activeFriends.length === 0 ? (
            <EmptyState
              icon={<Users size={20} />}
              title={t('dashboard.noFriendsOnline')}
              description={t('dashboard.noFriendsOnlineHint')}
              action={
                <Link to="/friends">
                  <Button variant="secondary" size="sm">
                    {t('nav.friends')}
                  </Button>
                </Link>
              }
            />
          ) : (
            <ul className="home__list">
              {activeFriends.map((f) => (
                <li key={f.id} className="home__row">
                  <div className="home__row-icon">
                    <Users size={16} />
                  </div>
                  <div className="home__row-text">
                    <strong>{f.username}</strong>
                    <span>
                      {f.status === 'in-game'
                        ? f.serverAddress || f.activity || t('dashboard.friendInGame')
                        : f.status === 'away'
                          ? t('dashboard.friendAway')
                          : t('dashboard.friendOnline')}
                    </span>
                  </div>
                </li>
              ))}
            </ul>
          )}
        </div>

        <div className="home__panel">
          <div className="home__panel-head">
            <h2>{t('dashboard.news')}</h2>
            <Link to="/news" className="home__panel-more">
              {t('dashboard.moreNews')}
              <ChevronRight size={16} />
            </Link>
          </div>
          <ul className="home__list">
            {topNews.map((item) => (
              <li key={item.id} className="home__news">
                <Badge variant={item.tag === 'update' ? 'red' : 'default'}>{t(`newsTag.${item.tag}`)}</Badge>
                <div>
                  <strong>{item.title}</strong>
                  <p>{item.summary}</p>
                </div>
              </li>
            ))}
          </ul>
        </div>
      </section>

      <AnimatePresence>{showLogin && <LoginModal onClose={() => setShowLogin(false)} />}</AnimatePresence>
      <AnimatePresence>
        {showUpdate && updateStatus && (
          <UpdateModal
            status={updateStatus}
            onClose={() => setShowUpdate(false)}
            onUpdated={() => void window.primeLauncher.update.check(true).then(setUpdateStatus)}
          />
        )}
      </AnimatePresence>
    </motion.div>
  )
}
