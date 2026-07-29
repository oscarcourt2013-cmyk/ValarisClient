import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react'
import type { StoreItem } from '@shared/content-types'
import type { PrimeThemeId } from '@shared/ipc'
import { normalizePrimeTheme } from '@shared/theme'
import { setUiSoundsEnabled } from '@renderer/lib/uiSounds'

interface ThemeContextValue {
  refreshTheme: () => Promise<void>
}

const ThemeContext = createContext<ThemeContextValue | null>(null)

async function applyThemeFromSettings(): Promise<void> {
  const [settings, catalog, wallpaperData] = await Promise.all([
    window.primeLauncher.settings.get(),
    window.primeLauncher.store.catalog(),
    window.primeLauncher.settings.wallpaperData()
  ])

  const ownsNebula = catalog.some((item: StoreItem) => item.id === 'bg-nebula' && item.owned)

  const theme: PrimeThemeId = normalizePrimeTheme(settings.theme)
  document.documentElement.dataset.theme = theme
  document.documentElement.dataset.background =
    ownsNebula && settings.backgroundNebula ? 'nebula' : 'default'

  const root = document.documentElement
  if (settings.accentColor) {
    // Custom accent overrides brand tokens consistently (not only --prime-red-bright).
    const accent = settings.accentColor
    root.style.setProperty('--prime-accent-override', accent)
    root.style.setProperty('--prime-red', accent)
    root.style.setProperty('--prime-red-bright', accent)
    root.style.setProperty('--prime-red-glow', `color-mix(in srgb, ${accent} 45%, transparent)`)
    root.style.setProperty('--prime-red-subtle', `color-mix(in srgb, ${accent} 14%, transparent)`)
  } else {
    root.style.removeProperty('--prime-accent-override')
    root.style.removeProperty('--prime-red')
    root.style.removeProperty('--prime-red-bright')
    root.style.removeProperty('--prime-red-glow')
    root.style.removeProperty('--prime-red-subtle')
  }

  if (wallpaperData) {
    root.style.setProperty('--prime-wallpaper', `url("${wallpaperData}")`)
    root.dataset.wallpaper = 'custom'
  } else {
    root.style.removeProperty('--prime-wallpaper')
    delete root.dataset.wallpaper
  }

  setUiSoundsEnabled(settings.uiSounds !== false)
}

export function ThemeProvider({ children }: { children: ReactNode }) {
  const [, setTick] = useState(0)

  const refreshTheme = useCallback(async () => {
    await applyThemeFromSettings()
    setTick((n) => n + 1)
  }, [])

  useEffect(() => {
    void refreshTheme()
  }, [refreshTheme])

  const value = useMemo(() => ({ refreshTheme }), [refreshTheme])

  return <ThemeContext.Provider value={value}>{children}</ThemeContext.Provider>
}

export function useTheme(): ThemeContextValue {
  const ctx = useContext(ThemeContext)
  if (!ctx) {
    throw new Error('useTheme must be used within ThemeProvider')
  }
  return ctx
}
