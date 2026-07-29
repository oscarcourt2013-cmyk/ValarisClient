import { FolderOpen, Trash2 } from 'lucide-react'
import { LaunchLogConsole } from '@renderer/components/LaunchLogConsole'
import { CrashReportPanel } from '@renderer/components/CrashReportPanel'
import { Button } from '@renderer/design-system/components'
import { PageShell } from '@renderer/pages/shared/PageShell'
import { useI18n } from '@renderer/context/I18nProvider'
import { useAccounts } from '@renderer/context/AccountProvider'
import { useEffect, useState } from 'react'

export function ConsolePage() {
  const { t } = useI18n()
  const { launchProgress } = useAccounts()
  const [crashDismissed, setCrashDismissed] = useState(false)

  useEffect(() => {
    if (launchProgress?.phase === 'crashed') {
      setCrashDismissed(false)
    }
  }, [launchProgress?.phase, launchProgress?.crash?.title])

  return (
    <PageShell
      title={t('pages.console.title')}
      subtitle={t('pages.console.subtitle')}
      actions={
        <>
          <Button
            variant="secondary"
            size="sm"
            icon={<FolderOpen size={14} />}
            onClick={() => void window.primeLauncher.launch.openLogFolder()}
          >
            {t('logs.openFolder')}
          </Button>
          <Button
            variant="secondary"
            size="sm"
            icon={<Trash2 size={14} />}
            onClick={() => void window.primeLauncher.launch.clearLogs()}
          >
            {t('logs.clear')}
          </Button>
        </>
      }
    >
      {launchProgress?.phase === 'crashed' && launchProgress.crash && !crashDismissed && (
        <div style={{ marginBottom: 16 }}>
          <CrashReportPanel crash={launchProgress.crash} onDismiss={() => setCrashDismissed(true)} />
        </div>
      )}
      <LaunchLogConsole />
    </PageShell>
  )
}
