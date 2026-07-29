# Discord Rich Presence â€” Configuration

## Application ID (obligatoire)

| Champ | Valeur |
|-------|--------|
| **Application ID** | `1525574680994648174` |
| Portail | [Discord Developer Portal](https://discord.com/developers/applications/1525574680994648174) |

Câ€™est le **seul ID** nÃ©cessaire. Pas de bot token, pas de client secret pour la Rich Presence IPC.

Le launcher et le mod in-game utilisent **le mÃªme Application ID** (dÃ©fini dans `launcher/src/main/discord/types.ts` et `DiscordRpcService.java`).

> Si tu nâ€™es pas propriÃ©taire de cette application Discord, crÃ©e la tienne sur [discord.com/developers](https://discord.com/developers/applications) et remplace lâ€™ID dans ces deux fichiers.

## Asset image (portail Discord)

Dans **Rich Presence â†’ Art Assets**, upload le logo avec cette clÃ© exacte :

| Asset key | Fichier |
|-----------|---------|
| `prime_logo` | `mc-1.21.11/src/main/resources/assets/stellarclient/textures/gui/logo.png` |

Sans cet asset, la prÃ©sence peut sâ€™afficher sans image ou Ãªtre rejetÃ©e.

## PrÃ©requis cÃ´tÃ© utilisateur

1. **Discord Desktop** ouvert (pas le navigateur seul)
2. **ParamÃ¨tres utilisateur Discord** â†’ ActivitÃ© de jeu â†’ *Afficher lâ€™activitÃ© actuelle* activÃ©
3. **Launcher** â†’ ParamÃ¨tres â†’ **Discord RPC** activÃ© (par dÃ©faut : oui)
4. Relancer le launcher **aprÃ¨s** avoir ouvert Discord si la RPC nâ€™apparaÃ®t pas (retry auto toutes les 10 s)

## Comportement

| Contexte | Affichage |
|----------|-----------|
| Launcher ouvert (sans jeu) | `StellarClient Launcher` Â· `Joueur â€¢ Ready to play` |
| TÃ©lÃ©chargement / lancement | `Launching Minecraft` |
| Jeu lancÃ© | Launcher efface sa prÃ©sence â†’ le **mod** prend le relais |
| In-game (dÃ©faut) | **details** `Elysia SMP` Â· **state** `â™¥ 20/20 Â· 50ms` (+ elapsed) |
| Menu | **details** `In Main Menu` Â· **state** `Minecraft 26.2 Â· Prime vâ€¦` |
| Jeu fermÃ© | Retour `StellarClient Launcher` |

PrÃ©sence in-game volontairement courte : pas de pseudo, PREMIUM, biome, item ou compteur modules par dÃ©faut (toggles dans le module Discord RPC). Boutons : **Website** / **Download**, ou **Server Status** en multi.

## Activer in-game

**Right Shift** â†’ **Prime** â†’ module **Discord RPC**

## DÃ©pannage

- Console launcher (**Console** dans la sidebar) : cherche `Discord Rich Presence active` ou `Discord RPC unavailable`
- VÃ©rifie lâ€™App ID dans le message de log
- Les **boutons** RPC doivent Ãªtre des objets `{ "label", "url" }` (max 2). Sur une app non vÃ©rifiÃ©e, Discord peut les refuser â€” le client rÃ©essaie alors sans boutons.
- Si Discord est ouvert mais le launcher ne le voit pas : **ferme complÃ¨tement le launcher** puis relance-le depuis une build Ã  jour (`npm run build` dans `launcher/`, ou rÃ©installe le setup). Lâ€™ancienne version installÃ©e peut tourner sans les correctifs IPC.
- Discord **PTB** / **Canary** : le launcher dÃ©tecte automatiquement les pipes `discord-ptb-ipc-*` et `discord-canary-ipc-*`.

Application ID : **1525574680994648174**
