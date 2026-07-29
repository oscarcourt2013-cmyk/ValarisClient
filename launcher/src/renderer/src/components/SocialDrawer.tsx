import { useCallback, useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { Users, X, Gamepad2 } from 'lucide-react'
import { Button } from '@renderer/design-system/components'
import { useAccounts } from '@renderer/context/AccountProvider'
import { useI18n } from '@renderer/context/I18nProvider'
import { EmptyState } from '@renderer/components/EmptyState'
import { playUiSound } from '@renderer/lib/uiSounds'
import './SocialDrawer.css'

interface FriendRow {
  id: string
  username: string
  status: string
  activity?: string
  serverAddress?: string | null
}

interface SocialDrawerProps {
  open: boolean
  onClose: () => void
}

function statusClass(status: string): string {
  if (status === 'online' || status === 'in-game') return 'is-online'
  if (status === 'away') return 'is-away'
  return 'is-offline'
}

export function SocialDrawer({ open, onClose }: SocialDrawerProps) {
  const { t } = useI18n()
  const { launch, profile } = useAccounts()
  const [friends, setFriends] = useState<FriendRow[]>([])
  const [busyId, setBusyId] = useState<string | null>(null)

  const refresh = useCallback(async () => {
    const list = await window.primeLauncher.friends.list()
    setFriends(list as FriendRow[])
  }, [])

  useEffect(() => {
    if (!open) return
    void refresh()
    void window.primeLauncher.social.connect().catch(() => {
      // offline
    })
  }, [open, refresh])

  useEffect(() => {
    if (!open) return
    const unsub = window.primeLauncher.social.onEvent((event) => {
      if (
        event.t === 'presence' ||
        event.t === 'friend_request' ||
        event.t === 'friend_accepted' ||
        event.t === 'friend_removed' ||
        event.t === 'friend_update' ||
        event.t === 'snapshot' ||
        event.t === 'party' ||
        event.t === 'party_invite'
      ) {
        void refresh()
      }
    })
    return unsub
  }, [open, refresh])

  async function joinFriend(friend: FriendRow) {
    const address = friend.serverAddress?.trim()
    if (!address || !profile?.instanceId) return
    setBusyId(friend.id)
    playUiSound('click')
    await window.primeLauncher.settings.update({ lastServerAddress: address })
    await launch(profile.instanceId, address)
    setBusyId(null)
    onClose()
  }

  if (!open) return null

  const online = friends.filter((f) => f.status === 'online' || f.status === 'in-game')

  return (
    <div className="social-drawer">
      <button type="button" className="social-drawer__backdrop" onClick={onClose} aria-label="Close" />
      <aside className="social-drawer__panel">
        <header className="social-drawer__head">
          <div>
            <h2>{t('social.title')}</h2>
            <p>{t('social.onlineCount', { count: online.length })}</p>
          </div>
          <button type="button" className="social-drawer__close" onClick={onClose}>
            <X size={16} />
          </button>
        </header>

        {friends.length === 0 ? (
          <EmptyState
            icon={<Users size={22} />}
            title={t('social.emptyTitle')}
            description={t('social.emptyDesc')}
            action={
              <Link to="/friends" onClick={onClose}>
                <Button variant="secondary" size="sm">
                  {t('social.openFriends')}
                </Button>
              </Link>
            }
          />
        ) : (
          <ul className="social-drawer__list">
            {friends.slice(0, 12).map((f) => (
              <li key={f.id} className="social-drawer__row">
                <span className={`social-drawer__dot ${statusClass(f.status)}`} />
                <div className="social-drawer__text">
                  <strong>{f.username}</strong>
                  <span>
                    {f.serverAddress
                      ? `Playing ${f.serverAddress}`
                      : f.activity || f.status}
                  </span>
                </div>
                {f.serverAddress ? (
                  <Button
                    variant="ghost"
                    size="sm"
                    icon={<Gamepad2 size={14} />}
                    disabled={busyId === f.id}
                    onClick={() => void joinFriend(f)}
                  >
                    {t('common.join')}
                  </Button>
                ) : null}
              </li>
            ))}
          </ul>
        )}

        <footer className="social-drawer__foot">
          <Link to="/friends" onClick={onClose}>
            {t('social.openFriends')}
          </Link>
        </footer>
      </aside>
    </div>
  )
}
