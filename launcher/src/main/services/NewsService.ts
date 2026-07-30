import { BUNDLED_NEWS } from '../../shared/ecosystem-catalog'
import { GITHUB_REPO_SLUG } from '../../shared/github'
import type { NewsItem } from '../../shared/types'

interface GitHubRelease {
  tag_name: string
  name: string
  body: string
  published_at: string
  prerelease: boolean
}

const NEWS_CACHE_MS = 30 * 60 * 1000
const DEFAULT_API = process.env.PRIME_API_BASE || 'http://194.9.172.102:26005'
let cachedNews: NewsItem[] | null = null
let cachedNewsAt = 0

function summarize(body: string): string {
  const line = body.split('\n').find((l) => l.trim().length > 0)
  return (line ?? 'See release notes on GitHub.').slice(0, 180)
}

function apiBase(): string {
  return (process.env.PRIME_API_BASE || DEFAULT_API).replace(/\/$/, '')
}

/** Prime API news + GitHub releases + bundled fallback (cached). */
export class NewsService {
  async getNews(): Promise<NewsItem[]> {
    if (cachedNews && Date.now() - cachedNewsAt < NEWS_CACHE_MS) {
      return cachedNews
    }

    const byId = new Map<string, NewsItem>()
    for (const item of BUNDLED_NEWS) {
      byId.set(item.id, item)
    }

    try {
      const res = await fetch(`${apiBase()}/v1/news`, {
        headers: { Accept: 'application/json', 'User-Agent': 'valeris-client-launcher' }
      })
      if (res.ok) {
        const data = (await res.json()) as { items?: NewsItem[] }
        for (const item of data.items ?? []) {
          if (item?.id && item.title) {
            byId.set(item.id, item)
          }
        }
      }
    } catch {
      // offline — continue
    }

    try {
      const response = await fetch(`https://api.github.com/repos/${GITHUB_REPO_SLUG}/releases?per_page=3`, {
        headers: { Accept: 'application/vnd.github+json', 'User-Agent': 'valeris-client-launcher' }
      })
      if (response.ok) {
        const releases = (await response.json()) as GitHubRelease[]
        for (const release of releases.filter((r) => !r.prerelease).slice(0, 2)) {
          const id = `gh-${release.tag_name}`
          byId.set(id, {
            id,
            title: release.name || release.tag_name,
            summary: summarize(release.body ?? ''),
            date: release.published_at.slice(0, 10),
            tag: 'update'
          })
        }
      }
    } catch {
      // offline
    }

    cachedNews = [...byId.values()].sort((a, b) => b.date.localeCompare(a.date))
    cachedNewsAt = Date.now()
    return cachedNews
  }
}

export const newsService = new NewsService()
