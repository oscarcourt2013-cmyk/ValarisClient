/** Join site base path with a public asset (GitHub Pages: /ValerisClient/...). */
export function assetUrl(path: string): string {
  const base = import.meta.env.BASE_URL.replace(/\/$/, '')
  const clean = path.replace(/^\//, '')
  return `${base}/${clean}`
}
