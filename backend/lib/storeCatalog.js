/**
 * Static store catalog — mirrors launcher ecosystem-catalog categories.
 * Cloud sync is optional; local launcher remains source of truth when offline.
 */

const STORE_CATALOG = [
  {
    id: 'cape-prime',
    name: 'Prime Cape',
    description: 'Official Prime Client cape — visible to Prime peers.',
    price: 0,
    category: 'cosmetic',
  },
  {
    id: 'cape-star',
    name: 'Star Cape',
    description: 'Gold star cape for Prime peers.',
    price: 200,
    category: 'cosmetic',
  },
  {
    id: 'cape-crimson',
    name: 'Crimson Cape',
    description: 'Signature crimson cape.',
    price: 250,
    category: 'cosmetic',
  },
  {
    id: 'cape-midnight',
    name: 'Midnight Cape',
    description: 'Indigo midnight cape.',
    price: 200,
    category: 'cosmetic',
  },
  {
    id: 'wings-aurora',
    name: 'Aurora Wings',
    description: 'Animated aurora wings — visible to Prime peers.',
    price: 0,
    category: 'cosmetic',
  },
  {
    id: 'wings-ember',
    name: 'Ember Wings',
    description: 'Animated fiery wings.',
    price: 400,
    category: 'cosmetic',
  },
  {
    id: 'theme-crimson',
    name: 'Crimson Theme',
    description: 'Signature red Prime theme.',
    price: 0,
    category: 'theme',
  },
  {
    id: 'theme-midnight',
    name: 'Midnight Theme',
    description: 'Cool indigo theme.',
    price: 0,
    category: 'theme',
  },
  {
    id: 'theme-aurora',
    name: 'Aurora Theme',
    description: 'Cyan aurora theme.',
    price: 0,
    category: 'theme',
  },
  {
    id: 'theme-obsidian',
    name: 'Obsidian Theme',
    description: 'Signature black & champagne gold — elevated Prime look.',
    price: 0,
    category: 'theme',
  },
  {
    id: 'theme-ember',
    name: 'Ember Theme',
    description: 'Copper glow on deep charcoal — elevated Prime look.',
    price: 0,
    category: 'theme',
  },
  {
    id: 'bg-nebula',
    name: 'Nebula Background',
    description: 'Animated space background.',
    price: 150,
    category: 'background',
  },
  {
    id: 'badge-founder',
    name: 'Founder Badge',
    description: 'Limited edition launcher profile badge.',
    price: 500,
    category: 'badge',
  },
];

const DEFAULT_OWNED = STORE_CATALOG.filter((i) => i.price === 0).map((i) => i.id);

const DEFAULT_EQUIPPED = ['cape-prime', 'wings-aurora'];

/** One-time promo codes (normalized uppercase). */
const PROMO_CODES = {
  WELCOME100: { coins: 100, label: 'Welcome bonus' },
  PRIME500: { coins: 500, label: 'Prime starter pack' },
  ELYSIA250: { coins: 250, label: 'Elysia promo' },
  FOUNDER1000: { coins: 1000, label: 'Founder gift' },
};

function getCatalogItem(itemId) {
  return STORE_CATALOG.find((i) => i.id === itemId) || null;
}

function catalogForUser(ownedIds) {
  const owned = new Set(ownedIds || []);
  return STORE_CATALOG.map((item) => ({
    ...item,
    owned: owned.has(item.id),
  }));
}

module.exports = {
  STORE_CATALOG,
  DEFAULT_OWNED,
  DEFAULT_EQUIPPED,
  PROMO_CODES,
  getCatalogItem,
  catalogForUser,
};
