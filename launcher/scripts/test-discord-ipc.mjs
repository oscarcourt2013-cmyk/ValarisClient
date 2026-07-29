import net from 'net'

const APP_ID = '1525574680994648174'
const paths = [
  '\\\\.\\pipe\\discord-ipc-0',
  '\\\\?\\pipe\\discord-ipc-0'
]

function test(path) {
  return new Promise((resolve) => {
    console.log('Trying', path)
    const socket = net.connect(path)
    const timer = setTimeout(() => {
      socket.destroy()
      resolve({ path, error: 'connect timeout' })
    }, 3000)

    socket.once('connect', () => {
      console.log('Connected to', path)
      const payload = JSON.stringify({ v: 1, client_id: APP_ID })
      const body = Buffer.from(payload, 'utf8')
      const header = Buffer.alloc(8)
      header.writeInt32LE(0, 0)
      header.writeInt32LE(body.length, 4)
      socket.write(Buffer.concat([header, body]))

      let buffer = Buffer.alloc(0)
      socket.on('data', (chunk) => {
        buffer = Buffer.concat([buffer, chunk])
        while (buffer.length >= 8) {
          const opcode = buffer.readInt32LE(0)
          const length = buffer.readInt32LE(4)
          if (buffer.length < 8 + length) {
            return
          }
          const json = buffer.subarray(8, 8 + length).toString('utf8')
          buffer = buffer.subarray(8 + length)
          clearTimeout(timer)
          socket.destroy()
          resolve({ path, opcode, json })
        }
      })
    })

    socket.once('error', (err) => {
      clearTimeout(timer)
      resolve({ path, error: `${err.code}: ${err.message}` })
    })
  })
}

for (const path of paths) {
  console.log(await test(path))
}
