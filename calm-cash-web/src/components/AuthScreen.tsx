import type { FormEvent } from 'react'
import type { AuthMode } from '../types'

type AuthScreenProps = {
  authMode: AuthMode
  authLoading: boolean
  authError: string
  email: string
  password: string
  displayName: string
  onAuthModeChange: (mode: AuthMode) => void
  onEmailChange: (value: string) => void
  onPasswordChange: (value: string) => void
  onDisplayNameChange: (value: string) => void
  onSubmit: (event: FormEvent) => Promise<void>
}

export function AuthScreen({
  authMode,
  authLoading,
  authError,
  email,
  password,
  displayName,
  onAuthModeChange,
  onEmailChange,
  onPasswordChange,
  onDisplayNameChange,
  onSubmit,
}: AuthScreenProps) {
  return (
    <main className="auth-screen">
      <section className="hero-panel">
        <p className="eyebrow">Calm Cash</p>
        <h1>Simple budgeting that stays clear as you scale.</h1>
        <p>
          A polished portfolio demo: strong backend foundations with a straightforward experience for everyday spending
          decisions.
        </p>
        <ul>
          <li>Quick auth and persistent sessions</li>
          <li>Monthly snapshots with category utilization</li>
          <li>Fast entry for budgets and transactions</li>
        </ul>
      </section>

      <section className="auth-card">
        <div className="auth-switch">
          <button className={authMode === 'login' ? 'active' : ''} onClick={() => onAuthModeChange('login')} type="button">
            Sign In
          </button>
          <button className={authMode === 'register' ? 'active' : ''} onClick={() => onAuthModeChange('register')} type="button">
            Create Account
          </button>
        </div>

        <form onSubmit={(event) => void onSubmit(event)} className="stack-form">
          <label>
            Email
            <input type="email" value={email} onChange={(event) => onEmailChange(event.target.value)} required />
          </label>

          {authMode === 'register' ? (
            <label>
              Display Name
              <input
                type="text"
                maxLength={100}
                value={displayName}
                onChange={(event) => onDisplayNameChange(event.target.value)}
                required
              />
            </label>
          ) : null}

          <label>
            Password
            <input
              type="password"
              minLength={12}
              maxLength={128}
              value={password}
              onChange={(event) => onPasswordChange(event.target.value)}
              required
            />
          </label>

          {authError ? <p className="error-text">{authError}</p> : null}

          <button disabled={authLoading} type="submit" className="primary-btn">
            {authLoading ? 'Working...' : authMode === 'login' ? 'Sign In' : 'Create Account'}
          </button>
        </form>
      </section>
    </main>
  )
}
