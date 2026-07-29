import { readdir, readFile, stat } from 'fs/promises'
import { join } from 'path'
import type { GameCrashAnalysisDto, GameExitInfoDto } from '../../shared/ipc'

export interface ParsedCrashReport {
  time?: string
  description?: string
  exceptionType?: string
  exceptionMessage?: string
  screen?: string
  primeFrames: Array<{ className: string; line?: number }>
  modIds: string[]
}

export interface AnalyzeGameExitInput {
  gameDir: string
  knownCrashReports: Set<string>
  sessionStartedAt: number
  exitCode: number | null
  signal: string | null
  intentionalKill: boolean
  recentLogLines: string[]
}

export async function snapshotCrashReports(gameDir: string): Promise<Set<string>> {
  const dir = join(gameDir, 'crash-reports')
  try {
    const files = await readdir(dir)
    return new Set(files.filter((name) => name.endsWith('.txt')))
  } catch {
    return new Set()
  }
}

async function findNewCrashReport(
  gameDir: string,
  known: Set<string>,
  sessionStartedAt: number
): Promise<string | null> {
  const dir = join(gameDir, 'crash-reports')
  let entries: string[]
  try {
    entries = await readdir(dir)
  } catch {
    return null
  }

  let bestPath: string | null = null
  let bestMtime = 0

  for (const name of entries) {
    if (!name.endsWith('.txt')) {
      continue
    }
    const fullPath = join(dir, name)
    try {
      const info = await stat(fullPath)
      const isNew = !known.has(name) || info.mtimeMs >= sessionStartedAt - 2000
      if (isNew && info.mtimeMs >= bestMtime) {
        bestMtime = info.mtimeMs
        bestPath = fullPath
      }
    } catch {
      // skip unreadable file
    }
  }

  return bestPath
}

export function parseCrashReport(content: string): ParsedCrashReport {
  const parsed: ParsedCrashReport = { primeFrames: [], modIds: [] }

  const descMatch = content.match(/^Description: (.+)$/m)
  if (descMatch) {
    parsed.description = descMatch[1].trim()
  }

  const timeMatch = content.match(/^Time: (.+)$/m)
  if (timeMatch) {
    parsed.time = timeMatch[1].trim()
  }

  const screenMatch = content.match(/^\tScreen name: (.+)$/m)
  if (screenMatch) {
    parsed.screen = screenMatch[1].trim()
  }

  const exceptionLine = content.match(/^(\S+(?:\.\S+)*Exception(?:Error)?: .+)$/m)
  if (exceptionLine) {
    parsed.exceptionType = exceptionLine[1].split(':')[0]?.trim()
    parsed.exceptionMessage = exceptionLine[1].slice(parsed.exceptionType.length + 1).trim()
  } else {
    const errorLine = content.match(/^(\S+(?:\.\S+)*Error: .+)$/m)
    if (errorLine) {
      parsed.exceptionType = errorLine[1].split(':')[0]?.trim()
      parsed.exceptionMessage = errorLine[1].slice(parsed.exceptionType.length + 1).trim()
    }
  }

  const framePattern = /at knot\/\/([^\(]+)\(([^:]+):(\d+)\)/g
  let match: RegExpExecArray | null
  while ((match = framePattern.exec(content)) !== null) {
    const className = match[1].trim()
    const line = Number.parseInt(match[3], 10)
    if (className.includes('StellarClient') || className.includes('dev.stellarclient')) {
      parsed.primeFrames.push({ className, line: Number.isFinite(line) ? line : undefined })
    }
    const modMatch = className.match(/dev\.([a-z0-9_]+)\./i)
    if (modMatch && !modMatch[1].startsWith('StellarClient')) {
      parsed.modIds.push(modMatch[1])
    }
  }

  parsed.modIds = [...new Set(parsed.modIds)]
  return parsed
}

function readLatestLogCrash(content: string): ParsedCrashReport | null {
  if (!content.includes('---- Minecraft Crash Report ----')) {
    return null
  }
  const marker = content.lastIndexOf('---- Minecraft Crash Report ----')
  return parseCrashReport(content.slice(marker))
}

function scanLaunchLogForCrash(lines: string[]): ParsedCrashReport | null {
  const tail = lines.slice(-120).join('\n')
  if (
    tail.includes('---- Minecraft Crash Report ----') ||
    tail.includes('FATAL ERROR in native method') ||
    tail.includes('Process crashed with exit code')
  ) {
    return parseCrashReport(tail)
  }

  const exceptionMatch = tail.match(
    /(?:Exception in thread|Caused by:)\s*"[^"]*"\s*(\S+(?:\.\S+)*Exception(?:Error)?: .+)/m
  )
  if (exceptionMatch) {
    const parsed = parseCrashReport(exceptionMatch[0])
    if (parsed.exceptionType) {
      return parsed
    }
  }

  return null
}

