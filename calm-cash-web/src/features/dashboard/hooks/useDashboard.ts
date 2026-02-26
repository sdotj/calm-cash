import { useCallback, useEffect, useMemo, useState } from 'react'
import type { FormEvent } from 'react'
import * as authApi from '../../../api/auth'
import * as budgetApi from '../../../api/budget'
import type { Alert, Budget, Category, MeResponse, MonthlySummary, TransactionSource, Txn } from '../../../types'
import { currentDateInputValue, currentMonthInputValue, dollarsToCents, formatMonthToApi } from '../../../utils/format'
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
  categories: Category[]
  budgets: Budget[]
  transactions: Txn[]
  summary: MonthlySummary | null
  alerts: Alert[]
  categoryMap: Map<string, string>
  unreadAlertsCount: number
  newCategoryName: string
  setNewCategoryName: (value: string) => void
  newBudgetCategoryId: string
  setNewBudgetCategoryId: (value: string) => void
  newBudgetLimitDollars: string
  setNewBudgetLimitDollars: (value: string) => void
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
  onAddCategory: (event: FormEvent) => Promise<void>
  onSetBudget: (event: FormEvent) => Promise<void>
  onAddTransaction: (event: FormEvent) => Promise<void>
  onMarkAlertRead: (alertId: string) => Promise<void>
}

export function useDashboard({ isAuthenticated, setMe, withValidAccess, clearSession }: UseDashboardParams): UseDashboardResult {
  const [appLoading, setAppLoading] = useState(false)
  const [globalError, setGlobalError] = useState('')

  const [selectedMonth, setSelectedMonth] = useState(currentMonthInputValue())
  const [categories, setCategories] = useState<Category[]>([])
  const [budgets, setBudgets] = useState<Budget[]>([])
  const [transactions, setTransactions] = useState<Txn[]>([])
  const [summary, setSummary] = useState<MonthlySummary | null>(null)
  const [alerts, setAlerts] = useState<Alert[]>([])

  const [newCategoryName, setNewCategoryName] = useState('')
  const [newBudgetCategoryId, setNewBudgetCategoryId] = useState('')
  const [newBudgetLimitDollars, setNewBudgetLimitDollars] = useState('')

  const [newTxnMerchant, setNewTxnMerchant] = useState('')
  const [newTxnDescription, setNewTxnDescription] = useState('')
  const [newTxnAmountDollars, setNewTxnAmountDollars] = useState('')
  const [newTxnDate, setNewTxnDate] = useState(currentDateInputValue())
  const [newTxnSource, setNewTxnSource] = useState<TransactionSource>('MANUAL')
  const [newTxnCategoryId, setNewTxnCategoryId] = useState('')

  const categoryMap = useMemo(() => {
    return new Map(categories.map((category) => [category.id, category.name]))
  }, [categories])

  const unreadAlertsCount = alerts.filter((alert) => !alert.readAt).length

  const handleSessionExpired = useCallback(() => {
    clearSession()
    setGlobalError('Your session expired. Please sign in again.')
  }, [clearSession])

  const loadProfileAndData = useCallback(async () => {
    if (!isAuthenticated) {
      return
    }

    setAppLoading(true)
    setGlobalError('')

    try {
      const monthParam = formatMonthToApi(selectedMonth)

      const [meResponse, categoriesResponse, budgetsResponse, txnsResponse, summaryResponse, alertsResponse] = await Promise.all([
        withValidAccess((accessToken) => authApi.me(accessToken)),
        withValidAccess((accessToken) => budgetApi.listCategories(accessToken)),
        withValidAccess((accessToken) => budgetApi.listBudgets(accessToken, monthParam)),
        withValidAccess((accessToken) => budgetApi.listTransactions(accessToken, monthParam)),
        withValidAccess((accessToken) => budgetApi.monthlySummary(accessToken, monthParam)),
        withValidAccess((accessToken) => budgetApi.listAlerts(accessToken, false, 10)),
      ])

      setMe(meResponse)
      setCategories(categoriesResponse)
      setBudgets(budgetsResponse)
      setTransactions(txnsResponse)
      setSummary(summaryResponse)
      setAlerts(alertsResponse)

      if (!newBudgetCategoryId && categoriesResponse[0]) {
        setNewBudgetCategoryId(categoriesResponse[0].id)
      }

      if (!newTxnCategoryId && categoriesResponse[0]) {
        setNewTxnCategoryId(categoriesResponse[0].id)
      }
    } catch (error) {
      if (isUnauthorizedError(error)) {
        handleSessionExpired()
        return
      }

      setGlobalError(toErrorMessage(error, 'Unable to load your dashboard data.'))
    } finally {
      setAppLoading(false)
    }
  }, [
    handleSessionExpired,
    isAuthenticated,
    newBudgetCategoryId,
    newTxnCategoryId,
    selectedMonth,
    setMe,
    withValidAccess,
  ])

  useEffect(() => {
    void loadProfileAndData()
  }, [loadProfileAndData])

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

  const onSetBudget = useCallback(
    async (event: FormEvent) => {
      event.preventDefault()

      const cents = dollarsToCents(newBudgetLimitDollars)
      if (!newBudgetCategoryId || cents === null || cents <= 0) {
        setGlobalError('Set a category and a positive budget amount.')
        return
      }

      setGlobalError('')

      try {
        await withValidAccess((accessToken) =>
          budgetApi.upsertBudget(accessToken, formatMonthToApi(selectedMonth), newBudgetCategoryId, cents),
        )
        setNewBudgetLimitDollars('')
        await loadProfileAndData()
      } catch (error) {
        if (isUnauthorizedError(error)) {
          handleSessionExpired()
          return
        }

        setGlobalError(toErrorMessage(error, 'Unable to update budget.'))
      }
    },
    [handleSessionExpired, loadProfileAndData, newBudgetCategoryId, newBudgetLimitDollars, selectedMonth, withValidAccess],
  )

  const onAddTransaction = useCallback(
    async (event: FormEvent) => {
      event.preventDefault()

      const cents = dollarsToCents(newTxnAmountDollars)
      if (!newTxnMerchant.trim() || cents === null || !newTxnDate) {
        setGlobalError('Transaction requires merchant, amount, and date.')
        return
      }

      setGlobalError('')

      try {
        await withValidAccess((accessToken) =>
          budgetApi.createTransaction(accessToken, {
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

        await loadProfileAndData()
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
      withValidAccess,
    ],
  )

  const onMarkAlertRead = useCallback(
    async (alertId: string) => {
      try {
        await withValidAccess((accessToken) => budgetApi.markAlertRead(accessToken, alertId))
        await loadProfileAndData()
      } catch (error) {
        if (isUnauthorizedError(error)) {
          handleSessionExpired()
          return
        }

        setGlobalError(toErrorMessage(error, 'Unable to update alert.'))
      }
    },
    [handleSessionExpired, loadProfileAndData, withValidAccess],
  )

  return {
    appLoading,
    globalError,
    selectedMonth,
    setSelectedMonth,
    categories,
    budgets,
    transactions,
    summary,
    alerts,
    categoryMap,
    unreadAlertsCount,
    newCategoryName,
    setNewCategoryName,
    newBudgetCategoryId,
    setNewBudgetCategoryId,
    newBudgetLimitDollars,
    setNewBudgetLimitDollars,
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
    onAddCategory,
    onSetBudget,
    onAddTransaction,
    onMarkAlertRead,
  }
}
