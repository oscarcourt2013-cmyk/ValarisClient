import { useEffect, useState } from 'react'
import { PageShell } from '@renderer/pages/shared/PageShell'
import { Badge, Card } from '@renderer/design-system/components'
import { useI18n } from '@renderer/context/I18nProvider'
import type { NewsItem } from '@shared/types'

export function NewsPage() {
  const { t } = useI18n()
  const [news, setNews] = useState<NewsItem[]>([])

  useEffect(() => {
    void window.primeLauncher.news.list().then(setNews)
  }, [])

  return (
    <PageShell title={t('pages.news.title')} subtitle={t('pages.news.subtitle')}>
      <div className="page-list">
        {news.map((item) => (
          <Card key={item.id} hover>
            <div style={{ display: 'flex', alignItems: 'center', gap: 12, marginBottom: 8 }}>
              <Badge variant={item.tag === 'update' ? 'red' : item.tag === 'event' ? 'success' : 'default'}>
                {t(`newsTag.${item.tag}`)}
              </Badge>
              <span className="text-mono" style={{ color: 'var(--prime-muted-2)' }}>
                {item.date}
              </span>
            </div>
            <h3 className="text-subtitle" style={{ marginBottom: 8 }}>
              {item.title}
            </h3>
            <p className="text-body" style={{ color: 'var(--prime-muted)' }}>
              {item.summary}
            </p>
          </Card>
        ))}
      </div>
    </PageShell>
  )
}
