# ValerisClient — Site web

Landing page marketing pour [ValerisClient](https://github.com/oscarcourt2013-cmyk/ValerisClient).

**URL (GitHub Pages)** : `https://oscarcourt2013-cmyk.github.io/ValerisClient/`

## Développement

```bash
cd website
npm install
npm run dev
# → http://localhost:4321/ValerisClient/
```

## Build

```bash
npm run build
# Sortie : website/dist/
```

## Déploiement

Le workflow `.github/workflows/pages.yml` déploie automatiquement sur **GitHub Pages** à chaque push sur `main` (dossier `website/`).

Dans les paramètres du repo GitHub : **Settings → Pages → Source : GitHub Actions**.

## Téléchargement .exe

Le bouton **Télécharger** interroge l'API GitHub :

`GET api.github.com/repos/oscarcourt2013-cmyk/ValerisClient/releases/latest`

Il cible le fichier `valeris-client-launcher-Setup-*.exe` attaché à la release.

Au clic, le site envoie aussi un ping anonyme `POST {PUBLIC_API_URL}/v1/stats/download` (par défaut `http://194.9.172.102:26005`). Le hero affiche `GET /v1/stats` → compteur **Téléchargements**.

Override build : `PUBLIC_API_URL=https://… npm run build`.

Pour publier une version téléchargeable (exemple — utilise le tag semver courant) :

```powershell
git tag v1.2.63
git push origin main
git push origin v1.2.63
```

La CI `release.yml` build le mod + le launcher `.exe` et les attache à la release.

## Thème

Design tokens alignés sur le launcher (`--prime-red: #e11d2e`, `--prime-black: #060608`).
