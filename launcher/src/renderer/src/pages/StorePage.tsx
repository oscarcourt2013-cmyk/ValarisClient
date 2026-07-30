import { useCallback, useEffect, useMemo, useState } from 'react'
import { ShoppingBag, Ticket, X } from 'lucide-react'
import { PageShell } from '@renderer/pages/shared/PageShell'
import { Badge, Button, Tabs } from '@renderer/design-system/components'
import type { StoreItem } from '@shared/content-types'
import { playerCapeUrl } from '@shared/format'
import { useI18n } from '@renderer/context/I18nProvider'
import { useTheme } from '@renderer/context/ThemeProvider'
import { useAccounts } from '@renderer/context/AccountProvider'
import { themeIdFromStoreId } from '@shared/theme'
import { SkinViewer3D } from '@renderer/components/SkinViewer3D'
import { EmptyState } from '@renderer/components/EmptyState'
import { playUiSound } from '@renderer/lib/uiSounds'
import './StorePage.css'

type StoreTab = 'catalog' | 'history' | 'promos'

interface HistoryRow {
  id: string
  itemId: string
  itemName: string
  price: number
  purchasedAt: string
}

interface PromoRow {
  code: string
  label: string
  coins: number
  redeemed: boolean
}

export function StorePage() {
  const { t } = useI18n()
  const { refreshTheme } = useTheme()
  const { activeAccount } = useAccounts()
  const [tab, setTab] = useState<StoreTab>('catalog')
  const [category, setCategory] = useState('all')
  const [items, setItems] = useState<StoreItem[]>([])
  const [history, setHistory] = useState<HistoryRow[]>([])
  const [promos, setPromos] = useState<PromoRow[]>([])
  const [promoCode, setPromoCode] = useState('')
  const [balance, setBalance] = useState(0)
  const [message, setMessage] = useState<string | null>(null)
  const [preview, setPreview] = useState<StoreItem | null>(null)

  const categories = useMemo(
    () => [
      { id: 'all', label: t('store.categories.all') },
      { id: 'cosmetic', label: t('store.categories.cosmetic') },
      { id: 'theme', label: t('store.categories.theme') },
      { id: 'background', label: t('store.categories.background') },
      { id: 'badge', label: t('store.categories.badge') }
    ],
    [t]
  )

  const mainTabs = useMemo(
    () => [
      { id: 'catalog', label: t('store.tabs.catalog') },
      { id: 'history', label: t('store.tabs.history') },
      { id: 'promos', label: t('store.tabs.promos') }
    ],
    [t]
  )

  const refresh = useCallback(async () => {
    const [catalog, coins, hist, promoList] = await Promise.all([
      window.primeLauncher.store.catalog(),
      window.primeLauncher.store.balance(),
      window.primeLauncher.store.history(),
      window.primeLauncher.store.promos()
    ])
    setItems(catalog)
    setBalance(coins)
    setHistory(hist)
    setPromos(promoList)
  }, [])

  useEffect(() => {
    void refresh()
  }, [refresh])

  async function handlePurchase(item: StoreItem) {
    setMessage(null)
    const result = await window.primeLauncher.store.purchase(item.id)
    if (result.ok) {
      playUiSound('success')
      setMessage(t('store.unlocked', { name: item.name }))
      if (item.category === 'theme') {
        const themeId = themeIdFromStoreId(item.id)
        if (themeId) {
          await window.primeLauncher.settings.update({ theme: themeId, accentColor: null })
        }
        await refreshTheme()
      }
      if (item.id === 'bg-nebula') {
        await window.primeLauncher.settings.update({ backgroundNebula: true })
        await refreshTheme()
      }
      await refresh()
      setPreview(null)
    } else {
      playUiSound('error')
      setMessage(result.error ?? t('store.purchaseFailed'))
    }
  }

  async function handleRedeem() {
    setMessage(null)
    const result = await window.primeLauncher.store.redeem(promoCode)
    if (result.ok) {
      playUiSound('success')
      setMessage(t('store.promoOk', { coins: result.coins ?? 0 }))
      setPromoCode('')
      await refresh()
    } else {
      playUiSound('error')
      setMessage(result.error ?? t('store.promoFailed'))
    }
  }

  const filtered = items.filter((i) => category === 'all' || i.category === category)
  const username = activeAccount?.username ?? 'Steve'
  const capeUrl = playerCapeUrl(activeAccount?.uuid, username, activeAccount?.capeUrl)

  return (
    <PageShell
      title={t('pages.store.title')}
      subtitle={t('pages.store.subtitle')}
      actions={
        <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
          <Badge variant="prime">{t('store.coins', { balance })}</Badge>
        </div>
      }
    >
      {message && (
        <p className="text-caption" style={{ marginBottom: 16, color: 'var(--prime-muted)' }}>
          {message}
        </p>
      )}

      <Tabs tabs={mainTabs} active={tab} onChange={(id) => setTab(id as StoreTab)} />

      {tab === 'catalog' && (
        <>
          <div style={{ marginTop: 16 }}>
            <Tabs tabs={categories} active={category} onChange={setCategory} />
          </div>
          {filtered.length === 0 ? (
            <EmptyState
              icon={<ShoppingBag size={24} />}
              title={t('store.emptyTitle')}
              description={t('store.emptyDesc')}
            />
          ) : (
            <div className="page-grid page-grid--3" style={{ marginTop: 24 }}>
              {filtered.map((item) => (
                <div key={item.id} className="tile">
                  <button
                    type="button"
                    className="tile__preview store-tile__preview"
                    onClick={() => setPreview(item)}
                  >
                    <ShoppingBag size={28} />
                    <span>{t('store.preview')}</span>
                  </button>
                  <div className="tile__name">{item.name}</div>
                  <div className="tile__desc">{item.description}</div>
                  <div
                    style={{
                      marginTop: 16,
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'space-between'
                    }}
                  >
                    {item.owned ? (
                      <Badge variant="success">{t('actions.owned')}</Badge>
                    ) : (
                      <span style={{ fontWeight: 700, color: 'var(--prime-red-bright)' }}>
                        {item.price === 0
                          ? t('actions.free')
                          : t('store.coinsPrice', { price: item.price })}
                      </span>
                    )}
                    <Button
                      variant={item.owned ? 'secondary' : 'primary'}
                      size="sm"
                      disabled={item.owned}
                      onClick={() => void handlePurchase(item)}
                    >
                      {item.owned
                        ? t('actions.owned')
                        : item.price === 0
                          ? t('actions.claim')
                          : t('actions.buy')}
                    </Button>
                  </div>
                </div>
              ))}
            </div>
          )}
        </>
      )}

      {tab === 'history' && (
        <div className="page-list" style={{ marginTop: 20 }}>
          {history.length === 0 ? (
            <EmptyState
              icon={<ShoppingBag size={24} />}
              title={t('store.historyEmptyTitle')}
              description={t('store.historyEmptyDesc')}
            />
          ) : (
            history.map((row) => (
              <div key={row.id} className="list-row">
                <div className="list-row__body">
                  <div className="list-row__title">{row.itemName}</div>
                  <div className="list-row__desc">{new Date(row.purchasedAt).toLocaleString()}</div>
                </div>
                <div className="list-row__meta text-mono">
                  {row.price === 0 ? t('actions.free') : t('store.coinsPrice', { price: row.price })}
                </div>
              </div>
            ))
          )}
        </div>
      )}

      {tab === 'promos' && (
        <div style={{ marginTop: 20 }}>
          <div className="servers-add" style={{ maxWidth: 480 }}>
            <input
              className="modal__field"
              placeholder={t('store.promoPlaceholder')}
              value={promoCode}
              onChange={(e) => setPromoCode(e.target.value)}
            />
            <Button
              variant="primary"
              icon={<Ticket size={16} />}
              disabled={promoCode.trim().length < 3}
              onClick={() => void handleRedeem()}
            >
              {t('store.redeem')}
            </Button>
          </div>
          <div className="page-list" style={{ marginTop: 16 }}>
            {promos.map((p) => (
              <div key={p.code} className="list-row">
                <div className="list-row__body">
                  <div className="list-row__title">{p.label}</div>
                  <div className="list-row__desc text-mono">{p.code}</div>
                </div>
                <div className="list-row__meta">
                  <Badge variant={p.redeemed ? 'success' : 'prime'}>
                    {p.redeemed ? t('store.redeemed') : t('store.coinsPrice', { price: p.coins })}
                  </Badge>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}

      {preview && (
        <div className="store-preview">
          <button type="button" className="store-preview__backdrop" onClick={() => setPreview(null)} />
          <div className="store-preview__card">
            <button type="button" className="store-preview__close" onClick={() => setPreview(null)}>
              <X size={16} />
            </button>
            <SkinViewer3D
              uuid={activeAccount?.uuid}
              username={username}
              capeUrl={capeUrl}
              pose="walk"
              width={240}
              height={320}
              showControls
              backdrop="soft"
            />
            <div className="store-preview__meta">
              <h3>{preview.name}</h3>
              <p>{preview.description}</p>
              <div className="store-preview__actions">
                {preview.owned ? (
                  <Badge variant="success">{t('actions.owned')}</Badge>
                ) : (
                  <Button variant="primary" onClick={() => void handlePurchase(preview)}>
                    {preview.price === 0
                      ? t('actions.claim')
                      : t('store.buyFor', { price: preview.price })}
                  </Button>
                )}
              </div>
            </div>
          </div>
        </div>
      )}
    </PageShell>
  )
}
