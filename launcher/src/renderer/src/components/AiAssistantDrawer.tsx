import { useCallback, useEffect, useRef, useState } from 'react'
import { Bot, Send, Sparkles, X } from 'lucide-react'
import { Button } from '@renderer/design-system/components'
import { useI18n } from '@renderer/context/I18nProvider'
import { useActiveInstance } from '@renderer/hooks/useActiveInstance'
import { playUiSound } from '@renderer/lib/uiSounds'
import { PRIME_AI_ASK_EVENT } from '@renderer/lib/askPrimeAssistant'
import './AiAssistantDrawer.css'

interface AiAssistantDrawerProps {
  open: boolean
  onClose: () => void
  onOpen?: () => void
}

type Proposal = {
  projectId: string
  title: string
  source: 'modrinth' | 'curseforge'
  type: 'mod' | 'resourcepack' | 'shader'
  description?: string
  downloads?: number
  iconUrl?: string
}

type ChatRow =
  | { id: string; kind: 'user'; text: string }
  | { id: string; kind: 'assistant'; text: string; proposals?: Proposal[] }
  | { id: string; kind: 'system'; text: string }

type InstallState = 'idle' | 'busy' | 'done' | 'error'

function promptForQuick(id: string): string {
  switch (id) {
    case 'diagnose':
      return 'Mon jeu a un problème. Appelle diagnose_instance, analyse le dernier crash report et latest.log, puis dis-moi la cause probable et les étapes pour corriger.'
    case 'latest':
      return 'Lis latest.log (read_latest_log), résume les erreurs ERROR/FATAL/Exception et dis-moi quoi faire.'
    case 'launcher':
      return 'Lis le log launcher (read_launcher_log) et dis-moi s’il y a une erreur de téléchargement, Fabric ou mods.'
    default:
      return ''
  }
}

