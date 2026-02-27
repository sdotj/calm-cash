export type AuthMode = 'login' | 'register'
export type TransactionSource = 'MANUAL' | 'PLAID' | 'IMPORT'
export type BudgetPeriodType = 'WEEKLY' | 'MONTHLY'
export type BudgetStatus = 'ACTIVE' | 'ARCHIVED'

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
  name: string
  periodType: BudgetPeriodType
  startDate: string
  endDate: string
  currency: string
  status: BudgetStatus
  totalLimitCents: number
  createdAt: string
  updatedAt: string
  categoryLimits: BudgetCategoryLimit[]
}

export type BudgetCategoryLimit = {
  id: string
  categoryId: string
  categoryName: string
  limitCents: number
  colorHex: string | null
  spentCents: number
  remainingCents: number
  utilizationPct: number | null
  createdAt: string
  updatedAt: string
}

export type Txn = {
  id: string
  budgetId: string
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
  colorHex: string | null
  limitCents: number | null
  spentCents: number
  remainingCents: number | null
  utilizationPct: number | null
}

export type MonthlySummary = {
  budgetId: string
  budgetName: string
  periodType: BudgetPeriodType
  startDate: string
  endDate: string
  totalLimitCents: number
  totalSpentCents: number
  totalRemainingCents: number
  utilizationPct: number | null
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
