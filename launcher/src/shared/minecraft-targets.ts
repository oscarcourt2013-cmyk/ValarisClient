/** Supported Minecraft targets for StellarClient instances. */

export interface MinecraftTarget {
  id: string
  mcVersion: string
  jarPrefix: string
  localBuildDir: string
  fabricApi: string
  fabricLoader: string
  /** Recommended Java major for this target. */
  javaMajor: number
  /** Shown as recommended in the UI. */
  recommended?: boolean
}

export const MINECRAFT_TARGETS: readonly MinecraftTarget[] = [
  {
    id: '26.2',
    mcVersion: '26.2',
    jarPrefix: 'stellar-client-26.2',
    localBuildDir: 'mc-26.2',
    fabricApi: '0.154.2+26.2',
    fabricLoader: '0.19.3',
    javaMajor: 25,
    recommended: true
  },
  {
    id: '1.21.11',
    mcVersion: '1.21.11',
    jarPrefix: 'stellar-client-1.21.11',
    localBuildDir: 'mc-1.21.11',
    fabricApi: '0.141.4+1.21.11',
    fabricLoader: '0.19.3',
    javaMajor: 21
  }
] as const

export const DEFAULT_MINECRAFT_TARGET = MINECRAFT_TARGETS[0]

/** Normalize user input and resolve a known Stellar target (falls back to recommended). */
export function resolveTarget(minecraftVersion: string | null | undefined): MinecraftTarget {
  const raw = (minecraftVersion ?? '').trim()
  if (!raw) {
    return DEFAULT_MINECRAFT_TARGET
  }
  const exact = MINECRAFT_TARGETS.find(
    (t) => t.mcVersion === raw || t.id === raw || t.localBuildDir === raw
  )
  if (exact) {
    return exact
  }
  // Soft match: "26.2.0" → 26.2, "1.21.11-rc" → 1.21.11
  const soft = MINECRAFT_TARGETS.find(
    (t) => raw === t.mcVersion || raw.startsWith(`${t.mcVersion}.`) || raw.startsWith(`${t.mcVersion}-`)
  )
  return soft ?? DEFAULT_MINECRAFT_TARGET
}

export function isSupportedStellarVersion(minecraftVersion: string | null | undefined): boolean {
  const raw = (minecraftVersion ?? '').trim()
  return MINECRAFT_TARGETS.some((t) => t.mcVersion === raw || t.id === raw)
}

export function primeJarPrefix(minecraftVersion: string | null | undefined): string {
  return resolveTarget(minecraftVersion).jarPrefix
}

/** Parse `stellar-client-<target>-1.2.63.jar` → [1,2,63]. */
export function parseStellarJarSemVer(fileName: string, jarPrefix: string): [number, number, number] | null {
  const escaped = jarPrefix.replace(/\./g, '\\.')
  const match = fileName.match(new RegExp(`^${escaped}-(\\d+)\\.(\\d+)\\.(\\d+)\\.jar$`))
  if (!match) {
    return null
  }
  return [Number(match[1]), Number(match[2]), Number(match[3])]
}

/** All known Stellar jar prefixes (for cleaning stale jars when switching MC version). */
export function allStellarJarPrefixes(): string[] {
  return MINECRAFT_TARGETS.map((t) => t.jarPrefix)
}
