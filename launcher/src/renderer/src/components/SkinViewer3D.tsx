import { lazy, Suspense } from 'react'
import type { SkinViewer3DProps } from './SkinViewer3DImpl'

export type { SkinViewerPose, SkinViewerBackdrop } from './SkinViewer3DImpl'

/**
 * Lazy wrapper around the real viewer.
 *
 * <p>skinview3d pulls in a WebGL renderer, which is one of the heaviest things
 * the launcher can load. Splitting it out keeps it off the startup path: the
 * dashboard renders immediately and the viewer streams in behind a placeholder,
 * and pages that never show a skin never download it at all.</p>
 */
const Impl = lazy(() =>
  import('./SkinViewer3DImpl').then((m) => ({ default: m.SkinViewer3DImpl }))
)

export function SkinViewer3D(props: SkinViewer3DProps) {
  // Reserve the final size so the surrounding layout does not jump when it loads.
  const placeholder = (
    <div
      className={props.className}
      style={{ width: props.width, height: props.height }}
      aria-busy="true"
    />
  )
  return (
    <Suspense fallback={placeholder}>
      <Impl {...props} />
    </Suspense>
  )
}
