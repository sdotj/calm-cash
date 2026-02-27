import { useCallback, useEffect, useMemo, useState } from 'react'
import type { FormEvent } from 'react'
import * as authApi from '../../../api/auth'
import * as budgetApi from '../../../api/budget'
import type {
  Alert,
  Budget,
  BudgetPeriodType,
  Category,
  MeResponse,
  MonthlySummary,
  TransactionSource,
  Txn,
} from '../../../types'
import { currentDateInputValue, currentMonthInputValue, dollarsToCents } from '../../../utils/format'
import { isUnauthorizedError, toErrorMessage } from '../../../utils/errors'

type UseDashboardParams = {
  isAuthenticated: boolean
  setMe: (value: MeResponse | null) => void
  withValidAccess: <T>(operation: (accessToken: string) => Promise<T>) => Promise<T>
  clearSession: () => void
}

type UseDashboardResult = {
  appLoading: boolean
  globalError: string
  selectedMonth: string
  setSelectedMonth: (value: string) => void
  selectedBudgetId: string
  setSelectedBudgetId: (value: string) => void
  selectedBudget: Budget | null
  categories: Category[]
  budgets: Budget[]
  transactions: Txn[]
  summary: MonthlySummary | null
  alerts: Alert[]
  categoryMap: Map<string, string>
  unreadAlertsCount: number
  newBudgetName: string
  setNewBudgetName: (value: string) => void
  newBudgetPeriodType: BudgetPeriodType
  setNewBudgetPeriodType: (value: BudgetPeriodType) => void
  newBudgetStartDate: string
  setNewBudgetStartDate: (value: string) => void
  newCategoryName: string
  setNewCategoryName: (value: string) => void
  newBudgetCategoryId: string
  setNewBudgetCategoryId: (value: string) => void
  newBudgetLimitDollars: string
  setNewBudgetLimitDollars: (value: string) => void
  newBudgetColorHex: string
  setNewBudgetColorHex: (value: string) => void
  newTxnMerchant: string
  setNewTxnMerchant: (value: string) => void
  newTxnDescription: string
  setNewTxnDescription: (value: string) => void
  newTxnAmountDollars: string
  setNewTxnAmountDollars: (value: string) => void
  newTxnDate: string
  setNewTxnDate: (value: string) => void
  newTxnSource: TransactionSource
  setNewTxnSource: (value: TransactionSource) => void
  newTxnCategoryId: string
  setNewTxnCategoryId: (value: string) => void
  onCreateBudget: (event: FormEvent) => Promise<void>
  onCreateBudgetWithLimits: (payload: {
    name: string
    periodType: BudgetPeriodType
    startDate: string
    categoryLimits: Array<{
      categoryId: string
      limitCents: number
      colorHex: string
    }>
  }) => Promise<boolean>
  onAddCategoryByName: (name: string) => Promise<boolean>
  onAddCategory: (event: FormEvent) => Promise<void>
  onSetBudgetCategoryLimit: (event: FormEvent) => Promise<void>
  onAddTransaction: (event: FormEvent) => Promise<void>
  onMarkAlertRead: (alertId: string) => Promise<void>
}

function firstDayOfMonth(month: string): string {
  return `${month}-01`
}

function lastDayOfMonth(month: string): string {
  const [yearString, monthString] = month.split('-')
  const year = Number(yearString)
  const monthIndex = Number(monthString)

  const date = new Date(Date.UTC(year, monthIndex, 0))
  return date.toISOString().slice(0, 10)
}

function monthLabel(month: string): string {
  const [yearString, monthString] = month.split('-')
  const year = Number(yearString)
  const monthIndex = Number(monthString) - 1
  return new Date(year, monthIndex, 1).toLocaleDateString('en-US', {
    month: 'long',
    year: 'numeric',
  })
}

