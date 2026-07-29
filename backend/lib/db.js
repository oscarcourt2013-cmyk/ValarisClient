/**
 * Prime Backend persistence — SQLite (better-sqlite3).
 * Migrates once from legacy data/prime.json when present.
 */
const fs = require('fs');
const path = require('path');
const crypto = require('crypto');
const Database = require('better-sqlite3');

const DATA_DIR = process.env.PRIME_DATA_DIR || path.join(__dirname, '..', 'data');
const SQLITE_PATH = path.join(DATA_DIR, 'prime.db');
const JSON_PATH = path.join(DATA_DIR, 'prime.json');

function ensureDir(dir) {
  if (!fs.existsSync(dir)) fs.mkdirSync(dir, { recursive: true });
}

ensureDir(DATA_DIR);

const sql = new Database(SQLITE_PATH);
sql.pragma('journal_mode = WAL');
sql.pragma('foreign_keys = ON');

sql.exec(`
CREATE TABLE IF NOT EXISTS users (
  uuid TEXT PRIMARY KEY,
  username TEXT NOT NULL,
  offline INTEGER NOT NULL DEFAULT 0,
  created_at TEXT NOT NULL,
  last_seen_at TEXT NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_users_username ON users(username COLLATE NOCASE);

CREATE TABLE IF NOT EXISTS sessions (
  token TEXT PRIMARY KEY,
  uuid TEXT NOT NULL,
  client TEXT NOT NULL,
  verified INTEGER NOT NULL DEFAULT 0,
  expires_at INTEGER NOT NULL,
  created_at TEXT NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_sessions_uuid_client ON sessions(uuid, client);

CREATE TABLE IF NOT EXISTS friendships (
  id TEXT PRIMARY KEY,
  from_uuid TEXT NOT NULL,
  to_uuid TEXT NOT NULL,
  status TEXT NOT NULL,
  created_at TEXT NOT NULL,
  UNIQUE(from_uuid, to_uuid)
);
CREATE INDEX IF NOT EXISTS idx_friendships_from ON friendships(from_uuid);
CREATE INDEX IF NOT EXISTS idx_friendships_to ON friendships(to_uuid);

CREATE TABLE IF NOT EXISTS user_notes (
  owner_uuid TEXT NOT NULL,
  target_uuid TEXT NOT NULL,
  note TEXT NOT NULL DEFAULT '',
  updated_at TEXT NOT NULL,
  PRIMARY KEY (owner_uuid, target_uuid)
);

CREATE TABLE IF NOT EXISTS conversations (
  id TEXT PRIMARY KEY,
  type TEXT NOT NULL,
  participant_uuids TEXT NOT NULL,
  created_at TEXT NOT NULL,
  updated_at TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS messages (
  id TEXT PRIMARY KEY,
  conversation_id TEXT NOT NULL,
  sender_uuid TEXT NOT NULL,
  text TEXT NOT NULL DEFAULT '',
  image_url TEXT,
  created_at TEXT NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_messages_conv ON messages(conversation_id, created_at);

CREATE TABLE IF NOT EXISTS parties (
  id TEXT PRIMARY KEY,
  leader_uuid TEXT NOT NULL,
  member_uuids TEXT NOT NULL,
  server_address TEXT,
  created_at TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS party_invites (
  id TEXT PRIMARY KEY,
  party_id TEXT NOT NULL,
  from_uuid TEXT NOT NULL,
  to_uuid TEXT NOT NULL,
  created_at TEXT NOT NULL,
  UNIQUE(party_id, to_uuid)
);
CREATE INDEX IF NOT EXISTS idx_party_invites_to ON party_invites(to_uuid);
`);

const { DEFAULT_OWNED, DEFAULT_EQUIPPED, getCatalogItem, PROMO_CODES } = require('./storeCatalog');

function columnExists(table, column) {
  return sql.prepare(`PRAGMA table_info(${table})`).all().some((c) => c.name === column);
}

function addColumnIfMissing(table, column, typedef) {
  if (!columnExists(table, column)) {
    sql.exec(`ALTER TABLE ${table} ADD COLUMN ${column} ${typedef}`);
  }
}

