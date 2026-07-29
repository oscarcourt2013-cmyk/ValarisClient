import type { CosmeticItem } from '../../shared/content-types'
import { COSMETIC_CATALOG, STORE_TO_COSMETIC } from '../../shared/ecosystem-catalog'
import { ecosystemStore } from '../storage/EcosystemStore'

const FREE_COSMETICS = new Set(['cape-prime', 'wings-aurora'])

export class CosmeticService {
  async list(): Promise<CosmeticItem[]> {
    const db = await ecosystemStore.load()
    const ownedCosmeticIds = new Set<string>(FREE_COSMETICS)

    for (const storeId of db.ownedStoreItems) {
      const cosmeticId = STORE_TO_COSMETIC[storeId]
      if (cosmeticId) {
        ownedCosmeticIds.add(cosmeticId)
      }
    }

    return COSMETIC_CATALOG.filter((c) => ownedCosmeticIds.has(c.id)).map((c) => ({
      ...c,
      equipped: db.equippedCosmetics.includes(c.id)
    }))
  }

  async toggleEquip(cosmeticId: string): Promise<{ ok: boolean; error?: string }> {
    const cosmetic = COSMETIC_CATALOG.find((c) => c.id === cosmeticId)
    if (!cosmetic) {
      return { ok: false, error: 'Cosmetic not found.' }
    }

    const items = await this.list()
    if (!items.find((c) => c.id === cosmeticId)) {
      return { ok: false, error: 'Cosmetic not owned. Get it from the Store.' }
    }

    await ecosystemStore.mutate((db) => {
      const equipped = db.equippedCosmetics.includes(cosmeticId)
      if (equipped) {
        db.equippedCosmetics = db.equippedCosmetics.filter((id) => id !== cosmeticId)
        return
      }

      if (cosmetic.type !== 'badge') {
        db.equippedCosmetics = db.equippedCosmetics.filter((id) => {
          const meta = COSMETIC_CATALOG.find((c) => c.id === id)
          return meta?.type !== cosmetic.type
        })
      }

      db.equippedCosmetics.push(cosmeticId)
    })

    const { instanceService } = await import('./InstanceService')
    const { launcherBridgeService } = await import('./LauncherBridgeService')
    const { profileService } = await import('./ProfileService')
    const { minecraftEngine } = await import('../minecraft/MinecraftEngine')

    const profile = await profileService.getActiveProfile()
    const targetIds = new Set<string>()
    if (profile.instanceId) {
      targetIds.add(profile.instanceId)
    }
    const runningId = minecraftEngine.getActiveInstanceId()
    if (runningId) {
      targetIds.add(runningId)
    }

    const instances = await instanceService.list()
    await Promise.all(
      instances
        .filter((inst) => inst.includePrimeMod && targetIds.has(inst.id))
        .map((inst) => launcherBridgeService.syncToInstance(inst.id))
    )

    return { ok: true }
  }
}

export const cosmeticService = new CosmeticService()
