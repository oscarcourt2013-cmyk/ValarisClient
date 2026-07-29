import logoUrl from '../../assets/prime-logo.png'
import './PrimeLogo.css'

const LOGO_ASPECT = 1024 / 559

interface PrimeLogoProps {
  size?: number
  /** Compact square mark for the title bar */
  compact?: boolean
}

export function PrimeLogo({ size = 48, compact = false }: PrimeLogoProps) {
  if (compact) {
    return (
      <img
        className="prime-logo prime-logo--compact"
        src={logoUrl}
        width={size}
        height={size}
        alt=""
        draggable={false}
        aria-hidden
      />
    )
  }

  return (
    <img
      className="prime-logo"
      src={logoUrl}
      width={Math.round(size * LOGO_ASPECT)}
      height={size}
      alt=""
      draggable={false}
      aria-hidden
    />
  )
}