/** Safe additive migrations for existing prime.db installs. */
function migrateSchema() {
  addColumnIfMissing('users', 'playtime_minutes', 'INTEGER');
  addColumnIfMissing('users', 'tier', "TEXT NOT NULL DEFAULT 'free'");
  addColumnIfMissing('users', 'badges', "TEXT NOT NULL DEFAULT '[]'");
  addColumnIfMissing('users', 'bio', 'TEXT');
  addColumnIfMissing('users', 'prime_coins', 'INTEGER NOT NULL DEFAULT 0');
  addColumnIfMissing('users', 'cosmetics_equipped', "TEXT NOT NULL DEFAULT '[]'");
  addColumnIfMissing('users', 'settings_json', "TEXT NOT NULL DEFAULT '{}'");

  sql.exec(`
CREATE TABLE IF NOT EXISTS store_ownership (
  user_uuid TEXT NOT NULL,
  item_id TEXT NOT NULL,
  purchased_at TEXT NOT NULL,
  PRIMARY KEY (user_uuid, item_id)
);
CREATE INDEX IF NOT EXISTS idx_store_ownership_user ON store_ownership(user_uuid);

CREATE TABLE IF NOT EXISTS store_history (
  id TEXT PRIMARY KEY,
  user_uuid TEXT NOT NULL,
  kind TEXT NOT NULL,
  item_id TEXT,
  amount INTEGER NOT NULL DEFAULT 0,
  code TEXT,
  created_at TEXT NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_store_history_user ON store_history(user_uuid, created_at);

CREATE TABLE IF NOT EXISTS crashes (
  id TEXT PRIMARY KEY,
  user_uuid TEXT NOT NULL,
  title TEXT NOT NULL DEFAULT 'crash',
  version TEXT,
  client TEXT,
  file_name TEXT,
  created_at TEXT NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_crashes_user ON crashes(user_uuid, created_at);

CREATE TABLE IF NOT EXISTS usage_stats (
  key TEXT PRIMARY KEY,
  value INTEGER NOT NULL DEFAULT 0,
  updated_at TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS stats_dedupe (
  kind TEXT NOT NULL,
  hash TEXT NOT NULL,
  expires_at INTEGER NOT NULL,
  PRIMARY KEY (kind, hash)
);
CREATE INDEX IF NOT EXISTS idx_stats_dedupe_expires ON stats_dedupe(expires_at);
`);

  const seedAt = nowIso();
  sql.prepare(
    `INSERT OR IGNORE INTO usage_stats (key, value, updated_at) VALUES (?, 0, ?)`
  ).run('downloads', seedAt);
  sql.prepare(
    `INSERT OR IGNORE INTO usage_stats (key, value, updated_at) VALUES (?, 0, ?)`
  ).run('launches', seedAt);
}

migrateSchema();

function id() {
  return crypto.randomUUID();
}

function nowIso() {
  return new Date().toISOString();
}

function parseJsonArray(raw, fallback = []) {
  if (!raw) return [...fallback];
  try {
    const parsed = JSON.parse(raw);
    return Array.isArray(parsed) ? parsed.map(String) : [...fallback];
  } catch {
    return [...fallback];
  }
}

function parseJsonObject(raw, fallback = {}) {
  if (!raw) return { ...fallback };
  try {
    const parsed = JSON.parse(raw);
    return parsed && typeof parsed === 'object' && !Array.isArray(parsed) ? parsed : { ...fallback };
  } catch {
    return { ...fallback };
  }
}

function normalizeTier(tier) {
  const t = String(tier || 'free').toLowerCase();
  if (t === 'prime' || t === 'prime_plus') return t;
  return 'free';
}

function mapUser(row) {
  if (!row) return null;
  const badges = parseJsonArray(row.badges, []);
  return {
    uuid: row.uuid,
    username: row.username,
    offline: !!row.offline,
    createdAt: row.created_at,
    lastSeenAt: row.last_seen_at,
    playtimeMinutes: row.playtime_minutes == null ? null : Number(row.playtime_minutes) || 0,
    tier: normalizeTier(row.tier),
    badges,
    bio: row.bio || null,
    primeCoins: Number(row.prime_coins) || 0,
  };
}

function mapProfile(row) {
  const user = mapUser(row);
  if (!user) return null;
  return {
    uuid: user.uuid,
    username: user.username,
    createdAt: user.createdAt,
    playtimeMinutes: user.playtimeMinutes,
    tier: user.tier,
    badges: user.badges,
    bio: user.bio,
  };
}

function mapFriendship(row) {
  if (!row) return null;
  return {
    id: row.id,
    fromUuid: row.from_uuid,
    toUuid: row.to_uuid,
    status: row.status,
    createdAt: row.created_at,
  };
}

function mapConversation(row) {
  if (!row) return null;
  return {
    id: row.id,
    type: row.type,
    participantUuids: JSON.parse(row.participant_uuids),
    createdAt: row.created_at,
    updatedAt: row.updated_at,
  };
}

function mapMessage(row) {
  if (!row) return null;
  return {
    id: row.id,
    conversationId: row.conversation_id,
    senderUuid: row.sender_uuid,
    text: row.text || '',
    imageUrl: row.image_url || null,
    createdAt: row.created_at,
  };
}

function mapParty(row) {
  if (!row) return null;
  return {
    id: row.id,
    leaderUuid: row.leader_uuid,
    memberUuids: JSON.parse(row.member_uuids),
    serverAddress: row.server_address || null,
    createdAt: row.created_at,
  };
}

function mapInvite(row) {
  if (!row) return null;
  return {
    id: row.id,
    partyId: row.party_id,
    fromUuid: row.from_uuid,
    toUuid: row.to_uuid,
    createdAt: row.created_at,
  };
}

