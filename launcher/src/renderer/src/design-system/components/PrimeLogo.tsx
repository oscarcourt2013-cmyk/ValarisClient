import logoUrl from '../../assets/stellar-logo.png'
import './PrimeLogo.css'

interface PrimeLogoProps {
  size?: number
  /** Compact square mark for the title bar */
  compact?: boolean
}

export function PrimeLogo({ size = 48, compact = false }: PrimeLogoProps) {
  return (
    <img
      className={compact ? 'prime-logo prime-logo--compact' : 'prime-logo'}
      src={logoUrl}
      width={size}
      height={size}
      alt=""
      draggable={false}
      aria-hidden
    />
  )
}
