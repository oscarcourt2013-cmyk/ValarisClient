import { useCallback, useEffect, useMemo, useState } from 'react'
import { Sparkles } from 'lucide-react'
import type { CosmeticItem } from '@shared/content-types'
import { PageShell } from '@renderer/pages/shared/PageShell'
import { Avatar, Badge, Button, Card, Tabs } from '@renderer/design-system/components'
import { useAccounts } from '@renderer/context/AccountProvider'
import { useI18n } from '@renderer/context/I18nProvider'

const RARITY_VARIANT: Record<CosmeticItem['rarity'], 'default' | 'red' | 'prime' | 'success'> = {
  common: 'default',
  rare: 'success',
  epic: 'red',
  legendary: 'prime'
}

export function CosmeticsPage() {
  const { t } = useI18n()
  const { activeAccount } = useAccounts()
  const [filter, setFilter] = useState('all')
  const [cosmetics, setCosmetics] = useState<CosmeticItem[]>([])

  const typeTabs = useMemo(
    () => [
      { id: 'all', label: t('cosmetics.all') },
      { id: 'cape', label: t('cosmetics.capes') },
      { id: 'wings', label: t('cosmetics.wings') },
      { id: 'badge', label: t('cosmetics.badges') }
    ],
    [t]
  )

  const refresh = useCallback(async () => {
    setCosmetics(await window.primeLauncher.cosmetic.list())
  }, [])

  useEffect(() => {
    void refresh()
  }, [refresh])

  const items = cosmetics.filter((c) => filter === 'all' || c.type === filter)

  async function handleToggle(item: CosmeticItem) {
    await window.primeLauncher.cosmetic.toggle(item.id)
    await refresh()
  }

  return (
    <PageShell
      title={t('pages.cosmetics.title')}
      subtitle={t('pages.cosmetics.subtitle')}
    >
      <div className="page-grid page-grid--2">
        <Card title={t('cosmetics.characterPreview')} glow className="cosmetics-preview">
          <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 16, padding: '24px 0' }}>
            <Avatar alt={activeAccount?.username ?? 'Steve'} uuid={activeAccount?.uuid} size="xl" glow />
            <p className="text-caption">{t('cosmetics.previewHint')}</p>
            <p className="text-caption" style={{ opacity: 0.75 }}>{t('cosmetics.peersNote')}</p>
            <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap', justifyContent: 'center' }}>
              {cosmetics
                .filter((c) => c.equipped)
                .map((c) => (
                  <Badge key={c.id} variant="prime">
                    {c.name}
                  </Badge>
                ))}
            </div>
          </div>
        </Card>

        <div>
          <Tabs tabs={typeTabs} active={filter} onChange={setFilter} />
          <div className="page-list" style={{ marginTop: 16 }}>
            {items.length === 0 ? (
              <p className="text-caption">{t('cosmetics.emptyOwned')}</p>
            ) : (
              items.map((item) => (
                <div key={item.id} className="list-row">
                  <div className="list-row__icon">
                    <Sparkles size={18} />
                  </div>
                  <div className="list-row__body">
                    <div className="list-row__title">{item.name}</div>
                    <div className="list-row__desc">{item.type}</div>
                  </div>
                  <div className="list-row__meta">
                    <Badge variant={RARITY_VARIANT[item.rarity]}>{item.rarity}</Badge>
                    <Button
                      variant={item.equipped ? 'secondary' : 'primary'}
                      size="sm"
                      onClick={() => void handleToggle(item)}
                    >
                      {item.equipped ? t('actions.unequip') : t('actions.equip')}
                    </Button>
                  </div>
                </div>
              ))
            )}
          </div>
        </div>
      </div>
    </PageShell>
  )
}
