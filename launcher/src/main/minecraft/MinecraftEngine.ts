import type { ChildProcess } from 'child_process'
import { mkdir } from 'fs/promises'
import { Launch } from 'minecraft-java-core'
import type { LaunchOptions } from 'minecraft-java-core'
import type { InstanceLaunchConfig } from './constants'
import { resolveJavaPath } from './JavaService'
import { ensureBundledJava } from './JavaManager'
import { installInstanceMods, ValerisClientBuildHint } from './ModPackService'
import { resolveLaunchAuthenticator } from './LaunchAuth'
import { formatLaunchError } from './formatLaunchError'
import { emitLaunchError, emitLaunchProgress } from './launchProgress'
import { getInstanceGameDir, getRuntimeRoot } from './paths'
import { syncMinecraftLanguage, syncMinecraftDisplaySettings } from '../content/options'
import { accountService } from '../services/AccountService'
import { instanceService } from '../services/InstanceService'
import { settingsStore, type GameDisplayMode } from '../storage/SettingsStore'
import { launchLogService } from '../services/LaunchLogService'
import { analyzeGameExit, snapshotCrashReports } from '../services/CrashAnalyzerService'

let gameRunning = false
let activeGameProcess: ChildProcess | null = null
let spawnPatched = false
let activeInstanceId: string | null = null
let sessionStartedAt = 0
let knownCrashReports = new Set<string>()
let intentionalKill = false
let exitHandled = false

function isLikelyMinecraftProcess(command: unknown, args: unknown): boolean {
  const joined = Array.isArray(args) ? args.join(' ').toLowerCase() : ''
  // Require MC/Fabric markers — bare `java` matches installers and breaks running sync.
  return (
    joined.includes('net.minecraft') ||
    joined.includes('net.fabricmc') ||
    joined.includes('cpw.mods') ||
    joined.includes('org.quiltmc')
  )
}

function processAlive(proc: ChildProcess | null): boolean {
  return Boolean(proc && !proc.killed && proc.exitCode == null)
}

function trackGameProcess(proc: ChildProcess): void {
  if (processAlive(activeGameProcess) && activeGameProcess !== proc) {
    return
  }
  activeGameProcess = proc
  gameRunning = true
  if (activeInstanceId) {
    emitLaunchProgress({
      phase: 'running',
      detail: 'Minecraft running',
      percent: 100
    })
  }
  proc.once('exit', (code, signal) => {
    if (activeGameProcess === proc) {
      activeGameProcess = null
      gameRunning = false
    }
    void handleGameExit(code, signal)
  })
}

async function beginGameSession(instanceId: string): Promise<void> {
  activeInstanceId = instanceId
  sessionStartedAt = Date.now()
  intentionalKill = false
  exitHandled = false
  knownCrashReports = await snapshotCrashReports(getInstanceGameDir(instanceId))
}

async function handleGameExit(code: number | null, signal: NodeJS.Signals | null): Promise<void> {
  if (exitHandled) {
    return
  }
  exitHandled = true
  gameRunning = false
  activeGameProcess = null

  const instanceId = activeInstanceId
  if (!instanceId) {
    emitLaunchProgress({ phase: 'stopped', detail: 'Minecraft closed.', percent: 0 })
    return
  }

  const gameDir = getInstanceGameDir(instanceId)
  const recentLogLines = launchLogService.list().slice(-120).map((entry) => entry.message)
  const result = await analyzeGameExit({
    gameDir,
    knownCrashReports,
    sessionStartedAt,
    exitCode: code,
    signal,
    intentionalKill,
    recentLogLines
  })

  if (result.kind === 'crash') {
    const { crash } = result
    emitLaunchProgress({
      phase: 'crashed',
      detail: crash.title,
      percent: 0,
      crash
    })
    launchLogService.append('error', `[CRASH] ${crash.title}`, 'crashed')
    if (crash.description) {
      launchLogService.append('error', crash.description, 'crashed')
    }
    if (crash.screen) {
      launchLogService.append('error', `Screen: ${crash.screen}`, 'crashed')
    }
    if (crash.primeInvolved && crash.primeLocation) {
      launchLogService.append('warn', `ValerisClient: ${crash.primeLocation}`, 'crashed')
    }
  } else {
    const detail =
      result.exit.reason === 'launcher_kill'
        ? 'Game stopped by launcher.'
        : 'Minecraft closed normally.'
    emitLaunchProgress({
      phase: 'stopped',
      detail,
      percent: 0,
      exit: result.exit
    })
  }

  activeInstanceId = null
  sessionStartedAt = 0
}

