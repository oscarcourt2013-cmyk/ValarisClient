import { en, type LocaleCatalog } from './locales/en'
import { fr } from './locales/fr'

export type Locale = 'en' | 'fr'

export const LOCALES: { id: Locale; label: string }[] = [
  { id: 'en', label: 'English' },
  { id: 'fr', label: 'Français' }
]

const catalogs: Record<Locale, LocaleCatalog> = { en, fr }

export function translate(locale: Locale, key: string, params?: Record<string, string | number>): string {
  const parts = key.split('.')
  let node: unknown = catalogs[locale]

  for (const part of parts) {
    if (node && typeof node === 'object' && part in node) {
      node = (node as Record<string, unknown>)[part]
    } else {
      return key
    }
  }

  if (typeof node !== 'string') {
    return key
  }

  if (!params) {
    return node
  }

  return node.replace(/\{(\w+)\}/g, (_, name: string) => String(params[name] ?? `{${name}}`))
}

export function isLocale(value: string): value is Locale {
  return value === 'en' || value === 'fr'
}
