import { useState } from 'react'
import type { FormEvent } from 'react'
import type { Category, BudgetPeriodType } from '../../../types'
import { dollarsToCents } from '../../../utils/format'
import type { DraftCategoryLimit } from '../components/CreateBudgetModal'

type UseCreateBudgetDraftParams = {
  selectedMonth: string
  categories: Category[]
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
  onCreated: () => void
}

type UseCreateBudgetDraftResult = {
  createBudgetName: string
  setCreateBudgetName: (value: string) => void
  createBudgetPeriodType: BudgetPeriodType
  setCreateBudgetPeriodType: (value: BudgetPeriodType) => void
  createBudgetStartDate: string
  setCreateBudgetStartDate: (value: string) => void
  createBudgetTotalDollars: string
  setCreateBudgetTotalDollars: (value: string) => void
  createBudgetError: string
  draftCategoryLimits: DraftCategoryLimit[]
  openCreateBudgetModal: () => void
  addDraftCategoryLimitRow: () => void
  removeDraftCategoryLimitRow: (rowId: string) => void
  updateDraftCategoryLimit: (rowId: string, patch: Partial<DraftCategoryLimit>) => void
  handleCreateBudgetSubmit: (event: FormEvent) => Promise<void>
}

const BUDGET_COLOR_OPTIONS = ['#F25F5C', '#3A86FF', '#FF9F1C', '#2EC4B6', '#8E7DBE', '#E76F51', '#43AA8B', '#FF006E']

function firstDayOfMonth(month: string): string {
  return `${month}-01`
}

function buildDraftRow(categoryId = '', colorHex = BUDGET_COLOR_OPTIONS[0]): DraftCategoryLimit {
  return {
    id: `${Date.now()}-${Math.random()}`,
    categoryId,
    limitDollars: '',
    colorHex,
  }
}

export function useCreateBudgetDraft({
  selectedMonth,
  categories,
  onCreateBudgetWithLimits,
  onCreated,
}: UseCreateBudgetDraftParams): UseCreateBudgetDraftResult {
  const [createBudgetName, setCreateBudgetName] = useState('')
  const [createBudgetPeriodType, setCreateBudgetPeriodType] = useState<BudgetPeriodType>('MONTHLY')
  const [createBudgetStartDate, setCreateBudgetStartDate] = useState('')
  const [createBudgetTotalDollars, setCreateBudgetTotalDollars] = useState('')
  const [createBudgetError, setCreateBudgetError] = useState('')
  const [draftCategoryLimits, setDraftCategoryLimits] = useState<DraftCategoryLimit[]>([])

  function openCreateBudgetModal() {
    setCreateBudgetName(`Budget for ${new Date(`${selectedMonth}-01`).toLocaleDateString('en-US', { month: 'long', year: 'numeric' })}`)
    setCreateBudgetPeriodType('MONTHLY')
    setCreateBudgetStartDate(firstDayOfMonth(selectedMonth))
    setCreateBudgetTotalDollars('')
    setCreateBudgetError('')
    setDraftCategoryLimits([buildDraftRow(categories[0]?.id ?? '', BUDGET_COLOR_OPTIONS[0])])
  }

  function addDraftCategoryLimitRow() {
    setDraftCategoryLimits((rows) => [...rows, buildDraftRow('', BUDGET_COLOR_OPTIONS[rows.length % BUDGET_COLOR_OPTIONS.length])])
  }

  function removeDraftCategoryLimitRow(rowId: string) {
    setDraftCategoryLimits((rows) => rows.filter((row) => row.id !== rowId))
  }

  function updateDraftCategoryLimit(rowId: string, patch: Partial<DraftCategoryLimit>) {
    setDraftCategoryLimits((rows) => rows.map((row) => (row.id === rowId ? { ...row, ...patch } : row)))
  }

  async function handleCreateBudgetSubmit(event: FormEvent) {
    event.preventDefault()

    const totalLimitCents = dollarsToCents(createBudgetTotalDollars)
    if (!createBudgetName.trim() || !createBudgetStartDate || totalLimitCents === null || totalLimitCents <= 0) {
      setCreateBudgetError('Provide budget name, start date, and a positive total budget amount.')
      return
    }

    const parsedLimits = draftCategoryLimits
      .map((row) => ({
        categoryId: row.categoryId,
        limitCents: dollarsToCents(row.limitDollars),
        colorHex: row.colorHex,
      }))
      .filter((row) => row.categoryId)

    if (!parsedLimits.length || parsedLimits.some((row) => row.limitCents === null || row.limitCents <= 0)) {
      setCreateBudgetError('Add at least one category limit with a positive amount.')
      return
    }

    const uniqueCategoryIds = new Set(parsedLimits.map((row) => row.categoryId))
    if (uniqueCategoryIds.size !== parsedLimits.length) {
      setCreateBudgetError('Each category can only be added once.')
      return
    }

    const categoryTotal = parsedLimits.reduce((sum, row) => sum + (row.limitCents ?? 0), 0)
    if (categoryTotal !== totalLimitCents) {
      setCreateBudgetError('Category limits must add up exactly to the total budget amount.')
      return
    }

    const created = await onCreateBudgetWithLimits({
      name: createBudgetName,
      periodType: createBudgetPeriodType,
      startDate: createBudgetStartDate,
      categoryLimits: parsedLimits.map((row) => ({
        categoryId: row.categoryId,
        limitCents: row.limitCents ?? 0,
        colorHex: row.colorHex,
      })),
    })

    if (created) {
      onCreated()
    }
  }

  return {
    createBudgetName,
    setCreateBudgetName,
    createBudgetPeriodType,
    setCreateBudgetPeriodType,
    createBudgetStartDate,
    setCreateBudgetStartDate,
    createBudgetTotalDollars,
    setCreateBudgetTotalDollars,
    createBudgetError,
    draftCategoryLimits,
    openCreateBudgetModal,
    addDraftCategoryLimitRow,
    removeDraftCategoryLimitRow,
    updateDraftCategoryLimit,
    handleCreateBudgetSubmit,
  }
}
