import type { FormEvent } from 'react'
import type { AuthMode } from '../../../types'
import { evaluatePasswordRequirements, getFormValidation } from '../utils/validation'

type AuthFormProps = {
  mode: AuthMode
  authLoading: boolean
  authError: string
  loginEmail: string
  loginPassword: string
  registerEmail: string
  registerPassword: string
  registerDisplayName: string
  onLoginEmailChange: (value: string) => void
  onLoginPasswordChange: (value: string) => void
  onRegisterEmailChange: (value: string) => void
  onRegisterPasswordChange: (value: string) => void
  onRegisterDisplayNameChange: (value: string) => void
  onSubmit: (event: FormEvent) => Promise<void>
  isPasswordVisible: boolean
  onRevealPassword: () => void
  onHidePassword: () => void
}

export function AuthForm({
  mode,
  authLoading,
  authError,
  loginEmail,
  loginPassword,
  registerEmail,
  registerPassword,
  registerDisplayName,
  onLoginEmailChange,
  onLoginPasswordChange,
  onRegisterEmailChange,
  onRegisterPasswordChange,
  onRegisterDisplayNameChange,
  onSubmit,
  isPasswordVisible,
  onRevealPassword,
  onHidePassword,
}: AuthFormProps) {
  const emailValue = mode === 'register' ? registerEmail : loginEmail
  const passwordValue = mode === 'register' ? registerPassword : loginPassword
  const displayNameValue = registerDisplayName
  const validation = getFormValidation(mode, emailValue, passwordValue, displayNameValue)
  const passwordChecks = evaluatePasswordRequirements(passwordValue)
  const passwordValid =
    passwordChecks.minLength && passwordChecks.upper && passwordChecks.lower && passwordChecks.number && passwordChecks.special

  return (
    <form onSubmit={(event) => void onSubmit(event)} className="stack-form auth-form grid gap-3.5 max-[980px]:gap-2" noValidate>
      <fieldset className="auth-fieldset m-0 grid border-0 p-0" disabled={authLoading}>
        <label>
          Email
          <input
            type="text"
            inputMode="email"
            autoComplete="username"
            autoCapitalize="none"
            autoCorrect="off"
            spellCheck={false}
            maxLength={254}
            value={emailValue}
            onChange={(event) =>
              mode === 'register' ? onRegisterEmailChange(event.target.value) : onLoginEmailChange(event.target.value)
            }
            aria-invalid={Boolean(validation.emailError)}
            placeholder={mode === 'register' ? 'you@example.com' : 'Enter your email'}
            required
          />
        </label>

        {mode === 'register' ? (
          <label>
            Display Name
            <input
              type="text"
              minLength={6}
              maxLength={100}
              autoComplete="name"
              value={displayNameValue}
              onChange={(event) => onRegisterDisplayNameChange(event.target.value)}
              aria-invalid={Boolean(validation.displayNameError)}
              placeholder="Choose a display name"
              required
            />
          </label>
        ) : null}

        <label>
          Password
          <div className="password-input-wrap relative">
            <input
              type={isPasswordVisible ? 'text' : 'password'}
              minLength={12}
              maxLength={128}
              autoComplete={mode === 'register' ? 'new-password' : 'current-password'}
              value={passwordValue}
              onChange={(event) =>
                mode === 'register' ? onRegisterPasswordChange(event.target.value) : onLoginPasswordChange(event.target.value)
              }
              aria-invalid={Boolean(validation.passwordError)}
              placeholder={mode === 'register' ? 'Create a strong password' : 'Enter your password'}
              required
            />
            <button
              type="button"
              className="password-eye-btn absolute right-1.5 top-1/2 inline-flex h-8 w-8 -translate-y-1/2 items-center justify-center rounded-full border-0 bg-transparent text-[#4f8646] hover:bg-[#edf7e8] focus-visible:bg-[#edf7e8] max-[980px]:h-6 max-[980px]:w-6"
              aria-label="Hold to show password"
              onPointerDown={onRevealPassword}
              onPointerUp={onHidePassword}
              onPointerCancel={onHidePassword}
              onPointerLeave={onHidePassword}
              onBlur={onHidePassword}
              onKeyDown={(event) => {
                if (event.key === ' ' || event.key === 'Enter') {
                  onRevealPassword()
                }
              }}
              onKeyUp={(event) => {
                if (event.key === ' ' || event.key === 'Enter') {
                  onHidePassword()
                }
              }}
            >
              <EyeIcon />
            </button>
          </div>
        </label>

        {mode === 'register' ? (
          <div className={`password-hint ${passwordValid ? 'is-valid' : ''}`} aria-live="polite">
            Password must include 12+ chars, upper, lower, number, and symbol.
          </div>
        ) : null}

        <button disabled={authLoading || !validation.canSubmit} type="submit" className="primary-btn">
          {authLoading ? (mode === 'register' ? 'Creating Account...' : 'Signing In...') : mode === 'login' ? 'Sign In' : 'Create Account'}
        </button>
      </fieldset>

      <div className="auth-feedback-slot min-h-[3.2rem] max-[980px]:min-h-[1.7rem]" aria-live="polite">
        {authLoading ? (
          <p className="inline-info" role="status">
            {mode === 'register' ? 'Creating your account securely...' : 'Signing you in securely...'}
          </p>
        ) : authError ? (
          <p className="inline-error" role="alert">
            <span className="inline-error-icon" aria-hidden="true">
              !
            </span>
            {authError}
          </p>
        ) : validation.firstError ? (
          <p className="inline-error" role="status">
            <span className="inline-error-icon" aria-hidden="true">
              !
            </span>
            {validation.firstError}
          </p>
        ) : null}
      </div>
    </form>
  )
}

function EyeIcon() {
  return (
    <svg viewBox="0 0 24 24" aria-hidden="true" focusable="false">
      <path
        d="M1.5 12s3.75-6.75 10.5-6.75S22.5 12 22.5 12s-3.75 6.75-10.5 6.75S1.5 12 1.5 12z"
        fill="none"
        stroke="currentColor"
        strokeWidth="1.8"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
      <circle cx="12" cy="12" r="3.2" fill="none" stroke="currentColor" strokeWidth="1.8" />
    </svg>
  )
}
