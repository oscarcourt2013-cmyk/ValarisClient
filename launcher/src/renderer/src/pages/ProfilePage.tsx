import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { Clock, Shirt, Users } from 'lucide-react'
import { PageShell } from '@renderer/pages/shared/PageShell'
import { Avatar, Badge, Button } from '@renderer/design-system/components'
import { useAccounts } from '@renderer/context/AccountProvider'
import { useI18n } from '@renderer/context/I18nProvider'
import { formatTier, playerCapeUrl } from '@shared/format'
import type { FriendEntry } from '@shared/content-types'
import { SkinViewer3D } from '@renderer/components/SkinViewer3D'
import { EmptyState } from '@renderer/components/EmptyState'
import './ProfilePage.css'

export function ProfilePage() {
  const { t, locale } = useI18n()
  const { prime, activeAccount, profile, accounts } = useAccounts()
  const [friends, setFriends] = useState<FriendEntry[]>([])
  const [skinDataUrl, setSkinDataUrl] = useState<string | null>(null)

  const username = activeAccount?.username ?? prime?.username ?? t('common.guest')
  const uuid = activeAccount?.uuid
  const capeUrl = playerCapeUrl(uuid, username, activeAccount?.capeUrl)
  const playMinutes = profile?.playTimeMinutes ?? 0
  const hours = Math.floor(playMinutes / 60)
  const minutes = playMinutes % 60
  const memberSince = prime?.createdAt
    ? new Date(prime.createdAt).toLocaleDateString(locale === 'fr' ? 'fr-FR' : 'en-US')
    : t('common.never')

  useEffect(() => {
    void window.primeLauncher.friends.list().then(setFriends).catch(() => setFriends([]))
    void window.primeLauncher.skins.activeData().then(setSkinDataUrl)
  }, [activeAccount?.id])

  const recentFriends = friends.slice(0, 6)

  return (
    <PageShell title={t('pages.profile.title')} subtitle={t('pages.profile.subtitle')}>
      <div className="profile">
        <section className="profile__hero">
          <SkinViewer3D
            uuid={uuid}
            username={username}
            skinUrl={skinDataUrl}
            capeUrl={capeUrl}
            pose="idle"
            width={200}
            height={280}
            showControls
            backdrop="soft"
          />
          <div className="profile__meta">
            <div className="profile__name-row">
              <Avatar alt={username} uuid={uuid} size="lg" src={activeAccount?.skinUrl} />
              <div>
                <h2>{username}</h2>
                <p className="text-caption">{activeAccount?.type ?? '—'}</p>
              </div>
            </div>
            <div className="profile__badges">
              <Badge variant="prime">{formatTier(prime?.tier ?? 'free', locale)}</Badge>
              {prime?.level != null && (
                <Badge variant="default">{t('accounts.level', { level: prime.level })}</Badge>
              )}
            </div>
            <dl className="profile__stats">
              <div>
                <dt>{t('profile.memberSince')}</dt>
                <dd>{memberSince}</dd>
              </div>
              <div>
                <dt>{t('profile.playtime')}</dt>
                <dd>
                  <Clock size={14} /> {t('profile.playtimeValue', { hours, minutes })}
                </dd>
              </div>
              <div>
                <dt>{t('profile.accounts')}</dt>
                <dd>{accounts.length}</dd>
              </div>
              <div>
                <dt>{t('profile.friends')}</dt>
                <dd>{friends.length}</dd>
              </div>
            </dl>
            <div className="profile__actions">
              <Link to="/skins">
                <Button variant="secondary" size="sm" icon={<Shirt size={14} />}>
                  {t('nav.skins')}
                </Button>
              </Link>
              <Link to="/friends">
                <Button variant="secondary" size="sm" icon={<Users size={14} />}>
                  {t('nav.friends')}
                </Button>
              </Link>
              <Link to="/accounts">
                <Button variant="ghost" size="sm">
                  {t('nav.accounts')}
                </Button>
              </Link>
            </div>
          </div>
        </section>

        <section className="profile__panel">
          <h3>{t('profile.recentFriends')}</h3>
          {recentFriends.length === 0 ? (
            <EmptyState
              icon={<Users size={20} />}
              title={t('profile.noFriends')}
              description={t('profile.noFriendsHint')}
            />
          ) : (
            <ul className="profile__friend-list">
              {recentFriends.map((f) => (
                <li key={f.id}>
                  <strong>{f.username}</strong>
                  <span>{f.status}</span>
                </li>
              ))}
            </ul>
          )}
        </section>
      </div>
    </PageShell>
  )
}