function patchSpawnForJvmFlags(): void {
  if (spawnPatched) {
    return
  }
  spawnPatched = true

  const badFlags = ['UseCompactObjectHeaders']
  // eslint-disable-next-line @typescript-eslint/no-require-imports
  const childProcess = require('child_process') as typeof import('child_process') & {
    spawn: (...args: unknown[]) => ChildProcess
  }
  const nativeSpawn = childProcess.spawn.bind(childProcess)

  childProcess.spawn = ((command: unknown, args?: unknown, options?: unknown) => {
    let spawnArgs = args
    if (Array.isArray(spawnArgs)) {
      spawnArgs = spawnArgs.filter(
        (arg) => typeof arg !== 'string' || !badFlags.some((flag) => arg.includes(flag))
      )
    }
    const proc = nativeSpawn(command, spawnArgs, options)
    if (isLikelyMinecraftProcess(command, spawnArgs)) {
      trackGameProcess(proc)
    }
    return proc
  }) as typeof childProcess.spawn
}

function filterJvmArgs(args: string[]): string[] {
  return args.filter((arg) => !arg.includes('UseCompactObjectHeaders'))
}

function buildGameArgs(
  gameDirectory: string,
  joinServer?: { host: string; port: number },
  display?: { mode: GameDisplayMode; width: number; height: number }
): string[] {
  const args = ['--gameDir', gameDirectory]
  if (joinServer) {
    args.push('--server', joinServer.host, '--port', String(joinServer.port))
  }
  if (display?.mode === 'fullscreen') {
    args.push('--fullscreen')
    if (display.width > 0 && display.height > 0) {
      args.push('--fullscreenWidth', String(display.width), '--fullscreenHeight', String(display.height))
    }
  }
  return args
}

async function resolveLaunchJava(config: InstanceLaunchConfig, settingsJavaPath?: string | null): Promise<string> {
  const override = config.javaPath?.trim() || settingsJavaPath?.trim()
  if (override) {
    return resolveJavaPath(override)
  }

  try {
    return await resolveJavaPath()
  } catch {
    emitLaunchProgress({
      phase: 'launch',
      detail: 'System Java not found. Downloading bundled Java 21…',
      percent: 70
    })
    return ensureBundledJava(21)
  }
}

function attachLauncherEvents(
  launcher: Launch,
  onSpawn: () => void,
  onFailure: (err: unknown) => void
): void {
  launcher.on('data', (line) => {
    const detail = String(line).trim()
    if (detail) {
      const lower = detail.toLowerCase()
      const level =
        lower.includes(' error') || lower.includes('/error]') || lower.startsWith('error')
          ? 'error'
          : lower.includes(' warn') || lower.includes('/warn]')
            ? 'warn'
            : 'debug'
      // Console page only — never push raw JVM lines into the Home progress strip.
      launchLogService.append(level, detail, 'log')
    }
    if (detail.includes('Launching with arguments')) {
      onSpawn()
    }
  })

  launcher.on('progress', (progress, size, element) => {
    const percent =
      typeof size === 'number' && size > 0 ? 20 + Math.round((progress / size) * 50) : undefined
    emitLaunchProgress({
      phase: 'download',
      detail: typeof element === 'string' && element ? element : 'Downloading Minecraft files…',
      percent
    })
  })

  launcher.on('error', (error) => {
    onFailure(error)
  })

  launcher.on('close', () => {
    if (!exitHandled) {
      void handleGameExit(null, null)
    }
  })
}

async function runMinecraftJavaCoreLaunch(options: LaunchOptions): Promise<void> {
  patchSpawnForJvmFlags()

  const launcher = new Launch()
  let settled = false

  await new Promise<void>((resolve, reject) => {
    const succeed = () => {
      if (settled) {
        return
      }
      settled = true
      resolve()
    }

    const fail = (err: unknown) => {
      if (settled) {
        return
      }
      settled = true
      reject(new Error(formatLaunchError(err)))
    }

    attachLauncherEvents(launcher, succeed, fail)
    void launcher.Launch(options)
  })
}

async function killProcessTree(proc: ChildProcess): Promise<void> {
  if (!proc.pid) {
    proc.kill()
    return
  }
  if (process.platform === 'win32') {
    // eslint-disable-next-line @typescript-eslint/no-require-imports
    const { spawn } = require('child_process') as typeof import('child_process')
    await new Promise<void>((resolve) => {
      spawn('taskkill', ['/PID', String(proc.pid), '/T', '/F'], { stdio: 'ignore' })
        .once('exit', () => resolve())
        .once('error', () => resolve())
    })
    return
  }
  proc.kill('SIGTERM')
}

export class MinecraftEngine {
  isRunning(): boolean {
    if (processAlive(activeGameProcess)) {
      return true
    }
    // Process handle can be missing on some JVMs — trust the session until exit is handled.
    if (activeInstanceId != null && !exitHandled) {
      return true
    }
    gameRunning = false
    return false
  }

  getActiveInstanceId(): string | null {
    return activeInstanceId
  }

  getActiveProcess(): ChildProcess | null {
    return activeGameProcess
  }

