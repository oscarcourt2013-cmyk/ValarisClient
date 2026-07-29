# StellarClient Launcher

Official launcher for **StellarClient** â€” premium Minecraft platform.

> Releases & updates via [GitHub](https://github.com/oscarcourt2013-cmyk/StellarClient) â€” see [docs/GITHUB.md](../docs/GITHUB.md)

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
â”œâ”€â”€ src/
â”‚   â”œâ”€â”€ main/                 # Electron main process
â”‚   â”‚   â”œâ”€â”€ index.ts          # Window, lifecycle
â”‚   â”‚   â”œâ”€â”€ ipc/handlers.ts   # IPC registration
â”‚   â”‚   â””â”€â”€ services/         # Backend services (stubs â†’ full impl)
â”‚   â”œâ”€â”€ preload/              # Secure contextBridge API
â”‚   â”œâ”€â”€ shared/               # Types + IPC channels (main â†” renderer)
â”‚   â””â”€â”€ renderer/
â”‚       â””â”€â”€ src/
â”‚           â”œâ”€â”€ design-system/   # Tokens, components, motion
â”‚           â”œâ”€â”€ layouts/         # TitleBar, Sidebar, AppShell
â”‚           â”œâ”€â”€ pages/           # Splash, Dashboard, placeholders
â”‚           â””â”€â”€ hooks/
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
| **1** | Architecture + Design System + Splash + Dashboard shell | âœ… Done |
| **2** | Full UI â€” all pages (Instances, Mods, Store, Settingsâ€¦) | âœ… Done |
| **3** | Account System (Microsoft OAuth, offline, Prime Account, PLAY prep) | âœ… Done |
| **4** | Minecraft Engine (local launch, Fabric, downloader) | âœ… Done |
| **5** | Instance Manager (CRUD, per-folder saves/mods) | âœ… Done |
| **6** | Mods / Resource Packs / Shaders (local + Modrinth) | âœ… Done |
| **7** | Prime Ecosystem (Store, Cosmetics, Friends, News, Media â€” local) | âœ… Done |
| **8** | Performance, Downloads, Settings, Updates | âœ… Done |

## Security

- `contextIsolation: true`
- `nodeIntegration: false`
- `sandbox: true`
- All main-process access via typed preload bridge

## Relation to StellarClient

This launcher:

1. Downloads Minecraft + Fabric from official CDNs (first launch)
2. Installs the StellarClient mod from **GitHub Releases** (latest `StellarClient-1.21.11*.jar`)
3. Uses your **local Gradle build** when developing from the monorepo (takes priority)
4. Downloads Fabric API from Modrinth (public API)
5. Removes stale StellarClient jars from the instance `mods/` folder on each launch
6. Keeps Prime profile / sync data **on disk only** â€” no Prime cloud server
7. Launches the game with Microsoft or offline auth

**Development** (optional local mod build):

```powershell
cd ..
.\gradlew :mc-1.21.11:build
```

When `mc-1.21.11/build/libs/` contains a jar, the launcher uses it instead of GitHub.

Optional override: `PRIME_CLIENT_JAR=C:\path\to\StellarClient-1.21.11-1.2.0.jar`

Runtime data lives in `%APPDATA%\stellar-client-launcher\`:

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
