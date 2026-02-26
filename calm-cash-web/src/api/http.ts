import { toApiError } from '../utils/errors'

export async function requestJson<T>(url: string, init: RequestInit): Promise<T> {
  const response = await fetch(url, init)

  if (!response.ok) {
    throw await toApiError(response)
  }

  return (await response.json()) as T
}

export async function requestMaybeJson<T>(url: string, init: RequestInit): Promise<T | null> {
  const response = await fetch(url, init)

  if (!response.ok) {
    throw await toApiError(response)
  }

  if (response.status === 204) {
    return null
  }

  try {
    return (await response.json()) as T
  } catch {
    return null
  }
}
