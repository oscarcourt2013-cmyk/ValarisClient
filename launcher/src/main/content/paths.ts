import { join } from 'path'
import { getInstanceGameDir } from '../minecraft/paths'

export function getModsDir(instanceId: string): string {
  return join(getInstanceGameDir(instanceId), 'mods')
}

export function getResourcePacksDir(instanceId: string): string {
  return join(getInstanceGameDir(instanceId), 'resourcepacks')
}

export function getShaderPacksDir(instanceId: string): string {
  return join(getInstanceGameDir(instanceId), 'shaderpacks')
}

export function getContentMetaPath(instanceId: string): string {
  return join(getInstanceGameDir(instanceId), '.prime-content.json')
}

export function getOptionsPath(instanceId: string): string {
  return join(getInstanceGameDir(instanceId), 'options.txt')
}
