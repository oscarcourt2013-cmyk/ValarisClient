# StellarClient Launcher â€” Rust (Tauri)

Pixel-identical React UI. Shell is **Rust + WebView2** (low RAM). Electron remains available as `npm run dev`.

## Run

```bash
cd launcher
npm install
npm install --prefix resources/launch-bridge
npm run dev:tauri
```

## Features (complete)

| Feature | Implementation |
|---------|----------------|
| Same UI (React/CSS) | Vite + existing renderer |
| Window chrome | Rust |
| Settings / accounts / Microsoft OAuth | Rust |
| Instances CRUD | Rust |
| Play (Fabric) | Rust prep (Prime jar + Fabric API) + short-lived Node `launch-bridge` for libraries/JVM |
| Modrinth + CurseForge search/install | Rust |
| Import mods / resource packs / shaders | Rust dialogs |
| Resource packs & shaders activate/remove | Rust (`options.txt`) |
| Friends / chat (+ image upload) / party | Rust â†’ Prime backend |
| Store / cosmetics / servers | Rust (`ecosystem.json`) |
| Performance presets / Java discovery | Rust |
| Downloads history / launch logs | Rust |
| GitHub updates (mod + NSIS launcher) | Rust |
| Discord RPC | Rust |
| Cosmetics sync into instance profile | Rust bridge |

## Data

`%APPDATA%\stellar-client-launcher\` â€” shared with the Electron build.

**Note:** Microsoft accounts created in Electron (msmc blob) need a **one-time re-login** in Tauri (new OAuth tokens).

## Scripts

| Command | Role |
|---------|------|
| `npm run dev:tauri` | Tauri + Vite UI |
| `npm run dist:tauri` | Native installer |
| `npm run dev` | Legacy Electron |
