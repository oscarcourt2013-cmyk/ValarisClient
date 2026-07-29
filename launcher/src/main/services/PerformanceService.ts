import { execFile } from 'child_process'
import { cpus, totalmem } from 'os'
import { promisify } from 'util'
import type { HardwareProfile, PerformancePreset } from '../../shared/content-types'
import { PERFORMANCE_PRESETS } from '../../shared/ecosystem-catalog'
import { readOptionsLines, setOptionValue, writeOptionsLines } from '../content/options'
import { instanceService } from './InstanceService'
import { launcherBridgeService } from './LauncherBridgeService'
import { profileService } from './ProfileService'
import { settingsStore } from '../storage/SettingsStore'

const execFileAsync = promisify(execFile)

async function detectGpuWindows(): Promise<string> {
  try {
    const { stdout } = await execFileAsync('wmic', ['path', 'win32_VideoController', 'get', 'name'], {
      timeout: 5000
    })
    const lines = stdout
      .split('\n')
      .map((l) => l.trim())
      .filter((l) => l && l !== 'Name')
    return lines[0] ?? 'Unknown GPU'
  } catch {
    return 'Unknown GPU'
  }
}

async function detectGpu(): Promise<string> {
  if (process.platform === 'win32') {
    return detectGpuWindows()
  }
  return process.env['GPU_DEVICE'] ?? 'Unknown GPU'
}

export class PerformanceService {
  async getHardware(): Promise<HardwareProfile> {
    const cpuList = cpus()
    const cpu = cpuList[0]?.model?.trim() ?? 'Unknown CPU'
    const gpu = await detectGpu()
    const ramGb = Math.round(totalmem() / (1024 * 1024 * 1024))
    return { cpu, gpu, ramGb }
  }

  getPresets() {
    return PERFORMANCE_PRESETS
  }

  async getSelectedPreset(): Promise<PerformancePreset> {
    const settings = await settingsStore.load()
    return settings.performancePreset
  }

  async applyPreset(presetId: PerformancePreset, instanceId?: string): Promise<{ ok: boolean; error?: string }> {
    const preset = PERFORMANCE_PRESETS.find((p) => p.id === presetId)
    if (!preset) {
      return { ok: false, error: 'Unknown preset.' }
    }

    let targetId = instanceId
    if (!targetId) {
      const profile = await profileService.getActiveProfile()
      targetId = profile.instanceId
    }
    if (!targetId) {
      const fallback = await instanceService.getDefault()
      targetId = fallback?.id
    }
    if (!targetId) {
      return { ok: false, error: 'No instance to optimize.' }
    }

    const settings = await settingsStore.load()
    const jvmArgs = [...new Set([...settings.jvmArgs, '-XX:+UseG1GC'])]

    await instanceService.update({
      id: targetId,
      ramMb: Math.min(preset.ramMb, Math.floor((await this.getHardware()).ramGb * 1024 * 0.75)),
      jvmArgs
    })

    let lines = await readOptionsLines(targetId)
    lines = setOptionValue(lines, 'renderDistance', String(preset.renderDistance))
    lines = setOptionValue(lines, 'simulationDistance', String(Math.min(preset.renderDistance, 12)))
    lines = setOptionValue(
      lines,
      'maxFps',
      preset.id === 'ultra' ? '260' : preset.id === 'performance' ? '240' : '120'
    )
    lines = setOptionValue(lines, 'graphicsMode', preset.id === 'low' ? '0' : preset.id === 'ultra' ? '2' : '1')
    await writeOptionsLines(targetId, lines)

    await settingsStore.mutate((s) => {
      s.performancePreset = presetId
      s.defaultRamMb = preset.ramMb
    })

    const bridge = await launcherBridgeService.syncToInstance(targetId)
    if (!bridge.ok) {
      return { ok: false, error: bridge.error ?? 'Bridge sync failed.' }
    }

    return { ok: true }
  }
}

export const performanceService = new PerformanceService()