export function AiAssistantDrawer({ open, onClose, onOpen }: AiAssistantDrawerProps) {
  const { t } = useI18n()
  const { instance, instanceId } = useActiveInstance()
  const [rows, setRows] = useState<ChatRow[]>([])
  const [draft, setDraft] = useState('')
  const [busy, setBusy] = useState(false)
  const [hasKey, setHasKey] = useState(true)
  const [viaProxy, setViaProxy] = useState(true)
  const [keyDraft, setKeyDraft] = useState('')
  const [installState, setInstallState] = useState<Record<string, InstallState>>({})
  const bottomRef = useRef<HTMLDivElement>(null)
  const busyRef = useRef(false)
  const rowsRef = useRef(rows)
  const instanceIdRef = useRef(instanceId)
  rowsRef.current = rows
  busyRef.current = busy
  instanceIdRef.current = instanceId

  useEffect(() => {
    if (!open) return
    void window.primeLauncher.ai.keyStatus().then((s) => {
      setHasKey(s.hasKey)
      setViaProxy(s.viaProxy)
    })
  }, [open])

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' })
  }, [rows, busy])

  const sendMessage = useCallback(
    async (text: string) => {
      const message = text.trim()
      if (!message || busyRef.current) return

      const status = await window.primeLauncher.ai.keyStatus()
      setHasKey(status.hasKey)
      setViaProxy(status.viaProxy)
      if (!status.hasKey) return

      const history = rowsRef.current
        .filter(
          (r): r is Extract<ChatRow, { kind: 'user' | 'assistant' }> =>
            r.kind === 'user' || r.kind === 'assistant'
        )
        .map((r) => ({ role: r.kind, content: r.text }))

      setDraft('')
      setRows((prev) => [...prev, { id: `u-${Date.now()}`, kind: 'user', text: message }])
      setBusy(true)
      playUiSound('click')

      try {
        const res = await window.primeLauncher.ai.chat({
          message,
          instanceId: instanceIdRef.current ?? undefined,
          history
        })
        if (!res.hasKey) {
          setHasKey(false)
        }
        if (!res.ok) {
          setRows((prev) => [
            ...prev,
            {
              id: `s-${Date.now()}`,
              kind: 'system',
              text: res.error || t('ai.errorGeneric')
            }
          ])
          return
        }
        setRows((prev) => [
          ...prev,
          {
            id: `a-${Date.now()}`,
            kind: 'assistant',
            text: res.reply,
            proposals: res.proposals
          }
        ])
      } catch {
        setRows((prev) => [
          ...prev,
          { id: `s-${Date.now()}`, kind: 'system', text: t('ai.errorGeneric') }
        ])
      } finally {
        setBusy(false)
      }
    },
    [t]
  )

  useEffect(() => {
    const handler = (event: Event) => {
      const detail = (event as CustomEvent<{ message?: string; open?: boolean }>).detail
      const message = detail?.message?.trim()
      if (!message) return
      if (detail.open !== false) {
        onOpen?.()
      }
      window.setTimeout(() => {
        void sendMessage(message)
      }, 80)
    }
    window.addEventListener(PRIME_AI_ASK_EVENT, handler)
    return () => window.removeEventListener(PRIME_AI_ASK_EVENT, handler)
  }, [onOpen, sendMessage])

  async function saveKey() {
    try {
      const status = await window.primeLauncher.ai.setKey(keyDraft.trim())
      setHasKey(status.hasKey)
      setViaProxy(status.viaProxy)
      setKeyDraft('')
      playUiSound('click')
    } catch {
      setHasKey(false)
    }
  }

  async function confirmInstall(p: Proposal) {
    const key = `${p.source}:${p.type}:${p.projectId}`
    setInstallState((prev) => ({ ...prev, [key]: 'busy' }))
    playUiSound('click')
    try {
      const result = await window.primeLauncher.ai.confirmInstall({
        projectId: p.projectId,
        title: p.title,
        source: p.source,
        type: p.type,
        instanceId: instanceId ?? undefined
      })
      setInstallState((prev) => ({ ...prev, [key]: result.ok ? 'done' : 'error' }))
    } catch {
      setInstallState((prev) => ({ ...prev, [key]: 'error' }))
    }
  }

  if (!open) return null

  return (
    <div className="ai-drawer">
      <button type="button" className="ai-drawer__backdrop" onClick={onClose} aria-label="Close" />
      <aside className="ai-drawer__panel">
        <header className="ai-drawer__head">
          <div>
            <h2>
              <Sparkles size={16} /> {t('ai.title')}
            </h2>
            <p>
              {instance
                ? t('ai.instance', {
                    name: instance.name,
                    version: instance.minecraftVersion
                  })
                : t('ai.subtitle')}
            </p>
          </div>
          <button type="button" className="ai-drawer__close" onClick={onClose}>
            <X size={16} />
          </button>
        </header>

        {!hasKey && !viaProxy && (
          <div className="ai-drawer__keybox">
            <p>{t('ai.needKey')}</p>
            <div className="ai-drawer__keyrow">
              <input
                className="ai-drawer__input"
                type="password"
                value={keyDraft}
                placeholder={t('ai.keyPlaceholder')}
                onChange={(e) => setKeyDraft(e.target.value)}
                onKeyDown={(e) => {
                  if (e.key === 'Enter') void saveKey()
                }}
              />
              <Button variant="secondary" size="sm" onClick={() => void saveKey()}>
                {t('ai.saveKey')}
              </Button>
            </div>
          </div>
        )}

        <div className="ai-drawer__quick">
          <button
            type="button"
            className="ai-drawer__chip"
            disabled={!hasKey || busy}
            onClick={() => void sendMessage(promptForQuick('diagnose'))}
          >
            {t('ai.quickDiagnose')}
          </button>
          <button
            type="button"
            className="ai-drawer__chip"
            disabled={!hasKey || busy}
            onClick={() => void sendMessage(promptForQuick('latest'))}
          >
            {t('ai.quickLatestLog')}
          </button>
          <button
            type="button"
            className="ai-drawer__chip"
            disabled={!hasKey || busy}
            onClick={() => void sendMessage(promptForQuick('launcher'))}
          >
            {t('ai.quickLauncherLog')}
          </button>
        </div>

        <div className="ai-drawer__messages">
          {rows.length === 0 && (
            <div className="ai-drawer__empty">
              <Bot size={28} />
              <strong>{t('ai.emptyTitle')}</strong>
              <span>{t('ai.emptyDesc')}</span>
            </div>
          )}
          {rows.map((row) => (
            <div key={row.id} className={`ai-drawer__bubble ai-drawer__bubble--${row.kind}`}>
              <div className="ai-drawer__bubble-text">{row.text}</div>
              {row.kind === 'assistant' && row.proposals && row.proposals.length > 0 && (
                <ul className="ai-drawer__proposals">
                  {row.proposals.map((p) => {
                    const key = `${p.source}:${p.type}:${p.projectId}`
                    const state = installState[key] ?? 'idle'
                    return (
                      <li key={key} className="ai-drawer__proposal">
                        {p.iconUrl ? (
                          <img src={p.iconUrl} alt="" className="ai-drawer__proposal-icon" />
                        ) : (
                          <span className="ai-drawer__proposal-icon ai-drawer__proposal-icon--fallback" />
                        )}
                        <div className="ai-drawer__proposal-text">
                          <strong>{p.title}</strong>
                          <span>
                            {p.source === 'curseforge'
                              ? t('ai.sourceCurseforge')
                              : t('ai.sourceModrinth')}{' '}
                            · {p.type}
                            {typeof p.downloads === 'number'
                              ? ` · ${p.downloads.toLocaleString()}`
                              : ''}
                          </span>
                        </div>
                        <Button
                          variant="secondary"
                          size="sm"
                          disabled={state === 'busy' || state === 'done'}
                          onClick={() => void confirmInstall(p)}
                        >
                          {state === 'busy'
                            ? t('ai.installing')
                            : state === 'done'
                              ? t('ai.installed')
                              : state === 'error'
                                ? t('ai.installFailed')
                                : t('ai.install')}
                        </Button>
                      </li>
                    )
                  })}
                </ul>
              )}
            </div>
          ))}
          {busy && <div className="ai-drawer__thinking">{t('ai.thinking')}</div>}
          <div ref={bottomRef} />
        </div>

        <footer className="ai-drawer__foot">
          <div className="ai-drawer__composer">
            <input
              className="ai-drawer__input"
              value={draft}
              disabled={!hasKey || busy}
              placeholder={t('ai.placeholder')}
              onChange={(e) => setDraft(e.target.value)}
              onKeyDown={(e) => {
                if (e.key === 'Enter' && !e.shiftKey) {
                  e.preventDefault()
                  void sendMessage(draft)
                }
              }}
            />
            <Button
              variant="primary"
              size="sm"
              icon={<Send size={14} />}
              disabled={!hasKey || busy || !draft.trim()}
              onClick={() => void sendMessage(draft)}
            >
              {t('ai.send')}
            </Button>
          </div>
          {rows.length > 0 && (
            <button
              type="button"
              className="ai-drawer__clear"
              onClick={() => {
                setRows([])
                setInstallState({})
              }}
            >
              {t('ai.clearChat')}
            </button>
          )}
        </footer>
      </aside>
    </div>
  )
}
