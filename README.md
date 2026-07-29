# StellarClient

Client Minecraft All-in-One : PvP, Performance, QoL, CrÃ©ation de contenu, Personnalisation.
Alternative professionnelle Ã  Lunar / Badlion / Feather â€” 100 % lÃ©gitime (visuel, confort, performance ; aucun cheat).

## Versions supportÃ©es

| Minecraft | Module Gradle | Java | Loom | Mappings |
|-----------|---------------|------|------|----------|
| 1.21.11   | `mc-1.21.11`  | 21   | `fabric-loom-remap` | Mojang (remap intermediary) |
| 26.2      | `mc-26.2`     | 25   | `fabric-loom`       | Mojang (runtime natif) |

> Yarn s'arrÃªte Ã  1.21.11 et n'existe pas pour 26.x (Fabric a migrÃ© vers les
> mappings Mojang officiels). Les deux couches utilisent donc mojmap : mÃªmes
> noms de classes partout, adapters quasi identiques.

## Structure

```
StellarClient/
â”œâ”€â”€ core/          Common Core â€” Java pur, zÃ©ro dÃ©pendance Minecraft
â”‚                  (modules, events, config, thÃ¨mes, HUD model, utils)
â”œâ”€â”€ mc-1.21.11/    Couche version 1.21.11 (mod Fabric)
â””â”€â”€ mc-26.2/       Couche version 26.2 (mod Fabric)
```

RÃ¨gle d'or : le core ne touche jamais une classe Minecraft. Tout passe par les
interfaces `dev.stellarclient.core.adapter.*`, implÃ©mentÃ©es dans chaque couche.
DÃ©tails : [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md).

API serveurs partenaires (plugins) : [docs/SERVER_API.md](docs/SERVER_API.md).

## Build & run

```bash
# Tout compiler (les deux versions)
./gradlew build

# Une seule version
./gradlew :mc-26.2:build
./gradlew :mc-1.21.11:build

# Lancer le client en dev
./gradlew :mc-26.2:runClient
./gradlew :mc-1.21.11:runClient
```

Jars finaux dans `mc-*/build/libs/` (le core est embarquÃ© en Jar-in-Jar).
PrÃ©requis : JDK 25 (compile aussi la cible 21 via `--release`).

## StellarClient Launcher

Le launcher officiel Electron vit dans [`launcher/`](launcher/) (v0.9). Releases : [GitHub](https://github.com/oscarcourt2013-cmyk/StellarClient) Â· [Site web](https://oscarcourt2013-cmyk.github.io/StellarClient/) Â· [docs/GITHUB.md](docs/GITHUB.md)

```bash
cd launcher
npm install
npm run dev
```

Voir [launcher/README.md](launcher/README.md) pour la roadmap complÃ¨te.

## Feuille de route

- [x] Phase 1 â€” Architecture (multi-module, adapters, build vert)
- [x] Phase 2 â€” Core (config JSON atomique, keybinds GLFW, thÃ¨mes, notifications, profils)
- [x] Phase 3 â€” Event System (bus typÃ© sans rÃ©flexion, ponts Fabric)
- [x] Phase 4 â€” Module System (settings typÃ©s sealed, ModuleManager, persistance)
- [x] Phase 5 â€” HUD System + HUD Editor (ancrage 9 points, drag & drop, Ã©chelle molette â€” touche H)
- [x] Phase 6 â€” ClickGUI (panneaux par catÃ©gorie, recherche live, sliders/toggles/enums, drag, persistance â€” touche Right Shift)
- [x] Phase 7 â€” 50 modules v1.0 (PvPÃ—15, PerformanceÃ—10, QoLÃ—15, CreatorÃ—5, PrimeÃ—5) Â· ponts Fabric chat/combat/santÃ©, mixin FOV zoom, adapters Ã©tendus
- [x] Phase 8 â€” Menu principal ClickGUI, favoris, animations, rotation/transparence HUD Editor, mixins hit color & camÃ©ra cinÃ©matique, tests unitaires
- [x] Phase 9 â€” v1.1 Premium : design system, Crosshair Editor (presets + profils serveur), Replay Tools (save/load), Cloud sync, Cosmetics (cape mixin), Color Picker, Text Input, Hit Particles, menu premium cartes, Settings/Cosmetics/Configurations, onboarding, loading screen, notifications premium, tooltips
- [x] Phase 10 â€” Discord Rich Presence (App ID `1525574680994648174`), auto-sync cloud, notifications toggle modules, Prime Account tier â€” voir [docs/DISCORD_RPC.md](docs/DISCORD_RPC.md)
- [x] Phase 11 â€” First-run experience (HUD starter, onboarding interactif, splash, favoris) â€” **[Guide utilisateur](docs/GUIDE_UTILISATEUR.md)**
