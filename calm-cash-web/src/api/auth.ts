import { AUTH_BASE_URL } from '../config/env'
import type { AuthResponse, MeResponse } from '../types'
import { requestJson, requestMaybeJson } from './http'

export function register(payload: { email: string; password: string; displayName: string }): Promise<AuthResponse> {
  return requestJson<AuthResponse>(`${AUTH_BASE_URL}/auth/register`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload),
  })
}

export function login(payload: { email: string; password: string }): Promise<AuthResponse> {
  return requestJson<AuthResponse>(`${AUTH_BASE_URL}/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload),
  })
}

export function refresh(refreshToken: string): Promise<AuthResponse> {
  return requestJson<AuthResponse>(`${AUTH_BASE_URL}/auth/refresh`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ refreshToken }),
  })
}

export function logout(refreshToken: string): Promise<null> {
  return requestMaybeJson<null>(`${AUTH_BASE_URL}/auth/logout`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ refreshToken }),
  })
}

export function me(accessToken: string): Promise<MeResponse> {
  return requestJson<MeResponse>(`${AUTH_BASE_URL}/auth/me`, {
    headers: {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${accessToken}`,
    },
  })
}
