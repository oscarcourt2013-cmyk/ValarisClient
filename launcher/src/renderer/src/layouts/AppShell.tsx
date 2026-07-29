import { useEffect, useState } from 'react'
import { Outlet } from 'react-router-dom'
import { AnimatePresence } from 'framer-motion'
import { Bot, Users } from 'lucide-react'
import { TitleBar } from './TitleBar'
import { Sidebar } from './Sidebar'
import { useAccounts } from '@renderer/context/AccountProvider'
import { useI18n } from '@renderer/context/I18nProvider'
import { SocialDrawer } from '@renderer/components/SocialDrawer'
import { AiAssistantDrawer } from '@renderer/components/AiAssistantDrawer'
import { OnboardingModal } from '@renderer/components/OnboardingModal'
import { WhatsNewModal, resolveWhatsNew } from '@renderer/components/WhatsNewModal'
import './AppShell.css'

interface AppShellProps {
  children?: React.ReactNode
}

export function AppShell({ children }: AppShellProps) {
  const { activeAccount, prime } = useAccounts()
  const { t } = useI18n()
  const username = activeAccount?.username ?? t('common.guest')
  const tier = prime?.tier ?? 'free'
  const [socialOpen, setSocialOpen] = useState(false)
  const [aiOpen, setAiOpen] = useState(false)
  const [showOnboarding, setShowOnboarding] = useState(false)
  const [whatsNew, setWhatsNew] = useState<ReturnType<typeof resolveWhatsNew>>(null)

  useEffect(() => {
    void (async () => {
      const [settings, version] = await Promise.all([
        window.primeLauncher.settings.get(),
        window.primeLauncher.app.getVersion()
      ])
      if (!settings.onboardingDone) {
        setShowOnboarding(true)
      }
      const entry = resolveWhatsNew(version)
      if (entry && settings.lastSeenLauncherVersion !== version) {
        setWhatsNew(entry)
      }
    })()
  }, [])

  return (
    <div className="app-shell">
      <TitleBar />
      <div className="app-shell__body">
        <Sidebar username={username} tier={tier} uuid={activeAccount?.uuid} />
        <main className="app-shell__content">
          <div className="app-shell__content-inner">{children ?? <Outlet />}</div>
          <div className="app-shell__fabs">
            <button
              type="button"
              className="app-shell__social-fab"
              onClick={() => setAiOpen(true)}
              title={t('ai.title')}
            >
              <Bot size={18} />
            </button>
            <button
              type="button"
              className="app-shell__social-fab"
              onClick={() => setSocialOpen(true)}
              title={t('social.title')}
            >
              <Users size={18} />
            </button>
          </div>
        </main>
      </div>
      <AiAssistantDrawer
        open={aiOpen}
        onClose={() => setAiOpen(false)}
        onOpen={() => setAiOpen(true)}
      />
      <SocialDrawer open={socialOpen} onClose={() => setSocialOpen(false)} />
      <AnimatePresence>
        {showOnboarding && <OnboardingModal onDone={() => setShowOnboarding(false)} />}
      </AnimatePresence>
      <AnimatePresence>
        {!showOnboarding && whatsNew && (
          <WhatsNewModal entry={whatsNew} onClose={() => setWhatsNew(null)} />
        )}
      </AnimatePresence>
    </div>
  )
}
