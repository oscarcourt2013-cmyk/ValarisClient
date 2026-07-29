/**
 * Voice relay (proximity + groups) — same protocol as the old voice-relay.
 * Path: /voice
 */
const { WebSocketServer } = require('ws');

const PROXIMITY_BLOCKS = 48;

/** @type {Map<string, Map<string, { ws, name, x, y, z, group }>>} */
const rooms = new Map();

function distance(a, b) {
  const dx = (a.x ?? 0) - (b.x ?? 0);
  const dy = (a.y ?? 0) - (b.y ?? 0);
  const dz = (a.z ?? 0) - (b.z ?? 0);
  return Math.sqrt(dx * dx + dy * dy + dz * dz);
}

function sameGroup(a, b) {
  return a.group && b.group && a.group === b.group;
}

function canHear(sender, recipient) {
  if (!sender || !recipient) return false;
  if (sameGroup(sender, recipient)) return true;
  return distance(sender, recipient) <= PROXIMITY_BLOCKS;
}

function broadcastRoom(roomId, json, exceptWs) {
  const room = rooms.get(roomId);
  if (!room) return;
  const text = JSON.stringify(json);
  for (const entry of room.values()) {
    if (entry.ws !== exceptWs && entry.ws.readyState === 1) {
      entry.ws.send(text);
    }
  }
}

function participantList(roomId) {
  const room = rooms.get(roomId);
  if (!room) return [];
  return [...room.entries()].map(([id, e]) => ({
    id,
    name: e.name,
    x: e.x ?? 0,
    y: e.y ?? 0,
    z: e.z ?? 0,
    group: e.group || '',
  }));
}

function sendParticipants(roomId) {
  broadcastRoom(roomId, { t: 'participants', list: participantList(roomId) });
}

function attachVoice(server) {
  const wss = new WebSocketServer({ noServer: true });

  wss.on('connection', (ws) => {
    let roomId = null;
    let userId = null;

    ws.on('message', (data, isBinary) => {
      if (isBinary) {
        if (!roomId || !userId) return;
        const room = rooms.get(roomId);
        if (!room) return;
        const sender = room.get(userId);
        if (!sender) return;
        for (const [id, entry] of room.entries()) {
          if (id !== userId && canHear(sender, entry) && entry.ws.readyState === 1) {
            entry.ws.send(data, { binary: true });
          }
        }
        return;
      }

      let msg;
      try {
        msg = JSON.parse(data.toString());
      } catch {
        return;
      }

      if (msg.t === 'join') {
        if (msg.client !== 'prime' || !msg.room || !msg.id || !msg.name) {
          ws.send(JSON.stringify({ t: 'error', message: 'Prime Client required' }));
          ws.close();
          return;
        }
        roomId = msg.room;
        userId = msg.id;
        if (!rooms.has(roomId)) rooms.set(roomId, new Map());
        const room = rooms.get(roomId);
        const existing = room.get(userId);
        room.set(userId, {
          ws,
          name: msg.name,
          x: existing?.x ?? 0,
          y: existing?.y ?? 0,
          z: existing?.z ?? 0,
          group: msg.group || existing?.group || '',
        });
        ws.send(JSON.stringify({ t: 'participants', list: participantList(roomId) }));
        broadcastRoom(roomId, { t: 'participants', list: participantList(roomId) }, ws);
        return;
      }

      if (msg.t === 'group' && roomId && userId) {
        const room = rooms.get(roomId);
        const entry = room?.get(userId);
        if (entry) {
          entry.group = msg.group || '';
          sendParticipants(roomId);
        }
        return;
      }

      if (msg.t === 'pos' && roomId && userId) {
        const room = rooms.get(roomId);
        const entry = room?.get(userId);
        if (entry) {
          entry.x = msg.x ?? 0;
          entry.y = msg.y ?? 0;
          entry.z = msg.z ?? 0;
        }
        return;
      }

      if (msg.t === 'leave' && roomId && userId) {
        const room = rooms.get(roomId);
        room?.delete(userId);
        if (room && room.size === 0) rooms.delete(roomId);
        sendParticipants(roomId);
      }
    });

    ws.on('close', () => {
      if (!roomId || !userId) return;
      const room = rooms.get(roomId);
      room?.delete(userId);
      if (room && room.size === 0) rooms.delete(roomId);
      else sendParticipants(roomId);
    });
  });

  return {
    wss,
    proximity: PROXIMITY_BLOCKS,
    handleUpgrade(req, socket, head) {
      wss.handleUpgrade(req, socket, head, (ws) => wss.emit('connection', ws, req));
    },
  };
}

module.exports = { attachVoice };
