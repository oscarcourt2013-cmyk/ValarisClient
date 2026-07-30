import { defineConfig } from 'astro/config'

/** GitHub Pages project site: https://oscarcourt2013-cmyk.github.io/ValerisClient/ */
export default defineConfig({
  site: 'https://oscarcourt2013-cmyk.github.io',
  base: '/ValerisClient',
  output: 'static',
  compressHTML: true
})