function migrateFromJsonIfNeeded() {
  const userCount = sql.prepare('SELECT COUNT(*) AS c FROM users').get().c;
  if (userCount > 0 || !fs.existsSync(JSON_PATH)) return false;
  let raw;
  try {
    raw = JSON.parse(fs.readFileSync(JSON_PATH, 'utf8'));
  } catch {
    return false;
  }
  const tx = sql.transaction(() => {
    for (const u of Object.values(raw.users || {})) {
      sql.prepare(
        `INSERT OR IGNORE INTO users (uuid, username, offline, created_at, last_seen_at)
         VALUES (?, ?, ?, ?, ?)`
      ).run(u.uuid, u.username || 'Player', u.offline ? 1 : 0, u.createdAt || nowIso(), u.lastSeenAt || nowIso());
    }
    for (const [token, s] of Object.entries(raw.sessions || {})) {
      sql.prepare(
        `INSERT OR IGNORE INTO sessions (token, uuid, client, verified, expires_at, created_at)
         VALUES (?, ?, ?, 0, ?, ?)`
      ).run(token, s.uuid, s.client || 'unknown', s.expiresAt || 0, nowIso());
    }
    for (const f of Object.values(raw.friendships || {})) {
      sql.prepare(
        `INSERT OR IGNORE INTO friendships (id, from_uuid, to_uuid, status, created_at)
         VALUES (?, ?, ?, ?, ?)`
      ).run(f.id || id(), f.fromUuid, f.toUuid, f.status || 'pending', f.createdAt || nowIso());
    }
    for (const c of Object.values(raw.conversations || {})) {
      sql.prepare(
        `INSERT OR IGNORE INTO conversations (id, type, participant_uuids, created_at, updated_at)
         VALUES (?, ?, ?, ?, ?)`
      ).run(
        c.id,
        c.type || 'dm',
        JSON.stringify(c.participantUuids || []),
        c.createdAt || nowIso(),
        c.updatedAt || nowIso()
      );
    }
    for (const m of Object.values(raw.messages || {})) {
      sql.prepare(
        `INSERT OR IGNORE INTO messages (id, conversation_id, sender_uuid, text, image_url, created_at)
         VALUES (?, ?, ?, ?, ?, ?)`
      ).run(m.id, m.conversationId, m.senderUuid, m.text || '', m.imageUrl || null, m.createdAt || nowIso());
    }
    for (const p of Object.values(raw.parties || {})) {
      sql.prepare(
        `INSERT OR IGNORE INTO parties (id, leader_uuid, member_uuids, server_address, created_at)
         VALUES (?, ?, ?, ?, ?)`
      ).run(
        p.id,
        p.leaderUuid,
        JSON.stringify(p.memberUuids || []),
        p.serverAddress || null,
        p.createdAt || nowIso()
      );
    }
  });
  tx();
  try {
    fs.copyFileSync(JSON_PATH, JSON_PATH + '.migrated.bak');
  } catch {
    // ignore
  }
  console.log('[db] Migrated legacy prime.json → prime.db');
  return true;
}

migrateFromJsonIfNeeded();

function upsertUser(uuid, username, offline) {
  const existing = getUser(uuid);
  const created = existing?.createdAt || nowIso();
  const seen = nowIso();
  sql.prepare(
    `INSERT INTO users (uuid, username, offline, created_at, last_seen_at)
     VALUES (?, ?, ?, ?, ?)
     ON CONFLICT(uuid) DO UPDATE SET
       username = excluded.username,
       offline = excluded.offline,
       last_seen_at = excluded.last_seen_at`
  ).run(uuid, username || existing?.username || 'Player', offline ? 1 : 0, created, seen);
  return getUser(uuid);
}

function getUser(uuid) {
  return mapUser(sql.prepare('SELECT * FROM users WHERE uuid = ?').get(uuid));
}

function findUserByName(username) {
  return mapUser(
    sql.prepare('SELECT * FROM users WHERE username = ? COLLATE NOCASE LIMIT 1').get(username)
  );
}

function createSession(uuid, client, verified = false) {
  const token = crypto.randomBytes(32).toString('hex');
  const expiresAt = Date.now() + 7 * 24 * 60 * 60 * 1000;
  const clientKey = client || 'unknown';
  sql.prepare('DELETE FROM sessions WHERE uuid = ? AND client = ?').run(uuid, clientKey);
  sql.prepare(
    `INSERT INTO sessions (token, uuid, client, verified, expires_at, created_at)
     VALUES (?, ?, ?, ?, ?, ?)`
  ).run(token, uuid, clientKey, verified ? 1 : 0, expiresAt, nowIso());
  return { token, expiresAt, client: clientKey, verified: !!verified };
}

function resolveSession(token) {
  if (!token) return null;
  const row = sql.prepare('SELECT * FROM sessions WHERE token = ?').get(token);
  if (!row) return null;
  if (row.expires_at < Date.now()) {
    sql.prepare('DELETE FROM sessions WHERE token = ?').run(token);
    return null;
  }
  return {
    uuid: row.uuid,
    client: row.client,
    verified: !!row.verified,
    expiresAt: row.expires_at,
  };
}

function friendshipKeyPair(a, b) {
  return a < b ? [a, b] : [b, a];
}

function getFriendship(a, b) {
  const row = sql
    .prepare(
      `SELECT * FROM friendships
       WHERE (from_uuid = ? AND to_uuid = ?) OR (from_uuid = ? AND to_uuid = ?)
       LIMIT 1`
    )
    .get(a, b, b, a);
  return mapFriendship(row);
}

function listFriendships(uuid) {
  return sql
    .prepare('SELECT * FROM friendships WHERE from_uuid = ? OR to_uuid = ?')
    .all(uuid, uuid)
    .map(mapFriendship);
}

function isBlocked(a, b) {
  const f = getFriendship(a, b);
  return !!(f && f.status === 'blocked');
}

