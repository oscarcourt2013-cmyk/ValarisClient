import { createHash, randomUUID } from 'crypto'

/** Standard Minecraft offline UUID from username. */
export function offlineUuid(username: string): string {
  const md5 = createHash('md5').update(`OfflinePlayer:${username}`, 'utf8').digest()
  const bytes = Buffer.from(md5)
  bytes[6] = (bytes[6]! & 0x0f) | 0x30
  bytes[8] = (bytes[8]! & 0x3f) | 0x80
  const hex = bytes.toString('hex')
  return `${hex.slice(0, 8)}-${hex.slice(8, 12)}-${hex.slice(12, 16)}-${hex.slice(16, 20)}-${hex.slice(20)}`
}

export function skinUrlForUuid(uuid: string): string {
  return `https://mc-heads.net/avatar/${uuid.replace(/-/g, '')}/64`
}

export function newAccountId(): string {
  return randomUUID()
}

export function validateUsername(username: string): string | null {
  const trimmed = username.trim()
  if (trimmed.length < 3 || trimmed.length > 16) {
    return 'Username must be 3–16 characters.'
  }
  if (!/^[a-zA-Z0-9_]+$/.test(trimmed)) {
    return 'Only letters, numbers, and underscores allowed.'
  }
  return null
}
