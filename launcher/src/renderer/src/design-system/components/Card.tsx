import type { ReactNode } from 'react'
import './Card.css'

interface CardProps {
  title?: string
  action?: ReactNode
  glow?: boolean
  hover?: boolean
  className?: string
  children: ReactNode
}

export function Card({ title, action, glow, hover = true, className = '', children }: CardProps) {
  const classes = ['prime-card', hover ? 'prime-card--hover' : '', glow ? 'prime-card--glow' : '', className]
    .filter(Boolean)
    .join(' ')

  return (
    <div className={classes}>
      {(title || action) && (
        <div className="prime-card__header">
          {title && <span className="prime-card__title">{title}</span>}
          {action}
        </div>
      )}
      <div className="prime-card__body">{children}</div>
    </div>
  )
}