export function useDashboard({ isAuthenticated, setMe, withValidAccess, clearSession }: UseDashboardParams): UseDashboardResult {
  const [appLoading, setAppLoading] = useState(false)
  const [globalError, setGlobalError] = useState('')

  const [selectedMonth, setSelectedMonth] = useState(currentMonthInputValue())
  const [selectedBudgetId, setSelectedBudgetId] = useState('')

  const [categories, setCategories] = useState<Category[]>([])
  const [budgets, setBudgets] = useState<Budget[]>([])
  const [transactions, setTransactions] = useState<Txn[]>([])
  const [summary, setSummary] = useState<MonthlySummary | null>(null)
  const [alerts, setAlerts] = useState<Alert[]>([])

  const [newBudgetName, setNewBudgetName] = useState(`Budget for ${monthLabel(currentMonthInputValue())}`)
  const [newBudgetPeriodType, setNewBudgetPeriodType] = useState<BudgetPeriodType>('MONTHLY')
  const [newBudgetStartDate, setNewBudgetStartDate] = useState(firstDayOfMonth(currentMonthInputValue()))

  const [newCategoryName, setNewCategoryName] = useState('')
  const [newBudgetCategoryId, setNewBudgetCategoryId] = useState('')
  const [newBudgetLimitDollars, setNewBudgetLimitDollars] = useState('')
  const [newBudgetColorHex, setNewBudgetColorHex] = useState('#68B531')

  const [newTxnMerchant, setNewTxnMerchant] = useState('')
  const [newTxnDescription, setNewTxnDescription] = useState('')
  const [newTxnAmountDollars, setNewTxnAmountDollars] = useState('')
  const [newTxnDate, setNewTxnDate] = useState(currentDateInputValue())
  const [newTxnSource, setNewTxnSource] = useState<TransactionSource>('MANUAL')
  const [newTxnCategoryId, setNewTxnCategoryId] = useState('')

  const categoryMap = useMemo(() => {
    return new Map(categories.map((category) => [category.id, category.name]))
  }, [categories])

  const selectedBudget = useMemo(() => budgets.find((budget) => budget.id === selectedBudgetId) ?? null, [budgets, selectedBudgetId])

  const unreadAlertsCount = alerts.filter((alert) => !alert.readAt).length

  const handleSessionExpired = useCallback(() => {
    clearSession()
    setGlobalError('Your session expired. Please sign in again.')
  }, [clearSession])

  const loadProfileAndData = useCallback(
    async (preferredBudgetId?: string) => {
      if (!isAuthenticated) {
        return
      }

      setAppLoading(true)
      setGlobalError('')

      try {
        const startDateFrom = firstDayOfMonth(selectedMonth)
        const startDateTo = lastDayOfMonth(selectedMonth)

        const [meResponse, categoriesResponse, budgetsResponse, alertsResponse] = await Promise.all([
          withValidAccess((accessToken) => authApi.me(accessToken)),
          withValidAccess((accessToken) => budgetApi.listCategories(accessToken)),
          withValidAccess((accessToken) =>
            budgetApi.listBudgets(accessToken, {
              periodType: 'MONTHLY',
              status: 'ACTIVE',
              startDateFrom,
              startDateTo,
            }),
          ),
          withValidAccess((accessToken) => budgetApi.listAlerts(accessToken, false, 10)),
        ])

        setMe(meResponse)
        setCategories(categoriesResponse)
        setBudgets(budgetsResponse)
        setAlerts(alertsResponse)

        const resolvedBudgetId =
          preferredBudgetId && budgetsResponse.some((budget) => budget.id === preferredBudgetId)
            ? preferredBudgetId
            : budgetsResponse.some((budget) => budget.id === selectedBudgetId)
              ? selectedBudgetId
              : budgetsResponse[0]?.id ?? ''

        if (resolvedBudgetId !== selectedBudgetId) {
          setSelectedBudgetId(resolvedBudgetId)
        }

        if (!newBudgetCategoryId && categoriesResponse[0]) {
          setNewBudgetCategoryId(categoriesResponse[0].id)
        }

        if (!newTxnCategoryId && categoriesResponse[0]) {
          setNewTxnCategoryId(categoriesResponse[0].id)
        }

        if (!resolvedBudgetId) {
          setSummary(null)
          setTransactions([])
          return
        }

        const [summaryResponse, txnsResponse] = await Promise.all([
          withValidAccess((accessToken) => budgetApi.budgetSummary(accessToken, resolvedBudgetId)),
          withValidAccess((accessToken) => budgetApi.listBudgetTransactions(accessToken, resolvedBudgetId, { limit: 100 })),
        ])

        setSummary(summaryResponse)
        setTransactions(txnsResponse)
      } catch (error) {
        if (isUnauthorizedError(error)) {
          handleSessionExpired()
          return
        }

        setGlobalError(toErrorMessage(error, 'Unable to load your dashboard data.'))
      } finally {
        setAppLoading(false)
      }
    },
    [
      handleSessionExpired,
      isAuthenticated,
      newBudgetCategoryId,
      newTxnCategoryId,
      selectedBudgetId,
      selectedMonth,
      setMe,
      withValidAccess,
    ],
  )

  useEffect(() => {
    void loadProfileAndData()
  }, [loadProfileAndData])

  useEffect(() => {
    setNewBudgetStartDate(firstDayOfMonth(selectedMonth))
    setNewBudgetName(`Budget for ${monthLabel(selectedMonth)}`)
  }, [selectedMonth])

  const onCreateBudget = useCallback(
    async (event: FormEvent) => {
      event.preventDefault()

      if (!newBudgetName.trim() || !newBudgetStartDate) {
        setGlobalError('Budget requires a name and start date.')
        return
      }

      setGlobalError('')

      try {
        const createdBudget = await withValidAccess((accessToken) =>
          budgetApi.createBudget(accessToken, {
            name: newBudgetName.trim(),
            periodType: newBudgetPeriodType,
            startDate: newBudgetStartDate,
            currency: 'USD',
            categoryLimits: [],
          }),
        )

        if (createdBudget?.id) {
          setSelectedBudgetId(createdBudget.id)
          await loadProfileAndData(createdBudget.id)
        } else {
          await loadProfileAndData()
        }
      } catch (error) {
        if (isUnauthorizedError(error)) {
          handleSessionExpired()
          return
        }

        setGlobalError(toErrorMessage(error, 'Unable to create budget.'))
      }
    },
    [handleSessionExpired, loadProfileAndData, newBudgetName, newBudgetPeriodType, newBudgetStartDate, withValidAccess],
  )

  const onCreateBudgetWithLimits = useCallback(
    async (payload: {
      name: string
      periodType: BudgetPeriodType
      startDate: string
      categoryLimits: Array<{
        categoryId: string
        limitCents: number
        colorHex: string
      }>
    }): Promise<boolean> => {
      if (!payload.name.trim() || !payload.startDate) {
        setGlobalError('Budget requires a name and start date.')
        return false
      }

      setGlobalError('')

      try {
        const createdBudget = await withValidAccess((accessToken) =>
          budgetApi.createBudget(accessToken, {
            name: payload.name.trim(),
            periodType: payload.periodType,
            startDate: payload.startDate,
            currency: 'USD',
            categoryLimits: payload.categoryLimits,
          }),
        )

        if (createdBudget?.id) {
          setSelectedBudgetId(createdBudget.id)
          await loadProfileAndData(createdBudget.id)
        } else {
          await loadProfileAndData()
        }
        return true
      } catch (error) {
        if (isUnauthorizedError(error)) {
          handleSessionExpired()
          return false
        }

        setGlobalError(toErrorMessage(error, 'Unable to create budget.'))
        return false
      }
    },
    [handleSessionExpired, loadProfileAndData, withValidAccess],
  )

  const onAddCategory = useCallback(
    async (event: FormEvent) => {
      event.preventDefault()
      if (!newCategoryName.trim()) {
        return
      }

      setGlobalError('')

      try {
        await withValidAccess((accessToken) => budgetApi.createCategory(accessToken, newCategoryName.trim()))
        setNewCategoryName('')
        await loadProfileAndData()
      } catch (error) {
        if (isUnauthorizedError(error)) {
          handleSessionExpired()
          return
        }

        setGlobalError(toErrorMessage(error, 'Unable to create category.'))
      }
    },
    [handleSessionExpired, loadProfileAndData, newCategoryName, withValidAccess],
  )

  const onAddCategoryByName = useCallback(
    async (name: string): Promise<boolean> => {
      if (!name.trim()) {
        return false
      }

      setGlobalError('')

      try {
        await withValidAccess((accessToken) => budgetApi.createCategory(accessToken, name.trim()))
        setNewCategoryName('')
        await loadProfileAndData()
        return true
      } catch (error) {
        if (isUnauthorizedError(error)) {
          handleSessionExpired()
          return false
        }

        setGlobalError(toErrorMessage(error, 'Unable to create category.'))
        return false
      }
    },
    [handleSessionExpired, loadProfileAndData, withValidAccess],
  )

  const onSetBudgetCategoryLimit = useCallback(
    async (event: FormEvent) => {
      event.preventDefault()

      const cents = dollarsToCents(newBudgetLimitDollars)
      if (!selectedBudgetId || !newBudgetCategoryId || cents === null || cents <= 0) {
        setGlobalError('Choose a budget/category and a positive limit amount.')
        return
      }

      setGlobalError('')

      try {
        await withValidAccess((accessToken) =>
          budgetApi.upsertBudgetCategoryLimit(accessToken, selectedBudgetId, newBudgetCategoryId, {
            limitCents: cents,
            colorHex: newBudgetColorHex,
          }),
        )
        setNewBudgetLimitDollars('')
        await loadProfileAndData(selectedBudgetId)
      } catch (error) {
        if (isUnauthorizedError(error)) {
          handleSessionExpired()
          return
        }

        setGlobalError(toErrorMessage(error, 'Unable to update category limit.'))
      }
    },
    [
      handleSessionExpired,
      loadProfileAndData,
      newBudgetCategoryId,
      newBudgetColorHex,
      newBudgetLimitDollars,
      selectedBudgetId,
      withValidAccess,
    ],
  )

  const onAddTransaction = useCallback(
    async (event: FormEvent) => {
      event.preventDefault()

      const cents = dollarsToCents(newTxnAmountDollars)
      if (!selectedBudgetId || !newTxnMerchant.trim() || cents === null || !newTxnDate) {
        setGlobalError('Transaction requires a budget, merchant, amount, and date.')
        return
      }

      setGlobalError('')

      try {
        await withValidAccess((accessToken) =>
          budgetApi.createTransaction(accessToken, {
            budgetId: selectedBudgetId,
            categoryId: newTxnCategoryId || null,
            merchant: newTxnMerchant.trim(),
            description: newTxnDescription.trim() || null,
            amountCents: cents,
            transactionDate: newTxnDate,
            source: newTxnSource,
          }),
        )

        setNewTxnMerchant('')
        setNewTxnDescription('')
        setNewTxnAmountDollars('')
        setNewTxnDate(currentDateInputValue())
        setNewTxnSource('MANUAL')

        await loadProfileAndData(selectedBudgetId)
      } catch (error) {
        if (isUnauthorizedError(error)) {
          handleSessionExpired()
          return
        }

        setGlobalError(toErrorMessage(error, 'Unable to create transaction.'))
      }
    },
    [
      handleSessionExpired,
      loadProfileAndData,
      newTxnAmountDollars,
      newTxnCategoryId,
      newTxnDate,
      newTxnDescription,
      newTxnMerchant,
      newTxnSource,
      selectedBudgetId,
      withValidAccess,
    ],
  )

  const onMarkAlertRead = useCallback(
    async (alertId: string) => {
      try {
        await withValidAccess((accessToken) => budgetApi.markAlertRead(accessToken, alertId))
        await loadProfileAndData(selectedBudgetId)
      } catch (error) {
        if (isUnauthorizedError(error)) {
          handleSessionExpired()
          return
        }

        setGlobalError(toErrorMessage(error, 'Unable to update alert.'))
      }
    },
    [handleSessionExpired, loadProfileAndData, selectedBudgetId, withValidAccess],
  )

  return {
    appLoading,
    globalError,
    selectedMonth,
    setSelectedMonth,
    selectedBudgetId,
    setSelectedBudgetId,
    selectedBudget,
    categories,
    budgets,
    transactions,
    summary,
    alerts,
    categoryMap,
    unreadAlertsCount,
    newBudgetName,
    setNewBudgetName,
    newBudgetPeriodType,
    setNewBudgetPeriodType,
    newBudgetStartDate,
    setNewBudgetStartDate,
    newCategoryName,
    setNewCategoryName,
    newBudgetCategoryId,
    setNewBudgetCategoryId,
    newBudgetLimitDollars,
    setNewBudgetLimitDollars,
    newBudgetColorHex,
    setNewBudgetColorHex,
    newTxnMerchant,
    setNewTxnMerchant,
    newTxnDescription,
    setNewTxnDescription,
    newTxnAmountDollars,
    setNewTxnAmountDollars,
    newTxnDate,
    setNewTxnDate,
    newTxnSource,
    setNewTxnSource,
    newTxnCategoryId,
    setNewTxnCategoryId,
    onCreateBudget,
    onCreateBudgetWithLimits,
    onAddCategoryByName,
    onAddCategory,
    onSetBudgetCategoryLimit,
    onAddTransaction,
    onMarkAlertRead,
  }
}
