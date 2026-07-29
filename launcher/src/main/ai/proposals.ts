/** Pure helpers for AI install proposals (no Electron imports). */

export type ContentKind = 'mod' | 'resourcepack' | 'shader'
export type ContentSource = 'modrinth' | 'curseforge'

export interface InstallProposal {
  projectId: string
  title: string
  source: ContentSource
  type: ContentKind
  description?: string
  downloads?: number
  iconUrl?: string
  slug?: string
}

export function maskApiKey(key: string): string {
  const trimmed = key.trim()
  if (trimmed.length < 12) {
    return '••••'
  }
  return `${trimmed.slice(0, 6)}…${trimmed.slice(-4)}`
}

/** Merge propose_install / search hit rows into unique proposals (last wins). */
export function normalizeProposals(raw: unknown[]): InstallProposal[] {
  const byKey = new Map<string, InstallProposal>()
  for (const item of raw) {
    const proposal = coerceProposal(item)
    if (!proposal) {
      continue
    }
    byKey.set(`${proposal.source}:${proposal.type}:${proposal.projectId}`, proposal)
  }
  return [...byKey.values()]
}

export function coerceProposal(item: unknown): InstallProposal | null {
  if (!item || typeof item !== 'object') {
    return null
  }
  const row = item as Record<string, unknown>
  const projectId = String(row.projectId ?? row.project_id ?? '').trim()
  const title = String(row.title ?? row.name ?? '').trim()
  if (!projectId || !title) {
    return null
  }
  const sourceRaw = String(row.source ?? 'modrinth').toLowerCase()
  const source: ContentSource = sourceRaw === 'curseforge' ? 'curseforge' : 'modrinth'
  const typeRaw = String(row.type ?? row.project_type ?? 'mod').toLowerCase()
  const type: ContentKind =
    typeRaw === 'resourcepack' || typeRaw === 'resource_pack'
      ? 'resourcepack'
      : typeRaw === 'shader'
        ? 'shader'
        : 'mod'
  return {
    projectId,
    title,
    source,
    type,
    description: row.description ? String(row.description) : undefined,
    downloads: typeof row.downloads === 'number' ? row.downloads : undefined,
    iconUrl: row.iconUrl
      ? String(row.iconUrl)
      : row.icon_url
        ? String(row.icon_url)
        : undefined,
    slug: row.slug ? String(row.slug) : undefined
  }
}

export function parseToolArguments(raw: string | undefined): Record<string, unknown> {
  if (!raw || !raw.trim()) {
    return {}
  }
  try {
    const parsed = JSON.parse(raw) as unknown
    if (parsed && typeof parsed === 'object' && !Array.isArray(parsed)) {
      return parsed as Record<string, unknown>
    }
  } catch {
    // ignore
  }
  return {}
}
