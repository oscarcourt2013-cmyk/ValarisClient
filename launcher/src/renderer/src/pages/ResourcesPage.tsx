import { useCallback, useEffect, useState } from 'react'
import { AnimatePresence } from 'framer-motion'
import { Download, Image, Plus, Trash2, Upload } from 'lucide-react'
import { PageShell } from '@renderer/pages/shared/PageShell'
import { Badge, Button } from '@renderer/design-system/components'
import { ContentBrowseModal } from '@renderer/components/ContentBrowseModal'
import { useActiveInstance } from '@renderer/hooks/useActiveInstance'
import { useI18n } from '@renderer/context/I18nProvider'
import type { ResourcePackEntry } from '@shared/content-types'

export function ResourcesPage() {
  const { t } = useI18n()
  const { instance, instanceId, refresh: refreshInstance } = useActiveInstance()
  const [packs, setPacks] = useState<ResourcePackEntry[]>([])
  const [showBrowse, setShowBrowse] = useState(false)

  const refresh = useCallback(async () => {
    if (!instanceId) {
      return
    }
    setPacks(await window.primeLauncher.content.listResourcePacks(instanceId))
  }, [instanceId])

  useEffect(() => {
    void refresh()
  }, [refresh])

  async function handleActivate(pack: ResourcePackEntry) {
    if (!instanceId) {
      return
    }
    const next = pack.active ? null : pack.fileName
    await window.primeLauncher.content.setResourcePackActive(next, instanceId)
    await refresh()
  }

  async function handleImport() {
    const result = await window.primeLauncher.content.importResourcePack(instanceId ?? undefined)
    if (result.ok) {
      await refresh()
    } else if (result.error !== 'Cancelled.') {
      alert(result.error)
    }
  }

  async function handleRemove(pack: ResourcePackEntry) {
    if (!instanceId || !confirm(t('confirm.removePack', { name: pack.name }))) {
      return
    }
    await window.primeLauncher.content.removeResourcePack(pack.fileName, instanceId)
    await refresh()
  }

  return (
    <PageShell
      title={t('pages.resources.title')}
      subtitle={t('pages.resources.subtitle')}
      actions={
        <div style={{ display: 'flex', gap: 8 }}>
          <Button variant="secondary" icon={<Upload size={16} />} onClick={() => void handleImport()}>
            {t('resources.importZip')}
          </Button>
          <Button variant="primary" icon={<Download size={16} />} onClick={() => setShowBrowse(true)}>
            {t('actions.browseContent')}
          </Button>
        </div>
      }
    >
      {packs.length === 0 ? (
        <p className="text-caption">{t('empty.noResourcePacks')}</p>
      ) : (
        <div className="page-grid page-grid--3">
          {packs.map((pack) => (
            <div
              key={pack.id}
              className={`tile${pack.active ? ' tile--active' : ''}`}
              onClick={() => void handleActivate(pack)}
              role="button"
              tabIndex={0}
            >
              <div className="tile__preview">
                <Image size={32} />
              </div>
              <div className="tile__name">{pack.name}</div>
              <div className="tile__desc">{pack.description}</div>
              <div style={{ marginTop: 12, display: 'flex', gap: 8, alignItems: 'center' }}>
                <Badge variant={pack.active ? 'prime' : 'default'}>
                  {pack.resolution}
                  {pack.active ? ` · ${t('resources.active')}` : ''}
                </Badge>
                <Button
                  variant="ghost"
                  size="sm"
                  icon={<Trash2 size={14} />}
                  onClick={(e) => {
                    e.stopPropagation()
                    void handleRemove(pack)
                  }}
                />
              </div>
            </div>
          ))}
        </div>
      )}

      <AnimatePresence>
        {showBrowse && (
          <ContentBrowseModal
            key="content-browse-resourcepack"
            type="resourcepack"
            instanceId={instanceId}
            onClose={() => setShowBrowse(false)}
            onInstalled={() => {
              void refresh()
              void refreshInstance()
            }}
          />
        )}
      </AnimatePresence>
    </PageShell>
  )
}
