import type { FavoriteServer, NewsItem } from '../../shared/types'
import { instanceService } from './InstanceService'

/** Minecraft metadata â€” instances are managed by InstanceService. */
export class MinecraftService {
  getNews(): NewsItem[] {
    return [
      {
        id: '1',
        title: 'StellarClient v1.1 â€” Premium Update',
        summary: 'New title screen, Discord RPC, onboarding, and 50 modules.',
        date: '2026-07-11',
        tag: 'update'
      },
      {
        id: '4',
        title: 'StellarClient Launcher Phase 5',
        summary: 'Create, edit, and launch multiple local instances â€” no cloud required.',
        date: '2026-07-11',
        tag: 'announcement'
      }
    ]
  }

  getFavoriteServers(): FavoriteServer[] {
    return [
      { id: '1', name: 'Hypixel', address: 'mc.hypixel.net', players: 84231, maxPlayers: 200000, ping: 24 },
      { id: '2', name: 'Prime Dev', address: 'localhost', players: 0, maxPlayers: 20, ping: 0 }
    ]
  }

  /** @deprecated Use instance.list via IPC â€” kept for backward compat during boot. */
  getInstances() {
    return instanceService.list()
  }
}

export const minecraftService = new MinecraftService()
