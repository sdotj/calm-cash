import { useCallback, useEffect, useMemo, useState } from 'react'
import * as authApi from '../../../api/auth'
import type { AuthMode, AuthResponse, MeResponse } from '../../../types'
import { isUnauthorizedError } from '../../../utils/errors'

const TOKEN_KEY = 'calm_cash_auth'

type TokenState = {
  accessToken: string
  refreshToken: string
}

type UseAuthResult = {
  tokens: TokenState | null
  me: MeResponse | null
  setMe: (value: MeResponse | null) => void
  isAuthenticated: boolean
  authenticate: (mode: AuthMode, payload: { email: string; password: string; displayName: string }) => Promise<void>
  logout: () => Promise<void>
  withValidAccess: <T>(operation: (accessToken: string) => Promise<T>) => Promise<T>
  clearSession: () => void
}

export function useAuth(): UseAuthResult {
  const [tokens, setTokens] = useState<TokenState | null>(null)
  const [me, setMe] = useState<MeResponse | null>(null)

  useEffect(() => {
    const raw = localStorage.getItem(TOKEN_KEY)
    if (!raw) {
      return
    }

    try {
      const parsed = JSON.parse(raw) as AuthResponse
      if (parsed.accessToken && parsed.refreshToken) {
        setTokens({ accessToken: parsed.accessToken, refreshToken: parsed.refreshToken })
      }
    } catch {
      localStorage.removeItem(TOKEN_KEY)
    }
  }, [])

  const persistTokens = useCallback((nextTokens: AuthResponse) => {
    const next = {
      accessToken: nextTokens.accessToken,
      refreshToken: nextTokens.refreshToken,
    }

    setTokens(next)
    localStorage.setItem(TOKEN_KEY, JSON.stringify(next))
  }, [])

  const clearSession = useCallback(() => {
    setTokens(null)
    setMe(null)
    localStorage.removeItem(TOKEN_KEY)
  }, [])

  const authenticate = useCallback(
    async (mode: AuthMode, payload: { email: string; password: string; displayName: string }) => {
      const authResponse =
        mode === 'register'
          ? await authApi.register(payload)
          : await authApi.login({ email: payload.email, password: payload.password })

      persistTokens(authResponse)
    },
    [persistTokens],
  )

  const refreshSession = useCallback(async (): Promise<TokenState | null> => {
    if (!tokens?.refreshToken) {
      return null
    }

    try {
      const refreshed = await authApi.refresh(tokens.refreshToken)
      persistTokens(refreshed)
      return refreshed
    } catch {
      clearSession()
      return null
    }
  }, [clearSession, persistTokens, tokens?.refreshToken])

  const withValidAccess = useCallback(
    async <T>(operation: (accessToken: string) => Promise<T>): Promise<T> => {
      if (!tokens?.accessToken) {
        throw new Error('No active session')
      }

      try {
        return await operation(tokens.accessToken)
      } catch (error) {
        if (!isUnauthorizedError(error)) {
          throw error
        }

        const refreshed = await refreshSession()
        if (!refreshed?.accessToken) {
          throw error
        }

        return operation(refreshed.accessToken)
      }
    },
    [refreshSession, tokens?.accessToken],
  )

  const logout = useCallback(async () => {
    try {
      if (tokens?.refreshToken) {
        await authApi.logout(tokens.refreshToken)
      }
    } catch {
      // Ignore API logout failures, clear client session regardless.
    } finally {
      clearSession()
    }
  }, [clearSession, tokens?.refreshToken])

  return {
    tokens,
    me,
    setMe,
    isAuthenticated: useMemo(() => Boolean(tokens?.accessToken && tokens?.refreshToken), [tokens]),
    authenticate,
    logout,
    withValidAccess,
    clearSession,
  }
}
