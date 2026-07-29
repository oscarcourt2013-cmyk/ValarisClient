/** Shared theme IDs between launcher and in-game client. */
export type PrimeThemeId =
  | 'prime-crimson'
  | 'prime-midnight'
  | 'prime-aurora'
  | 'prime-obsidian'
  | 'prime-ember'

export const PRIME_THEMES: readonly PrimeThemeId[] = [
  'prime-crimson',
  'prime-midnight',
  'prime-aurora',
  'prime-obsidian',
  'prime-ember'
] as const

/** Themes with richer palettes (visual tier, not paywalled). */
export const ELEVATED_PRIME_THEMES: readonly PrimeThemeId[] = [
  'prime-obsidian',
  'prime-ember'
] as const

export function storeIdForTheme(theme: PrimeThemeId): string {
  return `theme-${theme.replace('prime-', '')}`
}

export function themeIdFromStoreId(storeId: string): PrimeThemeId | null {
  if (!storeId.startsWith('theme-')) return null
  const id = `prime-${storeId.slice('theme-'.length)}` as PrimeThemeId
  return PRIME_THEMES.includes(id) ? id : null
}

export function isElevatedTheme(id: PrimeThemeId): boolean {
  return (ELEVATED_PRIME_THEMES as readonly string[]).includes(id)
}

/** @deprecated Use isElevatedTheme — kept for older imports. */
export function isPremiumTheme(id: PrimeThemeId): boolean {
  return isElevatedTheme(id)
}

export const FREE_PRIME_THEMES = PRIME_THEMES
export const PREMIUM_PRIME_THEMES = ELEVATED_PRIME_THEMES

/** Maps legacy settings / profile values onto known themes. */
export function normalizePrimeTheme(id: string | null | undefined): PrimeThemeId {
  switch (id) {
    case 'prime-midnight':
    case 'prime-aurora':
    case 'prime-crimson':
    case 'prime-obsidian':
    case 'prime-ember':
      return id
    case 'prime-light':
      return 'prime-midnight'
    case 'prime-dark':
    default:
      return 'prime-crimson'
  }
}
