import { useCallback, useEffect, useState } from 'react'
import { Camera, Film, PlayCircle, RefreshCw } from 'lucide-react'
import type { MediaItem } from '@shared/content-types'
import { PageShell } from '@renderer/pages/shared/PageShell'
import { Badge, Button } from '@renderer/design-system/components'
import { useActiveInstance } from '@renderer/hooks/useActiveInstance'
import { useI18n } from '@renderer/context/I18nProvider'

function MediaPreview({ item }: { item: MediaItem }) {
  if (item.type === 'clip' && item.mediaUrl) {
    return (
      <video
        src={item.mediaUrl}
        muted
        playsInline
        preload="metadata"
        style={{ width: '100%', height: '100%', objectFit: 'cover', borderRadius: 8 }}
      />
    )
  }

  if (item.thumbnailUrl) {
    return (
      <img
        src={item.thumbnailUrl}
        alt={item.title}
        style={{ width: '100%', height: '100%', objectFit: 'cover', borderRadius: 8 }}
      />
    )
  }

  if (item.type === 'replay') {
    return <PlayCircle size={28} />
  }
  if (item.type === 'clip') {
    return <Film size={28} />
  }
  return <Camera size={28} />
}

export function MediaPage() {
  const { t } = useI18n()
  const { instanceId } = useActiveInstance()
  const [items, setItems] = useState<MediaItem[]>([])

  const refresh = useCallback(async () => {
    setItems(await window.primeLauncher.media.list(instanceId ?? undefined))
  }, [instanceId])

  useEffect(() => {
    void refresh()
  }, [refresh])

  return (
    <PageShell
      title={t('pages.media.title')}
      subtitle={t('pages.media.subtitle')}
      actions={
        <>
          <Button variant="secondary" size="sm" onClick={() => void refresh()}>
            <RefreshCw size={14} style={{ marginRight: 6, verticalAlign: 'middle' }} />
            {t('media.refresh')}
          </Button>
          <Button
            variant="secondary"
            size="sm"
            onClick={() => void window.primeLauncher.media.openFolder(instanceId ?? undefined)}
          >
            {t('media.openFolder')}
          </Button>
        </>
      }
    >
      {items.length === 0 ? (
        <p className="text-caption">{t('empty.noScreenshots')}</p>
      ) : (
        <div className="page-grid page-grid--3">
          {items.map((item) => (
            <div
              key={item.id}
              className="tile"
              role="button"
              tabIndex={0}
              onClick={() => item.filePath && void window.primeLauncher.media.openFile(item.filePath)}
            >
              <div className="tile__preview">
                <MediaPreview item={item} />
              </div>
              <div className="tile__name">{item.title}</div>
              <div className="tile__desc">
                {item.date} · {item.size}
              </div>
              <div style={{ marginTop: 12 }}>
                <Badge variant="default">{item.type}</Badge>
              </div>
            </div>
          ))}
        </div>
      )}

      <p className="text-caption" style={{ marginTop: 24 }}>
        {t('media.replaysNote')}
        <Film size={14} style={{ verticalAlign: 'middle', marginLeft: 6 }} />
      </p>
    </PageShell>
  )
}
