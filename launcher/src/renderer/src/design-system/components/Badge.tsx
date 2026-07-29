import type { ReactNode } from 'react'
import './Badge.css'

type Variant = 'default' | 'red' | 'success' | 'prime'

interface BadgeProps {
  variant?: Variant
  children: ReactNode
}

export function Badge({ variant = 'default', children }: BadgeProps) {
  return <span className={`prime-badge prime-badge--${variant}`}>{children}</span>
}
