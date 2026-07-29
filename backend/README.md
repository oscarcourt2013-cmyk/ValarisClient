# Prime Backend

Unified **social + voice** server for StellarClient (friends / DM / party / presence), plus cloud-sync helpers for profiles, store, cosmetics, and settings.

**Version:** `2.1.4` â€” SQLite persistence, MS profile verify (optional), party invites, friend notes, block list, profiles, Prime Coins store, cosmetics/settings sync, crash index, **public download/launch stats**.

## Run locally

```bash
cd backend
npm install
npm start
```

Default: `http://0.0.0.0:8765`

Copy `.env.example` â†’ `.env` for local overrides (never commit `.env`).

## Deploy (VPS / Pterodactyl)

Point the process at this `backend/` folder (same host as the old voice relay):

```bash
PORT=26005 npm start
```

Public example: `http://194.9.172.102:26005` â€” keep `/voice` unchanged for proximity voice.

### AI (Groq proxy)

Set the key **only on the server** â€” never ship it to clients:

```bash
GROQ_API_KEY=gsk_... PORT=26005 npm start
```

| Route | Role |
|-------|------|
| `GET /v1/ai/status` | `{ available, models }` â€” no secrets |
| `POST /v1/ai/chat` | Proxies chat completions (+ tools) to Groq (rate-limited) |

Launcher + in-game `/ai` call this proxy. Users never see the API key.

After pulling `2.1.4`, restart the process once so SQLite migrates (`usage_stats` / `stats_dedupe` tables).

## Data & migration

| Path | Role |
|------|------|
| `data/prime.db` | SQLite store (users, sessions, friends, messages, parties, notes, store, crashes, **usage_stats**) |
| `data/prime.json` | Legacy JSON â€” **one-shot migrated** into SQLite on first boot if DB is empty, then kept as backup |
| `uploads/` | Chat image uploads |
| `uploads/crashes/` | Crash logs (+ DB index on POST) |

No manual migrate step: start the server; missing user columns / tables are added safely with `ALTER TABLE` / `CREATE IF NOT EXISTS`. If `prime.json` exists and `prime.db` has no users, legacy JSON migration runs automatically.

## Auth

`POST /v1/auth/session` body:

```json
{
  "uuid": "...",
  "username": "...",
  "offline": false,
  "client": "launcher" | "game",
  "accessToken": "optional Minecraft profile token"
}
```

- With `accessToken`: verified against `api.minecraftservices.com/minecraft/profile` (UUID/name must match).
- Without token: still allowed (compat) as `unverified` with tighter rate limits.
- Sessions are bound per `client`; logging in again from the same client revokes the previous session.

## Endpoints

| Path | Role |
|------|------|
| `GET /health` | `{ version: "2.1.4", db, ws }` |
| `GET /v1/stats` | Public counters `{ downloads, launches, updatedAt }` â€” no auth |
| `POST /v1/stats/download` | Increment downloads (rate-limited; optional `{ deviceId }`; IP hashed for short dedupe) |
| `POST /v1/stats/launch` | Increment launches (rate-limited; optional `{ deviceId, client }`) |
| `POST /v1/auth/session` | Session token |
| `GET /v1/me` | Self + profile fields (`createdAt`, `playtimeMinutes`, `tier`, `badges`, `bio`, `primeCoins`) |
| `GET /v1/profile/:uuid` | Public profile snapshot |
| `GET /v1/store/catalog` | Static catalog (mirrors launcher ecosystem categories) + owned flags |
| `GET /v1/store/balance` | `prime_coins` balance |
| `POST /v1/store/purchase` | `{ itemId }` â€” deduct coins, record ownership + history |
| `GET /v1/store/history` | Purchase / redeem history |
| `POST /v1/store/redeem` | `{ code }` â€” promo codes (`WELCOME100`, `PRIME500`, `ELYSIA250`, `FOUNDER1000`) |
| `GET /v1/cosmetics` | Owned + equipped cosmetic ids |
| `PUT /v1/cosmetics/equip` | `{ ids: string[] }` |
| `GET /v1/settings` Â· `PUT /v1/settings` | JSON blob cloud sync |
| `POST /v1/crash` Â· `GET /v1/crash` | Upload crash log / list recent meta |
| `POST /v1/network/event` | Plugin bridge stub â†’ `{ ok: true }` |
| `GET/POST /v1/friendsâ€¦` | Friends + requests |
| `POST /v1/friends/block` Â· `DELETE â€¦/block` | Block / unblock |
| `PUT /v1/friends/:uuid/note` | Server-persisted friend note |
| `GET/POST /v1/conversationsâ€¦` | DMs (text + imageUrl) |
| `POST /v1/upload` | Multipart image (max 5MB) |
| `POST /v1/party` Â· `/invite` Â· `/accept` Â· `/decline` Â· `/leave` Â· `/kick` Â· `/server` | Party lifecycle (invites are pending until accept) |
| `WS /social?token=` | Presence, live chat, typing, party events; client `ping` every ~25s |
| `WS /voice` | Existing proximity voice (unchanged) |

### Public stats notes

- CORS is open (`Access-Control-Allow-Origin: *`) so the GitHub Pages site can `GET /v1/stats` and `POST` increments.
- Raw IPs are **not** stored; only a salted hash lives in `stats_dedupe` until its TTL expires (`STATS_DOWNLOAD_DEDUPE_MS` / `STATS_LAUNCH_DEDUPE_MS`, optional `STATS_SALT`).
- Website download CTA â†’ `POST /v1/stats/download`; launcher session start â†’ `POST /v1/stats/launch`.

Store sync is optional / local-first: launcher can keep working offline; cloud routes mirror catalog + coin ledger when online.

## Smoke checklist

1. **Launcher DM â†” in-game chat** â€” send from launcher Chat, see in Social Hub Chat tab (and reverse).
2. **Dual presence** â€” open launcher + game; leave world â†’ presence demotes to launcher (not flash offline); close both â†’ offline.
3. **Party invite** â€” invite from Friends â†’ other client gets invite â†’ Accept/Decline; members list updates live.
4. **Join from drawer** â€” friend in-game with `serverAddress` â†’ Join uses that address (not the note text).
5. **Block** â€” block a user â†’ cannot DM / party-invite them.
6. **Notes** â€” save a friend note in launcher â†’ persists after restart (SQLite).
7. **Health** â€” `curl http://127.0.0.1:26005/health` â†’ `version` `2.1.4`, `db.ok` true.
8. **Store** â€” redeem `WELCOME100` â†’ purchase a paid catalog item â†’ history lists both.
9. **Crash list** â€” POST a crash â†’ GET `/v1/crash` returns `{ id, createdAt, version }`.
10. **Stats** â€” `GET /v1/stats` â†’ `{ downloads, launches, updatedAt }`; POST download/launch increments (deduped).
