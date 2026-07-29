import { useCallback, useEffect, useState } from 'react'
import { Trash2, X } from 'lucide-react'
import type { DownloadTask } from '@shared/content-types'
import { PageShell } from '@renderer/pages/shared/PageShell'
import { Badge, Button, ProgressBar } from '@renderer/design-system/components'
import { useI18n } from '@renderer/context/I18nProvider'

const STATUS_VARIANT: Record<string, 'default' | 'red' | 'success' | 'prime'> = {
  downloading: 'red',
  paused: 'default',
  completed: 'success',
  queued: 'default',
  failed: 'red'
}

export function DownloadsPage() {
  const { t } = useI18n()
  const [tasks, setTasks] = useState<DownloadTask[]>([])

  const refresh = useCallback(async () => {
    setTasks(await window.primeLauncher.downloads.list())
  }, [])

  useEffect(() => {
    void refresh()
    const unsubscribe = window.primeLauncher.launch.onProgress(() => {
      void refresh()
    })
    const timer = setInterval(() => void refresh(), 3000)
    return () => {
      unsubscribe()
      clearInterval(timer)
    }
  }, [refresh])

  return (
    <PageShell
      title={t('pages.downloads.title')}
      subtitle={t('pages.downloads.subtitle')}
      actions={
        <Button variant="secondary" size="sm" icon={<Trash2 size={14} />} onClick={() => void window.primeLauncher.downloads.clearCompleted().then(refresh)}>
          {t('actions.clearCompleted')}
        </Button>
      }
    >
      {tasks.length === 0 ? (
        <p className="text-caption">{t('empty.noDownloadsHint')}</p>
      ) : (
        <div className="page-list">
          {tasks.map((task) => (
            <div key={task.id} className="list-row" style={{ flexDirection: 'column', alignItems: 'stretch', gap: 12 }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: 16 }}>
                <div className="list-row__body">
                  <div className="list-row__title">{task.name}</div>
                  <div className="list-row__desc">{task.size}</div>
                </div>
                <Badge variant={STATUS_VARIANT[task.status] ?? 'default'}>{task.status}</Badge>
                <span className="text-mono" style={{ color: 'var(--prime-muted)' }}>
                  {task.speed}
                </span>
                <span className="text-mono" style={{ color: 'var(--prime-muted-2)' }}>
                  {task.eta}
                </span>
                <Button
                  variant="ghost"
                  size="sm"
                  className="prime-btn--icon"
                  onClick={() => void window.primeLauncher.downloads.remove(task.id).then(refresh)}
                >
                  <X size={14} />
                </Button>
              </div>
              <ProgressBar value={task.progress} large />
            </div>
          ))}
        </div>
      )}
    </PageShell>
  )
}
