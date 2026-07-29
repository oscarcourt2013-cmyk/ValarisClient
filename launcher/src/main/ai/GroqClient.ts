import { DEFAULT_API_BASE } from '../services/SocialService'

export type ChatRole = 'system' | 'user' | 'assistant' | 'tool'

export interface ChatMessage {
  role: ChatRole
  content: string | null
  tool_call_id?: string
  tool_calls?: ToolCall[]
  name?: string
}

export interface ToolCall {
  id: string
  type: 'function'
  function: {
    name: string
    arguments: string
  }
}

export interface ToolDefinition {
  type: 'function'
  function: {
    name: string
    description: string
    parameters: Record<string, unknown>
  }
}

export interface GroqChatResult {
  ok: boolean
  message?: ChatMessage
  error?: string
}

function apiBase(): string {
  return (process.env.PRIME_API_BASE || DEFAULT_API_BASE).replace(/\/$/, '')
}

/** Chat via Prime backend proxy — Groq key never leaves the server. */
export async function groqChat(options: {
  model: string
  messages: ChatMessage[]
  tools?: ToolDefinition[]
  temperature?: number
  maxTokens?: number
  /** Optional local override (self-host / offline). Prefer proxy. */
  apiKey?: string
}): Promise<GroqChatResult> {
  const { model, messages, tools, temperature = 0.3, maxTokens = 1200, apiKey } = options

  // Local key override (dev / self-host) — otherwise use shared backend proxy.
  if (apiKey?.trim()) {
    return directGroq({ apiKey: apiKey.trim(), model, messages, tools, temperature, maxTokens })
  }

  try {
    const body: Record<string, unknown> = {
      model,
      messages,
      temperature,
      max_tokens: maxTokens
    }
    if (tools && tools.length > 0) {
      body.tools = tools
      body.tool_choice = 'auto'
    }

    const res = await fetch(`${apiBase()}/v1/ai/chat`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(body)
    })
    const raw = await res.text()
    if (!res.ok) {
      return { ok: false, error: extractError(raw) || `HTTP ${res.status}` }
    }
    const json = JSON.parse(raw) as { message?: ChatMessage; error?: string }
    if (!json.message) {
      return { ok: false, error: json.error || 'Empty AI response' }
    }
    return { ok: true, message: json.message }
  } catch (err) {
    return {
      ok: false,
      error: err instanceof Error ? err.message : 'Network error'
    }
  }
}

export async function aiProxyAvailable(): Promise<boolean> {
  try {
    const res = await fetch(`${apiBase()}/v1/ai/status`, { method: 'GET' })
    if (!res.ok) return false
    const json = (await res.json()) as { available?: boolean }
    return Boolean(json.available)
  } catch {
    return false
  }
}

async function directGroq(options: {
  apiKey: string
  model: string
  messages: ChatMessage[]
  tools?: ToolDefinition[]
  temperature: number
  maxTokens: number
}): Promise<GroqChatResult> {
  const body: Record<string, unknown> = {
    model: options.model,
    messages: options.messages,
    temperature: options.temperature,
    max_tokens: options.maxTokens
  }
  if (options.tools && options.tools.length > 0) {
    body.tools = options.tools
    body.tool_choice = 'auto'
  }
  try {
    const res = await fetch('https://api.groq.com/openai/v1/chat/completions', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${options.apiKey}`
      },
      body: JSON.stringify(body)
    })
    const raw = await res.text()
    if (!res.ok) {
      return { ok: false, error: extractError(raw) || `HTTP ${res.status}` }
    }
    const json = JSON.parse(raw) as { choices?: Array<{ message?: ChatMessage }> }
    const message = json.choices?.[0]?.message
    if (!message) {
      return { ok: false, error: 'Empty Groq response' }
    }
    return { ok: true, message }
  } catch (err) {
    return {
      ok: false,
      error: err instanceof Error ? err.message : 'Network error'
    }
  }
}

function extractError(raw: string): string {
  try {
    const json = JSON.parse(raw) as { error?: string | { message?: string } }
    if (typeof json.error === 'string') return json.error
    return json.error?.message?.trim() ?? ''
  } catch {
    return raw.length > 180 ? `${raw.slice(0, 180)}…` : raw
  }
}
