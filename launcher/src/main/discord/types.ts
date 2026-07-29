export const DISCORD_APPLICATION_ID = '1525574680994648174'

export interface DiscordPresenceButton {
  label: string
  url: string
}

export interface DiscordPresencePayload {
  details: string
  state: string
  largeImageKey?: string
  largeImageText?: string
  smallImageKey?: string
  smallImageText?: string
  startTimestamp?: number
  buttons?: DiscordPresenceButton[]
}

export function presenceFingerprint(p: DiscordPresencePayload): string {
  return [
    p.details,
    p.state,
    p.largeImageKey,
    p.smallImageKey,
    p.startTimestamp,
    p.buttons?.map((b) => `${b.label}:${b.url}`).join('|') ?? ''
  ].join('\0')
}
