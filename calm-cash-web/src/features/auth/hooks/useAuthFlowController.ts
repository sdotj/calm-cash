import { useState } from 'react'
import type { FormEvent } from 'react'
import type { AuthMode } from '../../../types'
import { ApiError, toErrorMessage } from '../../../utils/errors'

type UseAuthFlowControllerParams = {
  authenticate: (
    mode: AuthMode,
    payload: { email: string; password: string; displayName: string },
  ) => Promise<void>
}

type UseAuthFlowControllerResult = {
  authMode: AuthMode
  authLoading: boolean
  authError: string
  loginEmail: string
  setLoginEmail: (value: string) => void
  loginPassword: string
  setLoginPassword: (value: string) => void
  registerEmail: string
  setRegisterEmail: (value: string) => void
  registerPassword: string
  setRegisterPassword: (value: string) => void
  registerDisplayName: string
  setRegisterDisplayName: (value: string) => void
  onAuthSubmit: (event: FormEvent) => Promise<void>
  handleAuthModeChange: (mode: AuthMode) => void
}

export function useAuthFlowController({ authenticate }: UseAuthFlowControllerParams): UseAuthFlowControllerResult {
  const [authMode, setAuthMode] = useState<AuthMode>('login')
  const [authLoading, setAuthLoading] = useState(false)
  const [authError, setAuthError] = useState('')
  const [loginEmail, setLoginEmail] = useState('')
  const [loginPassword, setLoginPassword] = useState('')
  const [registerEmail, setRegisterEmail] = useState('')
  const [registerPassword, setRegisterPassword] = useState('')
  const [registerDisplayName, setRegisterDisplayName] = useState('')

  async function onAuthSubmit(event: FormEvent) {
    event.preventDefault()
    setAuthLoading(true)
    setAuthError('')

    try {
      if (authMode === 'login') {
        await authenticate('login', {
          email: loginEmail,
          password: loginPassword,
          displayName: '',
        })
      } else {
        await authenticate('register', {
          email: registerEmail,
          password: registerPassword,
          displayName: registerDisplayName,
        })
      }
    } catch (error) {
      if (
        authMode === 'login' &&
        error instanceof ApiError &&
        (error.status === 400 || error.status === 401 || error.message.toLowerCase().includes('validation failed'))
      ) {
        setAuthError('Incorrect email or password. Please try again.')
        return
      }

      setAuthError(
        toErrorMessage(
          error,
          authMode === 'register'
            ? 'We could not create your account. Please review your details and try again.'
            : 'We could not sign you in. Please check your credentials and try again.',
        ),
      )
    } finally {
      setAuthLoading(false)
    }
  }

  function handleAuthModeChange(mode: AuthMode) {
    setAuthMode(mode)
    setAuthError('')
  }

  return {
    authMode,
    authLoading,
    authError,
    loginEmail,
    setLoginEmail,
    loginPassword,
    setLoginPassword,
    registerEmail,
    setRegisterEmail,
    registerPassword,
    setRegisterPassword,
    registerDisplayName,
    setRegisterDisplayName,
    onAuthSubmit,
    handleAuthModeChange,
  }
}
