import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { ImagePlus, MessageCircle, RefreshCw, Send } from 'lucide-react'
import { PageShell } from '@renderer/pages/shared/PageShell'
import { Avatar, Button } from '@renderer/design-system/components'
import { useI18n } from '@renderer/context/I18nProvider'
import { useAccounts } from '@renderer/context/AccountProvider'
import './ChatPage.css'

interface Conversation {
  id: string
  participants: { uuid: string; username: string }[]
  updatedAt: string
}

interface ChatMessage {
  id: string
  conversationId?: string
  senderUuid: string
  senderUsername?: string | null
  text: string
  imageUrl: string | null
  createdAt: string
}

type TimelineItem =
  | { kind: 'day'; key: string; label: string }
  | { kind: 'message'; key: string; message: ChatMessage }

function displayName(
  message: ChatMessage,
  participants: { uuid: string; username: string }[] | undefined
): string {
  if (message.senderUsername && message.senderUsername.trim()) {
    return message.senderUsername.trim()
  }
  const fromParticipants = participants?.find((p) => p.uuid === message.senderUuid)?.username
  if (fromParticipants && fromParticipants.trim()) {
    return fromParticipants.trim()
  }
  return message.senderUuid.slice(0, 8)
}

function peerOf(conversation: Conversation, selfUuid: string | undefined): {
  uuid: string
  username: string
} {
  const other = conversation.participants?.find((p) => p.uuid !== selfUuid)
  if (other) {
    return other
  }
  const first = conversation.participants?.[0]
  return first ?? { uuid: conversation.id, username: conversation.id.slice(0, 8) }
}

function formatClock(iso: string): string {
  try {
    return new Date(iso).toLocaleTimeString(undefined, { hour: '2-digit', minute: '2-digit' })
  } catch {
    return ''
  }
}

function formatDayLabel(
  iso: string,
  labels: { today: string; yesterday: string }
): string {
  const date = new Date(iso)
  const today = new Date()
  const yesterday = new Date()
  yesterday.setDate(today.getDate() - 1)
  const sameDay = (a: Date, b: Date) =>
    a.getFullYear() === b.getFullYear() &&
    a.getMonth() === b.getMonth() &&
    a.getDate() === b.getDate()
  if (sameDay(date, today)) return labels.today
  if (sameDay(date, yesterday)) return labels.yesterday
  return date.toLocaleDateString(undefined, {
    weekday: 'short',
    day: 'numeric',
    month: 'short'
  })
}

function formatRelative(iso: string): string {
  try {
    const then = new Date(iso).getTime()
    const diff = Date.now() - then
    if (diff < 60_000) return 'now'
    if (diff < 3_600_000) return `${Math.floor(diff / 60_000)}m`
    if (diff < 86_400_000) return `${Math.floor(diff / 3_600_000)}h`
    return new Date(iso).toLocaleDateString(undefined, { day: 'numeric', month: 'short' })
  } catch {
    return ''
  }
}

function buildTimeline(
  messages: ChatMessage[],
  labels: { today: string; yesterday: string }
): TimelineItem[] {
  const items: TimelineItem[] = []
  let lastDay = ''
  for (const message of messages) {
    const dayKey = message.createdAt.slice(0, 10)
    if (dayKey !== lastDay) {
      lastDay = dayKey
      items.push({
        kind: 'day',
        key: `day-${dayKey}`,
        label: formatDayLabel(message.createdAt, labels)
      })
    }
    items.push({ kind: 'message', key: message.id, message })
  }
  return items
}