function requestFriend(fromUuid, toUuid) {
  if (fromUuid === toUuid) throw new Error('Cannot friend yourself');
  const existing = getFriendship(fromUuid, toUuid);
  if (existing?.status === 'accepted') throw new Error('Already friends');
  if (existing?.status === 'blocked') throw new Error('Blocked');
  if (existing?.status === 'pending') {
    if (existing.toUuid === fromUuid) {
      sql.prepare(`UPDATE friendships SET status = 'accepted' WHERE id = ?`).run(existing.id);
      return getFriendship(fromUuid, toUuid);
    }
    throw new Error('Request already pending');
  }
  if (existing) {
    sql.prepare('DELETE FROM friendships WHERE id = ?').run(existing.id);
  }
  const friendship = {
    id: id(),
    fromUuid,
    toUuid,
    status: 'pending',
    createdAt: nowIso(),
  };
  sql.prepare(
    `INSERT INTO friendships (id, from_uuid, to_uuid, status, created_at) VALUES (?, ?, ?, ?, ?)`
  ).run(friendship.id, fromUuid, toUuid, 'pending', friendship.createdAt);
  return friendship;
}

function acceptFriend(uuid, otherUuid) {
  const f = getFriendship(uuid, otherUuid);
  if (!f || f.status !== 'pending') throw new Error('No pending request');
  if (f.toUuid !== uuid) throw new Error('Not your request to accept');
  sql.prepare(`UPDATE friendships SET status = 'accepted' WHERE id = ?`).run(f.id);
  return getFriendship(uuid, otherUuid);
}

function removeFriend(uuid, otherUuid) {
  const f = getFriendship(uuid, otherUuid);
  if (!f) throw new Error('Not friends');
  sql.prepare('DELETE FROM friendships WHERE id = ?').run(f.id);
}

function blockUser(uuid, otherUuid) {
  if (uuid === otherUuid) throw new Error('Cannot block yourself');
  const existing = getFriendship(uuid, otherUuid);
  if (existing) {
    sql.prepare(`UPDATE friendships SET status = 'blocked', from_uuid = ?, to_uuid = ? WHERE id = ?`).run(
      uuid,
      otherUuid,
      existing.id
    );
    return getFriendship(uuid, otherUuid);
  }
  const friendship = {
    id: id(),
    fromUuid: uuid,
    toUuid: otherUuid,
    status: 'blocked',
    createdAt: nowIso(),
  };
  sql.prepare(
    `INSERT INTO friendships (id, from_uuid, to_uuid, status, created_at) VALUES (?, ?, ?, ?, ?)`
  ).run(friendship.id, uuid, otherUuid, 'blocked', friendship.createdAt);
  return friendship;
}

function unblockUser(uuid, otherUuid) {
  const f = getFriendship(uuid, otherUuid);
  if (!f || f.status !== 'blocked') throw new Error('Not blocked');
  if (f.fromUuid !== uuid) throw new Error('You did not block this user');
  sql.prepare('DELETE FROM friendships WHERE id = ?').run(f.id);
}

function areFriends(a, b) {
  const f = getFriendship(a, b);
  return !!(f && f.status === 'accepted');
}

function setFriendNote(ownerUuid, targetUuid, note) {
  const text = String(note || '').slice(0, 200);
  sql.prepare(
    `INSERT INTO user_notes (owner_uuid, target_uuid, note, updated_at)
     VALUES (?, ?, ?, ?)
     ON CONFLICT(owner_uuid, target_uuid) DO UPDATE SET note = excluded.note, updated_at = excluded.updated_at`
  ).run(ownerUuid, targetUuid, text, nowIso());
  return { ownerUuid, targetUuid, note: text };
}

function getFriendNote(ownerUuid, targetUuid) {
  const row = sql
    .prepare('SELECT note FROM user_notes WHERE owner_uuid = ? AND target_uuid = ?')
    .get(ownerUuid, targetUuid);
  return row?.note || '';
}

function getOrCreateDm(a, b) {
  if (isBlocked(a, b)) throw new Error('Blocked');
  const sorted = [a, b].sort();
  const rows = sql.prepare(`SELECT * FROM conversations WHERE type = 'dm'`).all();
  for (const row of rows) {
    const parts = JSON.parse(row.participant_uuids);
    if (parts.length === 2 && parts.includes(sorted[0]) && parts.includes(sorted[1])) {
      return mapConversation(row);
    }
  }
  const conversation = {
    id: id(),
    type: 'dm',
    participantUuids: sorted,
    createdAt: nowIso(),
    updatedAt: nowIso(),
  };
  sql.prepare(
    `INSERT INTO conversations (id, type, participant_uuids, created_at, updated_at)
     VALUES (?, ?, ?, ?, ?)`
  ).run(
    conversation.id,
    'dm',
    JSON.stringify(sorted),
    conversation.createdAt,
    conversation.updatedAt
  );
  return conversation;
}

function listConversations(uuid) {
  return sql
    .prepare('SELECT * FROM conversations ORDER BY updated_at DESC')
    .all()
    .map(mapConversation)
    .filter((c) => c.participantUuids.includes(uuid));
}

function getConversation(conversationId) {
  return mapConversation(sql.prepare('SELECT * FROM conversations WHERE id = ?').get(conversationId));
}

