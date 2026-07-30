# StellarClient — Architecture

## Vue d'ensemble

```
â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”
│                    Common Core (:core)              │
│                 Java pur — aucun import Minecraft   │
│                                                     │
│  Module System · Event Bus · Config · Keybinds      │
│  Themes · Notifications · Profiles · HUD Layout     │
│  GUI Model · Utils                                  │
│                                                     │
│            dev.stellarclient.core.adapter.*           │
│         (interfaces = contrat vers Minecraft)       │
â””â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”¬â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€┘
                       │ implémente
        â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”´â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”
        │                              │
â”Œâ”€â”€â”€â”€â”€â”€â”€â–¼â”€â”€â”€â”€â”€â”€â”€â”€â”€â”          â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â–¼â”€â”€â”€â”€â”€â”€â”€â”
│  :mc-1.21.11    │          │   :mc-26.2      │
│  Java 21        │          │   Java 25       │
│  loom-remap     │          │   loom          │
│  mojmap→interm. │          │   mojmap natif  │
â””â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€┘          â””â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€┘
```

Un seul mod id (`StellarClient`), deux jars distribués :
`StellarClient-1.21.11-x.y.z.jar` et `StellarClient-26.2-x.y.z.jar`.
Le core est embarqué dans chaque jar via Jar-in-Jar (`include`).

## Règles absolues

1. **Le core n'importe jamais `net.minecraft.*` ni `net.fabricmc.*`** (hors
   slf4j). C'est vérifié structurellement : `:core` n'a pas ces dépendances
   sur son classpath — un import illégal casse la compilation.
2. **Les couches version ne contiennent aucune logique métier.** Uniquement :
   entrypoint, implémentations d'adapters, mixins, bridges d'événements
   Fabric. Une feature codée dans une couche version est un bug
   d'architecture.
3. **Une différence entre 1.21.11 et 26.2 = une méthode d'adapter.** Jamais de
   `if (version == ...)` dans le core.
4. **`Minecraft.getInstance()` jamais mis en cache dans un champ** construit à
   l'initialisation du mod (l'instance n'existe pas encore à ce moment-là).

## Packages

### `:core` — `dev.stellarclient.core`

| Package | Rôle | Phase |
|---|---|---|
| `.` (racine) | `StellarClient` : bootstrap + accès aux managers | 1 |
| `adapter` | Contrats vers Minecraft (`MinecraftAdapter`, puis `RenderAdapter`, `InputAdapter`, `ChatAdapter`, `PlayerAdapter`…) | 1+ |
| `event` | Event bus + événements client abstraits | 3 |
| `module` | `Module`, `ModuleManager`, `Setting<T>` typés, catégories | 4 |
| `config` | Persistance JSON (configs/, profils) | 2 |
| `keybind` | Keybinds abstraits (codes GLFW, indépendants de la version) | 2 |
| `theme` | Thèmes (palettes, polices, animations) | 2 |
| `notification` | File de notifications HUD | 2 |
| `serverapi` | Canal `StellarClient:main` (handshake, XP, rewards, debug) | — |
| `servers` | Serveurs partenaires épinglés (ex. Elysia SMP) | — |
| `profile` | Profils utilisateurs (pvp.json, survival.json…) | 2 |
| `hud` | Modèle de layout HUD (positions, ancres, échelle) — le rendu passe par `RenderAdapter` | 5 |
| `gui` | Modèle ClickGUI (arbre de composants abstraits) | 6 |
| `util` | Maths, couleurs, timing, caches | 1+ |

### Couches version — `dev.stellarclient.v1_21_11` / `dev.stellarclient.v26_2`

| Contenu | Rôle |
|---|---|
| `StellarClientEntrypoint` | `ClientModInitializer` → `StellarClient.bootstrap(adapter)` |
| `VersionAdapter` | Implémentation de `MinecraftAdapter` |
| `network/` | Payloads Fabric (`presence`, `main`) |
| `mixin/` | Mixins UI / render / server list partenaires |
| (plus tard) `render/`, `event/` | Implémentations `RenderAdapter` etc., ponts Fabric → event bus core |

Doc protocole serveurs : [SERVER_API.md](SERVER_API.md).

Les deux couches utilisant mojmap, une évolution d'API Minecraft se corrige
par un diff minimal entre les deux `VersionAdapter`.

## Décisions techniques figées

- **Mappings** : Mojang officiels partout. Yarn n'existe plus après 1.21.11 ;
  Fabric lui-même a migré. Un seul vocabulaire de noms pour tout le projet.
- **Loom 1.17.14** partagé (déclaré `apply false` à la racine → un seul
  classpath plugin pour les deux flavours).
- **Java** : core compilé `--release 21` (plus petit dénominateur), couche
  26.2 en `--release 25`. Un seul JDK (25) suffit pour tout builder.
- **Pas de mixins en Phase 1.** Chaque mixin futur devra être justifié : les
  événements Fabric API couvrent la majorité des besoins ; les mixins sont
  réservés aux hooks absents de l'API (ex. HUD vanilla à déplacer).
- **Config cache Gradle désactivé** (incompatibilité Loom/IDEA connue).

## Performance (contraintes dès maintenant)

- Aucun scan/tick permanent : tout est event-driven.
- Zéro allocation dans les chemins de rendu (pas de `new`, pas de boxing,
  pas de streams dans un render loop) — buffers et objets réutilisés.
- Caches invalidés par événement plutôt que recalculs par frame.
- Les settings des modules sont lus depuis des champs typés (pas de lookup
  map dans les hot paths).
