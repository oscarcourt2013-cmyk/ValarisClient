# GitHub — Releases & mises à jour

Repo public : **[github.com/oscarcourt2013-cmyk/StellarClient](https://github.com/oscarcourt2013-cmyk/StellarClient)**

Aucun serveur Prime requis — GitHub héberge le code et les binaires.

## Publier une version

```powershell
# 1. Commit tes changements
git add .
git commit -m "Describe your changes"

# 2. Tag semver (déclenche la CI Release)
git tag v0.8.1
git push origin main
git push origin v0.8.1
```

La workflow [`.github/workflows/release.yml`](../.github/workflows/release.yml) build automatiquement :

- **ValerisClient** mod jar (`mc-1.21.11`)
- **ValerisClient Launcher** installeur Windows (`.exe`)

Assets attachés à la GitHub Release du tag.

## Site web (GitHub Pages)

Le site marketing vit dans [`website/`](../website/). Déployé automatiquement via [`.github/workflows/pages.yml`](../.github/workflows/pages.yml).

| | |
|--|--|
| URL | `https://oscarcourt2013-cmyk.github.io/StellarClient/` |
| Dev local | `cd website && npm install && npm run dev` |
| Téléchargement | Bouton relié à `releases/latest` → `.exe` du launcher |

Active **Settings → Pages → GitHub Actions** sur le repo une première fois.

## Vérifier les màj in-game

Settings → **Check for updates** interroge l’API publique :

`GET api.github.com/repos/oscarcourt2013-cmyk/StellarClient/releases/latest`

## Build local de l’installeur

```powershell
cd launcher
npm install
npm run dist
# Sortie : launcher/release/valeris-client-launcher-Setup-0.8.0.exe
```

## Nom du repo

| GitHub | Valeur |
|--------|--------|
| Slug URL | `ValerisClient` |
| Nom affiché | ValerisClient |
| Owner | `oscarcourt2013-cmyk` |
