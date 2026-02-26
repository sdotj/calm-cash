import type { AppError } from '../types'

export class ApiError extends Error {
  status: number
  details?: string[]
  fieldErrors?: Record<string, string>

  constructor(status: number, message: string, details?: string[], fieldErrors?: Record<string, string>) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.details = details
    this.fieldErrors = fieldErrors
  }
}

export function isUnauthorizedError(error: unknown): boolean {
  return error instanceof ApiError && error.status === 401
}

export function toErrorMessage(error: unknown, fallback: string): string {
  if (error instanceof ApiError) {
    if (error.details?.length) {
      return `${error.message}: ${error.details.join(', ')}`
    }
    return error.message || fallback
  }

  if (error instanceof Error) {
    return error.message || fallback
  }

  return fallback
}

export async function toApiError(response: Response): Promise<ApiError> {
  const fallbackMessage = `Request failed with status ${response.status}`

  try {
    const payload = (await response.json()) as AppError
    return new ApiError(response.status, payload.message ?? fallbackMessage, payload.details, payload.fieldErrors)
  } catch {
    return new ApiError(response.status, fallbackMessage)
  }
}
