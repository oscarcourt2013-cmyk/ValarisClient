const MSMC_ERROR_MESSAGES: Record<string, string> = {
  'error.auth.minecraft.login':
    'Minecraft login failed. Check your internet connection and sign in again from Accounts.',
  'error.auth.minecraft.profile':
    'Could not load your Minecraft profile. Sign in again from Accounts.',
  'error.auth.minecraft.entitlements':
    'This Microsoft account does not own Minecraft Java Edition.',
  'error.auth.microsoft': 'Microsoft sign-in failed. Try again from Accounts.',
  'error.auth.xboxLive': 'Xbox Live authentication failed. Try again from Accounts.',
  'error.auth.xsts': 'Xbox session expired. Sign in again from Accounts.'
}

export function formatLaunchError(err: unknown): string {
  if (typeof err === 'string') {
    return err.trim() || 'Launch failed.'
  }

  if (err && typeof err === 'object') {
    const record = err as Record<string, unknown>

    if (typeof record.ts === 'string') {
      return MSMC_ERROR_MESSAGES[record.ts] ?? 'Microsoft session expired. Sign in again from Accounts.'
    }

    if (typeof record.message === 'string' && record.message.trim()) {
      return record.message
    }

    if (typeof record.error === 'string') {
      if (record.error === 'MissingLibraries' && Array.isArray(record.libraries)) {
        const names = record.libraries
          .map((lib) => (lib && typeof lib === 'object' && 'name' in lib ? String(lib.name) : ''))
          .filter(Boolean)
          .join(', ')
        return names ? `Missing libraries: ${names}` : 'Missing Minecraft libraries.'
      }

      if (record.error === 'CorruptedVersionJar') {
        return 'Minecraft version jar is missing or corrupted. Try launching again.'
      }

      if (record.error === 'MissingVersionJson') {
        return 'Minecraft version metadata is missing. Try launching again.'
      }

      if (record.error === 'Authenticator not found') {
        return 'No account session available for launch.'
      }

      if (typeof record.reason === 'string' && record.reason.trim()) {
        return record.reason
      }

      return record.error
    }
  }

  if (err instanceof Error && err.message.trim()) {
    return err.message
  }

  try {
    return JSON.stringify(err)
  } catch {
    return 'Launch failed.'
  }
}
