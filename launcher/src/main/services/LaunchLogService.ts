import { appendFile, mkdir, writeFile } from 'fs/promises'
import { app, BrowserWindow, shell } from 'electron'
import { join } from 'path'
import { IPC } from '../../shared/ipc'
import type { LaunchLogEntryDto, LaunchProgressDto } from '../../shared/ipc'

const MAX_ENTRIES = 2000

export class LaunchLogService {
  private entries: LaunchLogEntryDto[] = []
  private logFilePath: string | null = null

  private async ensureLogFile(): Promise<string> {
    if (this.logFilePath) {
      return this.logFilePath
    }
    const dir = join(app.getPath('userData'), 'logs')
    await mkdir(dir, { recursive: true })
    this.logFilePath = join(dir, 'launch.log')
    return this.logFilePath
  }

  async clear(): Promise<void> {
    this.entries = []
    const file = await this.ensureLogFile()
    await writeFile(file, '', 'utf8')
    this.broadcast()
  }

  list(): LaunchLogEntryDto[] {
    return [...this.entries]
  }

  async openFolder(): Promise<void> {
    const dir = join(app.getPath('userData'), 'logs')
    await mkdir(dir, { recursive: true })
    await shell.openPath(dir)
  }

  append(level: LaunchLogEntryDto['level'], message: string, phase?: LaunchProgressDto['phase']): void {
    const line = message.trim()
    if (!line) {
      return
    }

    const entry: LaunchLogEntryDto = {
      id: `${Date.now()}-${Math.random().toString(36).slice(2, 8)}`,
      timestamp: new Date().toISOString(),
      level,
      phase,
      message: line
    }

    this.entries.push(entry)
    if (this.entries.length > MAX_ENTRIES) {
      this.entries.splice(0, this.entries.length - MAX_ENTRIES)
    }

    void this.persist(entry)
    this.broadcast(entry)
  }

  fromProgress(payload: LaunchProgressDto): void {
    if (payload.phase === 'log') {
      this.append('debug', payload.detail, payload.phase)
      return
    }
    if (payload.phase === 'crashed') {
      return
    }
    const level =
      payload.phase === 'error' ? 'error' : payload.phase === 'stopped' ? 'info' : 'info'
    this.append(level, payload.detail, payload.phase)
  }

  private async persist(entry: LaunchLogEntryDto): Promise<void> {
    try {
      const file = await this.ensureLogFile()
      const stamp = entry.timestamp.replace('T', ' ').slice(0, 19)
      await appendFile(file, `[${stamp}] [${entry.level.toUpperCase()}] ${entry.message}\n`, 'utf8')
    } catch {
      // disk full / permissions — keep in-memory buffer
    }
  }

  private broadcast(entry?: LaunchLogEntryDto): void {
    for (const win of BrowserWindow.getAllWindows()) {
      if (entry) {
        win.webContents.send(IPC.LAUNCH_LOG_APPEND, entry)
      } else {
        win.webContents.send(IPC.LAUNCH_LOG_RESET)
      }
    }
  }
}

export const launchLogService = new LaunchLogService()
