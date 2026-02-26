export type AuthMode = 'login' | 'register'
export type TransactionSource = 'MANUAL' | 'PLAID' | 'IMPORT'

export type AuthResponse = {
  accessToken: string
  refreshToken: string
}

export type MeResponse = {
  userId: string
  email: string
  displayName: string
}

export type Category = {
  id: string
  name: string
  createdAt: string
}

export type Budget = {
  id: string
  month: string
  categoryId: string
  limitCents: number
  createdAt: string
}

export type Txn = {
  id: string
  categoryId: string | null
  merchant: string
  description: string | null
  amountCents: number
  transactionDate: string
  source: TransactionSource
  createdAt: string
  updatedAt: string
}

export type MonthlyCategorySummary = {
  categoryId: string
  categoryName: string
  spentCents: number
  budgetLimitCents: number | null
  utilizationPct: number | null
}

export type MonthlySummary = {
  month: string
  incomeCents: number
  expenseCents: number
  netCents: number
  categories: MonthlyCategorySummary[]
}

export type Alert = {
  id: string
  type: 'BUDGET_80' | 'BUDGET_100' | 'SYSTEM'
  message: string
  createdAt: string
  readAt: string | null
}

export type AppError = {
  status?: number
  message?: string
  details?: string[]
  fieldErrors?: Record<string, string>
}
