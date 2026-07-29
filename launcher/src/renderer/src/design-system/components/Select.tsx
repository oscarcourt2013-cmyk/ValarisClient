import { useEffect, useId, useRef, useState } from 'react'
import { Check, ChevronDown } from 'lucide-react'
import './Select.css'

export interface SelectOption {
  value: string
  label: string
  disabled?: boolean
}

interface SelectProps {
  value: string
  options: SelectOption[]
  onChange: (value: string) => void
  disabled?: boolean
  placeholder?: string
  className?: string
  /** Compact width for settings rows */
  size?: 'sm' | 'md'
  'aria-label'?: string
}

export function Select({
  value,
  options,
  onChange,
  disabled = false,
  placeholder = 'Select…',
  className = '',
  size = 'md',
  'aria-label': ariaLabel
}: SelectProps) {
  const [open, setOpen] = useState(false)
  const rootRef = useRef<HTMLDivElement>(null)
  const listId = useId()

  const selected = options.find((o) => o.value === value)
  const label = selected?.label ?? placeholder

  useEffect(() => {
    if (!open) return
    function onPointerDown(e: PointerEvent) {
      if (rootRef.current && !rootRef.current.contains(e.target as Node)) {
        setOpen(false)
      }
    }
    function onKey(e: KeyboardEvent) {
      if (e.key === 'Escape') setOpen(false)
    }
    document.addEventListener('pointerdown', onPointerDown)
    document.addEventListener('keydown', onKey)
    return () => {
      document.removeEventListener('pointerdown', onPointerDown)
      document.removeEventListener('keydown', onKey)
    }
  }, [open])

  function pick(next: string) {
    if (disabled) return
    onChange(next)
    setOpen(false)
  }

  return (
    <div
      ref={rootRef}
      className={`prime-select prime-select--${size}${open ? ' is-open' : ''} ${className}`.trim()}
    >
      <button
        type="button"
        className="prime-select__trigger"
        disabled={disabled}
        aria-haspopup="listbox"
        aria-expanded={open}
        aria-controls={listId}
        aria-label={ariaLabel}
        onClick={() => !disabled && setOpen((v) => !v)}
      >
        <span className={`prime-select__value${!selected ? ' is-placeholder' : ''}`}>{label}</span>
        <ChevronDown size={14} className="prime-select__chevron" />
      </button>

      {open && (
        <ul id={listId} className="prime-select__menu" role="listbox">
          {options.map((opt) => {
            const isActive = opt.value === value
            return (
              <li key={opt.value} role="option" aria-selected={isActive}>
                <button
                  type="button"
                  className={`prime-select__option${isActive ? ' is-active' : ''}`}
                  disabled={opt.disabled}
                  onClick={() => pick(opt.value)}
                >
                  <span>{opt.label}</span>
                  {isActive && <Check size={14} className="prime-select__check" />}
                </button>
              </li>
            )
          })}
        </ul>
      )}
    </div>
  )
}
