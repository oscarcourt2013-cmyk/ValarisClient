# GitHub â€” Releases & mises Ã  jour

Repo public : **[github.com/oscarcourt2013-cmyk/StellarClient](https://github.com/oscarcourt2013-cmyk/StellarClient)**

Aucun serveur Prime requis â€” GitHub hÃ©berge le code et les binaires.

## Publier une version

```powershell
# 1. Commit tes changements
git add .
git commit -m "Describe your changes"

# 2. Tag semver (dÃ©clenche la CI Release)
git tag v0.8.1
git push origin main
git push origin v0.8.1
```

La workflow [`.github/workflows/release.yml`](../.github/workflows/release.yml) build automatiquement :

- **StellarClient** mod jar (`mc-1.21.11`)
- **StellarClient Launcher** installeur Windows (`.exe`)

Assets attachÃ©s Ã  la GitHub Release du tag.

## Site web (GitHub Pages)

Le site marketing vit dans [`website/`](../website/). DÃ©ployÃ© automatiquement via [`.github/workflows/pages.yml`](../.github/workflows/pages.yml).

| | |
|--|--|
| URL | `https://oscarcourt2013-cmyk.github.io/StellarClient/` |
| Dev local | `cd website && npm install && npm run dev` |
| TÃ©lÃ©chargement | Bouton reliÃ© Ã  `releases/latest` â†’ `.exe` du launcher |

Active **Settings â†’ Pages â†’ GitHub Actions** sur le repo une premiÃ¨re fois.

## VÃ©rifier les mÃ j in-game

Settings â†’ **Check for updates** interroge lâ€™API publique :

`GET api.github.com/repos/oscarcourt2013-cmyk/StellarClient/releases/latest`

## Build local de lâ€™installeur

```powershell
cd launcher
npm install
npm run dist
# Sortie : launcher/release/stellar-client-launcher-Setup-0.8.0.exe
```

## Nom du repo

| GitHub | Valeur |
|--------|--------|
| Slug URL | `StellarClient` |
| Nom affichÃ© | StellarClient |
| Owner | `oscarcourt2013-cmyk` |
