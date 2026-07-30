import { lazy, Suspense } from 'react'
import { Routes, Route, Navigate } from 'react-router-dom'
import { DashboardPage } from '@renderer/pages/DashboardPage'
import type { FavoriteServer } from '@shared/types'

/**
 * The dashboard is the landing route, so it stays in the main bundle -- lazily
 * loading it would trade a faster boot for a visible flash on the first screen.
 * Every other page is split out and fetched the first time it is opened.
 */
const AccountsPage = lazy(() =>
  import('@renderer/pages/AccountsPage').then((m) => ({ default: m.AccountsPage }))
)
const ProfilePage = lazy(() =>
  import('@renderer/pages/ProfilePage').then((m) => ({ default: m.ProfilePage }))
)
const InstancesPage = lazy(() =>
  import('@renderer/pages/InstancesPage').then((m) => ({ default: m.InstancesPage }))
)
const SkinsPage = lazy(() =>
  import('@renderer/pages/SkinsPage').then((m) => ({ default: m.SkinsPage }))
)
const LibraryPage = lazy(() =>
  import('@renderer/pages/LibraryPage').then((m) => ({ default: m.LibraryPage }))
)
const ModsPage = lazy(() =>
  import('@renderer/pages/ModsPage').then((m) => ({ default: m.ModsPage }))
)
const ResourcesPage = lazy(() =>
  import('@renderer/pages/ResourcesPage').then((m) => ({ default: m.ResourcesPage }))
)
const ShadersPage = lazy(() =>
  import('@renderer/pages/ShadersPage').then((m) => ({ default: m.ShadersPage }))
)
const StorePage = lazy(() =>
  import('@renderer/pages/StorePage').then((m) => ({ default: m.StorePage }))
)
const ServersPage = lazy(() =>
  import('@renderer/pages/ServersPage').then((m) => ({ default: m.ServersPage }))
)
const MediaPage = lazy(() =>
  import('@renderer/pages/MediaPage').then((m) => ({ default: m.MediaPage }))
)
const PerformancePage = lazy(() =>
  import('@renderer/pages/PerformancePage').then((m) => ({ default: m.PerformancePage }))
)
const DownloadsPage = lazy(() =>
  import('@renderer/pages/DownloadsPage').then((m) => ({ default: m.DownloadsPage }))
)
const ConsolePage = lazy(() =>
  import('@renderer/pages/ConsolePage').then((m) => ({ default: m.ConsolePage }))
)
const SettingsPage = lazy(() =>
  import('@renderer/pages/SettingsPage').then((m) => ({ default: m.SettingsPage }))
)

interface AppRoutesProps {
  servers: FavoriteServer[]
}

/**
 * Deliberately blank: chunks are local files and resolve in a few ms, so a
 * spinner or skeleton here would flash rather than inform.
 */
function PageFallback() {
  return <div aria-busy="true" />
}

export function AppRoutes({ servers }: AppRoutesProps) {
  return (
    <Suspense fallback={<PageFallback />}>
      <Routes>
        <Route index element={<DashboardPage servers={servers} />} />
        <Route path="accounts" element={<AccountsPage />} />
        <Route path="profile" element={<ProfilePage />} />
        <Route path="instances" element={<InstancesPage />} />
        <Route path="skins" element={<SkinsPage />} />
        <Route path="library" element={<LibraryPage />} />
        <Route path="mods" element={<ModsPage />} />
        <Route path="resources" element={<ResourcesPage />} />
        <Route path="shaders" element={<ShadersPage />} />
        <Route path="store" element={<StorePage />} />
        <Route path="cosmetics" element={<Navigate to="/skins" replace />} />
        <Route path="servers" element={<ServersPage />} />
        <Route path="media" element={<MediaPage />} />
        <Route path="performance" element={<PerformancePage />} />
        <Route path="downloads" element={<DownloadsPage />} />
        <Route path="console" element={<ConsolePage />} />
        <Route path="settings" element={<SettingsPage />} />
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </Suspense>
  )
}
