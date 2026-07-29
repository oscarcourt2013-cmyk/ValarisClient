# Guide StellarClient v1.1

Bienvenue sur **StellarClient** â€” client Minecraft premium (Fabric) pour le PvP, la performance, le QoL et la personnalisation.

---

## DÃ©marrage rapide

### Lancer le jeu

```powershell
cd "C:\Users\Zorat\Desktop\Plugins MC\Elysia Client"
.\gradlew :mc-26.2:runClient
```

*(Ou `:mc-1.21.11:runClient` pour Minecraft 1.21.11)*

### Premier lancement

Au **premier dÃ©marrage**, StellarClient :

1. Affiche le **splash** avec ton logo
2. Active automatiquement un **pack HUD** (FPS, coords, keystrokes, crosshair, Discord RPCâ€¦)
3. Ouvre le **menu dâ€™accueil** avec lâ€™**assistant de configuration** (4 Ã©tapes)
4. Envoie une **notification** de bienvenue

Tu peux passer lâ€™assistant avec **Ã‰chap** â€” les rÃ©glages par dÃ©faut restent actifs.

---

## Raccourcis clavier

| Touche | Action |
|--------|--------|
| **Right Shift** | Ouvrir / fermer le **menu Prime** (ClickGUI) |
| **H** | Ouvrir lâ€™**HUD Editor** (dÃ©placer les Ã©lÃ©ments HUD) |
| **C** *(maintenir)* | **Zoom** (module Zoom activÃ©) |
| **V** *(maintenir)* | **Camera Zoom** (module Creator) |
| **Ã‰chap** | Fermer un sous-menu / effacer la recherche |
| **G** *(dans HUD Editor)* | Afficher la grille dâ€™alignement |

Les modules peuvent avoir leurs propres raccourcis â€” assignables dans les paramÃ¨tres Minecraft (ContrÃ´les â†’ Prime).

---

## Menu principal (Right Shift)

```
â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”
â”‚      [Logo Prime]       â”‚
â”‚         v1.1.0          â”‚
â”‚      Ton pseudo         â”‚
â”œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”¤
â”‚  Play        â†’ Fermer   â”‚
â”‚  Modules     â†’ Cartes   â”‚
â”‚  HUD Editor  â†’ Ã‰cran H  â”‚
â”‚  Configurations â†’ Cloud â”‚
â”‚  Cosmetics   â†’ Cape    â”‚
â”‚  Settings    â†’ RÃ©glages â”‚
â””â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”˜
Right Shift = menu â€¢ H = HUD Editor
```

| Bouton | Description |
|--------|-------------|
| **Play** | Retourne au jeu |
| **Modules** | Navigateur de modules en **cartes** (par catÃ©gorie) |
| **HUD Editor** | Ã‰diteur visuel du HUD |
| **Configurations** | Sync cloud, profils (**U** upload, **D** download) |
| **Cosmetics** | Cape Prime, cosmÃ©tiques |
| **Settings** | ThÃ¨me, compte, infos |

---

## Modules â€” navigateur (Modules)

- **Onglets** : PvP, Performance, QoL, Creator, Prime
- **Clic gauche** sur une carte â†’ ouvre le panneau de rÃ©glages Ã  droite
- **Clic sur le toggle** â†’ active / dÃ©sactive le module
- **Clic molette** â†’ ajoute aux **Favoris**
- **Barre de recherche** (en bas) : tape pour filtrer (`z` = zoom, etc.)

### CatÃ©gories (51 modules)

| CatÃ©gorie | Exemples |
|-----------|----------|
| **PvP** | FPS, CPS, Keystrokes, Crosshair Editor, Target HUD, Hit Color |
| **Performance** | FPS Booster, Entity Culling, Dynamic FPS, RAM Cleaner |
| **QoL** | Zoom, Toggle Sprint, Waypoints, Auto Respawn, Better Chat |
| **Creator** | Replay Tools, Cinematic Camera, Screenshot Mode |
| **Prime** | Discord RPC, Cosmetics, Cloud, Profiles, Account |

