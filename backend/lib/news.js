/**
 * Bundled + editable news / changelog for GET /v1/news and /v1/versions.
 */
const fs = require('fs');
const path = require('path');

const NEWS_PATH = process.env.PRIME_NEWS_PATH || path.join(__dirname, '..', 'data', 'news.json');

const DEFAULT_NEWS = [
  {
    id: 'prime-platform',
    title: 'Prime Platform',
    summary: 'Launcher, game, and friends — one ecosystem. Presence syncs across launcher and in-game.',
    date: new Date().toISOString().slice(0, 10),
    tag: 'launcher',
  },
  {
    id: 'account-switch',
    title: 'Switch accounts in-game',
    summary: 'Change Minecraft account from the title menu without relaunching.',
    date: new Date().toISOString().slice(0, 10),
    tag: 'mod',
  },
];

function loadNews() {
  try {
    if (fs.existsSync(NEWS_PATH)) {
      const raw = JSON.parse(fs.readFileSync(NEWS_PATH, 'utf8'));
      if (Array.isArray(raw?.items)) return raw.items;
      if (Array.isArray(raw)) return raw;
    }
  } catch {
    /* use defaults */
  }
  return DEFAULT_NEWS;
}

function versionsManifest() {
  const rootVersion = path.join(__dirname, '..', '..', 'VERSION.json');
  try {
    if (fs.existsSync(rootVersion)) {
      return JSON.parse(fs.readFileSync(rootVersion, 'utf8'));
    }
  } catch {
    /* fallback */
  }
  return {
    mod: '1.2.57',
    launcher: '0.9.12',
    backend: '1.1.0',
    apiBase: process.env.PRIME_PUBLIC_URL || 'http://194.9.172.102:26005',
  };
}

module.exports = { loadNews, versionsManifest, DEFAULT_NEWS };