export function ChatPage() {
  const { t } = useI18n()
  const { activeAccount } = useAccounts()
  const selfUuid = activeAccount?.uuid

  const [conversations, setConversations] = useState<Conversation[]>([])
  const [activeId, setActiveId] = useState<string | null>(null)
  const [messages, setMessages] = useState<ChatMessage[]>([])
  const [text, setText] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)
  const [live, setLive] = useState(false)
  const [peerTyping, setPeerTyping] = useState(false)
  const activeIdRef = useRef<string | null>(null)
  const bottomRef = useRef<HTMLDivElement | null>(null)
  const inputRef = useRef<HTMLInputElement | null>(null)
  const typingTimer = useRef<ReturnType<typeof setTimeout> | null>(null)
  const lastTypingSent = useRef(0)

  useEffect(() => {
    activeIdRef.current = activeId
  }, [activeId])

  const reload = useCallback(async () => {
    try {
      await window.primeLauncher.social.connect()
      setLive(true)
      const list = (await window.primeLauncher.chat.conversations()) as Conversation[]
      setConversations(list)
      setError(null)
    } catch (err) {
      setLive(false)
      setError(err instanceof Error ? err.message : 'Chat unavailable')
    }
  }, [])

  useEffect(() => {
    void reload()
  }, [reload])

  useEffect(() => {
    if (!activeId) {
      setMessages([])
      setPeerTyping(false)
      return
    }
    setPeerTyping(false)
    void window.primeLauncher.chat.messages(activeId).then((list) => {
      setMessages(list as ChatMessage[])
    })
  }, [activeId])

  useEffect(() => {
    const unsub = window.primeLauncher.social.onEvent((event) => {
      if (event.t === 'ready' || event.t === 'snapshot') {
        setLive(true)
      }
      if (event.t === 'typing') {
        const conversationId =
          typeof event.conversationId === 'string' ? event.conversationId : null
        if (conversationId && conversationId === activeIdRef.current) {
          setPeerTyping(true)
          if (typingTimer.current) clearTimeout(typingTimer.current)
          typingTimer.current = setTimeout(() => setPeerTyping(false), 3000)
        }
        return
      }
      if (event.t !== 'message' || !event.message || typeof event.message !== 'object') {
        return
      }
      const incoming = event.message as ChatMessage
      if (!incoming.id || !incoming.conversationId) {
        return
      }
      void window.primeLauncher.chat.conversations().then((list) => {
        setConversations(list as Conversation[])
      })
      if (incoming.conversationId !== activeIdRef.current) {
        return
      }
      setPeerTyping(false)
      setMessages((prev) => {
        if (prev.some((m) => m.id === incoming.id)) {
          return prev
        }
        return [...prev, incoming]
      })
    })
    return () => {
      unsub()
      if (typingTimer.current) clearTimeout(typingTimer.current)
    }
  }, [])

  function onComposerChange(value: string): void {
    setText(value)
    if (!activeIdRef.current || !value.trim()) return
    const now = Date.now()
    if (now - lastTypingSent.current < 2000) return
    lastTypingSent.current = now
    void window.primeLauncher.social.sendTyping(activeIdRef.current)
  }

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' })
  }, [messages.length])

  const active = useMemo(
    () => conversations.find((c) => c.id === activeId) ?? null,
    [conversations, activeId]
  )

  const peer = useMemo(
    () => (active ? peerOf(active, selfUuid) : null),
    [active, selfUuid]
  )

  const timeline = useMemo(
    () =>
      buildTimeline(messages, {
        today: t('chat.today'),
        yesterday: t('chat.yesterday')
      }),
    [messages, t]
  )

  async function send(): Promise<void> {
    if (!activeId || !text.trim()) return
    setBusy(true)
    try {
      const sent = (await window.primeLauncher.chat.send(activeId, text.trim())) as ChatMessage
      setText('')
      setMessages((prev) => {
        if (prev.some((m) => m.id === sent.id)) {
          return prev
        }
        return [...prev, sent]
      })
      inputRef.current?.focus()
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Send failed')
    } finally {
      setBusy(false)
    }
  }

  async function attachImage(): Promise<void> {
    if (!activeId) return
    const launcher = window.primeLauncher as typeof window.primeLauncher & {
      dialog?: {
        openFile?: (opts: {
          filters: { name: string; extensions: string[] }[]
        }) => Promise<string | { filePaths?: string[] } | null>
      }
    }
    const result = await launcher.dialog?.openFile?.({
      filters: [{ name: 'Images', extensions: ['png', 'jpg', 'jpeg', 'webp', 'gif'] }]
    })
    const filePath =
      typeof result === 'string'
        ? result
        : result && typeof result === 'object'
          ? result.filePaths?.[0]
          : undefined
    const path =
      filePath ||
      (() => {
        const manual = window.prompt(t('chat.imagePathPrompt'))
        return manual || null
      })()
    if (!path) return
    setBusy(true)
    try {
      const url = await window.primeLauncher.chat.upload(path)
      const sent = (await window.primeLauncher.chat.send(
        activeId,
        text.trim() || '',
        url
      )) as ChatMessage
      setText('')
      setMessages((prev) => {
        if (prev.some((m) => m.id === sent.id)) {
          return prev
        }
        return [...prev, sent]
      })
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Upload failed')
    } finally {
      setBusy(false)
    }
  }

  return (
    <PageShell
      title={t('pages.chat.title')}
      subtitle={t('pages.chat.subtitle')}
      actions={
        <Button variant="secondary" size="sm" icon={<RefreshCw size={14} />} onClick={() => void reload()}>
          {t('chat.refresh')}
        </Button>
      }
    >
      <div className="chat-page">
        {error ? <p className="chat-page__error">{error}</p> : null}

        <div className="chat-shell">
          <aside className="chat-sidebar">
            <div className="chat-sidebar__head">
              <span className="chat-sidebar__title">{t('chat.conversations')}</span>
              {live ? (
                <span className="chat-sidebar__live">
                  <span className="chat-sidebar__live-dot" />
                  {t('chat.live')}
                </span>
              ) : null}
            </div>
            <div className="chat-sidebar__list">
              {conversations.length === 0 ? (
                <p className="chat-sidebar__empty">{t('chat.empty')}</p>
              ) : (
                conversations.map((c) => {
                  const p = peerOf(c, selfUuid)
                  return (
                    <button
                      key={c.id}
                      type="button"
                      className={`chat-conv${activeId === c.id ? ' chat-conv--active' : ''}`}
                      onClick={() => setActiveId(c.id)}
                    >
                      <Avatar uuid={p.uuid} alt={p.username} size="sm" />
                      <div className="chat-conv__body">
                        <div className="chat-conv__name">{p.username}</div>
                        <div className="chat-conv__meta">{formatRelative(c.updatedAt)}</div>
                      </div>
                    </button>
                  )
                })
              )}
            </div>
          </aside>

          <section className="chat-thread">
            {active && peer ? (
              <>
                <header className="chat-thread__header">
                  <Avatar uuid={peer.uuid} alt={peer.username} size="md" glow />
                  <div className="chat-thread__peer">
                    <div className="chat-thread__peer-name">{peer.username}</div>
                    <div className="chat-thread__peer-sub">{t('chat.directMessage')}</div>
                  </div>
                </header>

                <div className="chat-thread__messages">
                  {messages.length === 0 ? (
                    <div className="chat-thread__empty">
                      <div className="chat-thread__empty-icon">
                        <MessageCircle size={28} />
                      </div>
                      <div className="chat-thread__empty-title">{t('chat.startTitle')}</div>
                      <p className="chat-thread__empty-text">{t('chat.startBody', { name: peer.username })}</p>
                    </div>
                  ) : (
                    timeline.map((item) => {
                      if (item.kind === 'day') {
                        return (
                          <div key={item.key} className="chat-day">
                            {item.label}
                          </div>
                        )
                      }
                      const m = item.message
                      const mine = Boolean(selfUuid && m.senderUuid === selfUuid)
                      const name = displayName(m, active.participants)
                      return (
                        <div
                          key={item.key}
                          className={`chat-row ${mine ? 'chat-row--mine' : 'chat-row--theirs'}`}
                        >
                          {!mine ? <Avatar uuid={m.senderUuid} alt={name} size="sm" /> : null}
                          <div className="chat-bubble">
                            {!mine ? <div className="chat-bubble__name">{name}</div> : null}
                            {m.text ? <div className="chat-bubble__text">{m.text}</div> : null}
                            {m.imageUrl ? (
                              <img className="chat-bubble__image" src={m.imageUrl} alt="" />
                            ) : null}
                            <div className="chat-bubble__time">{formatClock(m.createdAt)}</div>
                          </div>
                        </div>
                      )
                    })
                  )}
                  <div ref={bottomRef} />
                </div>

                {peerTyping ? (
                  <p className="chat-composer__hint" style={{ marginBottom: 4 }}>
                    {t('chat.typing', { name: peer.username })}
                  </p>
                ) : null}

                <div className="chat-composer">
                  <div className="chat-composer__bar">
                    <input
                      ref={inputRef}
                      className="chat-composer__input"
                      value={text}
                      disabled={busy}
                      placeholder={t('chat.messagePlaceholder')}
                      onChange={(e) => onComposerChange(e.target.value)}
                      onKeyDown={(e) => {
                        if (e.key === 'Enter' && !e.shiftKey) {
                          e.preventDefault()
                          void send()
                        }
                      }}
                    />
                    <Button
                      variant="ghost"
                      size="sm"
                      icon={<ImagePlus size={16} />}
                      disabled={busy}
                      onClick={() => void attachImage()}
                      aria-label={t('chat.image')}
                    />
                    <Button
                      variant="primary"
                      size="sm"
                      icon={<Send size={15} />}
                      disabled={busy || !text.trim()}
                      onClick={() => void send()}
                    >
                      {t('chat.send')}
                    </Button>
                  </div>
                  <p className="chat-composer__hint">{t('chat.composerHint')}</p>
                </div>
              </>
            ) : (
              <div className="chat-thread__empty">
                <div className="chat-thread__empty-icon">
                  <MessageCircle size={28} />
                </div>
                <div className="chat-thread__empty-title">{t('chat.selectConversation')}</div>
                <p className="chat-thread__empty-text">{t('chat.selectHint')}</p>
              </div>
            )}
          </section>
        </div>
      </div>
    </PageShell>
  )
}
