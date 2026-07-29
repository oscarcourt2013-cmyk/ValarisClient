import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import App from './App'
import '@renderer/design-system/global.css'
import { createTauriPrimeApi, isTauriRuntime } from './bridge/tauriPrimeApi'

if (isTauriRuntime() && !(window as unknown as { primeLauncher?: unknown }).primeLauncher) {
  ;(window as unknown as { primeLauncher: ReturnType<typeof createTauriPrimeApi> }).primeLauncher =
    createTauriPrimeApi()
}

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <App />
  </StrictMode>
)
