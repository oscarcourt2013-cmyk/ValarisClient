# Discord Rich Presence — Configuration

## Application ID (obligatoire)

| Champ | Valeur |
|-------|--------|
| **Application ID** | `1525574680994648174` |
| Portail | [Discord Developer Portal](https://discord.com/developers/applications/1525574680994648174) |

C’est le **seul ID** nécessaire. Pas de bot token, pas de client secret pour la Rich Presence IPC.

Le launcher et le mod in-game utilisent **le même Application ID** (défini dans `launcher/src/main/discord/types.ts` et `DiscordRpcService.java`).

> Si tu n’es pas propriétaire de cette application Discord, crée la tienne sur [discord.com/developers](https://discord.com/developers/applications) et remplace l’ID dans ces deux fichiers.

## Asset image (portail Discord)

Dans **Rich Presence → Art Assets**, upload le logo avec cette clé exacte :

| Asset key | Fichier |
|-----------|---------|
| `prime_logo` | `mc-1.21.11/src/main/resources/assets/stellarclient/textures/gui/logo.png` |

Sans cet asset, la présence peut s’afficher sans image ou être rejetée.

## Prérequis côté utilisateur

1. **Discord Desktop** ouvert (pas le navigateur seul)
2. **Paramètres utilisateur Discord** → Activité de jeu → *Afficher l’activité actuelle* activé
3. **Launcher** → Paramètres → **Discord RPC** activé (par défaut : oui)
4. Relancer le launcher **après** avoir ouvert Discord si la RPC n’apparaît pas (retry auto toutes les 10 s)

## Comportement

| Contexte | Affichage |
|----------|-----------|
| Launcher ouvert (sans jeu) | `StellarClient Launcher` · `Joueur • Ready to play` |
| Téléchargement / lancement | `Launching Minecraft` |
| Jeu lancé | Launcher efface sa présence → le **mod** prend le relais |
| In-game (défaut) | **details** `Elysia SMP` · **state** `♥ 20/20 · 50ms` (+ elapsed) |
| Menu | **details** `In Main Menu` · **state** `Minecraft 26.2 · Prime v…` |
| Jeu fermé | Retour `StellarClient Launcher` |

Présence in-game volontairement courte : pas de pseudo, PREMIUM, biome, item ou compteur modules par défaut (toggles dans le module Discord RPC). Boutons : **Website** / **Download**, ou **Server Status** en multi.

## Activer in-game

**Right Shift** → **Prime** → module **Discord RPC**

## Dépannage

- Console launcher (**Console** dans la sidebar) : cherche `Discord Rich Presence active` ou `Discord RPC unavailable`
- Vérifie l’App ID dans le message de log
- Les **boutons** RPC doivent être des objets `{ "label", "url" }` (max 2). Sur une app non vérifiée, Discord peut les refuser — le client réessaie alors sans boutons.
- Si Discord est ouvert mais le launcher ne le voit pas : **ferme complètement le launcher** puis relance-le depuis une build à jour (`npm run build` dans `launcher/`, ou réinstalle le setup). L’ancienne version installée peut tourner sans les correctifs IPC.
- Discord **PTB** / **Canary** : le launcher détecte automatiquement les pipes `discord-ptb-ipc-*` et `discord-canary-ipc-*`.

Application ID : **1525574680994648174**
