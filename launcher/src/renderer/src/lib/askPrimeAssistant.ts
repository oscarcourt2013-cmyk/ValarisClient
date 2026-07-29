/** Cross-UI bridge: ask Prime Assistant without tight coupling. */
export const PRIME_AI_ASK_EVENT = 'prime-ai-ask'

export function askPrimeAssistant(message: string, open = true): void {
  const text = message.trim()
  if (!text) return
  window.dispatchEvent(
    new CustomEvent(PRIME_AI_ASK_EVENT, {
      detail: { message: text, open }
    })
  )
}
