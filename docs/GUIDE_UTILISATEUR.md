# Guide ValerisClient v1.1

Bienvenue sur **ValerisClient** — client Minecraft premium (Fabric) pour le PvP, la performance, le QoL et la personnalisation.

---

## Démarrage rapide

### Lancer le jeu

```powershell
cd "C:\Users\Zorat\Desktop\Plugins MC\Elysia Client"
.\gradlew :mc-26.2:runClient
```

*(Ou `:mc-1.21.11:runClient` pour Minecraft 1.21.11)*

### Premier lancement

Au **premier démarrage**, ValerisClient :

1. Affiche le **splash** avec ton logo
2. Active automatiquement un **pack HUD** (FPS, coords, keystrokes, crosshair, Discord RPC…)
3. Ouvre le **menu d’accueil** avec l’**assistant de configuration** (4 étapes)
4. Envoie une **notification** de bienvenue

Tu peux passer l’assistant avec **Échap** — les réglages par défaut restent actifs.

---

## Raccourcis clavier

| Touche | Action |
|--------|--------|
| **Right Shift** | Ouvrir / fermer le **menu Prime** (ClickGUI) |
| **H** | Ouvrir l’**HUD Editor** (déplacer les éléments HUD) |
| **C** *(maintenir)* | **Zoom** (module Zoom activé) |
| **V** *(maintenir)* | **Camera Zoom** (module Creator) |
| **Échap** | Fermer un sous-menu / effacer la recherche |
| **G** *(dans HUD Editor)* | Afficher la grille d’alignement |

Les modules peuvent avoir leurs propres raccourcis — assignables dans les paramètres Minecraft (Contrôles → Prime).

---

## Menu principal (Right Shift)

```
┌─────────────────────────┐
│      [Logo Prime]       │
│         v1.1.0          │
│      Ton pseudo         │
├─────────────────────────┤
│  Play        → Fermer   │
│  Modules     → Cartes   │
│  HUD Editor  → Écran H  │
│  Configurations → Cloud │
│  Cosmetics   → Cape    │
│  Settings    → Réglages │
└─────────────────────────┘
Right Shift = menu • H = HUD Editor
```

| Bouton | Description |
|--------|-------------|
| **Play** | Retourne au jeu |
| **Modules** | Navigateur de modules en **cartes** (par catégorie) |
| **HUD Editor** | Éditeur visuel du HUD |
| **Configurations** | Sync cloud, profils (**U** upload, **D** download) |
| **Cosmetics** | Cape Prime, cosmétiques |
| **Settings** | Thème, compte, infos |

---

## Modules — navigateur (Modules)

- **Onglets** : PvP, Performance, QoL, Creator, Prime
- **Clic gauche** sur une carte → ouvre le panneau de réglages à droite
- **Clic sur le toggle** → active / désactive le module
- **Clic molette** → ajoute aux **Favoris**
- **Barre de recherche** (en bas) : tape pour filtrer (`z` = zoom, etc.)

### Catégories (51 modules)

| Catégorie | Exemples |
|-----------|----------|
| **PvP** | FPS, CPS, Keystrokes, Crosshair Editor, Target HUD, Hit Color |
| **Performance** | FPS Booster, Entity Culling, Dynamic FPS, RAM Cleaner |
| **QoL** | Zoom, Toggle Sprint, Waypoints, Auto Respawn, Better Chat |
| **Creator** | Replay Tools, Cinematic Camera, Screenshot Mode |
| **Prime** | Discord RPC, Cosmetics, Cloud, Profiles, Account |

---

## HUD Editor (H)

1. Appuie sur **H** en jeu
2. **Clic gauche + drag** → déplacer un élément
3. **Molette** → redimensionner (Shift/Ctrl pour modes avancés)
4. **G** → grille + snap (optionnel)
5. Ferme l’écran → positions **sauvegardées**

Éléments visibles par défaut au 1er lancement : watermark, FPS, CPS, coords, keystrokes, ping, crosshair custom.

---

## Profils

Trois profils intégrés (module **Prime Profiles**) :

| Profil | Idéal pour |
|--------|------------|
| **default** | Expérience équilibrée |
| **pvp** | + Target HUD, combo, hit color, armure, potions |
| **survival** | + waypoints, auto-respawn, item counter, sprint |

Fichiers : `.minecraft/config/ValerisClient/profiles/*.json`

---

## Crosshair Editor

Module **Crosshair Editor** (PvP) :

- Styles : Classic, Dot, Circle, Diamond…
- Presets intégrés + export/import
- **Profil par serveur** (auto au join/leave)
- Preview en direct en haut à droite

---

## Discord Rich Presence

1. Lance **Discord Desktop**
2. Active **Prime → Discord RPC**
3. Upload le logo sur le [portail dev](https://discord.com/developers/applications/1525574680994648174/rich-presence/assets) (clé `prime_logo`)

Affiche : pseudo, IP serveur, ping, vie, biome, session, boutons.

→ Détails : [DISCORD_RPC.md](DISCORD_RPC.md)

---

## Cloud & compte

- **Prime Account** : login auto avec ton pseudo Minecraft
- **Prime Config Cloud** : backup versionné (`U` / `D` dans Configurations)
- **Auto-sync** : upload à la déconnexion du monde (si activé)

---

## Cosmétiques

**Prime → Cosmetics** : cape Prime, teinte d’accent. Visible sur ton personnage (mixin cape).

---

## Replay Tools

**Creator → Replay Tools** :

- Enregistrement auto de ta position
- Trail + ghost en playback
- **Save / Load** : `config/ValerisClient/replays/`

---

## Notifications

Coin haut-droit : toggles de modules, sync cloud, infos. Réglables dans la config `notifications`.

---

## Dépannage

| Problème | Solution |
|----------|----------|
| Menu ne s’ouvre pas | Vérifie **Right Shift** dans Contrôles → Prime |
| HUD vide | Active des modules dans **Modules** ou repasse par l’assistant (supprime `config/ValerisClient` pour reset) |
| Discord RPC absent | Discord ouvert + module activé + asset `prime_logo` uploadé |
| Crosshair vanilla visible | Active **Crosshair Editor** |
| Config perdue | Vérifie `config/ValerisClient/profiles/default.json` |

---

## Structure config

```
.minecraft/config/ValerisClient/
  state.json              → profil actif
  profiles/
    default.json          → modules, HUD, thème, favoris…
    pvp.json
    survival.json
  cloud/                  → backups cloud
  replays/                → fichiers replay
```

---

## Philosophie ValerisClient

- **100 % visuel / QoL** — pas de triche gameplay
- **Core pur Java** + layers Minecraft 1.21.11 / 26.2
- **Modulaire** — tout s’active/désactive individuellement

Bon jeu — et profite du client. **Right Shift** est ton point d’entrée.
