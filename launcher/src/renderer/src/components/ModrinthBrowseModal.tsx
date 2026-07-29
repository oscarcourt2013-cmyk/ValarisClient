import { useEffect, useState } from 'react'
import { createPortal } from 'react-dom'
import { motion } from 'framer-motion'
import { Download } from 'lucide-react'
import { Button, SearchInput } from '@renderer/design-system/components'
import { useI18n } from '@renderer/context/I18nProvider'
import type { ContentMutationDto, ModrinthSearchHitDto } from '@shared/ipc'
import '@renderer/components/LoginModal.css'

interface ModrinthBrowseModalProps {
  type: 'mod' | 'resourcepack' | 'shader'
  instanceId: string | null
  onClose: () => void
  onInstalled: () => void
}

export function ModrinthBrowseModal({ type, instanceId, onClose, onInstalled }: ModrinthBrowseModalProps) {
  const { t } = useI18n()
  const [query, setQuery] = useState('')
  const [results, setResults] = useState<ModrinthSearchHitDto[]>([])
  const [searching, setSearching] = useState(false)
  const [installingId, setInstallingId] = useState<string | null>(null)
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
          const hits = await window.primeLauncher.content.searchModrinth(
            query.trim(),
            type,
            instanceId ?? undefined
          )
          setResults(Array.isArray(hits) ? hits : [])
        } catch (err) {
          setResults([])
          setError(err instanceof Error ? err.message : t('modals.modrinth.searchFailed'))
        } finally {
          setSearching(false)
        }
      })()
    }, 350)

    return () => clearTimeout(timer)
  }, [query, type, instanceId, t])

  async function handleInstall(hit: ModrinthSearchHitDto) {
    setInstallingId(hit.project_id)
    setError(null)

    try {
      let result: ContentMutationDto
      if (type === 'mod') {
        result = await window.primeLauncher.content.installMod(
          hit.project_id,
          hit.title,
          instanceId ?? undefined
        )
      } else if (type === 'resourcepack') {
        result = await window.primeLauncher.content.installResourcePack(
          hit.project_id,
          hit.title,
          instanceId ?? undefined
        )
      } else {
        result = await window.primeLauncher.content.installShader(
          hit.project_id,
          hit.title,
          instanceId ?? undefined
        )
      }

      if (result?.ok) {
        onInstalled()
        onClose()
      } else if (result?.error !== 'Cancelled.') {
        setError(result?.error ?? t('modals.modrinth.installFailed'))
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : t('modals.modrinth.installFailed'))
    } finally {
      setInstallingId(null)
    }
  }

  const title =
    type === 'mod'
      ? t('modals.modrinth.modsTitle')
      : type === 'resourcepack'
        ? t('modals.modrinth.resourcePacksTitle')
        : t('modals.modrinth.shadersTitle')

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
        <h2 className="modal__title">{title}</h2>
        <p className="modal__subtitle">{t('modals.modrinth.subtitle')}</p>

        <SearchInput value={query} onChange={setQuery} placeholder={t('actions.searchModrinth')} />

        {searching && <p className="text-caption">{t('modals.modrinth.searching')}</p>}
        {error && <div className="modal__error">{error}</div>}

        <div className="modal__scroll page-list">
          {results.map((hit) => (
            <div key={hit.project_id} className="list-row">
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
                disabled={installingId === hit.project_id}
                onClick={() => void handleInstall(hit)}
              >
                {installingId === hit.project_id ? t('actions.installing') : t('actions.install')}
              </Button>
            </div>
          ))}
        </div>

        <div className="modal__footer">
          <Button variant="ghost" onClick={onClose}>
            {t('modals.modrinth.close')}
          </Button>
        </div>
      </motion.div>
    </motion.div>,
    document.body
  )
}