  async killGame(): Promise<void> {
    intentionalKill = true
    const proc = activeGameProcess
    if (!proc || proc.killed) {
      gameRunning = false
      activeGameProcess = null
      if (!exitHandled) {
        void handleGameExit(0, null)
      }
      return
    }
    try {
      await killProcessTree(proc)
    } catch {
      try {
        proc.kill('SIGKILL')
      } catch {
        // ignore
      }
    }
    gameRunning = false
    activeGameProcess = null
  }

  async launchInstance(
    instanceId: string,
    joinServer?: { host: string; port: number }
  ): Promise<{ username: string; primeModInstalled: boolean }> {
    const stored = await instanceService.getStoredById(instanceId)
    if (!stored) {
      throw new Error(`Unknown instance "${instanceId}".`)
    }

    const config = instanceService.toLaunchConfig(stored)
    const account = await accountService.getStoredActiveAccount()
    if (!account) {
      throw new Error('No account selected.')
    }

    emitLaunchProgress({ phase: 'start', detail: 'Preparing launch…', percent: 0 })
    emitLaunchProgress({ phase: 'start', detail: 'Resolving Microsoft / offline session…', percent: 5 })

    const authenticator = await resolveLaunchAuthenticator(account)
    const runtimeRoot = getRuntimeRoot()
    const gameDirectory = getInstanceGameDir(instanceId)
    await mkdir(runtimeRoot, { recursive: true })
    await mkdir(gameDirectory, { recursive: true })

    let primeModInstalled = false
    if (config.loader === 'fabric' && config.includePrimeMod) {
      const { primeJar, unsupportedVersion } = await installInstanceMods(instanceId, config)
      if (!primeJar && !unsupportedVersion) {
        // The version IS supported, so a missing jar is a real problem worth failing on.
        throw new Error(`ValerisClient mod not found. ${ValerisClientBuildHint(config.minecraftVersion)}`)
      }
      if (primeJar) {
        primeModInstalled = true
        emitLaunchProgress({ phase: 'mods', detail: `ValerisClient mod installed: ${primeJar}`, percent: 15 })
      }
    }

    const settings = await settingsStore.load()
    await syncMinecraftLanguage(instanceId, settings.language)
    await syncMinecraftDisplaySettings(
      instanceId,
      settings.gameWidth,
      settings.gameHeight,
      settings.gameDisplayMode
    )
    emitLaunchProgress({ phase: 'launch', detail: 'Locating Java 21+…', percent: 68 })
    const javaPath = await resolveLaunchJava(config, settings.defaultJavaPath)
    emitLaunchProgress({ phase: 'launch', detail: `Using Java: ${javaPath}`, percent: 80 })

    const mergedJvm = filterJvmArgs([
      ...new Set([...(settings.jvmArgs ?? []), ...(config.jvmArgs ?? [])])
    ])

    emitLaunchProgress({
      phase: 'launch',
      detail: `Starting Minecraft ${config.minecraftVersion} (${config.loader})…`,
      percent: 85
    })

    const gameWidth = settings.gameWidth > 0 ? settings.gameWidth : 854
    const gameHeight = settings.gameHeight > 0 ? settings.gameHeight : 480

    const launchOptions: LaunchOptions = {
      path: runtimeRoot,
      authenticator,
      version: config.minecraftVersion,
      instance: null,
      detached: false,
      ignore_log4j: false,
      loader: {
        type: config.loader === 'fabric' ? 'fabric' : null,
        build: config.loader === 'fabric' ? config.fabricLoaderVersion || 'latest' : 'latest',
        enable: config.loader === 'fabric'
      },
      verify: false,
      ignored: [],
      JVM_ARGS: mergedJvm,
      GAME_ARGS: buildGameArgs(gameDirectory, joinServer, {
        mode: settings.gameDisplayMode,
        width: gameWidth,
        height: gameHeight
      }),
      java: {
        path: javaPath,
        version: '21',
        type: 'jre'
      },
      screen: {
        width: gameWidth,
        height: gameHeight,
        fullscreen: settings.gameDisplayMode === 'fullscreen'
      },
      memory: {
        min: '512M',
        max: `${config.ramMb}M`
      }
    }

    try {
      await beginGameSession(instanceId)
      await runMinecraftJavaCoreLaunch(launchOptions)
    } catch (err) {
      const message = formatLaunchError(err)
      emitLaunchError(message, err)
      throw new Error(message)
    }

    gameRunning = processAlive(activeGameProcess) || gameRunning
    await instanceService.touchLastPlayed(instanceId)

    emitLaunchProgress({
      phase: 'running',
      detail: `Minecraft running as ${account.username}`,
      percent: 100
    })

    return { username: account.username, primeModInstalled }
  }
}

export const minecraftEngine = new MinecraftEngine()
