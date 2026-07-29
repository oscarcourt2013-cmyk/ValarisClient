import { GITHUB_REPO_SLUG } from './github'

export interface GitHubReleaseAsset {
  name: string
  browser_download_url: string
  size: number
}

export interface GitHubRelease {
  tag_name: string
  html_url: string
  body?: string
  assets?: GitHubReleaseAsset[]
}

const GITHUB_API_HEADERS = {
  Accept: 'application/vnd.github+json',
  'User-Agent': 'stellar-client-launcher'
} as const

export function isPrimeModJarAsset(name: string, prefix: string): boolean {
  return (
    name.startsWith(prefix) &&
    name.endsWith('.jar') &&
    !name.includes('-sources') &&
    !name.includes('-dev')
  )
}

export function pickPrimeModAsset(release: GitHubRelease, prefix: string): GitHubReleaseAsset | undefined {
  const assets = release.assets ?? []
  return assets
    .filter((a) => isPrimeModJarAsset(a.name, prefix))
    .sort((a, b) => a.name.localeCompare(b.name))
    .at(-1)
}

export async function fetchLatestGitHubRelease(): Promise<GitHubRelease | null> {
  const response = await fetch(`https://api.github.com/repos/${GITHUB_REPO_SLUG}/releases/latest`, {
    headers: GITHUB_API_HEADERS
  })
  if (!response.ok) {
    return null
  }
  return (await response.json()) as GitHubRelease
}

export function compareSemver(a: string, b: string): number {
  const clean = (v: string) => v.replace(/^v/i, '')
  const pa = clean(a).split('.').map(Number)
  const pb = clean(b).split('.').map(Number)
  for (let i = 0; i < 3; i++) {
    const diff = (pa[i] ?? 0) - (pb[i] ?? 0)
    if (diff !== 0) {
      return diff
    }
  }
  return 0
}

/** Expected asset name: stellar-client-launcher-Setup-0.9.7.exe */
export function parseLauncherVersionFromAsset(name: string): string | null {
  const match = name.match(/stellar-client-launcher-Setup-(\d+\.\d+\.\d+)/i)
  return match?.[1] ?? null
}

/** Expected asset name: StellarClient-<target>-1.2.31.jar */
export function parseModVersionFromAsset(name: string, prefix?: string): string | null {
  if (prefix) {
    const escaped = prefix.replace(/\./g, '\\.')
    const match = name.match(new RegExp(`^${escaped}-(\\d+\\.\\d+\\.\\d+)\\.jar$`))
    return match?.[1] ?? null
  }
  const match = name.match(/^StellarClient-[^-]+(?:\.[^-]+)*-(\d+\.\d+\.\d+)\.jar$/)
  return match?.[1] ?? null
}

export function pickWindowsLauncherAsset(release: GitHubRelease): GitHubReleaseAsset | undefined {
  const assets = release.assets ?? []
  return (
    assets.find((a) => /stellar-client-launcher-Setup-.*\.exe$/i.test(a.name)) ??
    assets.find((a) => /setup.*\.exe$/i.test(a.name)) ??
    assets.find((a) => /\.exe$/i.test(a.name))
  )
}