function addMessage(conversationId, senderUuid, text, imageUrl) {
  const conversation = getConversation(conversationId);
  if (!conversation) throw new Error('Conversation not found');
  if (!conversation.participantUuids.includes(senderUuid)) {
    throw new Error('Not a participant');
  }
  for (const other of conversation.participantUuids) {
    if (other !== senderUuid && isBlocked(senderUuid, other)) {
      throw new Error('Blocked');
    }
  }
  const message = {
    id: id(),
    conversationId,
    senderUuid,
    text: text || '',
    imageUrl: imageUrl || null,
    createdAt: nowIso(),
  };
  sql.prepare(
    `INSERT INTO messages (id, conversation_id, sender_uuid, text, image_url, created_at)
     VALUES (?, ?, ?, ?, ?, ?)`
  ).run(message.id, conversationId, senderUuid, message.text, message.imageUrl, message.createdAt);
  sql.prepare(`UPDATE conversations SET updated_at = ? WHERE id = ?`).run(message.createdAt, conversationId);
  return message;
}

function listMessages(conversationId, limit = 50, before) {
  let rows;
  if (before) {
    rows = sql
      .prepare(
        `SELECT * FROM messages WHERE conversation_id = ? AND created_at < ?
         ORDER BY created_at DESC LIMIT ?`
      )
      .all(conversationId, before, limit);
  } else {
    rows = sql
      .prepare(
        `SELECT * FROM messages WHERE conversation_id = ?
         ORDER BY created_at DESC LIMIT ?`
      )
      .all(conversationId, limit);
  }
  return rows.map(mapMessage).reverse();
}

function createParty(leaderUuid) {
  const existing = getPartyForUser(leaderUuid);
  if (existing) {
    leaveParty(leaderUuid);
  }
  const party = {
    id: id(),
    leaderUuid,
    memberUuids: [leaderUuid],
    serverAddress: null,
    createdAt: nowIso(),
  };
  sql.prepare(
    `INSERT INTO parties (id, leader_uuid, member_uuids, server_address, created_at)
     VALUES (?, ?, ?, ?, ?)`
  ).run(party.id, leaderUuid, JSON.stringify(party.memberUuids), null, party.createdAt);
  return party;
}

function getParty(partyId) {
  return mapParty(sql.prepare('SELECT * FROM parties WHERE id = ?').get(partyId));
}

function getPartyForUser(uuid) {
  const rows = sql.prepare('SELECT * FROM parties').all();
  for (const row of rows) {
    const members = JSON.parse(row.member_uuids);
    if (members.includes(uuid)) return mapParty(row);
  }
  return null;
}

function saveParty(party) {
  if (!party || party.memberUuids.length === 0) {
    if (party) sql.prepare('DELETE FROM parties WHERE id = ?').run(party.id);
    return null;
  }
  sql.prepare(
    `UPDATE parties SET leader_uuid = ?, member_uuids = ?, server_address = ? WHERE id = ?`
  ).run(party.leaderUuid, JSON.stringify(party.memberUuids), party.serverAddress || null, party.id);
  return getParty(party.id);
}

function createPartyInvite(partyId, fromUuid, toUuid) {
  if (isBlocked(fromUuid, toUuid)) throw new Error('Blocked');
  const party = getParty(partyId);
  if (!party) throw new Error('Party not found');
  if (party.leaderUuid !== fromUuid && !party.memberUuids.includes(fromUuid)) {
    throw new Error('Not in party');
  }
  if (party.memberUuids.includes(toUuid)) throw new Error('Already in party');
  if (party.memberUuids.length >= 8) throw new Error('Party full');
  sql.prepare('DELETE FROM party_invites WHERE party_id = ? AND to_uuid = ?').run(partyId, toUuid);
  const invite = {
    id: id(),
    partyId,
    fromUuid,
    toUuid,
    createdAt: nowIso(),
  };
  sql.prepare(
    `INSERT INTO party_invites (id, party_id, from_uuid, to_uuid, created_at) VALUES (?, ?, ?, ?, ?)`
  ).run(invite.id, partyId, fromUuid, toUuid, invite.createdAt);
  return invite;
}

function listPartyInvitesFor(uuid) {
  return sql
    .prepare('SELECT * FROM party_invites WHERE to_uuid = ? ORDER BY created_at DESC')
    .all(uuid)
    .map(mapInvite);
}

function getPartyInvite(inviteId) {
  return mapInvite(sql.prepare('SELECT * FROM party_invites WHERE id = ?').get(inviteId));
}

function declinePartyInvite(uuid, inviteId) {
  const invite = getPartyInvite(inviteId);
  if (!invite || invite.toUuid !== uuid) throw new Error('Invite not found');
  sql.prepare('DELETE FROM party_invites WHERE id = ?').run(inviteId);
  return invite;
}

function acceptPartyInvite(uuid, inviteId) {
  const invite = getPartyInvite(inviteId);
  if (!invite || invite.toUuid !== uuid) throw new Error('Invite not found');
  const party = getParty(invite.partyId);
  if (!party) {
    sql.prepare('DELETE FROM party_invites WHERE id = ?').run(inviteId);
    throw new Error('Party not found');
  }
  if (party.memberUuids.length >= 8) throw new Error('Party full');
  const existing = getPartyForUser(uuid);
  if (existing && existing.id !== party.id) {
    leaveParty(uuid);
  }
  if (!party.memberUuids.includes(uuid)) {
    party.memberUuids.push(uuid);
    saveParty(party);
  }
  sql.prepare('DELETE FROM party_invites WHERE id = ?').run(inviteId);
  sql.prepare('DELETE FROM party_invites WHERE to_uuid = ?').run(uuid);
  return getParty(party.id);
}

