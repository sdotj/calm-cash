import { useEffect, useRef, useState } from 'react'
import type { FormEvent } from 'react'
import type { AuthMode } from '../../../types'
import calmCashLogo from '../../../assets/calm-cash-full.png'
import { SegmentedTabs } from '../../../components/shared/SegmentedTabs'
import { AuthForm } from './AuthForm'
import { getFormValidation } from '../utils/validation'
import '../styles/index.css'

type AuthScreenProps = {
  authMode: AuthMode
  authLoading: boolean
  authError: string
  loginEmail: string
  loginPassword: string
  registerEmail: string
  registerPassword: string
  registerDisplayName: string
  onAuthModeChange: (mode: AuthMode) => void
  onLoginEmailChange: (value: string) => void
  onLoginPasswordChange: (value: string) => void
  onRegisterEmailChange: (value: string) => void
  onRegisterPasswordChange: (value: string) => void
  onRegisterDisplayNameChange: (value: string) => void
  onSubmit: (event: FormEvent) => Promise<void>
}

export function AuthScreen({
  authMode,
  authLoading,
  authError,
  loginEmail,
  loginPassword,
  registerEmail,
  registerPassword,
  registerDisplayName,
  onAuthModeChange,
  onLoginEmailChange,
  onLoginPasswordChange,
  onRegisterEmailChange,
  onRegisterPasswordChange,
  onRegisterDisplayNameChange,
  onSubmit,
}: AuthScreenProps) {
  const transitionTimerRef = useRef<number | null>(null)
  const [visibleMode, setVisibleMode] = useState<AuthMode>(authMode)
  const [leavingMode, setLeavingMode] = useState<AuthMode | null>(null)
  const [transitionDirection, setTransitionDirection] = useState<'to-register' | 'to-login'>('to-register')
  const [showLoginPassword, setShowLoginPassword] = useState(false)
  const [showRegisterPassword, setShowRegisterPassword] = useState(false)

  useEffect(() => {
    return () => {
      if (transitionTimerRef.current !== null) {
        window.clearTimeout(transitionTimerRef.current)
      }
    }
  }, [])

  async function handleSubmit(event: FormEvent) {
    const validation = getFormValidation(
      authMode,
      authMode === 'register' ? registerEmail : loginEmail,
      authMode === 'register' ? registerPassword : loginPassword,
      registerDisplayName,
    )

    if (!validation.canSubmit) {
      event.preventDefault()
      return
    }

    await onSubmit(event)
  }

  function handleModeChange(nextMode: AuthMode) {
    if (authLoading || nextMode === authMode) {
      return
    }

    setTransitionDirection(nextMode === 'register' ? 'to-register' : 'to-login')
    setLeavingMode(authMode)
    setVisibleMode(nextMode)
    setShowLoginPassword(false)
    setShowRegisterPassword(false)
    onAuthModeChange(nextMode)

    if (transitionTimerRef.current !== null) {
      window.clearTimeout(transitionTimerRef.current)
    }
    transitionTimerRef.current = window.setTimeout(() => {
      setLeavingMode(null)
      transitionTimerRef.current = null
    }, 260)
  }

  function isPasswordVisible(mode: AuthMode): boolean {
    return mode === 'register' ? showRegisterPassword : showLoginPassword
  }

  function revealPassword(mode: AuthMode) {
    if (mode === 'register') {
      setShowRegisterPassword(true)
    } else {
      setShowLoginPassword(true)
    }
  }

  function hidePassword(mode: AuthMode) {
    if (mode === 'register') {
      setShowRegisterPassword(false)
    } else {
      setShowLoginPassword(false)
    }
  }

  return (
    <main className="auth-screen relative mx-auto grid min-h-screen max-w-[940px] content-center gap-4 bg-white px-4 py-5 max-[980px]:min-h-0 max-[980px]:grid-cols-1 max-[980px]:content-start max-[980px]:gap-3 max-[980px]:p-3">
      <div className="auth-currency-bg" aria-hidden="true">
        {Array.from({ length: 16 }).map((_, index) => (
          <span key={`currency-${index}`}>
            <span className="currency-glyph">$</span>
          </span>
        ))}
      </div>
      <section className="hero-panel flex h-[var(--auth-panel-height)] flex-col items-center justify-center gap-2 rounded-[var(--radius-lg)] border border-[var(--line)] bg-[#f4f8f1] p-4 shadow-[0_10px_24px_rgba(28,69,21,0.08)] max-[980px]:h-[264px] max-[980px]:p-3">
        <img
          className="hero-logo h-auto w-full max-w-[320px] drop-shadow-[0_6px_12px_rgba(18,56,13,0.12)] max-[980px]:max-w-[270px]"
          src={calmCashLogo}
          alt="Calm Cash logo"
        />
        <p className="hero-tagline m-0 text-center text-[clamp(1rem,1.7vw,1.25rem)] font-semibold tracking-[0.03em] text-[#2a6522] max-[980px]:text-base">
          Budgeting. Stress-free.
        </p>
      </section>

      <section className="auth-card flex h-[var(--auth-panel-height)] flex-col justify-start overflow-visible rounded-[var(--radius-lg)] border border-[var(--line)] bg-[var(--surface-1)] px-4 py-3.5 shadow-[0_10px_24px_rgba(28,69,21,0.08)] max-[980px]:h-[392px] max-[980px]:p-3.5">
        <SegmentedTabs
          className="auth-switch"
          ariaLabel="Authentication mode"
          tabs={[
            { id: 'login', label: 'Sign In' },
            { id: 'register', label: 'Create Account' },
          ]}
          value={authMode}
          onChange={handleModeChange}
          layout={{ transitionMs: 220, padding: '0.2rem', gap: '0.2rem' }}
          theme={{
            trackBg: 'var(--surface-2)',
            indicatorBg: '#ffffff',
            activeText: 'var(--ink-900)',
            inactiveText: 'var(--ink-700)',
            indicatorShadow: '0 4px 8px rgba(0, 0, 0, 0.06)',
          }}
        />

        <div className="auth-forms-stage relative overflow-hidden" aria-live="polite">
          {leavingMode ? (
            <div
              className={`auth-form-panel is-leaving ${
                transitionDirection === 'to-register' ? 'leave-to-left' : 'leave-to-right'
              }`}
            >
              <AuthForm
                mode={leavingMode}
                authLoading={authLoading}
                authError={authError}
                loginEmail={loginEmail}
                loginPassword={loginPassword}
                registerEmail={registerEmail}
                registerPassword={registerPassword}
                registerDisplayName={registerDisplayName}
                onLoginEmailChange={onLoginEmailChange}
                onLoginPasswordChange={onLoginPasswordChange}
                onRegisterEmailChange={onRegisterEmailChange}
                onRegisterPasswordChange={onRegisterPasswordChange}
                onRegisterDisplayNameChange={onRegisterDisplayNameChange}
                onSubmit={handleSubmit}
                isPasswordVisible={isPasswordVisible(leavingMode)}
                onRevealPassword={() => revealPassword(leavingMode)}
                onHidePassword={() => hidePassword(leavingMode)}
              />
            </div>
          ) : null}
          <div
            className={`auth-form-panel ${leavingMode ? 'is-entering' : ''} ${
              leavingMode ? (transitionDirection === 'to-register' ? 'enter-from-right' : 'enter-from-left') : ''
            }`}
          >
            <AuthForm
              mode={visibleMode}
              authLoading={authLoading}
              authError={authError}
              loginEmail={loginEmail}
              loginPassword={loginPassword}
              registerEmail={registerEmail}
              registerPassword={registerPassword}
              registerDisplayName={registerDisplayName}
              onLoginEmailChange={onLoginEmailChange}
              onLoginPasswordChange={onLoginPasswordChange}
              onRegisterEmailChange={onRegisterEmailChange}
              onRegisterPasswordChange={onRegisterPasswordChange}
              onRegisterDisplayNameChange={onRegisterDisplayNameChange}
              onSubmit={handleSubmit}
              isPasswordVisible={isPasswordVisible(visibleMode)}
              onRevealPassword={() => revealPassword(visibleMode)}
              onHidePassword={() => hidePassword(visibleMode)}
            />
          </div>
        </div>
      </section>
    </main>
  )
}
