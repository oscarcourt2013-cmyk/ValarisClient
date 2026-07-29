import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { Copy, Check } from 'lucide-react'
import type { LaunchLogEntryDto } from '@shared/ipc'
import { useI18n } from '@renderer/context/I18nProvider'
import './LaunchLogConsole.css'

type LogFilter = 'all' | LaunchLogEntryDto['level']

function formatTime(iso: string): string {
  return iso.slice(11, 19)
}

function formatLine(entry: LaunchLogEntryDto): string {
  return `[${formatTime(entry.timestamp)}] ${entry.level.toUpperCase()} ${entry.message}`
}

function levelClass(level: LaunchLogEntryDto['level']): string {
  switch (level) {
    case 'error':
      return 'launch-log__line--error'
    case 'warn':
      return 'launch-log__line--warn'
    case 'debug':
      return 'launch-log__line--debug'
    default:
      return 'launch-log__line--info'
  }
}

const FILTERS: LogFilter[] = ['all', 'info', 'warn', 'error', 'debug']

export function LaunchLogConsole() {
  const { t } = useI18n()
  const [filter, setFilter] = useState<LogFilter>('all')
  const [entries, setEntries] = useState<LaunchLogEntryDto[]>([])
  const [copied, setCopied] = useState(false)
  const scrollRef = useRef<HTMLDivElement>(null)
  const pinnedBottom = useRef(true)

  useEffect(() => {
    void window.primeLauncher.launch.listLogs().then(setEntries)
    const unsubAppend = window.primeLauncher.launch.onLogAppend((entry) => {
      setEntries((prev) => [...prev, entry].slice(-2000))
      if (entry.level === 'error') {
        setFilter('error')
      }
    })
    const unsubReset = window.primeLauncher.launch.onLogReset(() => setEntries([]))
    return () => {
      unsubAppend()
      unsubReset()
    }
  }, [])

  const counts = useMemo(() => {
    const tally = { all: entries.length, info: 0, warn: 0, error: 0, debug: 0 }
    for (const entry of entries) {
      tally[entry.level] += 1
    }
    return tally
  }, [entries])

  const filtered = useMemo(
    () => (filter === 'all' ? entries : entries.filter((e) => e.level === filter)),
    [entries, filter]
  )

  useEffect(() => {
    if (!pinnedBottom.current || !scrollRef.current) {
      return
    }
    scrollRef.current.scrollTop = scrollRef.current.scrollHeight
  }, [filtered])

  const onScroll = useCallback(() => {
    const el = scrollRef.current
    if (!el) {
      return
    }
    pinnedBottom.current = el.scrollHeight - el.scrollTop - el.clientHeight < 48
  }, [])

  const filterLabel = (key: LogFilter): string => {
    switch (key) {
      case 'all':
        return t('logs.filterAll')
      case 'info':
        return t('logs.filterInfo')
      case 'warn':
        return t('logs.filterWarn')
      case 'error':
        return t('logs.filterError')
      case 'debug':
        return t('logs.filterDebug')
    }
  }

  const copyLogs = useCallback(async () => {
    const text = filtered.map(formatLine).join('\n')
    if (!text) {
      return
    }
    await navigator.clipboard.writeText(text)
    setCopied(true)
    window.setTimeout(() => setCopied(false), 2000)
  }, [filtered])

  return (
    <section className="launch-log launch-log--page">
      <div className="launch-log__filters">
        {FILTERS.map((key) => (
          <button
            key={key}
            type="button"
            className={`launch-log__filter ${filter === key ? 'launch-log__filter--active' : ''}`}
            onClick={() => setFilter(key)}
          >
            {filterLabel(key)}
            <span className="launch-log__filter-count">{counts[key]}</span>
          </button>
        ))}
        <span className="launch-log__filters-spacer" />
        <button
          type="button"
          className="launch-log__copy-btn"
          disabled={filtered.length === 0}
          onClick={() => void copyLogs()}
        >
          {copied ? <Check size={14} /> : <Copy size={14} />}
          {copied ? t('logs.copied') : t('logs.copy')}
        </button>
      </div>

      <div ref={scrollRef} className="launch-log__body" onScroll={onScroll}>
        {filtered.length === 0 ? (
          <p className="launch-log__empty">{t('logs.empty')}</p>
        ) : (
          filtered.map((entry) => (
            <div key={entry.id} className={`launch-log__line ${levelClass(entry.level)}`}>
              <span className="launch-log__time">{formatTime(entry.timestamp)}</span>
              <span className="launch-log__level">{entry.level.toUpperCase()}</span>
              <span className="launch-log__msg">{entry.message}</span>
            </div>
          ))
        )}
      </div>
    </section>
  )
}
