import { useCallback, useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { Check, Plus, Sparkles, Trash2 } from 'lucide-react'
import type { CosmeticItem } from '@shared/content-types'
import { playerBodyUrl, playerCapeUrl } from '@shared/format'
import { PageShell } from '@renderer/pages/shared/PageShell'
import { Badge, Button, Tabs } from '@renderer/design-system/components'
import { SkinViewer3D } from '@renderer/components/SkinViewer3D'
import { EmptyState } from '@renderer/components/EmptyState'
import { useAccounts } from '@renderer/context/AccountProvider'
import { useI18n } from '@renderer/context/I18nProvider'
import { playUiSound } from '@renderer/lib/uiSounds'
import './SkinsPage.css'

type Pose = 'idle' | 'walk' | 'run'
type SkinTab = 'skins' | 'cape' | 'wings' | 'badge'

interface LocalSkin {
  id: string
  name: string
  dataUrl: string
}

const RARITY_VARIANT: Record<CosmeticItem['rarity'], 'default' | 'red' | 'prime' | 'success'> = {
  common: 'default',
  rare: 'success',
  epic: 'red',
  legendary: 'prime'
}

export function SkinsPage() {
  const { t } = useI18n()
  const { activeAccount } = useAccounts()
  const [tab, setTab] = useState<SkinTab>('skins')
  const [pose, setPose] = useState<Pose>('idle')
  const [cosmetics, setCosmetics] = useState<CosmeticItem[]>([])
  const [localSkins, setLocalSkins] = useState<LocalSkin[]>([])
  const [activeSkinId, setActiveSkinId] = useState<string | null>(null)
  const [selectedId, setSelectedId] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)

  const username = activeAccount?.username ?? 'Steve'
  const uuid = activeAccount?.uuid
  const bodyUrl = playerBodyUrl(uuid, username, 320)
  const capeUrl = playerCapeUrl(uuid, username, activeAccount?.capeUrl)

  const tabs = useMemo(
    () => [
      { id: 'skins', label: t('skins.tabs.skins') },
      { id: 'cape', label: t('skins.tabs.capes') },
      { id: 'wings', label: t('skins.tabs.wings') },
      { id: 'badge', label: t('skins.tabs.badges') }
    ],
    [t]
  )

  const refresh = useCallback(async () => {
    const [list, skins, settings] = await Promise.all([
      window.primeLauncher.cosmetic.list(),
      window.primeLauncher.skins.list(),
      window.primeLauncher.settings.get()
    ])
    setCosmetics(list)
    setLocalSkins(skins)
    setActiveSkinId(settings.activeSkinId ?? null)
  }, [])

  useEffect(() => {
    void refresh()
  }, [refresh])

  useEffect(() => {
    setSelectedId(null)
  }, [tab])

  const cosmeticItems = useMemo(
    () => cosmetics.filter((c) => c.type === tab),
    [cosmetics, tab]
  )

  const selectedCosmetic = cosmetics.find((c) => c.id === selectedId) ?? null
  const equippedCape = cosmetics.find((c) => c.type === 'cape' && c.equipped)
  const previewSkinUrl =
    (selectedId && localSkins.find((s) => s.id === selectedId)?.dataUrl) ||
    localSkins.find((s) => s.id === activeSkinId)?.dataUrl ||
    null

  async function handleImport() {
    setBusy(true)
    const result = await window.primeLauncher.skins.import()
    if (result.ok) {
      playUiSound('success')
      await refresh()
      if (result.skin) setSelectedId(result.skin.id)
    }
    setBusy(false)
  }

  async function handleApplySkin() {
    if (tab !== 'skins') return
    setBusy(true)
    await window.primeLauncher.skins.setActive(selectedId)
    playUiSound('success')
    await refresh()
    setBusy(false)
  }

  async function handleRemoveSkin(id: string) {
    setBusy(true)
    await window.primeLauncher.skins.remove(id)
    playUiSound('click')
    if (selectedId === id) setSelectedId(null)
    await refresh()
    setBusy(false)
  }

  async function handleApply() {
    if (tab === 'skins') return
    if (!selectedCosmetic) return
    if (!selectedCosmetic.equipped) {
      await window.primeLauncher.cosmetic.toggle(selectedCosmetic.id)
      playUiSound('success')
      await refresh()
    }
  }

  async function handleUnequipSelected() {
    if (!selectedCosmetic?.equipped) return
    await window.primeLauncher.cosmetic.toggle(selectedCosmetic.id)
    playUiSound('click')
    await refresh()
  }

  return (
    <PageShell title={t('pages.skins.title')} subtitle={t('pages.skins.subtitle')}>
      <Tabs tabs={tabs} active={tab} onChange={(id) => setTab(id as SkinTab)} />

      <div className="skins-hub">
        <section className="skins-hub__preview">
          <div className="skins-hub__stage">
            <SkinViewer3D
              className="skins-hub__viewer"
              uuid={uuid}
              username={username}
              skinUrl={previewSkinUrl}
              capeUrl={tab === 'badge' ? null : capeUrl}
              pose={pose}
              width={260}
              height={340}
              showControls
            />
            {equippedCape && tab !== 'badge' && (
              <div className="skins-hub__cape-tag">
                <Badge variant="prime">{equippedCape.name}</Badge>
              </div>
            )}
          </div>

          <div className="skins-hub__poses">
            {(
              [
                ['idle', t('skins.pose.idle')],
                ['walk', t('skins.pose.walk')],
                ['run', t('skins.pose.run')]
              ] as const
            ).map(([id, label]) => (
              <button
                key={id}
                type="button"
                className={`skins-hub__pose${pose === id ? ' is-active' : ''}`}
                onClick={() => setPose(id)}
              >
                {label}
              </button>
            ))}
          </div>

          <p className="skins-hub__name">{username}</p>
        </section>

        <section className="skins-hub__gallery">
          {tab === 'skins' ? (
            <div className="skins-hub__grid">
              <button
                type="button"
                className="skins-card skins-card--add"
                disabled={busy}
                onClick={() => void handleImport()}
              >
                <Plus size={28} />
                <span>{t('skins.addSkin')}</span>
              </button>
              <button
                type="button"
                className={`skins-card${!selectedId && !activeSkinId ? ' is-active' : ''}`}
                onClick={() => setSelectedId(null)}
              >
                <div className="skins-card__preview">
                  <img src={bodyUrl} alt={username} draggable={false} />
                </div>
                <span className="skins-card__name">{username}</span>
                {!activeSkinId && <span className="skins-card__equipped">{t('common.active')}</span>}
              </button>
              {localSkins.map((skin) => (
                <button
                  key={skin.id}
                  type="button"
                  className={`skins-card${selectedId === skin.id || (!selectedId && activeSkinId === skin.id) ? ' is-active' : ''}`}
                  onClick={() => setSelectedId(skin.id)}
                >
                  <div className="skins-card__preview">
                    <img src={skin.dataUrl} alt={skin.name} draggable={false} style={{ imageRendering: 'pixelated' }} />
                  </div>
                  <span className="skins-card__name">{skin.name}</span>
                  {activeSkinId === skin.id && (
                    <span className="skins-card__equipped">{t('common.active')}</span>
                  )}
                  <span
                    className="skins-card__remove"
                    role="button"
                    tabIndex={0}
                    onClick={(e) => {
                      e.stopPropagation()
                      void handleRemoveSkin(skin.id)
                    }}
                    onKeyDown={(e) => {
                      if (e.key === 'Enter') {
                        e.stopPropagation()
                        void handleRemoveSkin(skin.id)
                      }
                    }}
                    title={t('skins.remove')}
                  >
                    <Trash2 size={12} />
                  </span>
                </button>
              ))}
            </div>
          ) : cosmeticItems.length === 0 ? (
            <EmptyState
              icon={<Sparkles size={24} />}
              title={t('cosmetics.emptyOwned')}
              description={t('skins.emptyCosmeticsHint')}
              action={
                <Link to="/store">
                  <Button variant="secondary" size="sm">
                    {t('skins.openStore')}
                  </Button>
                </Link>
              }
            />
          ) : (
            <div className="skins-hub__grid">
              {cosmeticItems.map((item) => (
                <button
                  key={item.id}
                  type="button"
                  className={`skins-card${selectedId === item.id || (!selectedId && item.equipped) ? ' is-active' : ''}`}
                  onClick={() => setSelectedId(item.id)}
                >
                  <div className="skins-card__preview skins-card__preview--icon">
                    <Sparkles size={28} />
                    {item.equipped && <span className="skins-card__equipped">{t('common.active')}</span>}
                  </div>
                  <span className="skins-card__name">{item.name}</span>
                  <Badge variant={RARITY_VARIANT[item.rarity]}>{item.rarity}</Badge>
                </button>
              ))}
            </div>
          )}

          <div className="skins-hub__actions">
            {tab === 'skins' ? (
              <Button
                variant="primary"
                icon={<Check size={16} />}
                disabled={busy || selectedId === activeSkinId || (selectedId === null && !activeSkinId)}
                onClick={() => void handleApplySkin()}
              >
                {t('skins.applySkin')}
              </Button>
            ) : (
              <>
                <Button
                  variant="primary"
                  icon={<Check size={16} />}
                  disabled={!selectedCosmetic || selectedCosmetic.equipped}
                  onClick={() => void handleApply()}
                >
                  {t('skins.applyCosmetic')}
                </Button>
                <Button
                  variant="secondary"
                  disabled={!selectedCosmetic?.equipped}
                  onClick={() => void handleUnequipSelected()}
                >
                  {t('actions.unequip')}
                </Button>
                <Button variant="ghost" onClick={() => setTab('cape')}>
                  {t('skins.changeCape')}
                </Button>
              </>
            )}
          </div>
        </section>
      </div>
    </PageShell>
  )
}