/** @deprecated force-join — prefer createPartyInvite + accept */
function inviteToParty(partyId, leaderUuid, targetUuid) {
  const invite = createPartyInvite(partyId, leaderUuid, targetUuid);
  return { invite, party: getParty(partyId) };
}

function leaveParty(uuid) {
  const party = getPartyForUser(uuid);
  if (!party) return null;
  party.memberUuids = party.memberUuids.filter((u) => u !== uuid);
  if (party.memberUuids.length === 0) {
    sql.prepare('DELETE FROM parties WHERE id = ?').run(party.id);
    sql.prepare('DELETE FROM party_invites WHERE party_id = ?').run(party.id);
    return null;
  }
  if (party.leaderUuid === uuid) {
    party.leaderUuid = party.memberUuids[0];
  }
  return saveParty(party);
}

function kickFromParty(leaderUuid, targetUuid) {
  const party = getPartyForUser(leaderUuid);
  if (!party) throw new Error('Not in a party');
  if (party.leaderUuid !== leaderUuid) throw new Error('Only leader can kick');
  if (leaderUuid === targetUuid) throw new Error('Cannot kick yourself');
  if (!party.memberUuids.includes(targetUuid)) throw new Error('Not in party');
  party.memberUuids = party.memberUuids.filter((u) => u !== targetUuid);
  sql.prepare('DELETE FROM party_invites WHERE party_id = ? AND to_uuid = ?').run(party.id, targetUuid);
  return saveParty(party);
}

function setPartyServer(uuid, serverAddress) {
  const party = getPartyForUser(uuid);
  if (!party) throw new Error('Not in a party');
  if (party.leaderUuid !== uuid) throw new Error('Only leader can set server');
  party.serverAddress = serverAddress || null;
  return saveParty(party);
}

function getProfile(uuid) {
  return mapProfile(sql.prepare('SELECT * FROM users WHERE uuid = ?').get(uuid));
}

function updateProfileFields(uuid, fields = {}) {
  const row = sql.prepare('SELECT * FROM users WHERE uuid = ?').get(uuid);
  if (!row) throw new Error('User not found');
  let playtime = row.playtime_minutes;
  let tier = row.tier || 'free';
  let badges = row.badges || '[]';
  let bio = row.bio;
  if (fields.playtimeMinutes !== undefined) {
    playtime = fields.playtimeMinutes == null ? null : Math.max(0, Math.floor(Number(fields.playtimeMinutes) || 0));
  }
  if (fields.tier !== undefined) tier = normalizeTier(fields.tier);
  if (fields.badges !== undefined) {
    const list = Array.isArray(fields.badges) ? fields.badges.map(String).slice(0, 32) : [];
    badges = JSON.stringify(list);
  }
  if (fields.bio !== undefined) {
    bio = fields.bio == null ? null : String(fields.bio).slice(0, 280);
  }
  sql.prepare(
    `UPDATE users SET playtime_minutes = ?, tier = ?, badges = ?, bio = ? WHERE uuid = ?`
  ).run(playtime, tier, badges, bio, uuid);
  return getProfile(uuid);
}

function ensureDefaultOwnership(uuid) {
  const count = sql.prepare('SELECT COUNT(*) AS c FROM store_ownership WHERE user_uuid = ?').get(uuid).c;
  if (count > 0) return;
  const purchasedAt = nowIso();
  const insert = sql.prepare(
    `INSERT OR IGNORE INTO store_ownership (user_uuid, item_id, purchased_at) VALUES (?, ?, ?)`
  );
  const tx = sql.transaction(() => {
    for (const itemId of DEFAULT_OWNED) {
      insert.run(uuid, itemId, purchasedAt);
    }
    const equippedRow = sql.prepare('SELECT cosmetics_equipped FROM users WHERE uuid = ?').get(uuid);
    const equipped = parseJsonArray(equippedRow?.cosmetics_equipped, []);
    if (equipped.length === 0) {
      sql.prepare(`UPDATE users SET cosmetics_equipped = ? WHERE uuid = ?`).run(
        JSON.stringify(DEFAULT_EQUIPPED),
        uuid
      );
    }
  });
  tx();
}

function listOwnedItems(uuid) {
  ensureDefaultOwnership(uuid);
  return sql
    .prepare('SELECT item_id FROM store_ownership WHERE user_uuid = ? ORDER BY purchased_at ASC')
    .all(uuid)
    .map((r) => r.item_id);
}

function getBalance(uuid) {
  const row = sql.prepare('SELECT prime_coins FROM users WHERE uuid = ?').get(uuid);
  if (!row) throw new Error('User not found');
  ensureDefaultOwnership(uuid);
  return Number(row.prime_coins) || 0;
}

