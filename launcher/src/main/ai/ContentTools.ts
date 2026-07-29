import { contentService } from '../services/ContentService'
import { instanceService } from '../services/InstanceService'
import { profileService } from '../services/ProfileService'
import { settingsStore } from '../storage/SettingsStore'
import type { ToolDefinition } from './GroqClient'
import {
  coerceProposal,
  normalizeProposals,
  parseToolArguments,
  type ContentKind,
  type ContentSource,
  type InstallProposal
} from './proposals'
import {
  diagnoseInstance,
  listCrashReports,
  readCrashReportPayload,
  readLatestLogPayload,
  readLauncherLogPayload
} from './DiagnosticTools'

export const AI_TOOL_DEFINITIONS: ToolDefinition[] = [
  {
    type: 'function',
    function: {
      name: 'get_instance_context',
      description: 'Get the active Minecraft instance name, version, and loader.',
      parameters: {
        type: 'object',
        properties: {
          instanceId: { type: 'string', description: 'Optional instance id override' }
        }
      }
    }
  },
  {
    type: 'function',
    function: {
      name: 'list_installed_mods',
      description: 'List mods already installed on the active instance.',
      parameters: {
        type: 'object',
        properties: {
          instanceId: { type: 'string' }
        }
      }
    }
  },
  {
    type: 'function',
    function: {
      name: 'diagnose_instance',
      description:
        'Full troubleshooting bundle: newest crash report (parsed), latest.log errors, launcher log, and mod list. Use when the user reports a crash, freeze, or launch failure.',
      parameters: {
        type: 'object',
        properties: {
          instanceId: { type: 'string' }
        }
      }
    }
  },
  {
    type: 'function',
    function: {
      name: 'list_crash_reports',
      description: 'List recent Minecraft crash-reports for the instance (newest first).',
      parameters: {
        type: 'object',
        properties: {
          instanceId: { type: 'string' },
          limit: { type: 'number' }
        }
      }
    }
  },
  {
    type: 'function',
    function: {
      name: 'read_crash_report',
      description:
        'Read and parse a crash report file. Omit fileName to use the newest. Returns structured fields + excerpt.',
      parameters: {
        type: 'object',
        properties: {
          instanceId: { type: 'string' },
          fileName: { type: 'string', description: 'e.g. crash-2026-07-26_15.00.00-client.txt' }
        }
      }
    }
  },
  {
    type: 'function',
    function: {
      name: 'read_latest_log',
      description:
        'Read logs/latest.log: ERROR/FATAL/Exception lines plus a tail excerpt. Use for non-crash bugs and mixin errors.',
      parameters: {
        type: 'object',
        properties: {
          instanceId: { type: 'string' },
          errorsOnly: { type: 'boolean' }
        }
      }
    }
  },
  {
    type: 'function',
    function: {
      name: 'read_launcher_log',
      description: 'Read recent StellarClient Launcher launch log lines (download/fabric/mod install errors).',
      parameters: {
        type: 'object',
        properties: {
          limit: { type: 'number' }
        }
      }
    }
  },
  {
    type: 'function',
    function: {
      name: 'search_mods',
      description:
        'Search Modrinth (default) or CurseForge for Fabric mods compatible with the instance MC version. Prefer Modrinth.',
      parameters: {
        type: 'object',
        properties: {
          query: { type: 'string', description: 'Search query, e.g. Sodium' },
          source: {
            type: 'string',
            enum: ['modrinth', 'curseforge', 'auto'],
            description: 'auto = Modrinth first, CurseForge only if needed'
          },
          instanceId: { type: 'string' }
        },
        required: ['query']
      }
    }
  },
  {
    type: 'function',
    function: {
      name: 'search_content',
      description: 'Search resource packs or shaders on Modrinth/CurseForge.',
      parameters: {
        type: 'object',
        properties: {
          query: { type: 'string' },
          type: { type: 'string', enum: ['resourcepack', 'shader'] },
          source: { type: 'string', enum: ['modrinth', 'curseforge', 'auto'] },
          instanceId: { type: 'string' }
        },
        required: ['query', 'type']
      }
    }
  },
  {
    type: 'function',
    function: {
      name: 'propose_install',
      description:
        'Propose one or more content installs for the user to confirm in the UI. Does NOT download.',
      parameters: {
        type: 'object',
        properties: {
          items: {
            type: 'array',
            items: {
              type: 'object',
              properties: {
                projectId: { type: 'string' },
                title: { type: 'string' },
                source: { type: 'string', enum: ['modrinth', 'curseforge'] },
                type: { type: 'string', enum: ['mod', 'resourcepack', 'shader'] },
                description: { type: 'string' },
                downloads: { type: 'number' },
                iconUrl: { type: 'string' },
                slug: { type: 'string' }
              },
              required: ['projectId', 'title', 'source', 'type']
            }
          }
        },
        required: ['items']
      }
    }
  }
]

export interface ToolExecResult {
  payload: unknown
  proposals: InstallProposal[]
}

async function resolveInstanceId(instanceId?: string): Promise<string> {
  if (instanceId?.trim()) {
    return instanceId.trim()
  }
  const profile = await profileService.getActiveProfile()
  if (profile.instanceId) {
    const inst = await instanceService.getStoredById(profile.instanceId)
    if (inst) {
      return profile.instanceId
    }
  }
  const fallback = await instanceService.getDefault()
  if (!fallback) {
    throw new Error('No instance available.')
  }
  return fallback.id
}

async function hasCurseForgeKey(): Promise<boolean> {
  const settings = await settingsStore.load()
  return Boolean(settings.curseForgeApiKey?.trim())
}

