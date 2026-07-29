import { useCallback, useEffect, useRef, useState } from 'react'
import { AnimatePresence } from 'framer-motion'
import {
  Plus,
  Play,
  Box,
  FolderOpen,
  Copy,
  Trash2,
  Star,
  Settings2,
  ChevronDown,
  Import
} from 'lucide-react'
import { PageShell } from '@renderer/pages/shared/PageShell'
import { Badge, Button } from '@renderer/design-system/components'
import { InstanceModal, type InstancePreset } from '@renderer/components/InstanceModal'
import { ImportInstancesModal } from '@renderer/components/ImportInstancesModal'
import { LoginModal } from '@renderer/components/LoginModal'
import { EmptyState } from '@renderer/components/EmptyState'
import { Skeleton } from '@renderer/components/Skeleton'
import { useAccounts } from '@renderer/context/AccountProvider'
import { useI18n } from '@renderer/context/I18nProvider'
import { playUiSound } from '@renderer/lib/uiSounds'
import type { GameInstance } from '@shared/types'
import './InstancesPage.css'

type CreateModalState = {
  mode: 'create'
  preset: InstancePreset
  initialMcVersion?: string
}

type EditModalState = {
  mode: 'edit'
  instance: GameInstance
}

type ModalState = CreateModalState | EditModalState