function purchaseItem(uuid, itemId) {
  const item = getCatalogItem(itemId);
  if (!item) throw new Error('Unknown item');
  ensureDefaultOwnership(uuid);
  const owned = sql
    .prepare('SELECT 1 AS ok FROM store_ownership WHERE user_uuid = ? AND item_id = ?')
    .get(uuid, itemId);
  if (owned) throw new Error('Already owned');
  const balance = getBalance(uuid);
  if (balance < item.price) throw new Error('Insufficient coins');
  const createdAt = nowIso();
  const historyId = id();
  const tx = sql.transaction(() => {
    sql.prepare(`UPDATE users SET prime_coins = prime_coins - ? WHERE uuid = ?`).run(item.price, uuid);
    sql.prepare(
      `INSERT INTO store_ownership (user_uuid, item_id, purchased_at) VALUES (?, ?, ?)`
    ).run(uuid, itemId, createdAt);
    sql.prepare(
      `INSERT INTO store_history (id, user_uuid, kind, item_id, amount, code, created_at)
       VALUES (?, ?, 'purchase', ?, ?, NULL, ?)`
    ).run(historyId, uuid, itemId, -item.price, createdAt);
    if (item.category === 'badge') {
      const userRow = sql.prepare('SELECT badges FROM users WHERE uuid = ?').get(uuid);
      const badges = parseJsonArray(userRow?.badges, []);
      if (!badges.includes(itemId)) {
        badges.push(itemId);
        sql.prepare(`UPDATE users SET badges = ? WHERE uuid = ?`).run(JSON.stringify(badges), uuid);
      }
    }
  });
  tx();
  return {
    itemId,
    price: item.price,
    balance: getBalance(uuid),
    owned: listOwnedItems(uuid),
  };
}

function listStoreHistory(uuid, limit = 50) {
  const lim = Math.min(100, Math.max(1, Number(limit) || 50));
  return sql
    .prepare(
      `SELECT id, kind, item_id, amount, code, created_at
       FROM store_history WHERE user_uuid = ?
       ORDER BY created_at DESC LIMIT ?`
    )
    .all(uuid, lim)
    .map((r) => ({
      id: r.id,
      kind: r.kind,
      itemId: r.item_id || null,
      amount: Number(r.amount) || 0,
      code: r.code || null,
      createdAt: r.created_at,
    }));
}

function redeemCode(uuid, rawCode) {
  const code = String(rawCode || '')
    .trim()
    .toUpperCase()
    .replace(/[^A-Z0-9_-]/g, '');
  if (!code) throw new Error('code required');
  const promo = PROMO_CODES[code];
  if (!promo) throw new Error('Invalid code');
  const used = sql
    .prepare(
      `SELECT 1 AS ok FROM store_history WHERE user_uuid = ? AND kind = 'redeem' AND code = ? LIMIT 1`
    )
    .get(uuid, code);
  if (used) throw new Error('Code already redeemed');
  const createdAt = nowIso();
  const historyId = id();
  const tx = sql.transaction(() => {
    sql.prepare(`UPDATE users SET prime_coins = prime_coins + ? WHERE uuid = ?`).run(promo.coins, uuid);
    sql.prepare(
      `INSERT INTO store_history (id, user_uuid, kind, item_id, amount, code, created_at)
       VALUES (?, ?, 'redeem', NULL, ?, ?, ?)`
    ).run(historyId, uuid, promo.coins, code, createdAt);
  });
  tx();
  return {
    code,
    coins: promo.coins,
    label: promo.label,
    balance: getBalance(uuid),
  };
}

function getCosmetics(uuid) {
  ensureDefaultOwnership(uuid);
  const owned = listOwnedItems(uuid);
  const row = sql.prepare('SELECT cosmetics_equipped FROM users WHERE uuid = ?').get(uuid);
  let equipped = parseJsonArray(row?.cosmetics_equipped, DEFAULT_EQUIPPED);
  const ownedSet = new Set(owned);
  equipped = equipped.filter((id) => ownedSet.has(id));
  return { owned, equipped };
}

function equipCosmetics(uuid, ids) {
  if (!Array.isArray(ids)) throw new Error('ids must be an array');
  ensureDefaultOwnership(uuid);
  const ownedSet = new Set(listOwnedItems(uuid));
  const cleaned = [...new Set(ids.map(String))].filter((id) => ownedSet.has(id)).slice(0, 16);
  sql.prepare(`UPDATE users SET cosmetics_equipped = ? WHERE uuid = ?`).run(JSON.stringify(cleaned), uuid);
  return getCosmetics(uuid);
}

function getSettings(uuid) {
  const row = sql.prepare('SELECT settings_json FROM users WHERE uuid = ?').get(uuid);
  if (!row) throw new Error('User not found');
  return parseJsonObject(row.settings_json, {});
}

function putSettings(uuid, blob) {
  if (!blob || typeof blob !== 'object' || Array.isArray(blob)) {
    throw new Error('json object required');
  }
  const raw = JSON.stringify(blob);
  if (raw.length > 200_000) throw new Error('Settings too large');
  sql.prepare(`UPDATE users SET settings_json = ? WHERE uuid = ?`).run(raw, uuid);
  return parseJsonObject(raw, {});
}

function recordCrash({ id: crashId, uuid, title, version, client, fileName, createdAt }) {
  sql.prepare(
    `INSERT OR REPLACE INTO crashes (id, user_uuid, title, version, client, file_name, created_at)
     VALUES (?, ?, ?, ?, ?, ?, ?)`
  ).run(
    crashId,
    uuid,
    String(title || 'crash').slice(0, 120),
    version ? String(version).slice(0, 64) : null,
    client ? String(client).slice(0, 32) : null,
    fileName || null,
    createdAt || nowIso()
  );
  return {
    id: crashId,
    createdAt: createdAt || nowIso(),
    version: version || null,
    title: String(title || 'crash').slice(0, 120),
  };
}

