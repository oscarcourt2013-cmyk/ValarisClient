import { BrowserWindow } from 'electron'
import { IPC } from '../../shared/ipc'
import type { UpdateProgressDto } from '../../shared/ipc'

export function emitUpdateProgress(payload: UpdateProgressDto): void {
  for (const win of BrowserWindow.getAllWindows()) {
    win.webContents.send(IPC.UPDATE_PROGRESS, payload)
  }
}
