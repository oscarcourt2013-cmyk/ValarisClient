import { execFile } from 'child_process'
import { existsSync } from 'fs'
import { readdir } from 'fs/promises'
import { basename, dirname, join } from 'path'
import { promisify } from 'util'

const execFileAsync = promisify(execFile)

const MIN_JAVA_MAJOR = 21
const PREFERRED_JAVA_MAJOR = 21
const MAX_JAVA_MAJOR = 99

export interface JavaInstallation {
  path: string
  major: number
  label: string
}

function parseJavaMajor(versionOutput: string): number | null {
  const quoted = versionOutput.match(/version "(\d+)/)
  if (quoted) {
    return Number.parseInt(quoted[1]!, 10)
  }
  const legacy = versionOutput.match(/version "1\.(\d+)/)
  if (legacy) {
    return Number.parseInt(legacy[1]!, 10)
  }
  return null
}

/** Prefer java.exe over javaw.exe for probing and launch. */
export function normalizeJavaExecutable(javaPath: string): string {
  const trimmed = javaPath.trim()
  if (!trimmed) {
    return trimmed
  }

  if (process.platform === 'win32' && /javaw\.exe$/i.test(trimmed)) {
    const javaExe = join(dirname(trimmed), 'java.exe')
    if (existsSync(javaExe)) {
      return javaExe
    }
  }

  return trimmed
}

function isSupportedJavaMajor(major: number): boolean {
  return major >= MIN_JAVA_MAJOR && major <= MAX_JAVA_MAJOR
}

async function probeJava(javaPath: string): Promise<JavaInstallation | null> {
  const normalized = normalizeJavaExecutable(javaPath)
  if (!normalized) {
    return null
  }

  try {
    const { stdout, stderr } = await execFileAsync(normalized, ['-version'], { timeout: 8000 })
    const output = `${stdout}${stderr}`
    const major = parseJavaMajor(output)
    if (major !== null && isSupportedJavaMajor(major)) {
      return {
        path: normalized,
        major,
        label: `${basename(normalized)} — Java ${major}`
      }
    }
  } catch (err: unknown) {
    const execErr = err as { stderr?: string; stdout?: string }
    const output = `${execErr.stderr ?? ''}${execErr.stdout ?? ''}`
    const major = parseJavaMajor(output)
    if (major !== null && isSupportedJavaMajor(major)) {
      return {
        path: normalized,
        major,
        label: `${basename(normalized)} — Java ${major}`
      }
    }
  }
  return null
}

async function discoverJavaViaWhere(): Promise<string[]> {
  if (process.platform !== 'win32') {
    return []
  }
  try {
    const { stdout } = await execFileAsync('where', ['java'], { timeout: 5000 })
    return stdout
      .split(/\r?\n/)
      .map((line) => line.trim())
      .filter(Boolean)
  } catch {
    return []
  }
}

async function discoverJavaOnWindows(): Promise<string[]> {
  const roots = [
    process.env['JAVA_HOME'],
    process.env['JDK_HOME'],
    'C:\\Program Files\\Java',
    'C:\\Program Files\\Eclipse Adoptium',
    'C:\\Program Files\\Microsoft',
    'C:\\Program Files\\Zulu',
    'C:\\Program Files\\Amazon Corretto',
    'C:\\Program Files\\BellSoft'
  ].filter(Boolean) as string[]

  const candidates: string[] = ['java', ...(await discoverJavaViaWhere())]

  for (const root of roots) {
    const bin = join(root, 'bin', 'java.exe')
    if (existsSync(bin)) {
      candidates.push(bin)
    }
    const javaw = join(root, 'bin', 'javaw.exe')
    if (existsSync(javaw)) {
      candidates.push(javaw)
    }
    if (existsSync(root)) {
      try {
        const entries = await readdir(root, { withFileTypes: true })
        for (const entry of entries) {
          if (!entry.isDirectory()) {
            continue
          }
          const nestedJava = join(root, entry.name, 'bin', 'java.exe')
          if (existsSync(nestedJava)) {
            candidates.push(nestedJava)
          }
          const nestedJavaw = join(root, entry.name, 'bin', 'javaw.exe')
          if (existsSync(nestedJavaw)) {
            candidates.push(nestedJavaw)
          }
        }
      } catch {
        // ignore unreadable directories
      }
    }
  }

  return [...new Set(candidates)]
}

async function discoverJavaCandidates(): Promise<string[]> {
  if (process.platform === 'win32') {
    return discoverJavaOnWindows()
  }
  const roots = [process.env['JAVA_HOME'], process.env['JDK_HOME']].filter(Boolean) as string[]
  const candidates = ['java']
  for (const root of roots) {
    const bin = join(root, 'bin', 'java')
    if (existsSync(bin)) {
      candidates.push(bin)
    }
  }
  return [...new Set(candidates)]
}

export async function validateJavaExecutable(javaPath: string): Promise<JavaInstallation | null> {
  return probeJava(javaPath)
}

export async function listJavaInstallations(extraPaths: string[] = []): Promise<JavaInstallation[]> {
  const found: JavaInstallation[] = []
  const seen = new Set<string>()

  const candidates = [...(await discoverJavaCandidates()), ...extraPaths.map(normalizeJavaExecutable)]

  for (const candidate of candidates) {
    const install = await probeJava(candidate)
    if (install && !seen.has(install.path)) {
      seen.add(install.path)
      found.push(install)
    }
  }

  return found.sort((a, b) => {
    const aPreferred = a.major === PREFERRED_JAVA_MAJOR ? 1 : 0
    const bPreferred = b.major === PREFERRED_JAVA_MAJOR ? 1 : 0
    if (aPreferred !== bPreferred) {
      return bPreferred - aPreferred
    }
    return b.major - a.major
  })
}

/** Finds a Java 21+ binary — optional override from settings or instance. */
export async function resolveJavaPath(overridePath?: string | null): Promise<string> {
  if (overridePath?.trim()) {
    const custom = await probeJava(overridePath.trim())
    if (custom) {
      return custom.path
    }
    throw new Error(`Configured Java is invalid or below Java ${MIN_JAVA_MAJOR}: ${overridePath}`)
  }

  for (const candidate of await discoverJavaCandidates()) {
    const resolved = await probeJava(candidate)
    if (resolved) {
      return resolved.path
    }
  }

  throw new Error(
    `Java ${PREFERRED_JAVA_MAJOR}+ is required for Minecraft. Install Temurin 21+ or let the launcher download it automatically.`
  )
}
