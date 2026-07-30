import { AlertTriangle, Bot, ExternalLink, FileText, X } from 'lucide-react'
import type { GameCrashAnalysisDto } from '@shared/ipc'
import { Button } from '@renderer/design-system/components'
import { useI18n } from '@renderer/context/I18nProvider'
import { askPrimeAssistant } from '@renderer/lib/askPrimeAssistant'
import './CrashReportPanel.css'

interface CrashReportPanelProps {
  crash: GameCrashAnalysisDto
  onDismiss?: () => void
}

function formatDuration(seconds: number): string {
  if (seconds < 60) {
    return `${seconds}s`
  }
  const minutes = Math.floor(seconds / 60)
  const rest = seconds % 60
  return rest > 0 ? `${minutes}m ${rest}s` : `${minutes}m`
}

function fixSuggestion(
  t: (key: string, params?: Record<string, string | number>) => string,
  crash: GameCrashAnalysisDto
): string {
  switch (crash.fixKey) {
    case 'blurOnce':
      return t('crash.fix.blurOnce')
    case 'outOfMemory':
      return t('crash.fix.outOfMemory')
    case 'primeMod':
      return t('crash.fix.primeMod', { location: crash.primeLocation ?? 'StellarClient' })
    case 'modConflict':
      return crash.modIds.length > 0
        ? t('crash.fix.modConflictNamed', { mods: crash.modIds.join(', ') })
        : t('crash.fix.modConflict')
    case 'loaderError':
      return t('crash.fix.loaderError')
    default:
      return t('crash.fix.unknown')
  }
}

export function CrashReportPanel({ crash, onDismiss }: CrashReportPanelProps) {
  const { t } = useI18n()

  return (
    <section className="crash-panel" role="alert">
      <div className="crash-panel__header">
        <div className="crash-panel__title-row">
          <AlertTriangle size={20} className="crash-panel__icon" />
          <div>
            <h3 className="crash-panel__title">{t('crash.title')}</h3>
            <p className="crash-panel__subtitle">
              {t('crash.sessionDuration', { duration: formatDuration(crash.sessionDurationSec) })}
            </p>
          </div>
        </div>
        {onDismiss && (
          <button type="button" className="crash-panel__dismiss" onClick={onDismiss} aria-label={t('crash.dismiss')}>
            <X size={16} />
          </button>
        )}
      </div>

      <div className="crash-panel__body">
        <p className="crash-panel__exception">{crash.title}</p>
        {crash.description && (
          <p className="crash-panel__detail">
            <span className="crash-panel__label">{t('crash.context')}</span> {crash.description}
          </p>
        )}
        {crash.screen && (
          <p className="crash-panel__detail">
            <span className="crash-panel__label">{t('crash.screen')}</span> {crash.screen}
          </p>
        )}
        {crash.primeInvolved && (
          <p className="crash-panel__prime-badge">{t('crash.primeInvolved')}</p>
        )}
        <div className="crash-panel__suggestion">
          <span className="crash-panel__label">{t('crash.suggestion')}</span>
          <p>{fixSuggestion(t, crash)}</p>
        </div>
      </div>

      <div className="crash-panel__actions">
        <Button
          variant="primary"
          size="sm"
          icon={<Bot size={14} />}
          onClick={() => {
            const bits = [
              'Minecraft a crashé. Appelle diagnose_instance (et read_crash_report si besoin).',
              `Titre: ${crash.title}`,
              crash.description ? `Description: ${crash.description}` : '',
              crash.exceptionType ? `Exception: ${crash.exceptionType}` : '',
              crash.exceptionMessage ? `Message: ${crash.exceptionMessage}` : '',
              crash.screen ? `Screen: ${crash.screen}` : '',
              crash.primeInvolved ? `Prime impliqué: ${crash.primeLocation ?? 'oui'}` : '',
              crash.modIds.length ? `Mods suspects: ${crash.modIds.join(', ')}` : '',
              crash.crashReportPath ? `Fichier: ${crash.crashReportPath}` : '',
              'Dis-moi la cause probable et les étapes concrètes pour corriger.'
            ].filter(Boolean)
            askPrimeAssistant(bits.join('\n'))
          }}
        >
          {t('crash.askAi')}
        </Button>
        {crash.crashReportPath && (
          <Button
            variant="secondary"
            size="sm"
            icon={<FileText size={14} />}
            onClick={() => void window.primeLauncher.launch.openCrashReport(crash.crashReportPath!)}
          >
            {t('crash.openReport')}
          </Button>
        )}
        <Button
          variant="secondary"
          size="sm"
          onClick={() =>
            void (async () => {
              try {
                const session = (await window.primeLauncher.chat.connect()) as { token?: string }
                const token = session?.token
                if (!token) return
                const text = [crash.title, crash.description, crash.exceptionType, crash.exceptionMessage, crash.screen]
                  .filter(Boolean)
                  .join('\n')
                const base = (import.meta as { env?: { VITE_PRIME_API?: string } }).env?.VITE_PRIME_API
                  || 'http://194.9.172.102:26005'
                await fetch(`${base.replace(/\/$/, '')}/v1/crash`, {
                  method: 'POST',
                  headers: {
                    Authorization: `Bearer ${token}`,
                    'Content-Type': 'application/json'
                  },
                  body: JSON.stringify({ title: crash.title, text })
                })
              } catch {
                /* ignore */
              }
            })()
          }
        >
          {t('crash.sendReport')}
        </Button>
        <Button
          variant="ghost"
          size="sm"
          icon={<ExternalLink size={14} />}
          onClick={() => void window.primeLauncher.launch.openLogFolder()}
        >
          {t('crash.openLogs')}
        </Button>
      </div>
    </section>
  )
}
