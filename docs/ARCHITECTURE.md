# StellarClient â€” Architecture

## Vue d'ensemble

```
â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”
â”‚                    Common Core (:core)              â”‚
â”‚                 Java pur â€” aucun import Minecraft   â”‚
â”‚                                                     â”‚
â”‚  Module System Â· Event Bus Â· Config Â· Keybinds      â”‚
â”‚  Themes Â· Notifications Â· Profiles Â· HUD Layout     â”‚
â”‚  GUI Model Â· Utils                                  â”‚
â”‚                                                     â”‚
â”‚            dev.stellarclient.core.adapter.*           â”‚
â”‚         (interfaces = contrat vers Minecraft)       â”‚
â””â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”¬â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”˜
                       â”‚ implÃ©mente
        â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”´â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”
        â”‚                              â”‚
â”Œâ”€â”€â”€â”€â”€â”€â”€â–¼â”€â”€â”€â”€â”€â”€â”€â”€â”€â”          â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â–¼â”€â”€â”€â”€â”€â”€â”€â”
â”‚  :mc-1.21.11    â”‚          â”‚   :mc-26.2      â”‚
â”‚  Java 21        â”‚          â”‚   Java 25       â”‚
â”‚  loom-remap     â”‚          â”‚   loom          â”‚
â”‚  mojmapâ†’interm. â”‚          â”‚   mojmap natif  â”‚
â””â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”˜          â””â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”˜
```

Un seul mod id (`StellarClient`), deux jars distribuÃ©s :
`StellarClient-1.21.11-x.y.z.jar` et `StellarClient-26.2-x.y.z.jar`.
Le core est embarquÃ© dans chaque jar via Jar-in-Jar (`include`).

## RÃ¨gles absolues

1. **Le core n'importe jamais `net.minecraft.*` ni `net.fabricmc.*`** (hors
   slf4j). C'est vÃ©rifiÃ© structurellement : `:core` n'a pas ces dÃ©pendances
   sur son classpath â€” un import illÃ©gal casse la compilation.
2. **Les couches version ne contiennent aucune logique mÃ©tier.** Uniquement :
   entrypoint, implÃ©mentations d'adapters, mixins, bridges d'Ã©vÃ©nements
   Fabric. Une feature codÃ©e dans une couche version est un bug
   d'architecture.
3. **Une diffÃ©rence entre 1.21.11 et 26.2 = une mÃ©thode d'adapter.** Jamais de
   `if (version == ...)` dans le core.
4. **`Minecraft.getInstance()` jamais mis en cache dans un champ** construit Ã 
   l'initialisation du mod (l'instance n'existe pas encore Ã  ce moment-lÃ ).

## Packages

### `:core` â€” `dev.stellarclient.core`

| Package | RÃ´le | Phase |
|---|---|---|
| `.` (racine) | `StellarClient` : bootstrap + accÃ¨s aux managers | 1 |
| `adapter` | Contrats vers Minecraft (`MinecraftAdapter`, puis `RenderAdapter`, `InputAdapter`, `ChatAdapter`, `PlayerAdapter`â€¦) | 1+ |
| `event` | Event bus + Ã©vÃ©nements client abstraits | 3 |
| `module` | `Module`, `ModuleManager`, `Setting<T>` typÃ©s, catÃ©gories | 4 |
| `config` | Persistance JSON (configs/, profils) | 2 |
| `keybind` | Keybinds abstraits (codes GLFW, indÃ©pendants de la version) | 2 |
| `theme` | ThÃ¨mes (palettes, polices, animations) | 2 |
| `notification` | File de notifications HUD | 2 |
| `serverapi` | Canal `StellarClient:main` (handshake, XP, rewards, debug) | â€” |
| `servers` | Serveurs partenaires Ã©pinglÃ©s (ex. Elysia SMP) | â€” |
| `profile` | Profils utilisateurs (pvp.json, survival.jsonâ€¦) | 2 |
| `hud` | ModÃ¨le de layout HUD (positions, ancres, Ã©chelle) â€” le rendu passe par `RenderAdapter` | 5 |
| `gui` | ModÃ¨le ClickGUI (arbre de composants abstraits) | 6 |
| `util` | Maths, couleurs, timing, caches | 1+ |

### Couches version â€” `dev.stellarclient.v1_21_11` / `dev.stellarclient.v26_2`

| Contenu | RÃ´le |
|---|---|
| `StellarClientEntrypoint` | `ClientModInitializer` â†’ `StellarClient.bootstrap(adapter)` |
| `VersionAdapter` | ImplÃ©mentation de `MinecraftAdapter` |
| `network/` | Payloads Fabric (`presence`, `main`) |
| `mixin/` | Mixins UI / render / server list partenaires |
| (plus tard) `render/`, `event/` | ImplÃ©mentations `RenderAdapter` etc., ponts Fabric â†’ event bus core |

Doc protocole serveurs : [SERVER_API.md](SERVER_API.md).

Les deux couches utilisant mojmap, une Ã©volution d'API Minecraft se corrige
par un diff minimal entre les deux `VersionAdapter`.

## DÃ©cisions techniques figÃ©es

- **Mappings** : Mojang officiels partout. Yarn n'existe plus aprÃ¨s 1.21.11 ;
  Fabric lui-mÃªme a migrÃ©. Un seul vocabulaire de noms pour tout le projet.
- **Loom 1.17.14** partagÃ© (dÃ©clarÃ© `apply false` Ã  la racine â†’ un seul
  classpath plugin pour les deux flavours).
- **Java** : core compilÃ© `--release 21` (plus petit dÃ©nominateur), couche
  26.2 en `--release 25`. Un seul JDK (25) suffit pour tout builder.
- **Pas de mixins en Phase 1.** Chaque mixin futur devra Ãªtre justifiÃ© : les
  Ã©vÃ©nements Fabric API couvrent la majoritÃ© des besoins ; les mixins sont
  rÃ©servÃ©s aux hooks absents de l'API (ex. HUD vanilla Ã  dÃ©placer).
- **Config cache Gradle dÃ©sactivÃ©** (incompatibilitÃ© Loom/IDEA connue).

## Performance (contraintes dÃ¨s maintenant)

- Aucun scan/tick permanent : tout est event-driven.
- ZÃ©ro allocation dans les chemins de rendu (pas de `new`, pas de boxing,
  pas de streams dans un render loop) â€” buffers et objets rÃ©utilisÃ©s.
- Caches invalidÃ©s par Ã©vÃ©nement plutÃ´t que recalculs par frame.
- Les settings des modules sont lus depuis des champs typÃ©s (pas de lookup
  map dans les hot paths).
