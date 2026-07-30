import net from 'net'
import fs from 'fs'

const APP_ID = '1525574680994648174'

function listWindowsDiscordPipes() {
  try {
    return fs
      .readdirSync('\\\\.\\pipe\\')
      .filter((name) => /discord/i.test(name))
  } catch {
    return []
  }
}

function writePacket(socket, opcode, json) {
  const body = Buffer.from(json, 'utf8')
  const header = Buffer.alloc(8)
  header.writeInt32LE(opcode, 0)
  header.writeInt32LE(body.length, 4)
  socket.write(Buffer.concat([header, body]))
}

function readPacket(socket, buffer) {
  return new Promise((resolve, reject) => {
    const tryRead = () => {
      if (buffer.value.length < 8) return
      const opcode = buffer.value.readInt32LE(0)
      const length = buffer.value.readInt32LE(4)
      if (buffer.value.length < 8 + length) return
      const json = buffer.value.subarray(8, 8 + length).toString('utf8')
      buffer.value = buffer.value.subarray(8 + length)
      cleanup()
      resolve({ opcode, json })
    }
    const onData = (chunk) => {
      buffer.value = Buffer.concat([buffer.value, chunk])
      tryRead()
    }
    const cleanup = () => {
      socket.off('data', onData)
      clearTimeout(timer)
    }
    const timer = setTimeout(() => {
      cleanup()
      reject(new Error('read timeout'))
    }, 5000)
    socket.on('data', onData)
    tryRead()
  })
}

async function fullTest(pipePath) {
  return new Promise((resolve) => {
    const buffer = { value: Buffer.alloc(0) }
    const socket = net.connect(pipePath)
    const timer = setTimeout(() => {
      socket.destroy()
      resolve({ pipePath, error: 'connect timeout' })
    }, 3000)

    socket.once('connect', async () => {
      clearTimeout(timer)
      try {
        writePacket(socket, 0, JSON.stringify({ v: 1, client_id: APP_ID }))
        const ready = await readPacket(socket, buffer)
        const activity = {
          type: 0,
          details: 'ValerisClient Launcher test',
          state: 'Testing RPC',
          assets: {
            large_image: 'prime_logo',
            large_text: 'Prime'
          },
          buttons: [
            { label: 'ValerisClient', url: 'https://discord.com/applications/' + APP_ID },
            { label: 'Discord', url: 'https://discord.com/app' }
          ]
        }
        writePacket(
          socket,
          1,
          JSON.stringify({
            cmd: 'SET_ACTIVITY',
            nonce: `${Date.now()}`,
            args: { pid: process.pid, activity }
          })
        )
        const response = await readPacket(socket, buffer)
        socket.destroy()
        resolve({ pipePath, ready: ready.json.slice(0, 80), response: response.json.slice(0, 200) })
      } catch (err) {
        socket.destroy()
        resolve({ pipePath, error: err instanceof Error ? err.message : String(err) })
      }
    })

    socket.once('error', (err) => {
      clearTimeout(timer)
      resolve({ pipePath, error: `${err.code}: ${err.message}` })
    })
  })
}

console.log('Pipes:', listWindowsDiscordPipes())
for (const name of listWindowsDiscordPipes()) {
  console.log(await fullTest(`\\\\.\\pipe\\${name}`))
}
