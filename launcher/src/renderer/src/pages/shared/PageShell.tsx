import type { ReactNode } from 'react'
import { motion } from 'framer-motion'
import './page-shell.css'

interface PageShellProps {
  title: string
  subtitle?: string
  actions?: ReactNode
  children: ReactNode
}

export function PageShell({ title, subtitle, actions, children }: PageShellProps) {
  return (
    <motion.div
      className="page-shell"
      initial={{ opacity: 0 }}
      animate={{ opacity: 1 }}
      transition={{ duration: 0.35, ease: [0.16, 1, 0.3, 1] }}
    >
      <header className="page-shell__header">
        <div>
          <h1 className="page-shell__title">{title}</h1>
          {subtitle && <p className="page-shell__subtitle">{subtitle}</p>}
        </div>
        {actions && <div className="page-shell__actions">{actions}</div>}
      </header>
      {children}
    </motion.div>
  )
}
