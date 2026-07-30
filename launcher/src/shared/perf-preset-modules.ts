import type { PerformancePreset } from './content-types'

/** Module enable/disable applied into the mod profile via the launcher bridge. */
export interface PerfPresetModulePlan {
  enable: readonly string[]
  disable: readonly string[]
}

/**
 * Maps launcher performance presets to ValerisClient module IDs.
 * Heavy HUD overlays are turned off on competitive/low presets.
 */
export const PERF_PRESET_MODULES: Record<PerformancePreset, PerfPresetModulePlan> = {
  low: {
    enable: ['fps-booster', 'entity-culling', 'particle-optimizer', 'dynamic-fps', 'animation-optimizer'],
    disable: ['keystrokes', 'crosshair-editor', 'coordinates', 'target-hud', 'armor-hud', 'potion-hud']
  },
  balanced: {
    enable: ['dynamic-fps'],
    disable: ['fps-booster', 'entity-culling', 'particle-optimizer', 'animation-optimizer']
  },
  performance: {
    enable: ['fps-booster', 'entity-culling', 'particle-optimizer', 'animation-optimizer', 'dynamic-fps'],
    disable: ['keystrokes', 'crosshair-editor', 'target-hud']
  },
  ultra: {
    enable: [],
    disable: ['fps-booster', 'entity-culling', 'particle-optimizer', 'animation-optimizer', 'dynamic-fps']
  }
}

/** Merges enabled flags into an existing modules config object (does not wipe other keys). */
export function applyPerfPresetToModules(
  modules: Record<string, Record<string, unknown>>,
  preset: PerformancePreset
): Record<string, Record<string, unknown>> {
  const plan = PERF_PRESET_MODULES[preset]
  const next = { ...modules }
  for (const id of plan.enable) {
    next[id] = { ...(next[id] ?? {}), enabled: true }
  }
  for (const id of plan.disable) {
    next[id] = { ...(next[id] ?? {}), enabled: false }
  }
  return next
}