function listCrashes(uuid, limit = 25) {
  const lim = Math.min(100, Math.max(1, Number(limit) || 25));
  return sql
    .prepare(
      `SELECT id, title, version, client, created_at
       FROM crashes WHERE user_uuid = ?
       ORDER BY created_at DESC LIMIT ?`
    )
    .all(uuid, lim)
    .map((r) => ({
      id: r.id,
      title: r.title,
      version: r.version || null,
      client: r.client || null,
      createdAt: r.created_at,
    }));
}

function dbStats() {
  return {
    ok: true,
    path: SQLITE_PATH,
    users: sql.prepare('SELECT COUNT(*) AS c FROM users').get().c,
    friendships: sql.prepare('SELECT COUNT(*) AS c FROM friendships').get().c,
    messages: sql.prepare('SELECT COUNT(*) AS c FROM messages').get().c,
    parties: sql.prepare('SELECT COUNT(*) AS c FROM parties').get().c,
    crashes: sql.prepare('SELECT COUNT(*) AS c FROM crashes').get().c,
  };
}

const STATS_SALT = process.env.STATS_SALT || 'prime-stats-v1';

function purgeExpiredStatsDedupe() {
  const now = Date.now();
  sql.prepare('DELETE FROM stats_dedupe WHERE expires_at < ?').run(now);
}

function hashStatsIdentity(parts) {
  const raw = parts.filter(Boolean).join('|');
  return crypto.createHash('sha256').update(`${STATS_SALT}|${raw}`).digest('hex').slice(0, 32);
}

function getPublicStats() {
  const downloads =
    sql.prepare(`SELECT value, updated_at FROM usage_stats WHERE key = 'downloads'`).get() || {
      value: 0,
      updated_at: null,
    };
  const launches =
    sql.prepare(`SELECT value, updated_at FROM usage_stats WHERE key = 'launches'`).get() || {
      value: 0,
      updated_at: null,
    };
  const updatedAt = [downloads.updated_at, launches.updated_at]
    .filter(Boolean)
    .sort()
    .at(-1) || nowIso();
  return {
    downloads: Number(downloads.value) || 0,
    launches: Number(launches.value) || 0,
    updatedAt,
  };
}

/**
 * Increment a public counter with short-lived anonymous dedupe (hashed identity only).
 * @param {'downloads'|'launches'} key
 * @param {{ kind: string, identityParts: string[], ttlMs: number }} opts
 * @returns {{ counted: boolean, stats: ReturnType<typeof getPublicStats> }}
 */
function incrementUsageStat(key, opts) {
  purgeExpiredStatsDedupe();
  const kind = String(opts.kind || key);
  const hash = hashStatsIdentity(opts.identityParts || []);
  const ttlMs = Math.max(60_000, Number(opts.ttlMs) || 3_600_000);
  const expiresAt = Date.now() + ttlMs;

  const insert = sql.prepare(
    `INSERT OR IGNORE INTO stats_dedupe (kind, hash, expires_at) VALUES (?, ?, ?)`
  );
  const result = insert.run(kind, hash, expiresAt);
  let counted = result.changes > 0;
  if (counted) {
    const at = nowIso();
    sql.prepare(
      `UPDATE usage_stats SET value = value + 1, updated_at = ? WHERE key = ?`
    ).run(at, key);
  }
  return { counted, stats: getPublicStats() };
}

function recordDownload(identityParts) {
  return incrementUsageStat('downloads', {
    kind: 'download',
    identityParts,
    ttlMs: Number(process.env.STATS_DOWNLOAD_DEDUPE_MS) || 60 * 60 * 1000,
  });
}

function recordLaunch(identityParts) {
  return incrementUsageStat('launches', {
    kind: 'launch',
    identityParts,
    ttlMs: Number(process.env.STATS_LAUNCH_DEDUPE_MS) || 6 * 60 * 60 * 1000,
  });
}

function flushNow() {
  try {
    sql.pragma('wal_checkpoint(TRUNCATE)');
  } catch {
    // ignore
  }
}

process.on('exit', flushNow);
process.on('SIGINT', () => {
  flushNow();
  process.exit(0);
});

module.exports = {
  DATA_DIR,
  SQLITE_PATH,
  upsertUser,
  getUser,
  getProfile,
  updateProfileFields,
  findUserByName,
  createSession,
  resolveSession,
  listFriendships,
  getFriendship,
  requestFriend,
  acceptFriend,
  removeFriend,
  blockUser,
  unblockUser,
  isBlocked,
  areFriends,
  setFriendNote,
  getFriendNote,
  getOrCreateDm,
  listConversations,
  getConversation,
  addMessage,
  listMessages,
  createParty,
  getParty,
  getPartyForUser,
  createPartyInvite,
  listPartyInvitesFor,
  getPartyInvite,
  acceptPartyInvite,
  declinePartyInvite,
  inviteToParty,
  leaveParty,
  kickFromParty,
  setPartyServer,
  listOwnedItems,
  getBalance,
  purchaseItem,
  listStoreHistory,
  redeemCode,
  getCosmetics,
  equipCosmetics,
  getSettings,
  putSettings,
  recordCrash,
  listCrashes,
  dbStats,
  getPublicStats,
  recordDownload,
  recordLaunch,
  flushNow,
  id,
  friendshipKeyPair,
};
