import { BUDGET_BASE_URL } from '../config/env'
import type {
  Alert,
  Budget,
  BudgetPeriodType,
  BudgetStatus,
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

type ListBudgetsParams = {
  periodType?: BudgetPeriodType
  status?: BudgetStatus
  startDateFrom?: string
  startDateTo?: string
}

function toSearchParams(params: ListBudgetsParams): string {
  const searchParams = new URLSearchParams()

  if (params.periodType) {
    searchParams.set('periodType', params.periodType)
  }
  if (params.status) {
    searchParams.set('status', params.status)
  }
  if (params.startDateFrom) {
    searchParams.set('startDateFrom', params.startDateFrom)
  }
  if (params.startDateTo) {
    searchParams.set('startDateTo', params.startDateTo)
  }

  const query = searchParams.toString()
  return query ? `?${query}` : ''
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

export function listBudgets(accessToken: string, params: ListBudgetsParams): Promise<Budget[]> {
  return requestJson<Budget[]>(`${BUDGET_BASE_URL}/api/budgets${toSearchParams(params)}`, {
    headers: authHeaders(accessToken),
  })
}

export function createBudget(
  accessToken: string,
  payload: {
    name: string
    periodType: BudgetPeriodType
    startDate: string
    currency?: string
    categoryLimits?: Array<{
      categoryId: string
      limitCents: number
      colorHex?: string
    }>
  },
): Promise<Budget | null> {
  return requestMaybeJson<Budget>(`${BUDGET_BASE_URL}/api/budgets`, {
    method: 'POST',
    headers: authHeaders(accessToken),
    body: JSON.stringify(payload),
  })
}

export function upsertBudgetCategoryLimit(
  accessToken: string,
  budgetId: string,
  categoryId: string,
  payload: {
    limitCents: number
    colorHex?: string
  },
): Promise<Budget | null> {
  return requestMaybeJson<Budget>(`${BUDGET_BASE_URL}/api/budgets/${budgetId}/categories/${categoryId}`, {
    method: 'PUT',
    headers: authHeaders(accessToken),
    body: JSON.stringify(payload),
  })
}

export function listBudgetTransactions(
  accessToken: string,
  budgetId: string,
  params?: {
    categoryId?: string
    minDate?: string
    maxDate?: string
    limit?: number
  },
): Promise<Txn[]> {
  const searchParams = new URLSearchParams()

  if (params?.categoryId) {
    searchParams.set('categoryId', params.categoryId)
  }
  if (params?.minDate) {
    searchParams.set('minDate', params.minDate)
  }
  if (params?.maxDate) {
    searchParams.set('maxDate', params.maxDate)
  }
  if (params?.limit) {
    searchParams.set('limit', String(params.limit))
  }

  const query = searchParams.toString()
  return requestJson<Txn[]>(`${BUDGET_BASE_URL}/api/budgets/${budgetId}/transactions${query ? `?${query}` : ''}`, {
    headers: authHeaders(accessToken),
  })
}

export function createTransaction(
  accessToken: string,
  payload: {
    budgetId: string
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

export function budgetSummary(accessToken: string, budgetId: string): Promise<MonthlySummary> {
  return requestJson<MonthlySummary>(`${BUDGET_BASE_URL}/api/budgets/${budgetId}/summary`, {
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
