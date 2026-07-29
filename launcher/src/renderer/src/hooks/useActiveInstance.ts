import { useCallback, useEffect, useState } from 'react'
import { useAccounts } from '@renderer/context/AccountProvider'
import type { GameInstance } from '@shared/types'

export function useActiveInstance() {
  const { profile } = useAccounts()
  const [instance, setInstance] = useState<GameInstance | null>(null)
  const [instanceId, setInstanceId] = useState<string | null>(null)
  const [loading, setLoading] = useState(true)

  const refresh = useCallback(async () => {
    setLoading(true)
    let id = profile?.instanceId ?? null
    if (id) {
      const inst = await window.primeLauncher.instance.get(id)
      if (inst) {
        setInstanceId(id)
        setInstance(inst)
        setLoading(false)
        return
      }
    }
    const fallback = await window.primeLauncher.instance.getDefault()
    setInstanceId(fallback?.id ?? null)
    setInstance(fallback)
    setLoading(false)
  }, [profile?.instanceId])

  useEffect(() => {
    void refresh()
  }, [refresh])

  return { instance, instanceId, loading, refresh }
}
