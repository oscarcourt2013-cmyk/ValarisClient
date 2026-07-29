import './Skeleton.css'

interface SkeletonProps {
  width?: string | number
  height?: string | number
  radius?: number
  className?: string
}

export function Skeleton({ width = '100%', height = 16, radius = 8, className = '' }: SkeletonProps) {
  return (
    <div
      className={`skeleton ${className}`.trim()}
      style={{ width, height, borderRadius: radius }}
      aria-hidden
    />
  )
}

export function LaunchStrip({
  label,
  percent,
  phase,
  running
}: {
  label: string
  percent?: number
  phase?: string
  running?: boolean
}) {
  const pct = running
    ? 100
    : typeof percent === 'number'
      ? Math.max(0, Math.min(100, percent))
      : null
  return (
    <div
      className={`launch-strip${phase === 'crashed' || phase === 'error' ? ' is-error' : ''}${running ? ' is-running' : ''}`}
    >
      <div className="launch-strip__row">
        <span className="launch-strip__dot" aria-hidden />
        <span className="launch-strip__label">{label}</span>
        {pct !== null && !running && <span className="launch-strip__pct">{pct}%</span>}
      </div>
      <div className="launch-strip__track">
        <div
          className="launch-strip__fill"
          style={{ width: pct !== null ? `${pct}%` : '35%' }}
          data-indeterminate={pct === null ? 'true' : undefined}
        />
      </div>
    </div>
  )
}
