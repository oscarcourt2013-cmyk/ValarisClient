import { contentService } from './ContentService'
import { aiSecretsStore } from '../ai/AiSecretsStore'
import { aiProxyAvailable, groqChat, type ChatMessage } from '../ai/GroqClient'
import { AI_TOOL_DEFINITIONS, executeAiTool } from '../ai/ContentTools'
import { maskApiKey, normalizeProposals, type InstallProposal } from '../ai/proposals'

const MODEL = 'llama-3.3-70b-versatile'
const MAX_TOOL_ROUNDS = 6
const MAX_HISTORY = 12

export interface AiChatRequest {
  message: string
  instanceId?: string
  history?: Array<{ role: 'user' | 'assistant'; content: string }>
}

export interface AiChatResponse {
  ok: boolean
  reply: string
  proposals: InstallProposal[]
  error?: string
  /** True when AI can run (shared proxy and/or local override key). */
  hasKey: boolean
  viaProxy?: boolean
}

export interface AiKeyStatus {
  hasKey: boolean
  maskedKey: string | null
  viaProxy: boolean
}

export interface AiConfirmInstallRequest {
  projectId: string
  title: string
  source: 'modrinth' | 'curseforge'
  type: 'mod' | 'resourcepack' | 'shader'
  instanceId?: string
  versionId?: string
}

/** Launcher AI — shared backend Groq proxy + local content tools. */
export class AiAssistantService {
  async keyStatus(): Promise<AiKeyStatus> {
    const viaProxy = await aiProxyAvailable()
    const local = await aiSecretsStore.groqApiKey()
    return {
      hasKey: viaProxy || Boolean(local),
      maskedKey: local ? maskApiKey(local) : null,
      viaProxy
    }
  }

  async setKey(key: string): Promise<AiKeyStatus> {
    const trimmed = key.trim()
    if (trimmed && !trimmed.startsWith('gsk_')) {
      throw new Error('Invalid Groq key (must start with gsk_)')
    }
    await aiSecretsStore.setGroqApiKey(trimmed)
    return this.keyStatus()
  }

  async clearKey(): Promise<AiKeyStatus> {
    await aiSecretsStore.clearGroqApiKey()
    return this.keyStatus()
  }

  async chat(req: AiChatRequest): Promise<AiChatResponse> {
    const status = await this.keyStatus()
    if (!status.hasKey) {
      return {
        ok: false,
        reply: '',
        proposals: [],
        hasKey: false,
        viaProxy: false,
        error: 'AI indisponible (backend hors ligne). Réessaie plus tard.'
      }
    }

    const message = req.message?.trim()
    if (!message) {
      return {
        ok: false,
        reply: '',
        proposals: [],
        hasKey: true,
        viaProxy: status.viaProxy,
        error: 'Empty message'
      }
    }

    const localKey = status.viaProxy ? undefined : await aiSecretsStore.groqApiKey()
    const messages: ChatMessage[] = [
      { role: 'system', content: systemPrompt() },
      ...trimHistory(req.history).map((h) => ({
        role: h.role as 'user' | 'assistant',
        content: h.content
      })),
      { role: 'user', content: message }
    ]

    const collected: InstallProposal[] = []

    for (let round = 0; round < MAX_TOOL_ROUNDS; round++) {
      const result = await groqChat({
        model: MODEL,
        messages,
        tools: AI_TOOL_DEFINITIONS,
        apiKey: localKey || undefined
      })
      if (!result.ok || !result.message) {
        return {
          ok: false,
          reply: '',
          proposals: normalizeProposals(collected),
          hasKey: true,
          viaProxy: status.viaProxy,
          error: result.error ?? 'AI request failed'
        }
      }

      const assistant = result.message
      messages.push({
        role: 'assistant',
        content: assistant.content ?? null,
        tool_calls: assistant.tool_calls
      })

      const toolCalls = assistant.tool_calls ?? []
      if (toolCalls.length === 0) {
        const reply = (assistant.content ?? '').trim() || 'OK.'
        return {
          ok: true,
          reply,
          proposals: normalizeProposals(collected),
          hasKey: true,
          viaProxy: status.viaProxy
        }
      }

      for (const call of toolCalls) {
        const exec = await executeAiTool(call.function.name, call.function.arguments, req.instanceId)
        collected.push(...exec.proposals)
        messages.push({
          role: 'tool',
          tool_call_id: call.id,
          content: JSON.stringify(exec.payload)
        })
      }
    }

    return {
      ok: true,
      reply: 'J’ai trouvé des résultats — confirme les installations ci-dessous.',
      proposals: normalizeProposals(collected),
      hasKey: true,
      viaProxy: status.viaProxy
    }
  }

  async confirmInstall(req: AiConfirmInstallRequest): Promise<{ ok: boolean; error?: string; fileName?: string }> {
    const { projectId, title, source, type, instanceId, versionId } = req
    if (!projectId?.trim() || !title?.trim()) {
      return { ok: false, error: 'Invalid install request' }
    }

    if (type === 'mod') {
      return source === 'curseforge'
        ? contentService.installModFromCurseForge(projectId, title, instanceId, versionId)
        : contentService.installModFromModrinth(projectId, title, instanceId, versionId)
    }
    if (type === 'resourcepack') {
      return source === 'curseforge'
        ? contentService.installResourcePackFromCurseForge(projectId, title, instanceId, versionId)
        : contentService.installResourcePackFromModrinth(projectId, title, instanceId, versionId)
    }
    return source === 'curseforge'
      ? contentService.installShaderFromCurseForge(projectId, title, instanceId, versionId)
      : contentService.installShaderFromModrinth(projectId, title, instanceId, versionId)
  }
}

function systemPrompt(): string {
  return [
    'Tu es Prime Assistant, l’IA du launcher StellarClient (Minecraft Fabric).',
    'Tu aides à: installer des mods (Modrinth/CurseForge), conseils FPS/packs, ET dépanner crashes / erreurs de lancement.',
    'Dépannage:',
    '- Si crash, freeze, écran noir, "Minecraft a crashé", ou erreur de lancement → appelle d’abord diagnose_instance.',
    '- Tu peux aussi list_crash_reports, read_crash_report, read_latest_log, read_launcher_log.',
    '- Explique la cause probable (exception, mixin, OOM, conflit de mods, Fabric) avec des étapes concrètes.',
    '- Cite le fichier crash / lignes ERROR utiles. Propose désactiver un mod précis si identifié.',
    '- Ne invente pas de stack traces absentes des tools.',
    'Install:',
    '- Utilise search_mods / propose_install ; l’utilisateur confirme dans l’UI.',
    '- Prefère Modrinth. Réponds en français, concis et actionnable.'
  ].join('\n')
}

function trimHistory(
  history: AiChatRequest['history']
): Array<{ role: 'user' | 'assistant'; content: string }> {
  if (!history?.length) {
    return []
  }
  return history.slice(-MAX_HISTORY).filter((h) => h.content?.trim())
}

export const aiAssistantService = new AiAssistantService()
