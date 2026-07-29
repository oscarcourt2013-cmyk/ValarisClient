import './Toggle.css'

interface ToggleProps {
  checked: boolean
  onChange: (checked: boolean) => void
  label?: string
}

export function Toggle({ checked, onChange, label }: ToggleProps) {
  return (
    <button
      type="button"
      role="switch"
      aria-checked={checked}
      aria-label={label}
      className={`prime-toggle${checked ? ' prime-toggle--on' : ''}`}
      onClick={() => onChange(!checked)}
    >
      <span className="prime-toggle__knob" />
    </button>
  )
}
