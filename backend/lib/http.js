const fs = require('fs');
const path = require('path');
const https = require('https');
const crypto = require('crypto');
const { URL } = require('url');
const db = require('./db');
const rateLimit = require('./rateLimit');
const { loadNews, versionsManifest } = require('./news');
const ai = require('./ai');
const { catalogForUser } = require('./storeCatalog');

const UPLOAD_DIR = process.env.PRIME_UPLOAD_DIR || path.join(__dirname, '..', 'uploads');
const CRASH_DIR = process.env.PRIME_CRASH_DIR || path.join(UPLOAD_DIR, 'crashes');
const MAX_UPLOAD = 5 * 1024 * 1024;
const MAX_CRASH = 2 * 1024 * 1024;
const ALLOWED_TYPES = new Set(['image/png', 'image/jpeg', 'image/jpg', 'image/webp', 'image/gif']);
const BACKEND_VERSION = '2.1.4';
const STATS_SALT = process.env.STATS_SALT || 'prime-stats-v1';

function ensureUploadDir() {
  if (!fs.existsSync(UPLOAD_DIR)) fs.mkdirSync(UPLOAD_DIR, { recursive: true });
}

function ensureCrashDir() {
  if (!fs.existsSync(CRASH_DIR)) fs.mkdirSync(CRASH_DIR, { recursive: true });
}

function clientIp(req) {
  const fwd = req.headers['x-forwarded-for'];
  if (typeof fwd === 'string' && fwd.length) return fwd.split(',')[0].trim();
  return req.socket?.remoteAddress || 'unknown';
}

/** Short-lived IP fingerprint for stats dedupe — never persisted as raw IP. */
function cryptoHashIp(ip) {
  return crypto.createHash('sha256').update(`${STATS_SALT}|ip|${ip || 'unknown'}`).digest('hex').slice(0, 24);
}

function enforceRate(req, res, route, opts) {
  const result = rateLimit.check(clientIp(req), route, opts);
  if (!result.ok) {
    json(res, 429, { error: 'Too many requests', retryAfterMs: result.retryAfterMs });
    return false;
  }
  return true;
}

function json(res, status, body) {
  const raw = JSON.stringify(body);
  res.writeHead(status, {
    'Content-Type': 'application/json; charset=utf-8',
    'Access-Control-Allow-Origin': '*',
    'Access-Control-Allow-Headers': 'Content-Type, Authorization',
    'Access-Control-Allow-Methods': 'GET,POST,PUT,DELETE,OPTIONS',
  });
  res.end(raw);
}

function readBody(req, limit = 2 * 1024 * 1024) {
  return new Promise((resolve, reject) => {
    const chunks = [];
    let size = 0;
    req.on('data', (chunk) => {
      size += chunk.length;
      if (size > limit) {
        reject(new Error('Body too large'));
        req.destroy();
        return;
      }
      chunks.push(chunk);
    });
    req.on('end', () => resolve(Buffer.concat(chunks)));
    req.on('error', reject);
  });
}

function parseMultipart(buffer, boundary) {
  const parts = [];
  const sep = Buffer.from('--' + boundary);
  let start = buffer.indexOf(sep) + sep.length + 2;
  while (start < buffer.length) {
    const next = buffer.indexOf(sep, start);
    if (next < 0) break;
    let part = buffer.subarray(start, next - 2);
    const headerEnd = part.indexOf('\r\n\r\n');
    if (headerEnd < 0) {
      start = next + sep.length + 2;
      continue;
    }
    const headers = part.subarray(0, headerEnd).toString('utf8');
    const body = part.subarray(headerEnd + 4);
    const nameMatch = /name="([^"]+)"/.exec(headers);
    const fileMatch = /filename="([^"]+)"/.exec(headers);
    const typeMatch = /Content-Type:\s*([^\r\n]+)/i.exec(headers);
    parts.push({
      name: nameMatch?.[1] || '',
      filename: fileMatch?.[1] || null,
      contentType: typeMatch?.[1]?.trim() || 'application/octet-stream',
      data: body,
    });
    start = next + sep.length + 2;
  }
  return parts;
}

function bearer(req) {
  const h = req.headers.authorization || '';
  if (h.startsWith('Bearer ')) return h.slice(7).trim();
  return null;
}