function mapHits(
  hits: Array<{
    project_id: string
    title: string
    description: string
    downloads: number
    icon_url?: string
    slug: string
    project_type: string
  }>,
  source: ContentSource,
  type: ContentKind
): InstallProposal[] {
  return hits.slice(0, 8).map((hit) => ({
    projectId: hit.project_id,
    title: hit.title,
    source,
    type,
    description: hit.description,
    downloads: hit.downloads,
    iconUrl: hit.icon_url,
    slug: hit.slug
  }))
}

export async function executeAiTool(
  name: string,
  argsJson: string | undefined,
  defaultInstanceId?: string
): Promise<ToolExecResult> {
  const args = parseToolArguments(argsJson)
  const instanceId =
    typeof args.instanceId === 'string' && args.instanceId.trim()
      ? args.instanceId.trim()
      : defaultInstanceId

  switch (name) {
    case 'get_instance_context': {
      const id = await resolveInstanceId(instanceId)
      const stored = await instanceService.getStoredById(id)
      if (!stored) {
        return { payload: { error: 'Instance not found' }, proposals: [] }
      }
      return {
        payload: {
          instanceId: stored.id,
          name: stored.name,
          minecraftVersion: stored.minecraftVersion,
          loader: stored.loader,
          curseForgeAvailable: await hasCurseForgeKey()
        },
        proposals: []
      }
    }
    case 'list_installed_mods': {
      const mods = await contentService.listMods(instanceId)
      return {
        payload: {
          count: mods.length,
          mods: mods.map((m) => ({
            name: m.name,
            fileName: m.fileName,
            enabled: m.enabled,
            source: m.source,
            version: m.version
          }))
        },
        proposals: []
      }
    }
    case 'diagnose_instance': {
      const id = await resolveInstanceId(instanceId)
      return { payload: await diagnoseInstance(id), proposals: [] }
    }
    case 'list_crash_reports': {
      const id = await resolveInstanceId(instanceId)
      const limit = typeof args.limit === 'number' ? args.limit : 8
      const items = await listCrashReports(id, limit)
      return {
        payload: {
          count: items.length,
          reports: items.map((c) => ({
            fileName: c.fileName,
            mtime: new Date(c.mtimeMs).toISOString(),
            size: c.size
          }))
        },
        proposals: []
      }
    }
    case 'read_crash_report': {
      const id = await resolveInstanceId(instanceId)
      const fileName = typeof args.fileName === 'string' ? args.fileName : undefined
      return { payload: await readCrashReportPayload(id, fileName), proposals: [] }
    }
    case 'read_latest_log': {
      const id = await resolveInstanceId(instanceId)
      const errorsOnly = args.errorsOnly !== false
      return { payload: await readLatestLogPayload(id, { errorsOnly }), proposals: [] }
    }
    case 'read_launcher_log': {
      const limit = typeof args.limit === 'number' ? args.limit : 120
      return { payload: await readLauncherLogPayload(limit), proposals: [] }
    }
    case 'search_mods': {
      return searchContent('mod', args, instanceId)
    }
    case 'search_content': {
      const typeRaw = String(args.type ?? 'resourcepack')
      const type: ContentKind = typeRaw === 'shader' ? 'shader' : 'resourcepack'
      return searchContent(type, args, instanceId)
    }
    case 'propose_install': {
      const items = Array.isArray(args.items) ? args.items : []
      const proposals = normalizeProposals(items)
      return {
        payload: {
          proposed: proposals.length,
          items: proposals,
          note: 'Waiting for user confirmation in the launcher UI'
        },
        proposals
      }
    }
    default:
      return { payload: { error: `Unknown tool: ${name}` }, proposals: [] }
  }
}

async function searchContent(
  type: ContentKind,
  args: Record<string, unknown>,
  instanceId?: string
): Promise<ToolExecResult> {
  const query = String(args.query ?? '').trim()
  if (!query) {
    return { payload: { error: 'Empty query' }, proposals: [] }
  }
  const sourcePref = String(args.source ?? 'auto').toLowerCase()
  const cfOk = await hasCurseForgeKey()

  const wantCf = sourcePref === 'curseforge'
  const wantMr = sourcePref === 'modrinth' || sourcePref === 'auto' || !wantCf

  const results: Array<Record<string, unknown>> = []
  let proposals: InstallProposal[] = []

  if (wantMr && !(wantCf && sourcePref === 'curseforge')) {
    try {
      const hits = await contentService.searchModrinth(query, type, instanceId)
      const mapped = mapHits(hits, 'modrinth', type)
      proposals = proposals.concat(mapped)
      results.push({ source: 'modrinth', hits: mapped })
    } catch (err) {
      results.push({
        source: 'modrinth',
        error: err instanceof Error ? err.message : 'search failed'
      })
    }
  }

  if (wantCf || (sourcePref === 'auto' && proposals.length === 0)) {
    if (!cfOk) {
      results.push({
        source: 'curseforge',
        error: 'CurseForge API key missing in launcher settings'
      })
    } else {
      try {
        const hits = await contentService.searchCurseForge(query, type, instanceId)
        const mapped = mapHits(hits, 'curseforge', type)
        proposals = proposals.concat(mapped)
        results.push({ source: 'curseforge', hits: mapped })
      } catch (err) {
        results.push({
          source: 'curseforge',
          error: err instanceof Error ? err.message : 'search failed'
        })
      }
    }
  }

  // Best single match as soft proposal for the UI (user still confirms).
  const top = proposals[0] ? [coerceProposal(proposals[0])].filter(Boolean) : []

  return {
    payload: { query, type, results },
    proposals: top as InstallProposal[]
  }
}
