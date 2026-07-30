# ValarisClient

Client Minecraft All-in-One : PvP, Performance, QoL, Création de contenu, Personnalisation.
Client professionnel — 100 % légitime (visuel, confort, performance ; aucun cheat).

## Versions supportées

| Minecraft | Module Gradle | Java | Loom | Mappings |
|-----------|---------------|------|------|----------|
| 1.21.11   | `mc-1.21.11`  | 21   | `fabric-loom-remap` | Mojang (remap intermediary) |
| 26.2      | `mc-26.2`     | 25   | `fabric-loom`       | Mojang (runtime natif) |

> Yarn s'arrête à 1.21.11 et n'existe pas pour 26.x (Fabric a migré vers les
> mappings Mojang officiels). Les deux couches utilisent donc mojmap : mêmes
> noms de classes partout, adapters quasi identiques.

## Structure

```
ValarisClient/
├── core/          Common Core — Java pur, zéro dépendance Minecraft
│                  (modules, events, config, thèmes, HUD model, utils)
├── mc-1.21.11/    Couche version 1.21.11 (mod Fabric)
├── mc-26.2/       Couche version 26.2 (mod Fabric)
└── valaris-plugin/ Plugin serveur (canal `valarisclient:main`)
```

Règle d'or : le core ne touche jamais une classe Minecraft. Tout passe par les
interfaces `dev.valarisclient.core.adapter.*`, implémentées dans chaque couche.
API serveurs partenaires : voir [`valaris-plugin/`](valaris-plugin/), le plugin
serveur qui répond au canal `valarisclient:main` du client.

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

Jars finaux dans `mc-*/build/libs/` (le core est embarqué en Jar-in-Jar).
Prérequis : JDK 25 (compile aussi la cible 21 via `--release`).

## Installation

Fabric Loader + Fabric API, puis le jar de `mc-*/build/libs/` dans `.minecraft/mods/`.
Utilise n'importe quel launcher compatible Fabric.

## Feuille de route

- [x] Phase 1 — Architecture (multi-module, adapters, build vert)
- [x] Phase 2 — Core (config JSON atomique, keybinds GLFW, thèmes, notifications, profils)
- [x] Phase 3 — Event System (bus typé sans réflexion, ponts Fabric)
- [x] Phase 4 — Module System (settings typés sealed, ModuleManager, persistance)
- [x] Phase 5 — HUD System + HUD Editor (ancrage 9 points, drag & drop, échelle molette — touche H)
- [x] Phase 6 — ClickGUI (panneaux par catégorie, recherche live, sliders/toggles/enums, drag, persistance — touche Right Shift)
- [x] Phase 7 — 50 modules v1.0 (PvP×15, Performance×10, QoL×15, Creator×5, Valaris×5) · ponts Fabric chat/combat/santé, mixin FOV zoom, adapters étendus
- [x] Phase 8 — Menu principal ClickGUI, favoris, animations, rotation/transparence HUD Editor, mixins hit color & caméra cinématique, tests unitaires
- [x] Phase 9 — v1.1 Premium : design system, Crosshair Editor (presets + profils serveur), Replay Tools (save/load), Cloud sync, Cosmetics (cape mixin), Color Picker, Text Input, Hit Particles, menu premium cartes, Settings/Cosmetics/Configurations, onboarding, loading screen, notifications premium, tooltips
- [x] Phase 10 — Discord Rich Presence (App ID `1525574680994648174`), auto-sync cloud, notifications toggle modules, Valaris Account tier
- [x] Phase 11 — First-run experience (HUD starter, onboarding interactif, splash, favoris)