---

## HUD Editor (H)

1. Appuie sur **H** en jeu
2. **Clic gauche + drag** â†’ dÃ©placer un Ã©lÃ©ment
3. **Molette** â†’ redimensionner (Shift/Ctrl pour modes avancÃ©s)
4. **G** â†’ grille + snap (optionnel)
5. Ferme lâ€™Ã©cran â†’ positions **sauvegardÃ©es**

Ã‰lÃ©ments visibles par dÃ©faut au 1er lancement : watermark, FPS, CPS, coords, keystrokes, ping, crosshair custom.

---

## Profils

Trois profils intÃ©grÃ©s (module **Prime Profiles**) :

| Profil | IdÃ©al pour |
|--------|------------|
| **default** | ExpÃ©rience Ã©quilibrÃ©e |
| **pvp** | + Target HUD, combo, hit color, armure, potions |
| **survival** | + waypoints, auto-respawn, item counter, sprint |

Fichiers : `.minecraft/config/StellarClient/profiles/*.json`

---

## Crosshair Editor

Module **Crosshair Editor** (PvP) :

- Styles : Classic, Dot, Circle, Diamondâ€¦
- Presets intÃ©grÃ©s + export/import
- **Profil par serveur** (auto au join/leave)
- Preview en direct en haut Ã  droite

---

## Discord Rich Presence

1. Lance **Discord Desktop**
2. Active **Prime â†’ Discord RPC**
3. Upload le logo sur le [portail dev](https://discord.com/developers/applications/1525574680994648174/rich-presence/assets) (clÃ© `prime_logo`)

Affiche : pseudo, IP serveur, ping, vie, biome, session, boutons.

â†’ DÃ©tails : [DISCORD_RPC.md](DISCORD_RPC.md)

---

## Cloud & compte

- **Prime Account** : login auto avec ton pseudo Minecraft
- **Prime Config Cloud** : backup versionnÃ© (`U` / `D` dans Configurations)
- **Auto-sync** : upload Ã  la dÃ©connexion du monde (si activÃ©)

---

## CosmÃ©tiques

**Prime â†’ Cosmetics** : cape Prime, teinte dâ€™accent. Visible sur ton personnage (mixin cape).

---

## Replay Tools

**Creator â†’ Replay Tools** :

- Enregistrement auto de ta position
- Trail + ghost en playback
- **Save / Load** : `config/StellarClient/replays/`

---

## Notifications

Coin haut-droit : toggles de modules, sync cloud, infos. RÃ©glables dans la config `notifications`.

---

## DÃ©pannage

| ProblÃ¨me | Solution |
|----------|----------|
| Menu ne sâ€™ouvre pas | VÃ©rifie **Right Shift** dans ContrÃ´les â†’ Prime |
| HUD vide | Active des modules dans **Modules** ou repasse par lâ€™assistant (supprime `config/StellarClient` pour reset) |
| Discord RPC absent | Discord ouvert + module activÃ© + asset `prime_logo` uploadÃ© |
| Crosshair vanilla visible | Active **Crosshair Editor** |
| Config perdue | VÃ©rifie `config/StellarClient/profiles/default.json` |

---

## Structure config

```
.minecraft/config/StellarClient/
  state.json              â†’ profil actif
  profiles/
    default.json          â†’ modules, HUD, thÃ¨me, favorisâ€¦
    pvp.json
    survival.json
  cloud/                  â†’ backups cloud
  replays/                â†’ fichiers replay
```

---

## Philosophie StellarClient

- **100 % visuel / QoL** â€” pas de triche gameplay
- **Core pur Java** + layers Minecraft 1.21.11 / 26.2
- **Modulaire** â€” tout sâ€™active/dÃ©sactive individuellement

Bon jeu â€” et profite du client. **Right Shift** est ton point dâ€™entrÃ©e.
