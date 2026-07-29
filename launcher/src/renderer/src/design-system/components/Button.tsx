import type { ButtonHTMLAttributes, ReactNode } from 'react'
import './Button.css'

type Variant = 'primary' | 'secondary' | 'ghost' | 'danger'
type Size = 'sm' | 'md' | 'lg' | 'xl'

interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: Variant
  size?: Size
  icon?: ReactNode
  block?: boolean
}

export function Button({
  variant = 'secondary',
  size = 'md',
  icon,
  block,
  className = '',
  children,
  ...props
}: ButtonProps) {
  const classes = [
    'prime-btn',
    `prime-btn--${variant}`,
    `prime-btn--${size}`,
    block ? 'prime-btn--block' : '',
    className
  ]
    .filter(Boolean)
    .join(' ')

  return (
    <button className={classes} {...props}>
      {icon}
      {children}
    </button>
  )
}
