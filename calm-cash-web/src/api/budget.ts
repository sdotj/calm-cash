import { BUDGET_BASE_URL } from '../config/env'
import type {
  Alert,
  Budget,
  Category,
  MonthlySummary,
  TransactionSource,
  Txn,
} from '../types'
import { requestJson, requestMaybeJson } from './http'

function authHeaders(accessToken: string): HeadersInit {
  return {
    'Content-Type': 'application/json',
    Authorization: `Bearer ${accessToken}`,
  }
}

export function listCategories(accessToken: string): Promise<Category[]> {
  return requestJson<Category[]>(`${BUDGET_BASE_URL}/api/categories`, {
    headers: authHeaders(accessToken),
  })
}

export function createCategory(accessToken: string, name: string): Promise<Category | null> {
  return requestMaybeJson<Category>(`${BUDGET_BASE_URL}/api/categories`, {
    method: 'POST',
    headers: authHeaders(accessToken),
    body: JSON.stringify({ name }),
  })
}

export function listBudgets(accessToken: string, month: string): Promise<Budget[]> {
  return requestJson<Budget[]>(`${BUDGET_BASE_URL}/api/budgets?month=${month}`, {
    headers: authHeaders(accessToken),
  })
}

export function upsertBudget(accessToken: string, month: string, categoryId: string, limitCents: number): Promise<Budget | null> {
  return requestMaybeJson<Budget>(`${BUDGET_BASE_URL}/api/budgets/${month}/${categoryId}`, {
    method: 'PUT',
    headers: authHeaders(accessToken),
    body: JSON.stringify({ limitCents }),
  })
}

export function listTransactions(accessToken: string, month: string): Promise<Txn[]> {
  return requestJson<Txn[]>(`${BUDGET_BASE_URL}/api/transactions?month=${month}`, {
    headers: authHeaders(accessToken),
  })
}

export function createTransaction(
  accessToken: string,
  payload: {
    categoryId: string | null
    merchant: string
    description: string | null
    amountCents: number
    transactionDate: string
    source: TransactionSource
  },
): Promise<Txn | null> {
  return requestMaybeJson<Txn>(`${BUDGET_BASE_URL}/api/transactions`, {
    method: 'POST',
    headers: authHeaders(accessToken),
    body: JSON.stringify(payload),
  })
}

export function monthlySummary(accessToken: string, month: string): Promise<MonthlySummary> {
  return requestJson<MonthlySummary>(`${BUDGET_BASE_URL}/api/monthly-summary?month=${month}`, {
    headers: authHeaders(accessToken),
  })
}

export function listAlerts(accessToken: string, unreadOnly = false, limit = 10): Promise<Alert[]> {
  return requestJson<Alert[]>(`${BUDGET_BASE_URL}/api/alerts?unreadOnly=${unreadOnly}&limit=${limit}`, {
    headers: authHeaders(accessToken),
  })
}

export function markAlertRead(accessToken: string, alertId: string): Promise<Alert | null> {
  return requestMaybeJson<Alert>(`${BUDGET_BASE_URL}/api/alerts/${alertId}/read`, {
    method: 'PATCH',
    headers: authHeaders(accessToken),
  })
}
