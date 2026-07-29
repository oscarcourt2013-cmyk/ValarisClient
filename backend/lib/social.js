/**
 * Social WebSocket — presence, friends events, chat, party.
 * Path: /social
 * Auth: first message { t: 'auth', token } OR ?token= query
 */
const { WebSocketServer } = require('ws');
const db = require('./db');

/** @type {Map<string, Set<import('ws').WebSocket>>} uuid -> sockets */
const socketsByUser = new Map();
/** @type {Map<import('ws').WebSocket, { uuid: string, client: string, lastPong: number }>} */
const socketMeta = new WeakMap();
/** @type {Map<string, { status: string, activity: string, serverAddress: string|null, client: string, updatedAt: number }>} */
const presence = new Map();

const CLIENT_RANK = { game: 2, launcher: 1, unknown: 0 };

function addSocket(uuid, ws) {
  if (!socketsByUser.has(uuid)) socketsByUser.set(uuid, new Set());
  socketsByUser.get(uuid).add(ws);
}

function removeSocket(uuid, ws) {
  const set = socketsByUser.get(uuid);
  if (!set) return;
  set.delete(ws);
  if (set.size === 0) socketsByUser.delete(uuid);
}

function liveClients(uuid) {
  const set = socketsByUser.get(uuid);
  if (!set) return [];
  const clients = [];
  for (const ws of set) {
    if (ws.readyState !== 1) continue;
    const meta = socketMeta.get(ws);
    if (meta) clients.push(meta.client);
  }
  return clients;
}

function bestClient(clients) {
  let best = null;
  let rank = -1;
  for (const c of clients) {
    const r = CLIENT_RANK[c] ?? 0;
    if (r > rank) {
      rank = r;
      best = c;
    }
  }
  return best;
}

function send(ws, payload) {
  if (ws.readyState === 1) ws.send(JSON.stringify(payload));
}

function sendToUser(uuid, payload) {
  const set = socketsByUser.get(uuid);
  if (!set) return;
  const text = JSON.stringify(payload);
  for (const ws of set) {
    if (ws.readyState === 1) ws.send(text);
  }
}

function friendUuids(uuid) {
  return db
    .listFriendships(uuid)
    .filter((f) => f.status === 'accepted')
    .map((f) => (f.fromUuid === uuid ? f.toUuid : f.fromUuid));
}

function broadcastToFriends(uuid, payload) {
  for (const friend of friendUuids(uuid)) {
    sendToUser(friend, payload);
  }
}

function presencePayload(uuid) {
  const p = presence.get(uuid);
  const user = db.getUser(uuid);
  return {
    t: 'presence',
    uuid,
    username: user?.username || 'Player',
    status: p?.status || 'offline',
    activity: p?.activity || '',
    serverAddress: p?.serverAddress || null,
    client: p?.client || null,
  };
}

function partyDto(party) {
  if (!party) return null;
  return {
    ...party,
    members: party.memberUuids.map((id) => ({
      uuid: id,
      username: db.getUser(id)?.username || 'Player',
      leader: id === party.leaderUuid,
    })),
  };
}

function snapshotFor(uuid) {
  const friends = friendUuids(uuid).map((fid) => {
    const user = db.getUser(fid);
    const p = presence.get(fid);
    return {
      uuid: fid,
      username: user?.username || 'Player',
      status: p?.status || 'offline',
      activity: p?.activity || '',
      serverAddress: p?.serverAddress || null,
      note: db.getFriendNote(uuid, fid) || '',
    };
  });
  const pending = db
    .listFriendships(uuid)
    .filter((f) => f.status === 'pending')
    .map((f) => ({
      id: f.id,
      fromUuid: f.fromUuid,
      toUuid: f.toUuid,
      fromUsername: db.getUser(f.fromUuid)?.username || 'Player',
      toUsername: db.getUser(f.toUuid)?.username || 'Player',
      incoming: f.toUuid === uuid,
    }));
  const partyInvites = db.listPartyInvitesFor(uuid).map((inv) => ({
    ...inv,
    fromUsername: db.getUser(inv.fromUuid)?.username || 'Player',
    party: partyDto(db.getParty(inv.partyId)),
  }));
  const party = db.getPartyForUser(uuid);
  return {
    t: 'snapshot',
    friends,
    pending,
    partyInvites,
    party: partyDto(party),
    self: presencePayload(uuid),
  };
}

function recomputePresence(uuid) {
  const clients = liveClients(uuid);
  if (clients.length === 0) {
    presence.delete(uuid);
    broadcastToFriends(uuid, {
      t: 'presence',
      uuid,
      username: db.getUser(uuid)?.username || 'Player',
      status: 'offline',
      activity: '',
      serverAddress: null,
      client: null,
    });
    return;
  }
  const client = bestClient(clients) || 'unknown';
  const prev = presence.get(uuid);
  const next = {
    status: client === 'game' ? prev?.status === 'in-game' ? 'in-game' : 'online' : 'online',
    activity:
      client === 'game'
        ? prev?.client === 'game'
          ? prev.activity || 'In game'
          : 'In game'
        : 'In launcher',
    serverAddress: client === 'game' ? prev?.serverAddress || null : null,
    client,
    updatedAt: Date.now(),
  };
  // Keep in-game activity/server if a game socket remains.
  if (client === 'game' && prev?.client === 'game') {
    next.status = prev.status || 'in-game';
    next.activity = prev.activity || 'In game';
    next.serverAddress = prev.serverAddress || null;
  }
  presence.set(uuid, next);
  broadcastToFriends(uuid, presencePayload(uuid));
  sendToUser(uuid, presencePayload(uuid));
}

