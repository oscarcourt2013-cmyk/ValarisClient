import { useEffect, useState } from 'react'
import { Cpu, Monitor, Zap } from 'lucide-react'
import type { HardwareProfile, PerformancePreset, PerformancePresetInfo } from '@shared/content-types'
import { PageShell } from '@renderer/pages/shared/PageShell'
import { Badge, Button, Card } from '@renderer/design-system/components'
import { useActiveInstance } from '@renderer/hooks/useActiveInstance'
import { useI18n } from '@renderer/context/I18nProvider'

export function PerformancePage() {
  const { t } = useI18n()
  const { instanceId } = useActiveInstance()
  const [selected, setSelected] = useState<PerformancePreset>('balanced')
  const [hw, setHw] = useState<HardwareProfile | null>(null)
  const [presets, setPresets] = useState<PerformancePresetInfo[]>([])
  const [message, setMessage] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)

  useEffect(() => {
    void (async () => {
      const [hardware, list, active] = await Promise.all([
        window.primeLauncher.performance.hardware(),
        window.primeLauncher.performance.presets(),
        window.primeLauncher.performance.selected()
      ])
      setHw(hardware)
      setPresets(list)
      setSelected(active)
    })()
  }, [])

  async function handleApply() {
    setBusy(true)
    setMessage(null)
    const result = await window.primeLauncher.performance.apply(selected, instanceId ?? undefined)
    setBusy(false)
    setMessage(result.ok ? t('performance.applySuccess') : result.error ?? t('performance.applyFailed'))
  }

  return (
    <PageShell
      title={t('pages.performance.title')}
      subtitle={t('pages.performance.subtitle')}
      actions={
        <Button variant="primary" icon={<Zap size={16} />} disabled={busy} onClick={() => void handleApply()}>
          {busy ? t('actions.applying') : t('actions.applyPreset')}
        </Button>
      }
    >
      {message && (
        <p className="text-caption" style={{ marginBottom: 16, color: 'var(--prime-muted)' }}>
          {message}
        </p>
      )}

      <Card title={t('performance.detectedHardware')} glow>
        {hw ? (
          <div style={{ display: 'flex', gap: 32, flexWrap: 'wrap' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
              <Cpu size={20} style={{ color: 'var(--prime-red-bright)' }} />
              <div>
                <div className="text-caption">{t('performance.cpu')}</div>
                <div className="list-row__title">{hw.cpu}</div>
              </div>
            </div>
            <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
              <Monitor size={20} style={{ color: 'var(--prime-red-bright)' }} />
              <div>
                <div className="text-caption">{t('performance.gpu')}</div>
                <div className="list-row__title">{hw.gpu}</div>
              </div>
            </div>
            <div>
              <div className="text-caption">{t('performance.systemRam')}</div>
              <div className="list-row__title">{hw.ramGb} GB</div>
            </div>
          </div>
        ) : (
          <p className="text-caption">{t('performance.detecting')}</p>
        )}
      </Card>

      <div className="page-grid page-grid--4" style={{ marginTop: 8 }}>
        {presets.map((preset) => (
          <div
            key={preset.id}
            className={`tile${selected === preset.id ? ' tile--active' : ''}`}
            onClick={() => setSelected(preset.id)}
            role="button"
            tabIndex={0}
          >
            <div className="tile__name">{preset.label}</div>
            <div className="tile__desc">{preset.description}</div>
            <div style={{ marginTop: 16, display: 'flex', flexDirection: 'column', gap: 8 }}>
              <Badge variant="default">{t('instances.ramBadge', { mb: preset.ramMb })}</Badge>
              <Badge variant="default">{t('performance.chunks', { count: preset.renderDistance })}</Badge>
            </div>
          </div>
        ))}
      </div>
    </PageShell>
  )
}
