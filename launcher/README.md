# ValerisClient Launcher

Official launcher for **ValerisClient** — premium Minecraft platform.

> Releases & updates via [GitHub](https://github.com/oscarcourt2013-cmyk/ValerisClient) — see [docs/GITHUB.md](../docs/GITHUB.md)

## Stack

| Layer | Tech |
|-------|------|
| Shell | Electron 37 |
| UI | React 19 + TypeScript |
| Routing | React Router 7 |
| Motion | Framer Motion |
| Icons | Lucide React |
| Build | electron-vite + Vite 7 |

## Structure

```
launcher/
â”œâ”€─ src/
│   â”œâ”€─ main/                 # Electron main process
│   │   â”œâ”€─ index.ts          # Window, lifecycle
│   │   â”œâ”€─ ipc/handlers.ts   # IPC registration
│   │   â””â”€─ services/         # Backend services (stubs → full impl)
│   â”œâ”€─ preload/              # Secure contextBridge API
│   â”œâ”€─ shared/               # Types + IPC channels (main ↔ renderer)
│   â””â”€─ renderer/
│       â””â”€─ src/
│           â”œâ”€─ design-system/   # Tokens, components, motion
│           â”œâ”€─ layouts/         # TitleBar, Sidebar, AppShell
│           â”œâ”€─ pages/           # Splash, Dashboard, placeholders
│           â””â”€─ hooks/
```

## Design System

Prime identity:

- **Colors**: deep black `#060608`, premium red `#e11d2e`, bright red `#ff2d42`
- **Typography**: Inter (UI) + JetBrains Mono (status/code)
- **Components**: Button, Card, Badge, Avatar, ProgressBar, PrimeLogo
- **Effects**: glow, particles, glass blur, spring transitions

## Development

```bash
cd launcher
npm install
npm run dev
```

```bash
npm run build    # Production build
npm run typecheck
```

## Roadmap

| Phase | Scope | Status |
|-------|-------|--------|
| **1** | Architecture + Design System + Splash + Dashboard shell | ✅ Done |
| **2** | Full UI — all pages (Instances, Mods, Store, Settings…) | ✅ Done |
| **3** | Account System (Microsoft OAuth, offline, Prime Account, PLAY prep) | ✅ Done |
| **4** | Minecraft Engine (local launch, Fabric, downloader) | ✅ Done |
| **5** | Instance Manager (CRUD, per-folder saves/mods) | ✅ Done |
| **6** | Mods / Resource Packs / Shaders (local + Modrinth) | ✅ Done |
| **7** | Prime Ecosystem (Store, Cosmetics, Friends, News, Media — local) | ✅ Done |
| **8** | Performance, Downloads, Settings, Updates | ✅ Done |

## Security

- `contextIsolation: true`
- `nodeIntegration: false`
- `sandbox: true`
- All main-process access via typed preload bridge

## Relation to ValerisClient

This launcher:

1. Downloads Minecraft + Fabric from official CDNs (first launch)
2. Installs the ValerisClient mod from **GitHub Releases** (latest `ValerisClient-1.21.11*.jar`)
3. Uses your **local Gradle build** when developing from the monorepo (takes priority)
4. Downloads Fabric API from Modrinth (public API)
5. Removes stale ValerisClient jars from the instance `mods/` folder on each launch
6. Keeps Prime profile / sync data **on disk only** — no Prime cloud server
7. Launches the game with Microsoft or offline auth

**Development** (optional local mod build):

```powershell
cd ..
.\gradlew :mc-1.21.11:build
```

When `mc-1.21.11/build/libs/` contains a jar, the launcher uses it instead of GitHub.

Optional override: `PRIME_CLIENT_JAR=C:\path\to\ValerisClient-1.21.11-1.2.0.jar`

Runtime data lives in `%APPDATA%\valeris-client-launcher\`:

| File / folder | Purpose |
|---------------|---------|
| `accounts.json` | Microsoft / offline accounts |
| `instances.json` | Instance configs |
| `ecosystem.json` | Store ownership, cosmetics, friends, Prime Coins |
| `settings.json` | Launcher preferences |
| `downloads.json` | Recent launch/download progress |
| `runtime/` | Shared Minecraft versions |
| `instances/<id>/game/` | Per-instance saves, mods, screenshots |

The mod project lives in the parent repo (`core/`, `mc-*/`).