function setPresence(uuid, patch) {
  const clients = liveClients(uuid);
  const prev = presence.get(uuid) || {
    status: 'online',
    activity: '',
    serverAddress: null,
    client: 'unknown',
    updatedAt: Date.now(),
  };
  const nextClient = patch.client || prev.client || 'unknown';
  const rank = (c) => CLIENT_RANK[c] ?? 0;
  // Ignore stale launcher updates while a live game socket exists.
  if (rank(nextClient) < rank(prev.client) && clients.includes('game') && nextClient === 'launcher') {
    return;
  }
  const status = patch.status || prev.status || 'online';
  presence.set(uuid, {
    ...prev,
    ...patch,
    status,
    client: nextClient,
    updatedAt: Date.now(),
  });
  broadcastToFriends(uuid, presencePayload(uuid));
  sendToUser(uuid, presencePayload(uuid));
}

function sendToClient(uuid, client, payload) {
  const set = socketsByUser.get(uuid);
  if (!set) return;
  const text = JSON.stringify(payload);
  for (const ws of set) {
    const meta = socketMeta.get(ws);
    if (meta?.client === client && ws.readyState === 1) {
      ws.send(text);
    }
  }
}

function attachSocial(server) {
  const wss = new WebSocketServer({ noServer: true });

  // Drop dead sockets that stop answering pings.
  const sweep = setInterval(() => {
    const now = Date.now();
    for (const [uuid, set] of socketsByUser) {
      for (const ws of [...set]) {
        const meta = socketMeta.get(ws);
        if (!meta) continue;
        if (now - meta.lastPong > 90_000) {
          try {
            ws.terminate();
          } catch {
            // ignore
          }
          removeSocket(uuid, ws);
          recomputePresence(uuid);
          continue;
        }
        if (ws.readyState === 1) {
          send(ws, { t: 'ping', ts: now });
        }
      }
    }
  }, 25_000);
  sweep.unref?.();

  wss.on('connection', (ws, req) => {
    let uuid = null;

    const url = new URL(req.url || '/', 'http://localhost');
    const queryToken = url.searchParams.get('token');

    function authed(token) {
      const session = db.resolveSession(token);
      if (!session) {
        send(ws, { t: 'error', message: 'Invalid session' });
        ws.close();
        return false;
      }
      uuid = session.uuid;
      socketMeta.set(ws, { uuid, client: session.client, lastPong: Date.now() });
      addSocket(uuid, ws);
      if (!presence.has(uuid) || !liveClients(uuid).includes('game')) {
        const clients = liveClients(uuid);
        const best = bestClient(clients) || session.client;
        presence.set(uuid, {
          status: 'online',
          activity: best === 'game' ? 'In game' : 'In launcher',
          serverAddress: null,
          client: best,
          updatedAt: Date.now(),
        });
      } else {
        recomputePresence(uuid);
      }
      send(ws, { t: 'ready', uuid });
      send(ws, snapshotFor(uuid));
      broadcastToFriends(uuid, presencePayload(uuid));
      return true;
    }

    if (queryToken) {
      if (!authed(queryToken)) return;
    }

    ws.on('message', (data) => {
      let msg;
      try {
        msg = JSON.parse(data.toString());
      } catch {
        return;
      }

      const meta = socketMeta.get(ws);
      if (meta) meta.lastPong = Date.now();

      if (msg.t === 'auth') {
        if (!uuid) authed(msg.token);
        return;
      }

      if (msg.t === 'pong') {
        return;
      }

      if (!uuid) {
        send(ws, { t: 'error', message: 'Authenticate first' });
        return;
      }

      if (msg.t === 'presence') {
        setPresence(uuid, {
          status: msg.status || 'online',
          activity: msg.activity || '',
          serverAddress: msg.serverAddress || null,
          client: socketMeta.get(ws)?.client || 'unknown',
        });
        return;
      }

      if (msg.t === 'ping') {
        send(ws, { t: 'pong', ts: Date.now() });
        return;
      }

      if (msg.t === 'typing' && msg.conversationId) {
        const conversation = db.getConversation(msg.conversationId);
        if (!conversation || !conversation.participantUuids.includes(uuid)) return;
        for (const other of conversation.participantUuids) {
          if (other === uuid) continue;
          if (db.isBlocked(uuid, other)) continue;
          sendToUser(other, {
            t: 'typing',
            conversationId: msg.conversationId,
            uuid,
            username: db.getUser(uuid)?.username,
          });
        }
      }
    });

    ws.on('close', () => {
      if (!uuid) return;
      removeSocket(uuid, ws);
      recomputePresence(uuid);
    });
  });

  return {
    wss,
    sendToUser,
    sendToClient,
    broadcastToFriends,
    friendUuids,
    presence,
    presencePayload,
    snapshotFor,
    partyDto,
    setPresence,
    recomputePresence,
    socketCount() {
      let n = 0;
      for (const set of socketsByUser.values()) n += set.size;
      return n;
    },
    handleUpgrade(req, socket, head) {
      wss.handleUpgrade(req, socket, head, (ws) => wss.emit('connection', ws, req));
    },
  };
}

module.exports = { attachSocial };
