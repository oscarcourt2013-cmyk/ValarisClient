# StellarClient â€” Site web

Landing page marketing pour [StellarClient](https://github.com/oscarcourt2013-cmyk/StellarClient).

**URL (GitHub Pages)** : `https://oscarcourt2013-cmyk.github.io/StellarClient/`

## DÃ©veloppement

```bash
cd website
npm install
npm run dev
# â†’ http://localhost:4321/StellarClient/
```

## Build

```bash
npm run build
# Sortie : website/dist/
```

## DÃ©ploiement

Le workflow `.github/workflows/pages.yml` dÃ©ploie automatiquement sur **GitHub Pages** Ã  chaque push sur `main` (dossier `website/`).

Dans les paramÃ¨tres du repo GitHub : **Settings â†’ Pages â†’ Source : GitHub Actions**.

## TÃ©lÃ©chargement .exe

Le bouton **TÃ©lÃ©charger** interroge l'API GitHub :

`GET api.github.com/repos/oscarcourt2013-cmyk/StellarClient/releases/latest`

Il cible le fichier `stellar-client-launcher-Setup-*.exe` attachÃ© Ã  la release.

Au clic, le site envoie aussi un ping anonyme `POST {PUBLIC_API_URL}/v1/stats/download` (par dÃ©faut `http://194.9.172.102:26005`). Le hero affiche `GET /v1/stats` â†’ compteur **TÃ©lÃ©chargements**.

Override build : `PUBLIC_API_URL=https://â€¦ npm run build`.

Pour publier une version tÃ©lÃ©chargeable (exemple â€” utilise le tag semver courant) :

```powershell
git tag v1.2.63
git push origin main
git push origin v1.2.63
```

La CI `release.yml` build le mod + le launcher `.exe` et les attache Ã  la release.

## ThÃ¨me

Design tokens alignÃ©s sur le launcher (`--prime-red: #e11d2e`, `--prime-black: #060608`).
