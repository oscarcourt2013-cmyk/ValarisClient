import type { FriendEntry } from '../../shared/content-types'
import { socialService } from './SocialService'

function validateUsername(username: string): string | null {
  const trimmed = username.trim()
  if (trimmed.length < 3 || trimmed.length > 16) {
    return 'Username must be 3–16 characters.'
  }
  if (!/^[a-zA-Z0-9_]+$/.test(trimmed)) {
    return 'Only letters, numbers, and underscores.'
  }
  return null
}

/** Friends roster synced with Prime backend (launcher ↔ game). */
export class FriendsService {
  async list(): Promise<FriendEntry[]> {
    try {
      return await socialService.listFriends()
    } catch (err) {
      console.error('[friends]', err)
      return []
    }
  }

  async refreshAllStatuses(): Promise<FriendEntry[]> {
    return this.list()
  }

  async refreshStatus(_friendId: string): Promise<FriendEntry | null> {
    const list = await this.list()
    return list.find((f) => f.id === _friendId) || null
  }

  async add(username: string, note?: string): Promise<{ ok: boolean; error?: string }> {
    const err = validateUsername(username)
    if (err) return { ok: false, error: err }
    return socialService.requestFriend(username, note)
  }

  async accept(friendId: string): Promise<{ ok: boolean; error?: string }> {
    return socialService.acceptFriend(friendId)
  }

  async remove(friendId: string): Promise<{ ok: boolean; error?: string }> {
    return socialService.removeFriend(friendId)
  }

  async updateNote(friendId: string, activity: string): Promise<{ ok: boolean; error?: string }> {
    await socialService.updateNote(friendId, activity.trim())
    return { ok: true }
  }
}

export const friendsService = new FriendsService()
