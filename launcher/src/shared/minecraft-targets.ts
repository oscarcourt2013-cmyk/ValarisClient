/** Supported Minecraft targets for ValerisClient instances. */

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
    jarPrefix: 'valeris-client-26.2',
    localBuildDir: 'mc-26.2',
    fabricApi: '0.154.2+26.2',
    fabricLoader: '0.19.3',
    javaMajor: 25,
    recommended: true
  },
  {
    id: '1.21.11',
    mcVersion: '1.21.11',
    jarPrefix: 'valeris-client-1.21.11',
    localBuildDir: 'mc-1.21.11',
    fabricApi: '0.141.4+1.21.11',
    fabricLoader: '0.19.3',
    javaMajor: 21
  }
] as const

export const DEFAULT_MINECRAFT_TARGET = MINECRAFT_TARGETS[0]

/** Normalize user input and resolve a known Valeris target (falls back to recommended). */
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

/**
 * The target for this Minecraft version, or {@code null} when no Valeris layer
 * exists for it.
 *
 * <p>Use this anywhere the answer decides whether to ship the mod. {@link
 * resolveTarget} deliberately falls back to the recommended target so callers
 * that only need a jar prefix or build hint always get one -- but relying on
 * that fallback to gate injection would put, say, the 26.2 jar into a 1.20.1
 * instance and crash the game on launch with an unexplained mixin error.</p>
 */
export function findTarget(minecraftVersion: string | null | undefined): MinecraftTarget | null {
  const raw = (minecraftVersion ?? '').trim()
  if (!raw) {
    return null
  }
  const exact = MINECRAFT_TARGETS.find(
    (t) => t.mcVersion === raw || t.id === raw || t.localBuildDir === raw
  )
  if (exact) {
    return exact
  }
  // Same soft match as resolveTarget: "1.21.11-rc" is still 1.21.11.
  return (
    MINECRAFT_TARGETS.find(
      (t) => raw.startsWith(`${t.mcVersion}.`) || raw.startsWith(`${t.mcVersion}-`)
    ) ?? null
  )
}

export function isSupportedValerisVersion(minecraftVersion: string | null | undefined): boolean {
  return findTarget(minecraftVersion) !== null
}

/** Minecraft versions that currently have a Valeris client layer. */
export function supportedValerisVersions(): string[] {
  return MINECRAFT_TARGETS.map((t) => t.mcVersion)
}

export function primeJarPrefix(minecraftVersion: string | null | undefined): string {
  return resolveTarget(minecraftVersion).jarPrefix
}

/** Parse `valeris-client-<target>-1.2.63.jar` → [1,2,63]. */
export function parseValerisJarSemVer(fileName: string, jarPrefix: string): [number, number, number] | null {
  const escaped = jarPrefix.replace(/\./g, '\\.')
  const match = fileName.match(new RegExp(`^${escaped}-(\\d+)\\.(\\d+)\\.(\\d+)\\.jar$`))
  if (!match) {
    return null
  }
  return [Number(match[1]), Number(match[2]), Number(match[3])]
}

/** All known Valeris jar prefixes (for cleaning stale jars when switching MC version). */
export function allValerisJarPrefixes(): string[] {
  return MINECRAFT_TARGETS.map((t) => t.jarPrefix)
}