function suggestFix(parsed: ParsedCrashReport): GameCrashAnalysisDto['fixKey'] {
  const type = (parsed.exceptionType ?? '').toLowerCase()
  const msg = (parsed.exceptionMessage ?? '').toLowerCase()

  if (msg.includes('can only blur once per frame')) {
    return 'blurOnce'
  }
  if (type.includes('outofmemoryerror') || msg.includes('java heap space')) {
    return 'outOfMemory'
  }
  if (parsed.primeFrames.length > 0) {
    return 'primeMod'
  }
  if (parsed.modIds.length > 0) {
    return 'modConflict'
  }
  if (type.includes('classnotfoundexception') || type.includes('nosuchmethoderror')) {
    return 'modConflict'
  }
  if (type.includes('linkageerror') || msg.includes('fabric')) {
    return 'loaderError'
  }
  return 'unknown'
}

function buildTitle(parsed: ParsedCrashReport, fallback: string): string {
  if (parsed.exceptionType && parsed.exceptionMessage) {
    return `${parsed.exceptionType}: ${parsed.exceptionMessage}`
  }
  if (parsed.exceptionType) {
    return parsed.exceptionType
  }
  if (parsed.description) {
    return parsed.description
  }
  return fallback
}

function toCrashDto(
  parsed: ParsedCrashReport,
  source: GameCrashAnalysisDto['source'],
  exitCode: number | null,
  signal: string | null,
  crashReportPath: string | undefined,
  sessionDurationSec: number
): GameCrashAnalysisDto {
  const primeLocation =
    parsed.primeFrames[0] != null
      ? `${parsed.primeFrames[0].className}${parsed.primeFrames[0].line != null ? `:${parsed.primeFrames[0].line}` : ''}`
      : undefined

  return {
    source,
    exitCode,
    signal,
    crashReportPath,
    title: buildTitle(parsed, 'Minecraft crashed'),
    description: parsed.description,
    exceptionType: parsed.exceptionType,
    exceptionMessage: parsed.exceptionMessage,
    screen: parsed.screen,
    primeInvolved: parsed.primeFrames.length > 0,
    primeLocation,
    modIds: parsed.modIds.slice(0, 5),
    fixKey: suggestFix(parsed),
    sessionDurationSec
  }
}

export async function analyzeGameExit(
  input: AnalyzeGameExitInput
): Promise<{ kind: 'crash'; crash: GameCrashAnalysisDto } | { kind: 'exit'; exit: GameExitInfoDto }> {
  const sessionDurationSec =
    input.sessionStartedAt > 0 ? Math.round((Date.now() - input.sessionStartedAt) / 1000) : 0

  if (input.intentionalKill) {
    return {
      kind: 'exit',
      exit: {
        reason: 'launcher_kill',
        exitCode: input.exitCode,
        signal: input.signal,
        sessionDurationSec
      }
    }
  }

  const crashReportPath = await findNewCrashReport(
    input.gameDir,
    input.knownCrashReports,
    input.sessionStartedAt
  )

  if (crashReportPath) {
    try {
      const content = await readFile(crashReportPath, 'utf8')
      const parsed = parseCrashReport(content)
      return {
        kind: 'crash',
        crash: toCrashDto(
          parsed,
          'crash_report',
          input.exitCode,
          input.signal,
          crashReportPath,
          sessionDurationSec
        )
      }
    } catch {
      // fall through to other heuristics
    }
  }

  try {
    const latestLog = await readFile(join(input.gameDir, 'logs', 'latest.log'), 'utf8')
    const fromLog = readLatestLogCrash(latestLog)
    if (fromLog) {
      return {
        kind: 'crash',
        crash: toCrashDto(fromLog, 'latest_log', input.exitCode, input.signal, undefined, sessionDurationSec)
      }
    }
  } catch {
    // no latest.log
  }

  const fromLaunchLog = scanLaunchLogForCrash(input.recentLogLines)
  if (fromLaunchLog) {
    return {
      kind: 'crash',
      crash: toCrashDto(
        fromLaunchLog,
        'launch_log',
        input.exitCode,
        input.signal,
        undefined,
        sessionDurationSec
      )
    }
  }

  const abnormalExit = input.signal != null || (input.exitCode != null && input.exitCode !== 0)
  if (abnormalExit) {
    const parsed: ParsedCrashReport = { primeFrames: [], modIds: [] }
    return {
      kind: 'crash',
      crash: toCrashDto(
        parsed,
        'exit_code',
        input.exitCode,
        input.signal,
        undefined,
        sessionDurationSec
      )
    }
  }

  return {
    kind: 'exit',
    exit: {
      reason: 'clean_quit',
      exitCode: input.exitCode,
      signal: input.signal,
      sessionDurationSec
    }
  }
}
