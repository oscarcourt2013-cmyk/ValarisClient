import { useEffect, useState } from 'react'
import { createPortal } from 'react-dom'
import { motion } from 'framer-motion'
import { Download } from 'lucide-react'
import { Button, SearchInput, Tabs } from '@renderer/design-system/components'
import { useI18n } from '@renderer/context/I18nProvider'
import type { ContentMutationDto, ContentVersionDto, ModrinthSearchHitDto } from '@shared/ipc'
import '@renderer/components/LoginModal.css'

type ContentSource = 'modrinth' | 'curseforge'

interface ContentBrowseModalProps {
  type: 'mod' | 'resourcepack' | 'shader'
  instanceId: string | null
  onClose: () => void
  onInstalled: () => void
}

interface VersionPickState {
  hit: ModrinthSearchHitDto
  versions: ContentVersionDto[]
  selectedId: string
}

function safeJoin(values: string[] | null | undefined): string {
  return Array.isArray(values) && values.length > 0 ? values.join(', ') : '—'
}

export function ContentBrowseModal({ type, instanceId, onClose, onInstalled }: ContentBrowseModalProps) {
  const { t } = useI18n()
  const [source, setSource] = useState<ContentSource>('modrinth')
  const [query, setQuery] = useState('')
  const [results, setResults] = useState<ModrinthSearchHitDto[]>([])
  const [searching, setSearching] = useState(false)
  const [installingId, setInstallingId] = useState<string | null>(null)
  const [loadingVersionsId, setLoadingVersionsId] = useState<string | null>(null)
  const [versionPick, setVersionPick] = useState<VersionPickState | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (!query.trim()) {
      setResults([])
      return
    }

    const timer = setTimeout(() => {
      void (async () => {
        setSearching(true)
        setError(null)
        try {
          const hits =
            source === 'modrinth'
              ? await window.primeLauncher.content.searchModrinth(query.trim(), type, instanceId ?? undefined)
              : await window.primeLauncher.content.searchCurseForge(query.trim(), type, instanceId ?? undefined)
          setResults(Array.isArray(hits) ? hits : [])
        } catch (err) {
          setResults([])
          setError(err instanceof Error ? err.message : t('modals.browse.searchFailed'))
        } finally {
          setSearching(false)
        }
      })()
    }, 350)

    return () => clearTimeout(timer)
  }, [query, type, instanceId, source, t])

  async function handleInstall(hit: ModrinthSearchHitDto, versionId?: string) {
    setInstallingId(hit.project_id)
    setError(null)

    try {
      let result: ContentMutationDto
      if (type === 'mod') {
        result = await window.primeLauncher.content.installMod(
          hit.project_id,
          hit.title,
          instanceId ?? undefined,
          source,
          versionId
        )
      } else if (type === 'resourcepack') {
        result = await window.primeLauncher.content.installResourcePack(
          hit.project_id,
          hit.title,
          instanceId ?? undefined,
          source,
          versionId
        )
      } else {
        result = await window.primeLauncher.content.installShader(
          hit.project_id,
          hit.title,
          instanceId ?? undefined,
          source,
          versionId
        )
      }

      if (result?.ok) {
        onInstalled()
        onClose()
      } else if (result?.error !== 'Cancelled.') {
        setError(result?.error ?? t('modals.browse.installFailed'))
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : t('modals.browse.installFailed'))
    } finally {
      setInstallingId(null)
    }
  }

  async function openVersionPicker(hit: ModrinthSearchHitDto) {
    setLoadingVersionsId(hit.project_id)
    setError(null)
    try {
      const versions = await window.primeLauncher.content.listVersions(
        hit.project_id,
        type,
        source,
        instanceId ?? undefined
      )
      const list = Array.isArray(versions) ? versions : []
      if (list.length === 0) {
        setError(t('modals.browse.noVersions'))
        return
      }
      const recommended = list.find((version) => version.recommended) ?? list[0]!
      setVersionPick({
        hit,
        versions: list,
        selectedId: recommended.id
      })
    } catch (err) {
      setError(err instanceof Error ? err.message : t('modals.browse.versionsFailed'))
    } finally {
      setLoadingVersionsId(null)
    }
  }

  const title =
    type === 'mod'
      ? t('modals.browse.modsTitle')
      : type === 'resourcepack'
        ? t('modals.browse.resourcePacksTitle')
        : t('modals.browse.shadersTitle')

  return createPortal(
    <motion.div
      className="modal-backdrop"
      initial={{ opacity: 0 }}
      animate={{ opacity: 1 }}
      exit={{ opacity: 0 }}
      onClick={onClose}
    >
      <motion.div
        className="modal modal--browse"
        initial={{ opacity: 0, scale: 0.95, y: 12 }}
        animate={{ opacity: 1, scale: 1, y: 0 }}
        exit={{ opacity: 0, scale: 0.95, y: 12 }}
        onClick={(e) => e.stopPropagation()}
      >
        {versionPick ? (
          <>
            <h2 className="modal__title">{t('modals.browse.chooseVersion')}</h2>
            <p className="modal__subtitle">
              {versionPick.hit.title} — {t('modals.browse.chooseVersionHint')}
            </p>

            <div className="modal__scroll page-list">
              {versionPick.versions.map((version) => (
                <label key={version.id} className="list-row" style={{ cursor: 'pointer' }}>
                  <input
                    type="radio"
                    name="content-version"
                    checked={versionPick.selectedId === version.id}
                    onChange={() =>
                      setVersionPick((current) =>
                        current ? { ...current, selectedId: version.id } : current
                      )
                    }
                    style={{ marginRight: 12 }}
                  />
                  <div className="list-row__body">
                    <div className="list-row__title">
                      {version.versionNumber}
                      {version.recommended ? ` (${t('modals.browse.recommended')})` : ''}
                    </div>
                    <div className="list-row__desc">
                      {t('modals.browse.gameVersions')}: {safeJoin(version.gameVersions)}
                      {Array.isArray(version.loaders) && version.loaders.length > 0
                        ? ` · ${t('modals.browse.loaders')}: ${version.loaders.join(', ')}`
                        : ''}
                      {version.fileName ? ` · ${version.fileName}` : ''}
                    </div>
                  </div>
                </label>
              ))}
            </div>

            {error && <div className="modal__error">{error}</div>}

            <div className="modal__footer">
              <Button variant="ghost" onClick={() => setVersionPick(null)}>
                {t('modals.browse.back')}
              </Button>
              <Button
                variant="primary"
                disabled={installingId === versionPick.hit.project_id}
                onClick={() => void handleInstall(versionPick.hit, versionPick.selectedId)}
              >
                {installingId === versionPick.hit.project_id ? t('actions.installing') : t('actions.install')}
              </Button>
            </div>
          </>
        ) : (
          <>
            <h2 className="modal__title">{title}</h2>
            <p className="modal__subtitle">{t('modals.browse.subtitle')}</p>

            <Tabs
              tabs={[
                { id: 'modrinth', label: 'Modrinth' },
                { id: 'curseforge', label: 'CurseForge' }
              ]}
              active={source}
              onChange={(id) => setSource(id as ContentSource)}
            />

            <div style={{ marginTop: 12 }}>
              <SearchInput
                value={query}
                onChange={setQuery}
                placeholder={
                  source === 'modrinth' ? t('actions.searchModrinth') : t('actions.searchCurseForge')
                }
              />
            </div>

            {searching && <p className="text-caption">{t('modals.browse.searching')}</p>}
            {error && <div className="modal__error">{error}</div>}

            <div className="modal__scroll page-list">
              {results.map((hit) => (
                <div key={`${source}-${hit.project_id}`} className="list-row">
                  {hit.icon_url ? (
                    <img
                      src={hit.icon_url}
                      alt=""
                      width={40}
                      height={40}
                      style={{ borderRadius: 8, objectFit: 'cover' }}
                    />
                  ) : (
                    <div className="list-row__icon">
                      <Download size={18} />
                    </div>
                  )}
                  <div className="list-row__body">
                    <div className="list-row__title">{hit.title}</div>
                    <div className="list-row__desc">{hit.description}</div>
                  </div>
                  <Button
                    variant="primary"
                    size="sm"
                    disabled={loadingVersionsId === hit.project_id || installingId === hit.project_id}
                    onClick={() => void openVersionPicker(hit)}
                  >
                    {loadingVersionsId === hit.project_id
                      ? t('modals.browse.searching')
                      : installingId === hit.project_id
                        ? t('actions.installing')
                        : t('actions.install')}
                  </Button>
                </div>
              ))}
            </div>

            <div className="modal__footer">
              <Button variant="ghost" onClick={onClose}>
                {t('modals.browse.close')}
              </Button>
            </div>
          </>
        )}
      </motion.div>
    </motion.div>,
    document.body
  )
}
