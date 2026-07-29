import { protocol, net } from 'electron'
import { pathToFileURL } from 'url'
import { normalize, resolve } from 'path'
import { app } from 'electron'
import { getInstanceGameDir } from '../minecraft/paths'

const ALLOWED_ROOTS: string[] = []

export function registerMediaScheme(): void {
  protocol.registerSchemesAsPrivileged([
    {
      scheme: 'prime-media',
      privileges: {
        secure: true,
        supportFetchAPI: true,
        bypassCSP: true,
        stream: true,
        standard: true
      }
    }
  ])
}

function registerAllowedRoots(): void {
  ALLOWED_ROOTS.length = 0
  ALLOWED_ROOTS.push(app.getPath('userData'))
}

function isAllowedPath(filePath: string): boolean {
  const normalized = normalize(resolve(filePath))
  return ALLOWED_ROOTS.some((root) => normalized.startsWith(normalize(resolve(root))))
}

export function registerMediaProtocol(): void {
  registerAllowedRoots()

  protocol.handle('prime-media', async (request) => {
    const url = new URL(request.url)
    const encoded = url.pathname.replace(/^\//, '')
    const filePath = decodeURIComponent(encoded)

    if (!isAllowedPath(filePath)) {
      return new Response('Forbidden', { status: 403 })
    }

    return net.fetch(pathToFileURL(filePath).href)
  })
}

/** Registers an instance game directory as a valid media root. */
export function allowInstanceMedia(instanceId: string): void {
  const gameDir = getInstanceGameDir(instanceId)
  if (!ALLOWED_ROOTS.includes(gameDir)) {
    ALLOWED_ROOTS.push(gameDir)
  }
}

export function toMediaUrl(filePath: string): string {
  return `prime-media://local/${encodeURIComponent(filePath.replace(/\\/g, '/'))}`
}
