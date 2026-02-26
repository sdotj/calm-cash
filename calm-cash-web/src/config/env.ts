function readUrl(name: 'VITE_AUTH_API_URL' | 'VITE_BUDGET_API_URL', fallback: string): string {
  const raw = import.meta.env[name] ?? fallback

  let parsed: URL
  try {
    parsed = new URL(raw)
  } catch {
    throw new Error(`${name} must be a valid absolute URL. Received: ${raw}`)
  }

  const isLocalhost = parsed.hostname === 'localhost' || parsed.hostname === '127.0.0.1'
  const isSecure = parsed.protocol === 'https:'

  if (!isSecure && !isLocalhost) {
    throw new Error(`${name} must use HTTPS outside localhost. Received: ${raw}`)
  }

  if (import.meta.env.PROD && !isSecure) {
    throw new Error(`${name} must use HTTPS in production builds. Received: ${raw}`)
  }

  return parsed.origin
}

export const AUTH_BASE_URL = readUrl('VITE_AUTH_API_URL', 'http://localhost:8081')
export const BUDGET_BASE_URL = readUrl('VITE_BUDGET_API_URL', 'http://localhost:8082')
