/**
 * Prime Backend — unified social + voice server.
 *
 * Default local:  http://0.0.0.0:8765
 * Production:     same host as before (e.g. :26005)
 *
 * Endpoints:
 *   GET  /health
 *   GET  /v1/stats · POST /v1/stats/download · POST /v1/stats/launch
 *   POST /v1/auth/session
 *   GET  /v1/ai/status · POST /v1/ai/chat  (Groq proxy — key stays on server)
 *   GET  /v1/me · /v1/profile/:uuid
 *   REST /v1/store | /v1/cosmetics | /v1/settings | /v1/crash | /v1/network/event
 *   REST /v1/friends | /v1/conversations | /v1/party | /v1/upload
 *   WS   /voice   (unchanged Prime voice protocol)
 *   WS   /social  (?token= or first {t:"auth",token})
 */
const http = require('http');
const fs = require('fs');
const path = require('path');
const { attachVoice } = require('./lib/voice');
const { attachSocial } = require('./lib/social');
const { handleHttp } = require('./lib/http');

/** Load backend/.env into process.env without a dependency (never overwrite existing). */
function loadDotEnv() {
  const envPath = path.join(__dirname, '.env');
  if (!fs.existsSync(envPath)) return;
  const lines = fs.readFileSync(envPath, 'utf8').split(/\r?\n/);
  for (const line of lines) {
    const trimmed = line.trim();
    if (!trimmed || trimmed.startsWith('#')) continue;
    const eq = trimmed.indexOf('=');
    if (eq <= 0) continue;
    const key = trimmed.slice(0, eq).trim();
    let val = trimmed.slice(eq + 1).trim();
    if (
      (val.startsWith('"') && val.endsWith('"')) ||
      (val.startsWith("'") && val.endsWith("'"))
    ) {
      val = val.slice(1, -1);
    }
    if (!(key in process.env)) process.env[key] = val;
  }
}

loadDotEnv();

const PORT = Number(process.env.PORT || process.env.SERVER_PORT || 8765);
const HOST = process.env.HOST || '0.0.0.0';

const server = http.createServer(async (req, res) => {
  try {
    const handled = await handleHttp(req, res, { social });
    if (!handled) {
      res.writeHead(404, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({ error: 'Not found' }));
    }
  } catch (err) {
    console.error(err);
    if (!res.headersSent) {
      res.writeHead(500, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({ error: 'Internal error' }));
    }
  }
});

const voice = attachVoice(server);
const social = attachSocial(server);

server.on('upgrade', (req, socket, head) => {
  const pathOnly = (req.url || '').split('?')[0];
  if (pathOnly === '/voice') {
    voice.handleUpgrade(req, socket, head);
    return;
  }
  if (pathOnly === '/social') {
    social.handleUpgrade(req, socket, head);
    return;
  }
  socket.destroy();
});

server.listen(PORT, HOST, () => {
  const ai = require('./lib/ai');
  console.log(`Prime backend on http://${HOST}:${PORT}`);
  console.log(`  voice  ws://${HOST}:${PORT}/voice (proximity ${voice.proximity}b)`);
  console.log(`  social ws://${HOST}:${PORT}/social`);
  console.log(`  api    http://${HOST}:${PORT}/v1`);
  console.log(`  ai     ${ai.isAvailable() ? 'enabled (GROQ_API_KEY set)' : 'disabled (set GROQ_API_KEY)'}`);
});
