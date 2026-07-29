/**
 * Groq proxy — API key stays on the server (GROQ_API_KEY).
 * Clients call POST /v1/ai/chat ; the key is never returned.
 */
const ALLOWED_MODELS = new Set([
  'llama-3.1-8b-instant',
  'llama-3.3-70b-versatile',
  'openai/gpt-oss-20b',
  'openai/gpt-oss-120b',
]);

const DEFAULT_MODEL = 'llama-3.3-70b-versatile';
const GROQ_URL = 'https://api.groq.com/openai/v1/chat/completions';

function groqApiKey() {
  return (process.env.GROQ_API_KEY || '').trim();
}

function isAvailable() {
  return Boolean(groqApiKey());
}

/**
 * @param {object} body client payload
 * @returns {Promise<{ ok: boolean, message?: object, error?: string, status?: number }>}
 */
async function proxyChat(body) {
  const key = groqApiKey();
  if (!key) {
    return { ok: false, error: 'AI unavailable (server key not configured)', status: 503 };
  }

  const modelRaw = String(body?.model || DEFAULT_MODEL).trim();
  const model = ALLOWED_MODELS.has(modelRaw) ? modelRaw : DEFAULT_MODEL;
  const messages = Array.isArray(body?.messages) ? body.messages : null;
  if (!messages || messages.length === 0) {
    return { ok: false, error: 'messages required', status: 400 };
  }
  if (messages.length > 40) {
    return { ok: false, error: 'Too many messages', status: 400 };
  }

  const temperature =
    typeof body.temperature === 'number' ? Math.min(1.5, Math.max(0, body.temperature)) : 0.3;
  const maxTokens = Math.min(
    2048,
    Math.max(64, Number(body.max_tokens ?? body.maxTokens ?? 1200) || 1200)
  );

  const payload = {
    model,
    messages,
    temperature,
    max_tokens: maxTokens,
  };
  if (Array.isArray(body.tools) && body.tools.length > 0) {
    if (body.tools.length > 20) {
      return { ok: false, error: 'Too many tools', status: 400 };
    }
    payload.tools = body.tools;
    payload.tool_choice = body.tool_choice || 'auto';
  }

  try {
    const res = await fetch(GROQ_URL, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${key}`,
      },
      body: JSON.stringify(payload),
    });
    const raw = await res.text();
    if (!res.ok) {
      let detail = `HTTP ${res.status}`;
      try {
        const errJson = JSON.parse(raw);
        if (errJson?.error?.message) detail = errJson.error.message;
      } catch {
        /* ignore */
      }
      return { ok: false, error: detail, status: res.status >= 500 ? 502 : res.status };
    }
    const json = JSON.parse(raw);
    const message = json?.choices?.[0]?.message;
    if (!message) {
      return { ok: false, error: 'Empty Groq response', status: 502 };
    }
    return { ok: true, message };
  } catch (err) {
    return {
      ok: false,
      error: err instanceof Error ? err.message : 'Upstream network error',
      status: 502,
    };
  }
}

module.exports = {
  isAvailable,
  proxyChat,
  DEFAULT_MODEL,
  ALLOWED_MODELS,
};
