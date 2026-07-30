import type { CosmeticItem, PerformancePresetInfo, StoreItem } from './content-types'
import type { NewsItem } from './types'

/** Static store catalog — purchases unlock locally (Prime Coins, no payment server). */
export const STORE_CATALOG: StoreItem[] = [
  { id: 'cape-prime', name: 'Valeris Cape', description: 'Official ValerisClient cape — visible to Valeris peers.', price: 0, category: 'cosmetic', owned: true },
  { id: 'cape-star', name: 'Star Cape', description: 'Gold star cape for Valeris peers.', price: 200, category: 'cosmetic', owned: false },
  { id: 'cape-crimson', name: 'Crimson Cape', description: 'Signature crimson cape.', price: 250, category: 'cosmetic', owned: false },
  { id: 'cape-midnight', name: 'Midnight Cape', description: 'Indigo midnight cape.', price: 200, category: 'cosmetic', owned: false },
  { id: 'wings-aurora', name: 'Aurora Wings', description: 'Animated aurora wings — visible to Valeris peers.', price: 0, category: 'cosmetic', owned: true },
  { id: 'wings-ember', name: 'Ember Wings', description: 'Animated fiery wings.', price: 400, category: 'cosmetic', owned: false },
  { id: 'theme-crimson', name: 'Crimson Theme', description: 'Signature red Valeris theme.', price: 0, category: 'theme', owned: true },
  { id: 'theme-midnight', name: 'Midnight Theme', description: 'Cool indigo theme.', price: 0, category: 'theme', owned: true },
  { id: 'theme-aurora', name: 'Aurora Theme', description: 'Cyan aurora theme.', price: 0, category: 'theme', owned: true },
  {
    id: 'theme-obsidian',
    name: 'Obsidian Theme',
    description: 'Signature black & champagne gold — elevated Valeris look.',
    price: 0,
    category: 'theme',
    owned: true
  },
  {
    id: 'theme-ember',
    name: 'Ember Theme',
    description: 'Copper glow on deep charcoal — elevated Valeris look.',
    price: 0,
    category: 'theme',
    owned: true
  },
  { id: 'bg-nebula', name: 'Nebula Background', description: 'Animated space background.', price: 150, category: 'background', owned: false },
  { id: 'badge-founder', name: 'Founder Badge', description: 'Limited edition launcher profile badge.', price: 500, category: 'badge', owned: false }
]

export const COSMETIC_CATALOG: Omit<CosmeticItem, 'equipped'>[] = [
  { id: 'cape-prime', name: 'Valeris Cape', type: 'cape', rarity: 'legendary' },
  { id: 'cape-star', name: 'Star Cape', type: 'cape', rarity: 'epic' },
  { id: 'cape-crimson', name: 'Crimson Cape', type: 'cape', rarity: 'epic' },
  { id: 'cape-midnight', name: 'Midnight Cape', type: 'cape', rarity: 'rare' },
  { id: 'wings-aurora', name: 'Aurora Wings', type: 'wings', rarity: 'epic' },
  { id: 'wings-ember', name: 'Ember Wings', type: 'wings', rarity: 'legendary' },
  { id: 'badge-founder', name: 'Founder', type: 'badge', rarity: 'legendary' }
]

export const STORE_TO_COSMETIC: Record<string, string> = {
  'cape-prime': 'cape-prime',
  'cape-star': 'cape-star',
  'cape-crimson': 'cape-crimson',
  'cape-midnight': 'cape-midnight',
  'wings-aurora': 'wings-aurora',
  'wings-ember': 'wings-ember',
  'badge-founder': 'badge-founder'
}

export const BUNDLED_NEWS: NewsItem[] = [
  {
    id: 'n1',
    title: 'ValerisClient v1.1 — Premium Update',
    summary: 'New title screen, Discord RPC, onboarding wizard, and 50 modules shipped.',
    date: '2026-07-11',
    tag: 'update'
  },
  {
    id: 'n2',
    title: 'ValerisClient Launcher v0.8',
    summary: 'Local store, cosmetics, friends list, performance optimizer, and settings persistence.',
    date: '2026-07-11',
    tag: 'announcement'
  },
  {
    id: 'n3',
    title: 'Summer PvP Event (offline roster)',
    summary: 'Track scrim dates locally — add friends and notes from the Friends page.',
    date: '2026-07-08',
    tag: 'event'
  },
  {
    id: 'n4',
    title: 'Local sync only',
    summary: 'Valeris profile and configs stay on this PC. No cloud account required.',
    date: '2026-07-01',
    tag: 'announcement'
  }
]

export const PERFORMANCE_PRESETS: PerformancePresetInfo[] = [
  { id: 'low', label: 'Low PC', ramMb: 2048, renderDistance: 8, description: 'Minimum settings for weak hardware.' },
  { id: 'balanced', label: 'Balanced', ramMb: 4096, renderDistance: 12, description: 'Recommended for most players.' },
  { id: 'performance', label: 'Performance', ramMb: 6144, renderDistance: 16, description: 'High FPS competitive setup.' },
  { id: 'ultra', label: 'Ultra', ramMb: 8192, renderDistance: 24, description: 'Maximum quality for powerful PCs.' }
]

export const DEFAULT_OWNED_STORE = [
  'cape-prime',
  'wings-aurora',
  'theme-crimson',
  'theme-midnight',
  'theme-aurora',
  'theme-obsidian',
  'theme-ember'
]
export const DEFAULT_EQUIPPED_COSMETICS = ['cape-prime', 'wings-aurora']
