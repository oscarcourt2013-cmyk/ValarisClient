const net = require("net");
const fs = require("fs");
const os = require("os");

const OPCODE_HANDSHAKE = 0;
const OPCODE_FRAME = 1;

function writePacket(socket, opcode, payload) {
  const body = Buffer.from(JSON.stringify(payload), "utf8");
  const header = Buffer.alloc(8);
  header.writeInt32LE(opcode, 0);
  header.writeInt32LE(body.length, 4);
  socket.write(Buffer.concat([header, body]));
}

function readPacket(socket, buffer, timeoutMs = 5000) {
  return new Promise((resolve, reject) => {
    const cleanup = () => {
      clearTimeout(timer);
      socket.off("data", onData);
      socket.off("error", onError);
    };
    const tryRead = () => {
      if (buffer.value.length < 8) return;
      const opcode = buffer.value.readInt32LE(0);
      const length = buffer.value.readInt32LE(4);
      if (buffer.value.length < 8 + length) return;
      const raw = buffer.value.subarray(8, 8 + length).toString("utf8");
      buffer.value = buffer.value.subarray(8 + length);
      cleanup();
      try {
        resolve({ opcode, payload: JSON.parse(raw) });
      } catch {
        resolve({ opcode, payload: raw });
      }
    };
    const onData = (chunk) => {
      buffer.value = Buffer.concat([buffer.value, chunk]);
      tryRead();
    };
    const onError = (error) => {
      cleanup();
      reject(error);
    };
    const timer = setTimeout(() => {
      cleanup();
      reject(new Error("Discord IPC read timeout."));
    }, timeoutMs);
    socket.on("data", onData);
    socket.once("error", onError);
    tryRead();
  });
}

function windowsPipes() {
  const fallback = Array.from({ length: 10 }, (_, index) => `\\\\.\\pipe\\discord-ipc-${index}`);
  try {
    const listed = fs.readdirSync("\\\\.\\pipe\\")
      .filter((name) => /^discord-ipc-\d+$/i.test(name))
      .map((name) => `\\\\.\\pipe\\${name}`);
    return [...listed, ...fallback.filter((item) => !listed.includes(item))];
  } catch {
    return fallback;
  }
}

function unixPipes() {
  const roots = [process.env.XDG_RUNTIME_DIR, process.env.TMPDIR, process.env.TMP, process.env.TEMP, "/tmp"].filter(Boolean);
  const paths = [];
  for (const root of roots) {
    for (let index = 0; index < 10; index += 1) {
      paths.push(`${root.replace(/\/$/, "")}/discord-ipc-${index}`);
    }
  }
  return paths;
}

function pipeCandidates() {
  return process.platform === "win32" ? windowsPipes() : unixPipes();
}

function connectPipe(pipePath, timeoutMs = 3000) {
  return new Promise((resolve, reject) => {
    const socket = net.connect(pipePath);
    const timer = setTimeout(() => {
      socket.destroy();
      reject(new Error("Discord IPC connect timeout."));
    }, timeoutMs);
    socket.once("connect", () => {
      clearTimeout(timer);
      resolve(socket);
    });
    socket.once("error", (error) => {
      clearTimeout(timer);
      reject(error);
    });
  });
}

class DiscordIpcClient {
  constructor(clientId) {
    this.clientId = String(clientId || "");
    this.socket = null;
    this.buffer = { value: Buffer.alloc(0) };
    this.lastError = "";
  }

  isConnected() {
    return Boolean(this.socket && !this.socket.destroyed);
  }

  async connect() {
    if (this.isConnected()) return true;
    if (!this.clientId) {
      this.lastError = "Missing Discord application ID.";
      return false;
    }
    for (const pipe of pipeCandidates()) {
      try {
        const socket = await connectPipe(pipe);
        this.socket = socket;
        this.buffer.value = Buffer.alloc(0);
        socket.once("close", () => {
          if (this.socket === socket) this.socket = null;
        });
        writePacket(socket, OPCODE_HANDSHAKE, { v: 1, client_id: this.clientId });
        const ready = await readPacket(socket, this.buffer);
        if (ready.payload?.evt === "READY" || ready.payload?.cmd === "DISPATCH") return true;
        if (ready.payload?.code) throw new Error(ready.payload.message || "Discord rejected the handshake.");
        return true;
      } catch (error) {
        this.lastError = error.message;
        await this.destroy();
      }
    }
    this.lastError = this.lastError || "Discord IPC pipe was not found.";
    return false;
  }

  async setActivity(activity) {
    const connected = await this.connect();
    if (!connected || !this.socket) return false;
    try {
      const nonce = `${Date.now()}-${Math.random().toString(16).slice(2)}`;
      writePacket(this.socket, OPCODE_FRAME, {
        cmd: "SET_ACTIVITY",
        nonce,
        args: {
          pid: process.pid,
          activity: {
            type: 0,
            details: activity.details || "Valaris Client",
            state: activity.state || "In the launcher",
            timestamps: activity.startTimestamp ? { start: Math.floor(new Date(activity.startTimestamp).getTime() / 1000) } : undefined,
            assets: {
              large_image: activity.largeImageKey || "valaris_logo",
              large_text: activity.largeImageText || "Valaris Client"
            },
            buttons: activity.buttons
          }
        }
      });
      const response = await readPacket(this.socket, this.buffer);
      if (response.payload?.evt === "ERROR" || response.payload?.code) {
        throw new Error(response.payload.message || "Discord rejected the activity.");
      }
      this.lastError = "";
      return true;
    } catch (error) {
      this.lastError = error.message;
      await this.destroy();
      return false;
    }
  }

  async destroy() {
    if (this.socket && !this.socket.destroyed) this.socket.destroy();
    this.socket = null;
  }
}

module.exports = { DiscordIpcClient };
