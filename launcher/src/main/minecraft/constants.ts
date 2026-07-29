/** Launch-time instance configuration (from persisted instance). */
export interface InstanceLaunchConfig {
  id: string
  name: string
  minecraftVersion: string
  loader: 'vanilla' | 'fabric'
  fabricLoaderVersion: string
  fabricApiModrinthVersion: string
  includePrimeMod: boolean
  ramMb: number
  javaPath?: string
  jvmArgs: string[]
}

export function fabricVersionId(config: InstanceLaunchConfig): string {
  return `${config.minecraftVersion}-fabric${config.fabricLoaderVersion}`
}