function requireAuth(req, res) {
  const token = bearer(req);
  const session = db.resolveSession(token);
  if (!session) {
    json(res, 401, { error: 'Unauthorized' });
    return null;
  }
  return session;
}

function dashUuid(raw) {
  const u = String(raw || '').replace(/-/g, '').toLowerCase();
  if (u.length !== 32) return String(raw || '');
  return `${u.slice(0, 8)}-${u.slice(8, 12)}-${u.slice(12, 16)}-${u.slice(16, 20)}-${u.slice(20)}`;
}

function verifyMinecraftProfile(accessToken) {
  return new Promise((resolve) => {
    const req = https.request(
      {
        hostname: 'api.minecraftservices.com',
        path: '/minecraft/profile',
        method: 'GET',
        headers: {
          Authorization: `Bearer ${accessToken}`,
          Accept: 'application/json',
          'User-Agent': 'Prime-Backend/2.0',
        },
        timeout: 8000,
      },
      (res) => {
        const chunks = [];
        res.on('data', (c) => chunks.push(c));
        res.on('end', () => {
          if (res.statusCode !== 200) {
            resolve(null);
            return;
          }
          try {
            resolve(JSON.parse(Buffer.concat(chunks).toString('utf8')));
          } catch {
            resolve(null);
          }
        });
      }
    );
    req.on('error', () => resolve(null));
    req.on('timeout', () => {
      req.destroy();
      resolve(null);
    });
    req.end();
  });
}

