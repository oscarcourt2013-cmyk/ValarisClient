import { defineConfig } from 'astro/config'

/** GitHub Pages project site: https://oscarcourt2013-cmyk.github.io/StellarClient/ */
export default defineConfig({
  site: 'https://oscarcourt2013-cmyk.github.io',
  base: '/StellarClient',
  output: 'static',
  compressHTML: true
})
