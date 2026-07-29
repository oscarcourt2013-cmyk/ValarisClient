import { useCallback, useEffect, useRef, useState } from 'react'
import {
  SkinViewer,
  WalkingAnimation,
  RunningAnimation,
  IdleAnimation
} from 'skinview3d'
import { Maximize2, Minimize2, RotateCcw, Image as ImageIcon } from 'lucide-react'
import { playerSkinUrl } from '@shared/format'
import './SkinViewer3D.css'

export type SkinViewerPose = 'idle' | 'walk' | 'run'
export type SkinViewerBackdrop = 'none' | 'soft' | 'grid'

interface SkinViewer3DProps {
  uuid?: string
  username: string
  /** Override skin texture (local library data URL). */
  skinUrl?: string | null
  /** Optional cape texture URL (PNG). */
  capeUrl?: string | null
  pose?: SkinViewerPose
  className?: string
  /** CSS pixel size of the canvas area. */
  width?: number
  height?: number
  /** Show reset / fullscreen / backdrop controls. */
  showControls?: boolean
  backdrop?: SkinViewerBackdrop
  onBackdropChange?: (backdrop: SkinViewerBackdrop) => void
}

function animationFor(pose: SkinViewerPose) {
  if (pose === 'walk') return new WalkingAnimation()
  if (pose === 'run') {
    const anim = new RunningAnimation()
    anim.speed = 1.15
    return anim
  }
  return new IdleAnimation()
}

function resolveSkin(uuid: string | undefined, username: string, skinUrl?: string | null): string {
  if (skinUrl && skinUrl.trim()) return skinUrl.trim()
  return playerSkinUrl(uuid, username)
}

export function SkinViewer3D({
  uuid,
  username,
  skinUrl,
  capeUrl,
  pose = 'idle',
  className = '',
  width = 280,
  height = 380,
  showControls = false,
  backdrop: backdropProp = 'none',
  onBackdropChange
}: SkinViewer3DProps) {
  const canvasRef = useRef<HTMLCanvasElement>(null)
  const rootRef = useRef<HTMLDivElement>(null)
  const viewerRef = useRef<SkinViewer | null>(null)
  const [fullscreen, setFullscreen] = useState(false)
  const [localBackdrop, setLocalBackdrop] = useState<SkinViewerBackdrop>(backdropProp)
  const backdrop = onBackdropChange ? backdropProp : localBackdrop

  const setBackdrop = useCallback(
    (next: SkinViewerBackdrop) => {
      if (onBackdropChange) onBackdropChange(next)
      else setLocalBackdrop(next)
    },
    [onBackdropChange]
  )

  const effectiveWidth = fullscreen ? Math.min(520, typeof window !== 'undefined' ? window.innerWidth - 80 : 520) : width
  const effectiveHeight = fullscreen
    ? Math.min(680, typeof window !== 'undefined' ? window.innerHeight - 120 : 680)
    : height

  useEffect(() => {
    const canvas = canvasRef.current
    if (!canvas) return

    const viewer = new SkinViewer({
      canvas,
      width: effectiveWidth,
      height: effectiveHeight,
      skin: resolveSkin(uuid, username, skinUrl),
      model: 'auto-detect',
      enableControls: true,
      animation: animationFor(pose),
      zoom: 0.72,
      fov: 42
    })

    viewer.renderer.setClearColor(0x000000, 0)
    viewer.scene.background = null
    viewer.controls.enablePan = false
    viewer.controls.enableZoom = true
    viewer.controls.minDistance = 25
    viewer.controls.maxDistance = 90
    viewer.autoRotate = false

    if (capeUrl) {
      void viewer.loadCape(capeUrl)
    }

    viewerRef.current = viewer

    return () => {
      viewer.dispose()
      viewerRef.current = null
    }
    // Recreate when identity / size / skin source changes — pose/cape handled below.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [uuid, username, skinUrl, effectiveWidth, effectiveHeight])

  useEffect(() => {
    const viewer = viewerRef.current
    if (!viewer) return
    viewer.animation = animationFor(pose)
  }, [pose])

  useEffect(() => {
    const viewer = viewerRef.current
    if (!viewer) return
    if (capeUrl) {
      void viewer.loadCape(capeUrl)
    } else {
      viewer.loadCape(null)
    }
  }, [capeUrl])

  useEffect(() => {
    const viewer = viewerRef.current
    if (!viewer) return
    void viewer.loadSkin(resolveSkin(uuid, username, skinUrl))
  }, [skinUrl, uuid, username])

  useEffect(() => {
    const viewer = viewerRef.current
    if (!viewer) return
    viewer.setSize(effectiveWidth, effectiveHeight)
  }, [effectiveWidth, effectiveHeight])

  useEffect(() => {
    if (!fullscreen) return
    function onKey(e: KeyboardEvent) {
      if (e.key === 'Escape') setFullscreen(false)
    }
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  }, [fullscreen])

  function resetCamera() {
    const viewer = viewerRef.current
    if (!viewer) return
    viewer.resetCameraPose()
    viewer.zoom = 0.72
  }

  function cycleBackdrop() {
    const order: SkinViewerBackdrop[] = ['none', 'soft', 'grid']
    const idx = order.indexOf(backdrop)
    setBackdrop(order[(idx + 1) % order.length]!)
  }

  const shell = (
    <div
      ref={rootRef}
      className={`skin-viewer3d skin-viewer3d--backdrop-${backdrop}${fullscreen ? ' is-fullscreen' : ''} ${className}`.trim()}
      style={{ width: effectiveWidth, height: effectiveHeight }}
    >
      <canvas ref={canvasRef} className="skin-viewer3d__canvas" />
      {showControls && (
        <div className="skin-viewer3d__controls">
          <button type="button" className="skin-viewer3d__btn" onClick={resetCamera} title="Reset camera">
            <RotateCcw size={14} />
          </button>
          <button type="button" className="skin-viewer3d__btn" onClick={cycleBackdrop} title="Backdrop">
            <ImageIcon size={14} />
          </button>
          <button
            type="button"
            className="skin-viewer3d__btn"
            onClick={() => setFullscreen((v) => !v)}
            title={fullscreen ? 'Exit fullscreen' : 'Fullscreen'}
          >
            {fullscreen ? <Minimize2 size={14} /> : <Maximize2 size={14} />}
          </button>
        </div>
      )}
    </div>
  )

  if (fullscreen) {
    return (
      <div className="skin-viewer3d__overlay" onClick={() => setFullscreen(false)}>
        <div onClick={(e) => e.stopPropagation()}>{shell}</div>
      </div>
    )
  }

  return shell
}
