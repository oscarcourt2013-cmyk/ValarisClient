import { app } from 'electron'
import { DiscordIpcClient } from '../src/main/discord/DiscordIpcClient.ts'

app.whenReady().then(async () => {
  const client = new DiscordIpcClient('1525574680994648174')
  const ok = await client.setActivity({
    details: 'StellarClient Launcher',
    state: 'Electron IPC test',
    largeImageKey: 'prime_logo',
    largeImageText: 'Prime',
    buttons: [
      { label: 'StellarClient', url: 'https://discord.com/applications/1525574680994648174' },
      { label: 'Discord', url: 'https://discord.com/app' }
    ]
  })
  console.log(JSON.stringify({ ok, connected: client.isConnected(), lastError: client.lastError }))
  app.quit()
})
