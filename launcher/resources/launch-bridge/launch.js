/**
 * Lightweight launch helper — only spawned when Play is pressed.
 * Keeps Tauri UI RAM low; uses minecraft-java-core for Fabric/vanilla.
 *
 * stdin: one JSON line { authenticator, options, gameDir, runtimePath }
 * stdout: JSON lines { t: "progress"|"done"|"error", ... }
 */
const { Launch } = require('minecraft-java-core');
const { spawn } = require('child_process');
const path = require('path');
const fs = require('fs');

function send(obj) {
  process.stdout.write(JSON.stringify(obj) + '\n');
}

async function main() {
  const chunks = [];
  for await (const c of process.stdin) chunks.push(c);
  const raw = Buffer.concat(chunks).toString('utf8').trim();
  if (!raw) {
    send({ t: 'error', message: 'No launch config' });
    process.exit(1);
  }
  const cfg = JSON.parse(raw);
  const authenticator = cfg.authenticator;
  const gameDir = cfg.gameDir;
  const runtimePath = cfg.runtimePath;
  const o = cfg.options || {};

  fs.mkdirSync(gameDir, { recursive: true });
  fs.mkdirSync(runtimePath, { recursive: true });

  send({ t: 'progress', phase: 'start', detail: 'Preparing launch…', percent: 5 });

  const launcher = new Launch();
  let settled = false;

  const done = (ok, message) => {
    if (settled) return;
    settled = true;
    send(ok ? { t: 'done', message } : { t: 'error', message });
  };

  launcher.on('progress', (progress, size, element) => {
    const pct = 20 + Math.round((progress / Math.max(1, size)) * 50);
    send({
      t: 'progress',
      phase: 'download',
      detail: String(element || 'Downloading…'),
      percent: Math.min(70, pct)
    });
  });
  launcher.on('data', (line) => {
    const text = String(line);
    send({ t: 'progress', phase: 'log', detail: text });
    if (text.includes('Launching with arguments')) {
      send({ t: 'progress', phase: 'running', detail: 'Minecraft started', percent: 100 });
      done(true, 'running');
    }
  });
  launcher.on('close', (code) => {
    send({ t: 'progress', phase: 'stopped', detail: `Exit ${code}` });
    if (!settled) done(true, 'closed');
  });
  launcher.on('error', (err) => {
    done(false, err?.message || String(err));
  });

  const javaPath = o.javaPath;
  const opts = {
    path: runtimePath,
    authenticator,
    version: o.minecraftVersion || '1.21.11',
    instance: null,
    detached: false,
    ignored: [],
    verify: false,
    JVM_ARGS: (o.jvmArgs || []).filter((a) => !String(a).includes('UseCompactObjectHeaders')),
    GAME_ARGS: ['--gameDir', gameDir].concat(o.gameArgs || []),
    java: javaPath
      ? { path: javaPath, version: String(o.javaVersion || 21), type: 'jre' }
      : { version: String(o.javaVersion || 21), type: 'jre' },
    screen: {
      width: o.width || 854,
      height: o.height || 480,
      fullscreen: !!o.fullscreen
    },
    memory: {
      min: '512M',
      max: `${o.ramMb || 4096}M`
    },
    loader: {
      type: o.loader === 'fabric' ? 'fabric' : null,
      build: o.fabricLoaderVersion || 'latest',
      enable: o.loader === 'fabric'
    }
  };

  send({ t: 'progress', phase: 'launch', detail: 'Resolving Java & libraries…', percent: 75 });
  try {
    await launcher.Launch(opts);
    send({ t: 'progress', phase: 'running', detail: 'Launch resolved', percent: 95 });
    if (!settled) done(true, 'launched');
  } catch (e) {
    done(false, e?.message || String(e));
    process.exit(1);
  }
}

main().catch((e) => {
  send({ t: 'error', message: e?.message || String(e) });
  process.exit(1);
});
