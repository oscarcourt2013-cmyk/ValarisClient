import { app } from 'electron'
import { existsSync, readFileSync } from 'fs'
import { join } from 'path'

/** Shared Minecraft runtime (versions, libraries, assets). */
export function getRuntimeRoot(): string {
  return join(app.getPath('userData'), 'runtime', 'minecraft')
}

/** Default instances root under Electron userData. */
export function getDefaultInstancesRoot(): string {
  return join(app.getPath('userData'), 'instances')
}

/** Resolved instances root (custom preference or default). */
export function getInstancesRoot(): string {
  try {
    const settingsPath = join(app.getPath('userData'), 'settings.json')
    if (existsSync(settingsPath)) {
      const raw = JSON.parse(readFileSync(settingsPath, 'utf8')) as { instancesRoot?: string | null }
      const custom = raw.instancesRoot?.trim()
      if (custom) {
        return custom
      }
    }
  } catch {
    // fall through to default
  }
  return getDefaultInstancesRoot()
}

/** Per-instance game directory (saves, mods, options). */
export function getInstanceGameDir(instanceId: string): string {
  return join(getInstancesRoot(), instanceId, 'game')
}

export function getInstanceModsDir(instanceId: string): string {
  return join(getInstanceGameDir(instanceId), 'mods')
}

/** Monorepo root when developing from the parent project. */
export function getRepoRoot(): string {
  const appPath = app.getAppPath()
  const candidates = [
    process.env.PRIME_REPO_ROOT,
    join(appPath, '..', '..'),
    join(appPath, '..'),
    join(process.cwd(), '..'),
    process.cwd(),
    join(process.cwd(), '..', '..')
  ].filter((value): value is string => Boolean(value))

  for (const root of candidates) {
    if (
      existsSync(join(root, 'mc-26.2', 'build.gradle')) ||
      existsSync(join(root, 'mc-1.21.11', 'build.gradle'))
    ) {
      return root
    }
  }

  return join(appPath, '..', '..')
}
