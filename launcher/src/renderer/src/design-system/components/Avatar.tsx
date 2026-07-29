import { isSkinTextureUrl, playerHeadUrl } from '@shared/format'
import './Avatar.css'

type Size = 'sm' | 'md' | 'lg' | 'xl'

const HEAD_PIXELS: Record<Size, number> = {
  sm: 32,
  md: 48,
  lg: 64,
  xl: 80
}

interface AvatarProps {
  uuid?: string
  src?: string
  alt: string
  size?: Size
  glow?: boolean
}

export function Avatar({ uuid, src, alt, size = 'md', glow }: AvatarProps) {
  const pixels = HEAD_PIXELS[size]
  const url =
    src && !isSkinTextureUrl(src)
      ? src
      : playerHeadUrl(uuid, alt, pixels)

  return (
    <img
      className={`prime-avatar prime-avatar--${size}${glow ? ' prime-avatar--glow' : ''}`}
      src={url}
      alt={alt}
      draggable={false}
    />
  )
}
