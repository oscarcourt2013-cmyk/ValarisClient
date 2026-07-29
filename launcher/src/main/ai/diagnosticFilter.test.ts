import assert from 'node:assert/strict'
import { describe, it } from 'node:test'

/** Mirrors extractErrorishLines logic for unit coverage without Electron. */
function extractErrorishLines(content: string, maxLines = 80): string[] {
  const lines = content.split(/\r?\n/)
  const interesting: string[] = []
  const re =
    /(ERROR|FATAL|Exception|Caused by:|Mixin apply|Incompatible|NoClassDefFound|NoSuchMethod|OutOfMemory|FAILED|\bcrash\b)/i
  for (const line of lines) {
    if (re.test(line)) {
      interesting.push(line.slice(0, 400))
      if (interesting.length >= maxLines) break
    }
  }
  return interesting
}

describe('diagnostic log filter', () => {
  it('keeps error and exception lines', () => {
    const sample = [
      '[12:00:00] [main/INFO]: Loading',
      '[12:00:01] [main/ERROR]: Mixin apply failed',
      'java.lang.NullPointerException: Cannot invoke',
      'Caused by: java.lang.RuntimeException: boom',
      '[12:00:02] [main/INFO]: Done'
    ].join('\n')
    const lines = extractErrorishLines(sample)
    assert.equal(lines.length, 3)
    assert.ok(lines[0]?.includes('ERROR'))
    assert.ok(lines[1]?.includes('NullPointerException'))
  })
})
