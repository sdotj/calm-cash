import type { AuthMode } from '../../../types'

export function isValidEmail(value: string): boolean {
  return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value.trim())
}

export function evaluatePasswordRequirements(value: string): {
  minLength: boolean
  upper: boolean
  lower: boolean
  number: boolean
  special: boolean
} {
  return {
    minLength: value.length >= 12,
    upper: /[A-Z]/.test(value),
    lower: /[a-z]/.test(value),
    number: /\d/.test(value),
    special: /[^A-Za-z0-9]/.test(value),
  }
}

export function getFormValidation(mode: AuthMode, emailValue: string, passwordValue: string, displayNameValue: string): {
  canSubmit: boolean
  firstError: string
  emailError: string
  displayNameError: string
  passwordError: string
} {
  const result = {
    canSubmit: false,
    firstError: '',
    emailError: '',
    displayNameError: '',
    passwordError: '',
  }

  const email = emailValue.trim()
  const password = passwordValue
  const displayName = displayNameValue.trim()

  if (email && (email.length > 254 || !isValidEmail(email))) {
    result.emailError = 'Please enter a valid email address.'
  }

  if (mode === 'register') {
    if (displayName && (displayName.length < 6 || displayName.length > 100)) {
      result.displayNameError = 'Display name must be at least 6 characters.'
    }
  }

  if (password.length > 128) {
    result.passwordError = 'Password must be at most 128 characters.'
  } else if (mode === 'register') {
    const checks = evaluatePasswordRequirements(password)
    if (password && (!checks.minLength || !checks.upper || !checks.lower || !checks.number || !checks.special)) {
      result.passwordError = 'Password does not meet the required complexity.'
    }
  }

  result.firstError = result.emailError || result.displayNameError || result.passwordError || ''

  const hasRequiredLoginFields = email.length > 0 && password.length > 0
  const hasRequiredRegisterFields = email.length > 0 && password.length > 0 && displayName.length >= 6
  const hasRequiredFields = mode === 'register' ? hasRequiredRegisterFields : hasRequiredLoginFields

  result.canSubmit = hasRequiredFields && !result.firstError
  return result
}
