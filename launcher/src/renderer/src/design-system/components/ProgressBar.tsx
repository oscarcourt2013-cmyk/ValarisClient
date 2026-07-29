import './ProgressBar.css'

interface ProgressBarProps {
  value: number
  large?: boolean
}

export function ProgressBar({ value, large }: ProgressBarProps) {
  const clamped = Math.max(0, Math.min(100, value))
  return (
    <div className={`prime-progress${large ? ' prime-progress--lg' : ''}`}>
      <div className="prime-progress__bar" style={{ width: `${clamped}%` }} />
    </div>
  )
}
