/** Soft UI click sounds — Web Audio oscillators, no asset files. */

let ctx: AudioContext | null = null
let enabled = true

export function setUiSoundsEnabled(value: boolean): void {
  enabled = value
}

function getCtx(): AudioContext | null {
  if (typeof window === 'undefined') return null
  if (!ctx) {
    const AC = window.AudioContext || (window as unknown as { webkitAudioContext?: typeof AudioContext }).webkitAudioContext
    if (!AC) return null
    ctx = new AC()
  }
  return ctx
}

export function playUiSound(kind: 'click' | 'success' | 'error' = 'click'): void {
  if (!enabled) return
  const audio = getCtx()
  if (!audio) return
  void audio.resume().catch(() => undefined)

  const now = audio.currentTime
  const osc = audio.createOscillator()
  const gain = audio.createGain()
  osc.connect(gain)
  gain.connect(audio.destination)

  if (kind === 'success') {
    osc.frequency.setValueAtTime(520, now)
    osc.frequency.exponentialRampToValueAtTime(780, now + 0.08)
    gain.gain.setValueAtTime(0.0001, now)
    gain.gain.exponentialRampToValueAtTime(0.045, now + 0.01)
    gain.gain.exponentialRampToValueAtTime(0.0001, now + 0.16)
    osc.start(now)
    osc.stop(now + 0.18)
    return
  }

  if (kind === 'error') {
    osc.type = 'triangle'
    osc.frequency.setValueAtTime(220, now)
    osc.frequency.exponentialRampToValueAtTime(140, now + 0.12)
    gain.gain.setValueAtTime(0.0001, now)
    gain.gain.exponentialRampToValueAtTime(0.05, now + 0.01)
    gain.gain.exponentialRampToValueAtTime(0.0001, now + 0.18)
    osc.start(now)
    osc.stop(now + 0.2)
    return
  }

  osc.type = 'sine'
  osc.frequency.setValueAtTime(640, now)
  gain.gain.setValueAtTime(0.0001, now)
  gain.gain.exponentialRampToValueAtTime(0.035, now + 0.008)
  gain.gain.exponentialRampToValueAtTime(0.0001, now + 0.07)
  osc.start(now)
  osc.stop(now + 0.08)
}
