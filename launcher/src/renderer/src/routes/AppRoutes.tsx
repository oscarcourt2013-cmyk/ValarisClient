import { Routes, Route, Navigate } from 'react-router-dom'
import { DashboardPage } from '@renderer/pages/DashboardPage'
import { AccountsPage } from '@renderer/pages/AccountsPage'
import { ProfilePage } from '@renderer/pages/ProfilePage'
import { InstancesPage } from '@renderer/pages/InstancesPage'
import { SkinsPage } from '@renderer/pages/SkinsPage'
import { LibraryPage } from '@renderer/pages/LibraryPage'
import { ModsPage } from '@renderer/pages/ModsPage'
import { ResourcesPage } from '@renderer/pages/ResourcesPage'
import { ShadersPage } from '@renderer/pages/ShadersPage'
import { StorePage } from '@renderer/pages/StorePage'
import { ServersPage } from '@renderer/pages/ServersPage'
import { MediaPage } from '@renderer/pages/MediaPage'
import { PerformancePage } from '@renderer/pages/PerformancePage'
import { DownloadsPage } from '@renderer/pages/DownloadsPage'
import { ConsolePage } from '@renderer/pages/ConsolePage'
import { SettingsPage } from '@renderer/pages/SettingsPage'
import type { FavoriteServer } from '@shared/types'

interface AppRoutesProps {
  servers: FavoriteServer[]
}

export function AppRoutes({ servers }: AppRoutesProps) {
  return (
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
  )
}