function partyMembers(party) {
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

function broadcastParty(ctx, party, alsoNullTo) {
  if (party) {
    const dto = { t: 'party', party: partyMembers(party) };
    for (const u of party.memberUuids) ctx.social.sendToUser(u, dto);
  }
  if (alsoNullTo) {
    for (const u of alsoNullTo) ctx.social.sendToUser(u, { t: 'party', party: null });
  }
}

/**
 * @param {import('http').IncomingMessage} req
 * @param {import('http').ServerResponse} res
 * @param {{ social: any }} ctx
 */
async function handleHttp(req, res, ctx) {
  const url = new URL(req.url || '/', `http://${req.headers.host || 'localhost'}`);
  const pathname = url.pathname;

  if (req.method === 'OPTIONS') {
    json(res, 204, {});
    return true;
  }

  if (req.method === 'GET' && (pathname === '/' || pathname === '/health')) {
    const stats = db.dbStats();
    json(res, 200, {
      ok: true,
      service: 'prime-backend',
      version: BACKEND_VERSION,
      db: stats,
      ws: { social: ctx.social.socketCount?.() ?? 0 },
      voice: '/voice',
      social: '/social',
      api: '/v1',
    });
    return true;
  }

  if (req.method === 'GET' && pathname === '/v1/news') {
    json(res, 200, { items: loadNews() });
    return true;
  }

  if (req.method === 'GET' && pathname === '/v1/versions') {
    const manifest = versionsManifest();
    json(res, 200, { ...manifest, backend: BACKEND_VERSION });
    return true;
  }

  if (req.method === 'GET' && pathname === '/v1/stats') {
    json(res, 200, db.getPublicStats());
    return true;
  }

  if (req.method === 'POST' && pathname === '/v1/stats/download') {
    if (!enforceRate(req, res, 'stats-download', { limit: 20, windowMs: 60_000 })) return true;
    let body = {};
    try {
      const raw = (await readBody(req, 4 * 1024)).toString('utf8') || '{}';
      body = JSON.parse(raw || '{}');
    } catch {
      body = {};
    }
    const deviceId = body.deviceId ? String(body.deviceId).slice(0, 128) : '';
    const ipHash = cryptoHashIp(clientIp(req));
    const result = db.recordDownload([ipHash, deviceId]);
    json(res, 200, { ok: true, counted: result.counted, ...result.stats });
    return true;
  }

  if (req.method === 'POST' && pathname === '/v1/stats/launch') {
    if (!enforceRate(req, res, 'stats-launch', { limit: 30, windowMs: 60_000 })) return true;
    let body = {};
    try {
      const raw = (await readBody(req, 4 * 1024)).toString('utf8') || '{}';
      body = JSON.parse(raw || '{}');
    } catch {
      body = {};
    }
    const deviceId = body.deviceId ? String(body.deviceId).slice(0, 128) : '';
    const client = body.client ? String(body.client).slice(0, 32) : 'launcher';
    const ipHash = cryptoHashIp(clientIp(req));
    const result = db.recordLaunch([ipHash, deviceId, client]);
    json(res, 200, { ok: true, counted: result.counted, ...result.stats });
    return true;
  }

  if (req.method === 'GET' && pathname === '/v1/ai/status') {
    json(res, 200, {
      available: ai.isAvailable(),
      defaultModel: ai.DEFAULT_MODEL,
      models: [...ai.ALLOWED_MODELS],
    });
    return true;
  }

  if (req.method === 'POST' && pathname === '/v1/ai/chat') {
    if (!enforceRate(req, res, 'ai-chat', { limit: 20, windowMs: 60_000 })) return true;
    if (!ai.isAvailable()) {
      json(res, 503, { error: 'AI unavailable (server key not configured)' });
      return true;
    }
    let body;
    try {
      body = JSON.parse((await readBody(req, 512 * 1024)).toString('utf8') || '{}');
    } catch {
      json(res, 400, { error: 'Invalid JSON' });
      return true;
    }
    const result = await ai.proxyChat(body);
    if (!result.ok) {
      json(res, result.status || 502, { error: result.error || 'AI failed' });
      return true;
    }
    json(res, 200, { ok: true, message: result.message });
    return true;
  }

  if (req.method === 'GET' && pathname.startsWith('/uploads/')) {
    ensureUploadDir();
    const file = path.basename(pathname);
    const full = path.join(UPLOAD_DIR, file);
    if (!full.startsWith(UPLOAD_DIR) || !fs.existsSync(full)) {
      json(res, 404, { error: 'Not found' });
      return true;
    }
    const ext = path.extname(file).toLowerCase();
    const types = {
      '.png': 'image/png',
      '.jpg': 'image/jpeg',
      '.jpeg': 'image/jpeg',
      '.webp': 'image/webp',
      '.gif': 'image/gif',
    };
    res.writeHead(200, {
      'Content-Type': types[ext] || 'application/octet-stream',
      'Access-Control-Allow-Origin': '*',
      'Cache-Control': 'public, max-age=86400',
    });
    fs.createReadStream(full).pipe(res);
    return true;
  }

  if (!pathname.startsWith('/v1/')) return false;

  try {
    if (req.method === 'POST' && pathname === '/v1/auth/session') {
      const body = JSON.parse((await readBody(req)).toString('utf8') || '{}');
      const uuid = dashUuid(String(body.uuid || '').trim());
      const username = String(body.username || '').trim();
      const offline = !!body.offline;
      const client = String(body.client || 'unknown');
      const accessToken = body.accessToken ? String(body.accessToken).trim() : '';
      if (!uuid || !username) {
        json(res, 400, { error: 'uuid and username required' });
        return true;
      }

      let verified = false;
      if (!offline && accessToken) {
        if (!enforceRate(req, res, 'auth-verify', { limit: 20, windowMs: 60_000 })) return true;
        const profile = await verifyMinecraftProfile(accessToken);
        if (!profile?.id) {
          json(res, 401, { error: 'Invalid Minecraft access token' });
          return true;
        }
        const profileUuid = dashUuid(profile.id);
        if (profileUuid.toLowerCase() !== uuid.toLowerCase()) {
          json(res, 401, { error: 'UUID does not match Minecraft profile' });
          return true;
        }
        verified = true;
        if (profile.name) {
          // Prefer live Mojang name when verified.
          body.username = profile.name;
        }
      } else {
        const limit = offline ? 40 : 15;
        if (!enforceRate(req, res, offline ? 'auth-offline' : 'auth-unverified', {
          limit,
          windowMs: 60_000,
        })) {
          return true;
        }
      }

      const user = db.upsertUser(uuid, String(body.username || username).trim(), offline);
      const session = db.createSession(uuid, client, verified);
      json(res, 200, {
        token: session.token,
        expiresAt: session.expiresAt,
        verified: session.verified,
        user,
      });
      return true;
    }

    if (req.method === 'GET' && pathname === '/v1/me') {
      const session = requireAuth(req, res);
      if (!session) return true;
      const user = db.getUser(session.uuid);
      json(res, 200, {
        user,
        profile: db.getProfile(session.uuid),
        presence: ctx.social.presence.get(session.uuid) || null,
        verified: !!session.verified,
      });
      return true;
    }

    if (req.method === 'GET' && pathname.startsWith('/v1/profile/')) {
      const targetUuid = dashUuid(pathname.slice('/v1/profile/'.length));
      if (!targetUuid || targetUuid.includes('/')) {
        json(res, 404, { error: 'Not found' });
        return true;
      }
      const profile = db.getProfile(targetUuid);
      if (!profile) {
        json(res, 404, { error: 'Not found' });
        return true;
      }
      json(res, 200, { profile });
      return true;
    }

    // Store / currency
    if (req.method === 'GET' && pathname === '/v1/store/catalog') {
      const session = requireAuth(req, res);
      if (!session) return true;
      const owned = db.listOwnedItems(session.uuid);
      json(res, 200, { items: catalogForUser(owned), balance: db.getBalance(session.uuid) });
      return true;
    }

    if (req.method === 'GET' && pathname === '/v1/store/balance') {
      const session = requireAuth(req, res);
      if (!session) return true;
      json(res, 200, { balance: db.getBalance(session.uuid), currency: 'prime_coins' });
      return true;
    }

    if (req.method === 'POST' && pathname === '/v1/store/purchase') {
      if (!enforceRate(req, res, 'store-purchase', { limit: 30, windowMs: 60_000 })) return true;
      const session = requireAuth(req, res);
      if (!session) return true;
      const body = JSON.parse((await readBody(req)).toString('utf8') || '{}');
      const itemId = String(body.itemId || '').trim();
      if (!itemId) {
        json(res, 400, { error: 'itemId required' });
        return true;
      }
      const result = db.purchaseItem(session.uuid, itemId);
      json(res, 200, { ok: true, ...result });
      return true;
    }

    if (req.method === 'GET' && pathname === '/v1/store/history') {
      const session = requireAuth(req, res);
      if (!session) return true;
      const limit = parseInt(url.searchParams.get('limit') || '50', 10);
      json(res, 200, { history: db.listStoreHistory(session.uuid, limit) });
      return true;
    }

    if (req.method === 'POST' && pathname === '/v1/store/redeem') {
      if (!enforceRate(req, res, 'store-redeem', { limit: 10, windowMs: 60_000 })) return true;
      const session = requireAuth(req, res);
      if (!session) return true;
      const body = JSON.parse((await readBody(req)).toString('utf8') || '{}');
      const result = db.redeemCode(session.uuid, body.code);
      json(res, 200, { ok: true, ...result });
      return true;
    }

    // Cosmetics sync
    if (req.method === 'GET' && pathname === '/v1/cosmetics') {
      const session = requireAuth(req, res);
      if (!session) return true;
      json(res, 200, db.getCosmetics(session.uuid));
      return true;
    }

    if (req.method === 'PUT' && pathname === '/v1/cosmetics/equip') {
      const session = requireAuth(req, res);
      if (!session) return true;
      const body = JSON.parse((await readBody(req)).toString('utf8') || '{}');
      const result = db.equipCosmetics(session.uuid, body.ids || []);
      json(res, 200, { ok: true, ...result });
      return true;
    }

    // Settings sync
    if (req.method === 'GET' && pathname === '/v1/settings') {
      const session = requireAuth(req, res);
      if (!session) return true;
      json(res, 200, { settings: db.getSettings(session.uuid) });
      return true;
    }

    if (req.method === 'PUT' && pathname === '/v1/settings') {
      const session = requireAuth(req, res);
      if (!session) return true;
      const body = JSON.parse((await readBody(req, 256 * 1024)).toString('utf8') || '{}');
      const blob = body.settings && typeof body.settings === 'object' ? body.settings : body;
      const settings = db.putSettings(session.uuid, blob);
      json(res, 200, { ok: true, settings });
      return true;
    }

    // Crash list
    if (req.method === 'GET' && pathname === '/v1/crash') {
      const session = requireAuth(req, res);
      if (!session) return true;
      const limit = parseInt(url.searchParams.get('limit') || '25', 10);
      json(res, 200, { crashes: db.listCrashes(session.uuid, limit) });
      return true;
    }

    // Plugin bridge stub
    if (req.method === 'POST' && pathname === '/v1/network/event') {
      if (!enforceRate(req, res, 'network-event', { limit: 60, windowMs: 60_000 })) return true;
      const session = requireAuth(req, res);
      if (!session) return true;
      try {
        JSON.parse((await readBody(req, 64 * 1024)).toString('utf8') || '{}');
      } catch {
        json(res, 400, { error: 'Invalid JSON' });
        return true;
      }
      json(res, 200, { ok: true });
      return true;
    }

    // Friends
    if (req.method === 'GET' && pathname === '/v1/friends') {
      const session = requireAuth(req, res);
      if (!session) return true;
      const list = db.listFriendships(session.uuid).map((f) => {
        const other = f.fromUuid === session.uuid ? f.toUuid : f.fromUuid;
        const user = db.getUser(other);
        const p = ctx.social.presence.get(other);
        return {
          friendshipId: f.id,
          status: f.status,
          incoming: f.status === 'pending' && f.toUuid === session.uuid,
          uuid: other,
          username: user?.username || 'Player',
          note: db.getFriendNote(session.uuid, other) || '',
          presence: p
            ? { status: p.status, activity: p.activity, serverAddress: p.serverAddress }
            : { status: 'offline', activity: '', serverAddress: null },
          serverAddress: p?.serverAddress || null,
        };
      });
      json(res, 200, { friends: list });
      return true;
    }

    if (req.method === 'POST' && pathname === '/v1/friends/request') {
      if (!enforceRate(req, res, 'friend-request', { limit: 15, windowMs: 60_000 })) return true;
      const session = requireAuth(req, res);
      if (!session) return true;
      const body = JSON.parse((await readBody(req)).toString('utf8') || '{}');
      let targetUuid = body.uuid ? dashUuid(String(body.uuid)) : null;
      const username = body.username ? String(body.username).trim() : null;
      if (!targetUuid && username) {
        const found = db.findUserByName(username);
        if (found) targetUuid = found.uuid;
        else {
          json(res, 404, {
            error: 'User has never opened Prime yet. They must launch Prime Client or Launcher once.',
          });
          return true;
        }
      }
      if (!targetUuid) {
        json(res, 400, { error: 'uuid or username required' });
        return true;
      }
      if (db.isBlocked(session.uuid, targetUuid)) {
        json(res, 403, { error: 'Blocked' });
        return true;
      }
      const friendship = db.requestFriend(session.uuid, targetUuid);
      ctx.social.sendToUser(targetUuid, {
        t: 'friend_request',
        fromUuid: session.uuid,
        fromUsername: db.getUser(session.uuid)?.username,
        friendship,
      });
      ctx.social.sendToUser(session.uuid, { t: 'friend_update', friendship });
      json(res, 200, { friendship });
      return true;
    }

    if (req.method === 'POST' && pathname === '/v1/friends/accept') {
      const session = requireAuth(req, res);
      if (!session) return true;
      const body = JSON.parse((await readBody(req)).toString('utf8') || '{}');
      const otherUuid = dashUuid(String(body.uuid || ''));
      const friendship = db.acceptFriend(session.uuid, otherUuid);
      ctx.social.sendToUser(otherUuid, { t: 'friend_accepted', uuid: session.uuid, friendship });
      ctx.social.sendToUser(session.uuid, { t: 'friend_accepted', uuid: otherUuid, friendship });
      ctx.social.sendToUser(session.uuid, ctx.social.snapshotFor(session.uuid));
      ctx.social.sendToUser(otherUuid, ctx.social.snapshotFor(otherUuid));
      json(res, 200, { friendship });
      return true;
    }

    if (req.method === 'POST' && pathname === '/v1/friends/block') {
      const session = requireAuth(req, res);
      if (!session) return true;
      const body = JSON.parse((await readBody(req)).toString('utf8') || '{}');
      const otherUuid = dashUuid(String(body.uuid || ''));
      const friendship = db.blockUser(session.uuid, otherUuid);
      ctx.social.sendToUser(otherUuid, { t: 'friend_removed', uuid: session.uuid });
      ctx.social.sendToUser(session.uuid, { t: 'friend_update', friendship });
      json(res, 200, { friendship });
      return true;
    }

    if (req.method === 'DELETE' && pathname === '/v1/friends/block') {
      const session = requireAuth(req, res);
      if (!session) return true;
      const otherUuid = dashUuid(String(url.searchParams.get('uuid') || ''));
      db.unblockUser(session.uuid, otherUuid);
      json(res, 200, { ok: true });
      return true;
    }

    if (req.method === 'PUT' && pathname.startsWith('/v1/friends/') && pathname.endsWith('/note')) {
      const session = requireAuth(req, res);
      if (!session) return true;
      const otherUuid = dashUuid(pathname.slice('/v1/friends/'.length, -'/note'.length));
      const body = JSON.parse((await readBody(req)).toString('utf8') || '{}');
      const note = db.setFriendNote(session.uuid, otherUuid, body.note);
      json(res, 200, { note });
      return true;
    }

    if (req.method === 'DELETE' && pathname.startsWith('/v1/friends/')) {
      const session = requireAuth(req, res);
      if (!session) return true;
      const otherUuid = dashUuid(pathname.slice('/v1/friends/'.length));
      if (otherUuid.includes('/')) {
        json(res, 404, { error: 'Not found' });
        return true;
      }
      db.removeFriend(session.uuid, otherUuid);
      ctx.social.sendToUser(otherUuid, { t: 'friend_removed', uuid: session.uuid });
      ctx.social.sendToUser(session.uuid, { t: 'friend_removed', uuid: otherUuid });
      json(res, 200, { ok: true });
      return true;
    }

    // Chat
    if (req.method === 'GET' && pathname === '/v1/conversations') {
      const session = requireAuth(req, res);
      if (!session) return true;
      const conversations = db.listConversations(session.uuid).map((c) => {
        const others = c.participantUuids.filter((u) => u !== session.uuid);
        return {
          ...c,
          participants: others.map((u) => ({
            uuid: u,
            username: db.getUser(u)?.username || 'Player',
          })),
        };
      });
      json(res, 200, { conversations });
      return true;
    }

    if (req.method === 'POST' && pathname === '/v1/conversations/dm') {
      const session = requireAuth(req, res);
      if (!session) return true;
      const body = JSON.parse((await readBody(req)).toString('utf8') || '{}');
      const otherUuid = dashUuid(String(body.uuid || ''));
      if (db.isBlocked(session.uuid, otherUuid)) {
        json(res, 403, { error: 'Blocked' });
        return true;
      }
      if (!db.areFriends(session.uuid, otherUuid)) {
        json(res, 403, { error: 'Must be friends to DM' });
        return true;
      }
      const conversation = db.getOrCreateDm(session.uuid, otherUuid);
      json(res, 200, { conversation });
      return true;
    }

    if (req.method === 'GET' && pathname.startsWith('/v1/conversations/') && pathname.endsWith('/messages')) {
      const session = requireAuth(req, res);
      if (!session) return true;
      const conversationId = pathname.slice('/v1/conversations/'.length, -'/messages'.length);
      const conversation = db.getConversation(conversationId);
      if (!conversation || !conversation.participantUuids.includes(session.uuid)) {
        json(res, 404, { error: 'Not found' });
        return true;
      }
      const limit = Math.min(100, parseInt(url.searchParams.get('limit') || '50', 10));
      const before = url.searchParams.get('before') || undefined;
      const messages = db.listMessages(conversationId, limit, before).map((m) => ({
        ...m,
        senderUsername: db.getUser(m.senderUuid)?.username || null,
      }));
      json(res, 200, { messages });
      return true;
    }

    if (req.method === 'POST' && pathname.startsWith('/v1/conversations/') && pathname.endsWith('/messages')) {
      if (!enforceRate(req, res, 'chat-send', { limit: 30, windowMs: 60_000 })) return true;
      const session = requireAuth(req, res);
      if (!session) return true;
      const conversationId = pathname.slice('/v1/conversations/'.length, -'/messages'.length);
      const conversation = db.getConversation(conversationId);
      if (!conversation || !conversation.participantUuids.includes(session.uuid)) {
        json(res, 404, { error: 'Not found' });
        return true;
      }
      const body = JSON.parse((await readBody(req)).toString('utf8') || '{}');
      const text = String(body.text || '').slice(0, 2000);
      const imageUrl = body.imageUrl ? String(body.imageUrl).slice(0, 500) : null;
      if (!text && !imageUrl) {
        json(res, 400, { error: 'text or imageUrl required' });
        return true;
      }
      const message = db.addMessage(conversationId, session.uuid, text, imageUrl);
      const senderUsername = db.getUser(session.uuid)?.username || null;
      const enriched = { ...message, senderUsername };
      const event = { t: 'message', message: enriched };
      for (const u of conversation.participantUuids) {
        ctx.social.sendToUser(u, event);
      }
      json(res, 200, { message: enriched });
      return true;
    }

    // Upload
    if (req.method === 'POST' && pathname === '/v1/upload') {
      if (!enforceRate(req, res, 'upload', { limit: 15, windowMs: 60_000 })) return true;
      const session = requireAuth(req, res);
      if (!session) return true;
      const contentType = req.headers['content-type'] || '';
      const match = /boundary=(.+)$/i.exec(contentType);
      if (!match) {
        json(res, 400, { error: 'multipart/form-data required' });
        return true;
      }
      const raw = await readBody(req, MAX_UPLOAD + 64 * 1024);
      const parts = parseMultipart(raw, match[1].trim());
      const file = parts.find((p) => p.filename && p.name === 'file');
      if (!file) {
        json(res, 400, { error: 'file field required' });
        return true;
      }
      if (!ALLOWED_TYPES.has(file.contentType)) {
        json(res, 400, { error: 'Only png/jpeg/webp/gif allowed' });
        return true;
      }
      if (file.data.length > MAX_UPLOAD) {
        json(res, 400, { error: 'Max 5MB' });
        return true;
      }
      ensureUploadDir();
      const ext =
        file.contentType === 'image/png'
          ? '.png'
          : file.contentType === 'image/webp'
            ? '.webp'
            : file.contentType === 'image/gif'
              ? '.gif'
              : '.jpg';
      const name = `${require('crypto').randomUUID()}${ext}`;
      fs.writeFileSync(path.join(UPLOAD_DIR, name), file.data);
      const urlPath = `/uploads/${name}`;
      json(res, 200, { url: urlPath, fullUrl: urlPath });
      return true;
    }

    if (req.method === 'POST' && pathname === '/v1/crash') {
      if (!enforceRate(req, res, 'crash', { limit: 8, windowMs: 60_000 })) return true;
      const session = requireAuth(req, res);
      if (!session) return true;
      const body = JSON.parse((await readBody(req, MAX_CRASH + 64 * 1024)).toString('utf8') || '{}');
      const text = String(body.text || body.report || '').slice(0, MAX_CRASH);
      const title = String(body.title || 'crash').slice(0, 120);
      const version = body.version ? String(body.version).slice(0, 64) : null;
      if (!text.trim()) {
        json(res, 400, { error: 'text required' });
        return true;
      }
      ensureCrashDir();
      const crashId = require('crypto').randomUUID();
      const fileName = `${crashId}.log`;
      const createdAt = new Date().toISOString();
      const meta = {
        id: crashId,
        uuid: session.uuid,
        username: db.getUser(session.uuid)?.username || 'Player',
        title,
        version,
        createdAt,
        client: session.client,
      };
      fs.writeFileSync(path.join(CRASH_DIR, fileName), text, 'utf8');
      fs.writeFileSync(path.join(CRASH_DIR, `${crashId}.json`), JSON.stringify(meta, null, 2));
      db.recordCrash({
        id: crashId,
        uuid: session.uuid,
        title,
        version,
        client: session.client,
        fileName,
        createdAt,
      });
      json(res, 200, { ok: true, id: crashId, fileName });
      return true;
    }

    // Party
    if (req.method === 'GET' && pathname === '/v1/party') {
      const session = requireAuth(req, res);
      if (!session) return true;
      const party = db.getPartyForUser(session.uuid);
      const invites = db.listPartyInvitesFor(session.uuid).map((inv) => ({
        ...inv,
        fromUsername: db.getUser(inv.fromUuid)?.username || 'Player',
        party: partyMembers(db.getParty(inv.partyId)),
      }));
      json(res, 200, {
        party: partyMembers(party),
        invites,
      });
      return true;
    }

    if (req.method === 'POST' && pathname === '/v1/party') {
      const session = requireAuth(req, res);
      if (!session) return true;
      const party = db.createParty(session.uuid);
      ctx.social.sendToUser(session.uuid, { t: 'party', party: partyMembers(party) });
      json(res, 200, { party: partyMembers(party) });
      return true;
    }

    if (req.method === 'POST' && pathname === '/v1/party/invite') {
      const session = requireAuth(req, res);
      if (!session) return true;
      const body = JSON.parse((await readBody(req)).toString('utf8') || '{}');
      const targetUuid = dashUuid(String(body.uuid || ''));
      if (!db.areFriends(session.uuid, targetUuid)) {
        json(res, 403, { error: 'Can only invite friends' });
        return true;
      }
      if (db.isBlocked(session.uuid, targetUuid)) {
        json(res, 403, { error: 'Blocked' });
        return true;
      }
      let party = db.getPartyForUser(session.uuid);
      if (!party) party = db.createParty(session.uuid);
      const invite = db.createPartyInvite(party.id, session.uuid, targetUuid);
      ctx.social.sendToUser(targetUuid, {
        t: 'party_invite',
        inviteId: invite.id,
        fromUuid: session.uuid,
        fromUsername: db.getUser(session.uuid)?.username,
        partyId: party.id,
        serverAddress: party.serverAddress,
      });
      ctx.social.sendToUser(session.uuid, { t: 'party', party: partyMembers(party) });
      json(res, 200, { invite, party: partyMembers(party) });
      return true;
    }

    if (req.method === 'POST' && pathname === '/v1/party/accept') {
      const session = requireAuth(req, res);
      if (!session) return true;
      const body = JSON.parse((await readBody(req)).toString('utf8') || '{}');
      const inviteId = String(body.inviteId || body.id || '');
      const party = db.acceptPartyInvite(session.uuid, inviteId);
      broadcastParty(ctx, party);
      json(res, 200, { party: partyMembers(party) });
      return true;
    }

    if (req.method === 'POST' && pathname === '/v1/party/decline') {
      const session = requireAuth(req, res);
      if (!session) return true;
      const body = JSON.parse((await readBody(req)).toString('utf8') || '{}');
      const inviteId = String(body.inviteId || body.id || '');
      db.declinePartyInvite(session.uuid, inviteId);
      json(res, 200, { ok: true });
      return true;
    }

    if (req.method === 'POST' && pathname === '/v1/party/kick') {
      const session = requireAuth(req, res);
      if (!session) return true;
      const body = JSON.parse((await readBody(req)).toString('utf8') || '{}');
      const targetUuid = dashUuid(String(body.uuid || ''));
      const before = db.getPartyForUser(session.uuid);
      const party = db.kickFromParty(session.uuid, targetUuid);
      broadcastParty(ctx, party, [targetUuid]);
      if (before) {
        for (const u of before.memberUuids) {
          if (u !== targetUuid) ctx.social.sendToUser(u, { t: 'party', party: partyMembers(party) });
        }
      }
      json(res, 200, { party: partyMembers(party) });
      return true;
    }

    if (req.method === 'POST' && pathname === '/v1/party/leave') {
      const session = requireAuth(req, res);
      if (!session) return true;
      const before = db.getPartyForUser(session.uuid);
      const party = db.leaveParty(session.uuid);
      if (before) {
        for (const u of before.memberUuids) {
          if (u === session.uuid) continue;
          ctx.social.sendToUser(u, { t: 'party', party: partyMembers(party) });
        }
      }
      ctx.social.sendToUser(session.uuid, { t: 'party', party: null });
      json(res, 200, { ok: true });
      return true;
    }

    if (req.method === 'POST' && pathname === '/v1/party/server') {
      const session = requireAuth(req, res);
      if (!session) return true;
      const body = JSON.parse((await readBody(req)).toString('utf8') || '{}');
      const party = db.setPartyServer(session.uuid, body.serverAddress || null);
      for (const u of party.memberUuids) {
        ctx.social.sendToUser(u, { t: 'party', party: partyMembers(party) });
        if (u !== session.uuid && party.serverAddress) {
          ctx.social.sendToUser(u, {
            t: 'party_join_server',
            serverAddress: party.serverAddress,
            fromUuid: session.uuid,
            fromUsername: db.getUser(session.uuid)?.username,
          });
        }
      }
      json(res, 200, { party: partyMembers(party) });
      return true;
    }

    json(res, 404, { error: 'Not found' });
    return true;
  } catch (err) {
    json(res, 400, { error: err instanceof Error ? err.message : 'Bad request' });
    return true;
  }
}

module.exports = { handleHttp, UPLOAD_DIR, BACKEND_VERSION };
