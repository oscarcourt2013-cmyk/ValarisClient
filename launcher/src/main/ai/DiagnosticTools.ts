import { readdir, readFile, stat } from 'fs/promises'
import { join } from 'path'
import { getInstanceGameDir } from '../minecraft/paths'
import { parseCrashReport } from '../services/CrashAnalyzerService'
import { launchLogService } from '../services/LaunchLogService'
import { contentService } from '../services/ContentService'

const MAX_CRASH_CHARS = 14_000
const MAX_LOG_CHARS = 12_000
const MAX_ERROR_LINES = 80

export interface CrashReportListItem {
  fileName: string
  path: string
  mtimeMs: number
  size: number
}

/** List crash-reports/*.txt newest first. */
export async function listCrashReports(instanceId: string, limit = 8): Promise<CrashReportListItem[]> {
  const dir = join(getInstanceGameDir(instanceId), 'crash-reports')
  let names: string[]
  try {
    names = await readdir(dir)
  } catch {
    return []
  }

  const items: CrashReportListItem[] = []
  for (const fileName of names) {
    if (!fileName.endsWith('.txt')) continue
    const full = join(dir, fileName)
    try {
      const info = await stat(full)
      items.push({
        fileName,
        path: full,
        mtimeMs: info.mtimeMs,
        size: info.size
      })
    } catch {
      // skip
    }
  }
  items.sort((a, b) => b.mtimeMs - a.mtimeMs)
  return items.slice(0, Math.max(1, Math.min(limit, 20)))
}

function truncateMiddle(text: string, max: number): string {
  if (text.length <= max) return text
  const head = Math.floor(max * 0.55)
  const tail = max - head - 40
  return `${text.slice(0, head)}\n\n…[tronqué]…\n\n${text.slice(-tail)}`
}

function extractErrorishLines(content: string, maxLines = MAX_ERROR_LINES): string[] {
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

export async function readCrashReportPayload(
  instanceId: string,
  fileName?: string
): Promise<Record<string, unknown>> {
  const list = await listCrashReports(instanceId, 20)
  if (list.length === 0) {
    return { error: 'No crash reports found', path: join(getInstanceGameDir(instanceId), 'crash-reports') }
  }

  let target = list[0]
  if (fileName?.trim()) {
    const match = list.find(
      (item) => item.fileName === fileName.trim() || item.path.endsWith(fileName.trim())
    )
    if (!match) {
      return { error: `Crash report not found: ${fileName}`, available: list.map((i) => i.fileName) }
    }
    target = match
  }

  const raw = await readFile(target.path, 'utf8')
  const parsed = parseCrashReport(raw)
  return {
    fileName: target.fileName,
    path: target.path,
    mtime: new Date(target.mtimeMs).toISOString(),
    parsed: {
      time: parsed.time,
      description: parsed.description,
      exceptionType: parsed.exceptionType,
      exceptionMessage: parsed.exceptionMessage,
      screen: parsed.screen,
      primeFrames: parsed.primeFrames.slice(0, 12),
      modIds: parsed.modIds.slice(0, 20)
    },
    reportExcerpt: truncateMiddle(raw, MAX_CRASH_CHARS)
  }
}

export async function readLatestLogPayload(
  instanceId: string,
  options?: { maxChars?: number; errorsOnly?: boolean }
): Promise<Record<string, unknown>> {
  const logPath = join(getInstanceGameDir(instanceId), 'logs', 'latest.log')
  let raw: string
  try {
    raw = await readFile(logPath, 'utf8')
  } catch {
    return { error: 'latest.log not found', path: logPath }
  }

  const errorsOnly = options?.errorsOnly !== false
  const maxChars = options?.maxChars ?? MAX_LOG_CHARS
  const errorLines = extractErrorishLines(raw)
  const tail = raw.slice(-Math.min(raw.length, maxChars))

  // Embedded crash report in latest.log
  let embeddedCrash: ReturnType<typeof parseCrashReport> | null = null
  if (raw.includes('---- Minecraft Crash Report ----')) {
    const marker = raw.lastIndexOf('---- Minecraft Crash Report ----')
    embeddedCrash = parseCrashReport(raw.slice(marker))
  }

  return {
    path: logPath,
    sizeChars: raw.length,
    errorLineCount: errorLines.length,
    errorLines: errorsOnly ? errorLines : errorLines.slice(0, 40),
    tail: truncateMiddle(tail, maxChars),
    embeddedCrash: embeddedCrash
      ? {
          description: embeddedCrash.description,
          exceptionType: embeddedCrash.exceptionType,
          exceptionMessage: embeddedCrash.exceptionMessage,
          screen: embeddedCrash.screen,
          primeFrames: embeddedCrash.primeFrames.slice(0, 8),
          modIds: embeddedCrash.modIds.slice(0, 15)
        }
      : null
  }
}

export async function readLauncherLogPayload(limit = 120): Promise<Record<string, unknown>> {
  const entries = launchLogService.list().slice(-Math.max(20, Math.min(limit, 400)))
  return {
    count: entries.length,
    entries: entries.map((e) => ({
      time: e.timestamp,
      level: e.level,
      phase: e.phase,
      message: e.message.slice(0, 500)
    }))
  }
}

/** One-shot diagnosis bundle for the AI. */
export async function diagnoseInstance(instanceId: string): Promise<Record<string, unknown>> {
  const [crashes, latest, launcher, mods] = await Promise.all([
    listCrashReports(instanceId, 5),
    readLatestLogPayload(instanceId),
    readLauncherLogPayload(100),
    contentService.listMods(instanceId).catch(() => [])
  ])

  let latestCrash: Record<string, unknown> | null = null
  if (crashes[0]) {
    latestCrash = await readCrashReportPayload(instanceId, crashes[0].fileName)
  }

  return {
    summary: {
      crashReportCount: crashes.length,
      latestCrashFile: crashes[0]?.fileName ?? null,
      latestLogErrors: typeof latest.errorLineCount === 'number' ? latest.errorLineCount : 0,
      enabledMods: mods.filter((m) => m.enabled).length,
      totalMods: mods.length
    },
    crashReports: crashes.map((c) => ({
      fileName: c.fileName,
      mtime: new Date(c.mtimeMs).toISOString(),
      size: c.size
    })),
    latestCrash,
    latestLog: latest,
    launcherLog: launcher,
    modsSample: mods.slice(0, 40).map((m) => ({
      name: m.name,
      fileName: m.fileName,
      enabled: m.enabled,
      source: m.source
    })),
    guidance:
      'Use this data to explain the likely cause and give step-by-step fixes (disable mod, raise RAM, update Fabric, etc.). Cite exception type and file names.'
  }
}
