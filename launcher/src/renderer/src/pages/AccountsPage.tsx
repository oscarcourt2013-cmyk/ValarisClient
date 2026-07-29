import { useState } from 'react'
import { Link } from 'react-router-dom'
import { AnimatePresence } from 'framer-motion'
import { Cloud, KeyRound, RefreshCw, Shirt, Trash2, UserPlus } from 'lucide-react'
import { useI18n } from '@renderer/context/I18nProvider'
import { formatTier } from '@shared/format'
import { PageShell } from '@renderer/pages/shared/PageShell'
import { Avatar, Badge, Button } from '@renderer/design-system/components'
import { useAccounts } from '@renderer/context/AccountProvider'
import { LoginModal } from '@renderer/components/LoginModal'
import { EmptyState } from '@renderer/components/EmptyState'
import { playUiSound } from '@renderer/lib/uiSounds'
import './AccountsPage.css'

export function AccountsPage() {
  const { t, locale } = useI18n()
  const {
    prime,
    accounts,
    activeAccount,
    loginMicrosoft,
    removeAccount,
    setActive,
    syncPrime,
    refresh
  } = useAccounts()
  const [showLogin, setShowLogin] = useState(false)
  const [busy, setBusy] = useState<string | null>(null)
  const [message, setMessage] = useState<string | null>(null)

  const displayName = activeAccount?.username ?? prime?.username ?? t('common.guest')

  async function handleSync() {
    setBusy('sync')
    playUiSound('click')
    const result = await syncPrime()
    setMessage(result.message)
    playUiSound(result.message ? 'success' : 'click')
    setBusy(null)
  }

  async function handleRefreshMicrosoft(accountId: string) {
    setBusy(accountId)
    const result = await window.primeLauncher.account.refreshMicrosoft(accountId)
    setMessage(result.ok ? t('accounts.refreshSuccess') : result.error ?? t('accounts.refreshFailed'))
    if (result.ok) {
      playUiSound('success')
      await refresh()
    } else {
      playUiSound('error')
    }
    setBusy(null)
  }

  async function handleRemove(id: string) {
    setBusy(id)
    playUiSound('click')
    await removeAccount(id)
    setBusy(null)
  }

  async function handleUse(id: string) {
    playUiSound('click')
    await setActive(id)
  }

  async function handleMicrosoft() {
    setBusy('ms')
    playUiSound('click')
    const result = await loginMicrosoft()
    if (result.error) {
      setMessage(result.error)
      playUiSound('error')
    } else {
      playUiSound('success')
    }
    setBusy(null)
  }

  return (
    <PageShell
      title={t('pages.accounts.title')}
      subtitle={t('pages.accounts.subtitle')}
      actions={
        <Button variant="primary" icon={<UserPlus size={16} />} onClick={() => setShowLogin(true)}>
          {t('accounts.addAccount')}
        </Button>
      }
    >
      <div className="accounts">
        <section className="accounts__hero">
          <div className="accounts__hero-avatar">
            <div className="accounts__hero-glow" />
            <Avatar
              alt={displayName}
              uuid={activeAccount?.uuid}
              size="lg"
              glow
            />
          </div>

          <div className="accounts__hero-copy">
            <p className="accounts__eyebrow">{t('accounts.activeProfile')}</p>
            <h2 className="accounts__name">{displayName}</h2>

            <div className="accounts__badges">
              {prime ? (
                <>
                  <Badge variant="prime">{formatTier(prime.tier, locale)}</Badge>
                  <Badge variant="default">{t('accounts.level', { level: prime.level })}</Badge>
                </>
              ) : (
                <Badge variant="default">{t('common.guest')}</Badge>
              )}
              {activeAccount && (
                <span className="accounts__type-chip">
                  {activeAccount.type === 'microsoft' ? t('accounts.microsoft') : t('accounts.offline')}
                </span>
              )}
            </div>

            <p className="accounts__hint">{t('accounts.syncDescription')}</p>

            <div className="accounts__hero-actions">
              <Button
                variant="secondary"
                size="sm"
                icon={<Cloud size={14} />}
                disabled={busy === 'sync' || !prime}
                onClick={() => void handleSync()}
              >
                {t('accounts.syncButton')}
              </Button>
              <Link to="/skins" className="accounts__link">
                <Shirt size={14} />
                {t('accounts.editSkin')}
              </Link>
            </div>

            {message && <p className="accounts__toast">{message}</p>}
          </div>
        </section>

        <section className="accounts__list-section">
          <div className="accounts__list-head">
            <h3>{t('accounts.minecraftAccounts')}</h3>
            <span className="accounts__count">
              {t('accounts.count', { count: accounts.length })}
            </span>
          </div>

          {accounts.length === 0 ? (
            <EmptyState
              icon={<UserPlus size={22} />}
              title={t('accounts.noAccounts')}
              description={t('accounts.emptyHint')}
              action={
                <div className="accounts__empty-actions">
                  <Button
                    variant="primary"
                    icon={<KeyRound size={16} />}
                    disabled={!!busy}
                    onClick={() => void handleMicrosoft()}
                  >
                    {t('accounts.signInMicrosoft')}
                  </Button>
                  <Button variant="secondary" onClick={() => setShowLogin(true)}>
                    {t('accounts.addOffline')}
                  </Button>
                </div>
              }
            />
          ) : (
            <ul className="accounts__list">
              {accounts.map((acc) => {
                const isActive = acc.id === activeAccount?.id
                return (
                  <li
                    key={acc.id}
                    className={`accounts__row${isActive ? ' is-active' : ''}`}
                  >
                    <button
                      type="button"
                      className="accounts__row-main"
                      onClick={() => {
                        if (!isActive) void handleUse(acc.id)
                      }}
                    >
                      <Avatar alt={acc.username} uuid={acc.uuid} size="md" glow={isActive} />
                      <div className="accounts__row-text">
                        <strong>{acc.username}</strong>
                        <span>
                          {acc.type === 'microsoft' ? t('accounts.microsoft') : t('accounts.offline')}
                        </span>
                      </div>
                    </button>

                    <div className="accounts__row-actions">
                      {isActive ? (
                        <Badge variant="prime">{t('common.active')}</Badge>
                      ) : (
                        <Button variant="ghost" size="sm" onClick={() => void handleUse(acc.id)}>
                          {t('common.use')}
                        </Button>
                      )}
                      {acc.type === 'microsoft' && (
                        <button
                          type="button"
                          className="accounts__icon-btn"
                          disabled={busy === acc.id}
                          onClick={() => void handleRefreshMicrosoft(acc.id)}
                          title={t('accounts.refreshMicrosoft')}
                        >
                          <RefreshCw size={14} />
                        </button>
                      )}
                      <button
                        type="button"
                        className="accounts__icon-btn accounts__icon-btn--danger"
                        disabled={busy === acc.id}
                        onClick={() => void handleRemove(acc.id)}
                        title={t('accounts.remove')}
                      >
                        <Trash2 size={14} />
                      </button>
                    </div>
                  </li>
                )
              })}
            </ul>
          )}

          {accounts.length > 0 && (
            <div className="accounts__add-bar">
              <Button
                variant="secondary"
                size="sm"
                icon={<KeyRound size={14} />}
                disabled={!!busy}
                onClick={() => void handleMicrosoft()}
              >
                {t('accounts.signInMicrosoft')}
              </Button>
              <Button variant="ghost" size="sm" icon={<UserPlus size={14} />} onClick={() => setShowLogin(true)}>
                {t('accounts.addOffline')}
              </Button>
            </div>
          )}
        </section>
      </div>

      <AnimatePresence>{showLogin && <LoginModal onClose={() => setShowLogin(false)} />}</AnimatePresence>
    </PageShell>
  )
}
