/// <reference types="vite/client" />

import type { PrimeLauncherApi } from '../../preload/index'

declare global {
  interface Window {
    primeLauncher: PrimeLauncherApi
  }
}


export {}
