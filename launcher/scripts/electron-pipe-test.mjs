import { app } from 'electron'
import { createRequire } from 'module'

const require = createRequire(import.meta.url)
// Load compiled bundle - DiscordIpcClient is internal, test via dynamic import of built module
// Instead duplicate minimal test using net from electron's node

import net from 'net'

const APP_ID = '1525574680994648174'

function testPipe() {
  return new Promise((resolve) => {
    const socket = net.connect('\\\\.\\pipe\\discord-ipc-0')
    socket.once('connect', () => {
      socket.destroy()
      resolve({ connect: true })
    })
    socket.once('error', (err) => {
      resolve({ connect: false, error: err.message, code: err.code })
    })
    setTimeout(() => {
      socket.destroy()
      resolve({ connect: false, error: 'timeout' })
    }, 3000)
  })
}

app.whenReady().then(async () => {
  const result = await testPipe()
  console.log('Electron pipe test:', JSON.stringify(result))
  console.log('Electron pid:', process.pid)
  console.log('Electron version:', process.versions.electron)
  app.quit()
})
