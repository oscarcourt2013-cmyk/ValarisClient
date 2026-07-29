import { useCallback, useEffect, useState } from 'react'
import { UserPlus, Users, RefreshCw } from 'lucide-react'
import type { FriendEntry } from '@shared/content-types'
import { PageShell } from '@renderer/pages/shared/PageShell'
import { Avatar, Button } from '@renderer/design-system/components'
import { useI18n } from '@renderer/context/I18nProvider'
import { useAccounts } from '@renderer/context/AccountProvider'

function statusDot(status: FriendEntry['status']): string {
  switch (status) {
    case 'online':
      return 'status-dot--online'
    case 'away':
      return 'status-dot--away'
    case 'in-game':
      return 'status-dot--ingame'
    default:
      return 'status-dot--offline'
  }
}

interface PartyInvite {
  id: string
  fromUsername?: string
  partyId?: string
  serverAddress?: string | null
}

export function FriendsPage() {
  const { t } = useI18n()
  const { launch } = useAccounts()
  const [friends, setFriends] = useState<FriendEntry[]>([])
  const [error, setError] = useState<string | null>(null)
  const [username, setUsername] = useState('')
  const [note, setNote] = useState('')
  const [editingId, setEditingId] = useState<string | null>(null)
  const [editNote, setEditNote] = useState('')
  const [party, setParty] = useState<{
    serverAddress?: string | null
    members?: { uuid: string; username: string; leader?: boolean }[]
  } | null>(null)
  const [partyInvites, setPartyInvites] = useState<PartyInvite[]>([])
  const [shareAddress, setShareAddress] = useState('')
  const [joinPrompt, setJoinPrompt] = useState<string | null>(null)

  const [busy, setBusy] = useState(false)

  const refresh = useCallback(async () => {
    setFriends(await window.primeLauncher.friends.list())
    try {
      const p = (await window.primeLauncher.party.get()) as {
        party?: {
          serverAddress?: string | null
          members?: { uuid: string; username: string; leader?: boolean }[]
        } | null
      }
      setParty(p?.party ?? null)
    } catch {
      setParty(null)
    }
  }, [])

  async function handleRefreshAll() {
    setBusy(true)
    setFriends(await window.primeLauncher.friends.refreshAll())
    setBusy(false)
  }

  useEffect(() => {
    void refresh()
    void window.primeLauncher.social.connect().catch(() => {
      // offline
    })
  }, [refresh])

  useEffect(() => {
    const unsub = window.primeLauncher.social.onEvent((event) => {
      if (
        event.t === 'presence' ||
        event.t === 'friend_request' ||
        event.t === 'friend_accepted' ||
        event.t === 'friend_removed' ||
        event.t === 'friend_update' ||
        event.t === 'snapshot'
      ) {
        if (event.t === 'snapshot') {
          const invites = Array.isArray(event.partyInvites)
            ? (event.partyInvites as Array<{
                id?: string
                fromUsername?: string
                partyId?: string
                party?: { serverAddress?: string | null }
              }>).flatMap((inv) => {
                if (!inv.id) return []
                return [
                  {
                    id: inv.id,
                    fromUsername: inv.fromUsername,
                    partyId: inv.partyId,
                    serverAddress: inv.party?.serverAddress ?? null
                  }
                ]
              })
            : []
          setPartyInvites(invites)
          if (event.party && typeof event.party === 'object') {
            setParty(event.party as typeof party)
          }
        }
        void refresh()
        return
      }
      if (event.t === 'party') {
        const partyPayload = event.party as typeof party
        setParty(partyPayload ?? null)
        return
      }
      if (event.t === 'party_invite') {
        const inviteId = typeof event.inviteId === 'string' ? event.inviteId : null
        if (inviteId) {
          setPartyInvites((prev) => {
            if (prev.some((i) => i.id === inviteId)) return prev
            return [
              ...prev,
              {
                id: inviteId,
                fromUsername: typeof event.fromUsername === 'string' ? event.fromUsername : undefined,
                partyId: typeof event.partyId === 'string' ? event.partyId : undefined,
                serverAddress:
                  typeof event.serverAddress === 'string' ? event.serverAddress : null
              }
            ]
          })
        }
        return
      }
      if (event.t === 'party_join_server') {
        const addr = typeof event.serverAddress === 'string' ? event.serverAddress : null
        if (addr) {
          setJoinPrompt(addr)
          setParty((prev) => (prev ? { ...prev, serverAddress: addr } : { serverAddress: addr }))
        }
        void refresh()
      }
    })
    return unsub
  }, [refresh])

  async function handleAdd() {
    if (username.trim().length < 3) {
      return
    }
    const result = await window.primeLauncher.friends.add(username, note || undefined)
    if (result.ok) {
      setError(null)
      setUsername('')
      setNote('')
      await refresh()
    } else {
      setError(result.error ?? t('friends.addFailed'))
    }
  }

  async function handleRemove(friendId: string) {
    await window.primeLauncher.friends.remove(friendId)
    await refresh()
  }

  async function handleSaveNote(friendId: string) {
    await window.primeLauncher.friends.updateNote(friendId, editNote)
    setEditingId(null)
    await refresh()
  }

  return (
    <PageShell
      title={t('pages.friends.title')}
      subtitle={t('pages.friends.subtitle')}
      actions={
        <Button variant="secondary" size="sm" icon={<RefreshCw size={14} />} disabled={busy} onClick={() => void handleRefreshAll()}>
          {t('friends.refreshStatus')}
        </Button>
      }
    >
      <div className="page-grid page-grid--2" style={{ marginBottom: 16 }}>
        <input
          className="modal__field"
          placeholder={t('friends.usernamePrompt')}
          value={username}
          maxLength={16}
          onChange={(e) => setUsername(e.target.value)}
        />
        <input
          className="modal__field"
          placeholder={t('friends.notePrompt')}
          value={note}
          onChange={(e) => setNote(e.target.value)}
        />
      </div>
      <Button
        variant="primary"
        icon={<UserPlus size={16} />}
        style={{ marginBottom: 16 }}
        disabled={username.trim().length < 3}
        onClick={() => void handleAdd()}
      >
        {t('friends.addFriend')}
      </Button>

      {error && (
        <p className="text-caption" style={{ color: 'var(--prime-error)', marginBottom: 12 }}>
          {error}
        </p>
      )}

      {partyInvites.map((invite) => (
        <div key={invite.id} className="list-row" style={{ marginBottom: 12, borderColor: 'var(--prime-red)' }}>
          <div className="list-row__body">
            <div className="list-row__title">{t('friends.partyInvite')}</div>
            <div className="list-row__desc">
              {invite.fromUsername || t('friends.partyInviteUnknown')}
              {invite.serverAddress ? ` · ${invite.serverAddress}` : ''}
            </div>
          </div>
          <Button
            variant="primary"
            size="sm"
            onClick={() =>
              void window.primeLauncher.party.accept(invite.id).then(() => {
                setPartyInvites((prev) => prev.filter((i) => i.id !== invite.id))
                return refresh()
              })
            }
          >
            {t('friends.acceptParty')}
          </Button>
          <Button
            variant="ghost"
            size="sm"
            onClick={() =>
              void window.primeLauncher.party.decline(invite.id).then(() => {
                setPartyInvites((prev) => prev.filter((i) => i.id !== invite.id))
              })
            }
          >
            {t('friends.declineParty')}
          </Button>
        </div>
      ))}

      {joinPrompt ? (
        <div className="list-row" style={{ marginBottom: 16, borderColor: 'var(--prime-red)' }}>
          <div className="list-row__body">
            <div className="list-row__title">{t('friends.partyJoinPrompt')}</div>
            <div className="list-row__desc">{joinPrompt}</div>
          </div>
          <Button
            variant="primary"
            size="sm"
            onClick={() =>
              void (async () => {
                const inst = await window.primeLauncher.instance.getDefault()
                if (!inst?.id) return
                await window.primeLauncher.settings.update({ lastServerAddress: joinPrompt })
                await launch(inst.id, joinPrompt)
                setJoinPrompt(null)
              })()
            }
          >
            {t('friends.joinPartyServer')}
          </Button>
          <Button variant="ghost" size="sm" onClick={() => setJoinPrompt(null)}>
            {t('friends.dismiss')}
          </Button>
        </div>
      ) : null}

      {party ? (
        <div className="list-row" style={{ marginBottom: 16 }}>
          <Users size={16} style={{ marginRight: 8, opacity: 0.7 }} />
          <div className="list-row__body">
            <div className="list-row__title">{t('friends.partyMembers')}</div>
            <div className="list-row__desc">
              {(party.members || []).map((m) => m.username).join(', ') || t('friends.partyAlone')}
              {party.serverAddress ? ` · ${party.serverAddress}` : ''}
            </div>
          </div>
          {party.serverAddress ? (
            <Button
              variant="primary"
              size="sm"
              onClick={() =>
                void (async () => {
                  const inst = await window.primeLauncher.instance.getDefault()
                  if (!inst?.id || !party.serverAddress) return
                  await window.primeLauncher.settings.update({ lastServerAddress: party.serverAddress })
                  await launch(inst.id, party.serverAddress)
                })()
              }
            >
              {t('friends.joinPartyServer')}
            </Button>
          ) : null}
          <Button variant="ghost" size="sm" onClick={() => void window.primeLauncher.party.leave().then(refresh)}>
            {t('friends.leaveParty')}
          </Button>
        </div>
      ) : (
        <Button
          variant="secondary"
          size="sm"
          style={{ marginBottom: 16 }}
          onClick={() => void window.primeLauncher.party.create().then(refresh)}
        >
          {t('friends.createParty')}
        </Button>
      )}

      <div className="page-grid page-grid--2" style={{ marginBottom: 12 }}>
        <input
          className="modal__field"
          placeholder={t('friends.shareServerPrompt')}
          value={shareAddress}
          onChange={(e) => setShareAddress(e.target.value)}
        />
        <Button
          variant="secondary"
          disabled={shareAddress.trim().length < 3}
          onClick={() =>
            void (async () => {
              try {
                const result = (await window.primeLauncher.party.setServer(shareAddress.trim())) as {
                  ok?: boolean
                  error?: string
                }
                if (result && result.ok === false) {
                  setError(result.error ?? t('friends.shareServerFailed'))
                  return
                }
                setError(null)
                setShareAddress('')
                await refresh()
              } catch (err) {
                setError(err instanceof Error ? err.message : t('friends.shareServerFailed'))
              }
            })()
          }
        >
          {t('friends.shareServer')}
        </Button>
      </div>

      {friends.length === 0 ? (
        <p className="text-caption">{t('friends.empty')}</p>
      ) : (
        <div className="page-list">
          {friends.map((friend) => (
            <div key={friend.id} className="list-row">
              <Avatar alt={friend.username} size="sm" />
              <div className={`status-dot ${statusDot(friend.status)}`} style={{ marginLeft: -8 }} />
              <div className="list-row__body">
                <div className="list-row__title">{friend.username}</div>
                {editingId === friend.id ? (
                  <input
                    className="modal__field"
                    style={{ marginTop: 4 }}
                    value={editNote}
                    onChange={(e) => setEditNote(e.target.value)}
                    placeholder={t('friends.notePlaceholder')}
                  />
                ) : (
                  <div className="list-row__desc">
                    {friend.note
                      ? friend.note
                      : friend.serverAddress
                        ? `Playing ${friend.serverAddress}`
                        : friend.activity ?? t('friends.offline')}
                  </div>
                )}
              </div>
              <div className="list-row__meta">
                {editingId === friend.id ? (
                  <Button variant="secondary" size="sm" onClick={() => void handleSaveNote(friend.id)}>
                    {t('friends.saveNote')}
                  </Button>
                ) : (
                  <Button
                    variant="ghost"
                    size="sm"
                    onClick={() => {
                      setEditingId(friend.id)
                      setEditNote(friend.note ?? '')
                    }}
                  >
                    {t('actions.save')}
                  </Button>
                )}
                <Button
                  variant="ghost"
                  size="sm"
                  onClick={() =>
                    void window.primeLauncher.chat.openDm(friend.id).then(() => {
                      window.location.hash = '#/chat'
                    })
                  }
                >
                  {t('friends.message')}
                </Button>
                <Button
                  variant="ghost"
                  size="sm"
                  onClick={() => void window.primeLauncher.party.invite(friend.id)}
                >
                  {t('friends.inviteParty')}
                </Button>
                {friend.incoming ? (
                  <Button variant="secondary" size="sm" onClick={() => void window.primeLauncher.friends.accept(friend.id).then(refresh)}>
                    {t('friends.accept')}
                  </Button>
                ) : null}
                <Button variant="ghost" size="sm" onClick={() => void handleRemove(friend.id)}>
                  {t('friends.remove')}
                </Button>
              </div>
            </div>
          ))}
        </div>
      )}
    </PageShell>
  )
}