export function InstancesPage() {
  const { t, locale } = useI18n()
  const { launch, activeAccount } = useAccounts()
  const [instances, setInstances] = useState<GameInstance[]>([])
  const [loading, setLoading] = useState(true)
  const [modal, setModal] = useState<ModalState | null>(null)
  const [showLogin, setShowLogin] = useState(false)
  const [showImport, setShowImport] = useState(false)
  const [busyId, setBusyId] = useState<string | null>(null)
  const [createOpen, setCreateOpen] = useState(false)
  const createRef = useRef<HTMLDivElement>(null)

  const refresh = useCallback(async () => {
    const list = await window.primeLauncher.instance.list()
    setInstances(list)
    setLoading(false)
  }, [])

  useEffect(() => {
    void refresh()
  }, [refresh])

  useEffect(() => {
    if (!createOpen) return
    function onPointerDown(e: PointerEvent) {
      if (createRef.current && !createRef.current.contains(e.target as Node)) {
        setCreateOpen(false)
      }
    }
    document.addEventListener('pointerdown', onPointerDown)
    return () => document.removeEventListener('pointerdown', onPointerDown)
  }, [createOpen])

  async function handlePlay(inst: GameInstance) {
    if (!activeAccount) {
      setShowLogin(true)
      return
    }
    setBusyId(inst.id)
    playUiSound('click')
    await window.primeLauncher.profile.setInstance(inst.id)
    await launch(inst.id)
    setBusyId(null)
    await refresh()
  }

  async function handleSetDefault(id: string) {
    playUiSound('click')
    await window.primeLauncher.instance.setDefault(id)
    await refresh()
  }

  async function handleDuplicate(id: string) {
    playUiSound('click')
    await window.primeLauncher.instance.duplicate(id)
    await refresh()
  }

  async function handleDelete(inst: GameInstance) {
    if (!confirm(t('confirm.deleteInstance', { name: inst.name }))) {
      return
    }
    const deleteFiles = confirm(t('confirm.deleteFiles'))
    const result = await window.primeLauncher.instance.remove(inst.id, deleteFiles)
    if (!result.ok) {
      alert(result.error ?? t('errors.deleteInstance'))
      return
    }
    playUiSound('click')
    await refresh()
  }

  function openCreate(preset: InstancePreset, initialMcVersion?: string) {
    setCreateOpen(false)
    setModal({ mode: 'create', preset, initialMcVersion })
  }

  function loaderLabel(inst: GameInstance): string {
    if (inst.includePrimeMod) return t('instances.StellarClient')
    if (inst.loader === 'fabric') return t('instances.fabric')
    return t('instances.vanilla')
  }

  return (
    <PageShell
      title={t('pages.instances.title')}
      subtitle={t('pages.instances.subtitle')}
      actions={
        <div className="instances__actions-bar">
          <Button
            variant="secondary"
            icon={<Import size={16} />}
            onClick={() => setShowImport(true)}
          >
            {t('instances.import')}
          </Button>
          <div className="instances__create" ref={createRef}>
            <Button
              variant="primary"
              icon={<Plus size={16} />}
              onClick={() => setCreateOpen((v) => !v)}
            >
              {t('instances.newInstance')}
              <ChevronDown size={14} />
            </Button>
            {createOpen && (
              <div className="instances__create-menu">
                <button type="button" onClick={() => openCreate('prime', '26.2')}>
                  <strong>{t('instances.createPrime26')}</strong>
                  <span>Minecraft 26.2 Â· Prime</span>
                </button>
                <button type="button" onClick={() => openCreate('prime', '1.21.11')}>
                  <strong>{t('instances.createPrime121')}</strong>
                  <span>Minecraft 1.21.11 Â· Prime</span>
                </button>
                <button type="button" onClick={() => openCreate('fabric', '26.2')}>
                  <strong>{t('instances.fabric')}</strong>
                  <span>Minecraft 26.2 Â· Fabric</span>
                </button>
                <button type="button" onClick={() => openCreate('vanilla', '26.2')}>
                  <strong>{t('instances.vanilla')}</strong>
                  <span>Minecraft 26.2 Â· Vanilla</span>
                </button>
              </div>
            )}
          </div>
        </div>
      }
    >
      <div className="instances">
        {loading ? (
          <div className="instances__loading">
            <Skeleton height={88} radius={16} />
            <Skeleton height={88} radius={16} />
          </div>
        ) : instances.length === 0 ? (
          <EmptyState
            icon={<Box size={22} />}
            title={t('instances.emptyTitle')}
            description={t('instances.emptyDesc')}
            action={
              <Button variant="primary" icon={<Plus size={16} />} onClick={() => openCreate('prime', '26.2')}>
                {t('instances.createPrime26')}
              </Button>
            }
          />
        ) : (
          <ul className="instances__list">
            {instances.map((inst) => (
              <li key={inst.id} className={`instances__row${inst.isDefault ? ' is-default' : ''}`}>
                <div className="instances__icon">
                  <Box size={20} />
                </div>

                <div className="instances__body">
                  <div className="instances__title-row">
                    <h3>{inst.name}</h3>
                    {inst.isDefault && <Badge variant="prime">{t('instances.default')}</Badge>}
                  </div>
                  <p className="instances__meta">
                    MC {inst.minecraftVersion}
                    <span>Â·</span>
                    {loaderLabel(inst)}
                    <span>Â·</span>
                    {inst.ramMb} MB
                    <span>Â·</span>
                    {t('instances.modsBadge', { count: inst.modCount })}
                  </p>
                  {inst.lastPlayed && (
                    <p className="instances__played">
                      {t('instances.lastPlayed', {
                        date: new Date(inst.lastPlayed).toLocaleDateString(locale)
                      })}
                    </p>
                  )}
                </div>

                <div className="instances__actions">
                  <Button
                    variant="primary"
                    size="sm"
                    icon={<Play size={14} fill="currentColor" />}
                    disabled={busyId === inst.id}
                    onClick={() => void handlePlay(inst)}
                  >
                    {activeAccount ? t('instances.play') : t('instances.signInToPlay')}
                  </Button>
                  <Button
                    variant="secondary"
                    size="sm"
                    icon={<Settings2 size={14} />}
                    onClick={() => setModal({ mode: 'edit', instance: inst })}
                  >
                    {t('actions.configure')}
                  </Button>
                  <div className="instances__icon-actions">
                    <button
                      type="button"
                      className="instances__icon-btn"
                      title={t('actions.folder')}
                      onClick={() => void window.primeLauncher.instance.openFolder(inst.id)}
                    >
                      <FolderOpen size={15} />
                    </button>
                    {!inst.isDefault && (
                      <button
                        type="button"
                        className="instances__icon-btn"
                        title={t('instances.setDefault')}
                        onClick={() => void handleSetDefault(inst.id)}
                      >
                        <Star size={15} />
                      </button>
                    )}
                    <button
                      type="button"
                      className="instances__icon-btn"
                      title={t('actions.duplicate')}
                      onClick={() => void handleDuplicate(inst.id)}
                    >
                      <Copy size={15} />
                    </button>
                    {instances.length > 1 && (
                      <button
                        type="button"
                        className="instances__icon-btn instances__icon-btn--danger"
                        title={t('actions.delete')}
                        onClick={() => void handleDelete(inst)}
                      >
                        <Trash2 size={15} />
                      </button>
                    )}
                  </div>
                </div>
              </li>
            ))}
          </ul>
        )}
      </div>

      <AnimatePresence>
        {modal && (
          <InstanceModal
            mode={modal.mode}
            preset={modal.mode === 'create' ? modal.preset : undefined}
            initialMcVersion={modal.mode === 'create' ? modal.initialMcVersion : undefined}
            instance={modal.mode === 'edit' ? modal.instance : undefined}
            onClose={() => setModal(null)}
            onSaved={() => void refresh()}
          />
        )}
        {showLogin && <LoginModal onClose={() => setShowLogin(false)} />}
        {showImport && (
          <ImportInstancesModal
            onClose={() => setShowImport(false)}
            onImported={() => void refresh()}
          />
        )}
      </AnimatePresence>
    </PageShell>
  )
}
