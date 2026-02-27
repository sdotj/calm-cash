import { useEffect, useMemo, useRef, useState } from 'react'
import type { FormEvent, ReactNode } from 'react'
import './features/dashboard/styles/index.css'
import { AuthScreen } from './features/auth/components/AuthScreen'
import { BudgetPanel } from './features/dashboard/components/BudgetPanel'
import { SummaryCards } from './features/dashboard/components/SummaryCards'
import { TopBar } from './features/dashboard/components/TopBar'
import { TransactionFormPanel } from './features/dashboard/components/TransactionFormPanel'
import { TransactionsPanel } from './features/dashboard/components/TransactionsPanel'
import { useAuth } from './features/auth/hooks/useAuth'
import { useDashboard } from './features/dashboard/hooks/useDashboard'
import type { AuthMode, BudgetPeriodType } from './types'
import { toErrorMessage } from './utils/errors'
import { dollarsToCents } from './utils/format'

type DashboardModal = 'create-budget' | 'add-transaction' | null

type DraftCategoryLimit = {
  id: string
  categoryId: string
  limitDollars: string
  colorHex: string
}

const BUDGET_COLOR_OPTIONS = ['#F25F5C', '#3A86FF', '#FF9F1C', '#2EC4B6', '#8E7DBE', '#E76F51', '#43AA8B', '#FF006E']
const MONTH_LABELS = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec']

function formatPeriodRange(startDate: string, endDate: string): string {
  const start = new Date(startDate)
  const end = new Date(endDate)
  const startLabel = start.toLocaleDateString('en-US', { month: 'short', day: 'numeric' })
  const endLabel = end.toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' })
  return `${startLabel} - ${endLabel}`
}

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

type DashboardModalProps = {
  title: string
  onClose: () => void
  children: ReactNode
}

function DashboardModal({ title, onClose, children }: DashboardModalProps) {
  return (
    <div className="dashboard-modal-backdrop" role="presentation" onClick={onClose}>
      <section
        className="dashboard-modal"
        role="dialog"
        aria-modal="true"
        aria-label={title}
        onClick={(event) => event.stopPropagation()}
      >
        <header className="dashboard-modal-header">
          <h3>{title}</h3>
          <button className="quiet-btn" type="button" onClick={onClose}>
            Close
          </button>
        </header>
        {children}
      </section>
    </div>
  )
}

type MonthPickerProps = {
  value: string
  onChange: (value: string) => void
}

function MonthPicker({ value, onChange }: MonthPickerProps) {
  const [isOpen, setIsOpen] = useState(false)
  const [displayYear, setDisplayYear] = useState(Number(value.slice(0, 4)))
  const rootRef = useRef<HTMLDivElement | null>(null)

  const selectedYear = Number(value.slice(0, 4))
  const selectedMonth = Number(value.slice(5, 7))

  useEffect(() => {
    if (!isOpen) {
      return
    }

    function handleOutsideClick(event: MouseEvent | TouchEvent) {
      const target = event.target as Node | null
      if (!target) {
        return
      }
      if (!rootRef.current?.contains(target)) {
        setIsOpen(false)
      }
    }

    function handleEscape(event: KeyboardEvent) {
      if (event.key === 'Escape') {
        setIsOpen(false)
      }
    }

    document.addEventListener('mousedown', handleOutsideClick)
    document.addEventListener('touchstart', handleOutsideClick)
    document.addEventListener('keydown', handleEscape)

    return () => {
      document.removeEventListener('mousedown', handleOutsideClick)
      document.removeEventListener('touchstart', handleOutsideClick)
      document.removeEventListener('keydown', handleEscape)
    }
  }, [isOpen])

  useEffect(() => {
    setDisplayYear(selectedYear)
  }, [selectedYear])

  const triggerLabel = useMemo(() => {
    const monthIndex = Math.max(0, Math.min(11, selectedMonth - 1))
    return `${MONTH_LABELS[monthIndex]} ${selectedYear}`
  }, [selectedMonth, selectedYear])

  return (
    <div className="month-picker" ref={rootRef}>
      <button className="dashboard-control-btn" type="button" onClick={() => setIsOpen((open) => !open)} aria-expanded={isOpen}>
        <span>{triggerLabel}</span>
        <svg viewBox="0 0 24 24" aria-hidden="true" className="month-trigger-icon">
          <path d="M7 2a1 1 0 0 1 1 1v1h8V3a1 1 0 1 1 2 0v1h1a2 2 0 0 1 2 2v12a3 3 0 0 1-3 3H6a3 3 0 0 1-3-3V6a2 2 0 0 1 2-2h1V3a1 1 0 0 1 1-1Zm12 8H5v8a1 1 0 0 0 1 1h12a1 1 0 0 0 1-1v-8ZM6 6a1 1 0 0 0-1 1v1h14V7a1 1 0 0 0-1-1H6Z" />
        </svg>
      </button>

      {isOpen ? (
        <div className="month-picker-popover">
          <div className="month-picker-head">
            <button className="quiet-btn month-year-nav" type="button" onClick={() => setDisplayYear((year) => year - 1)}>
              <svg viewBox="0 0 24 24" aria-hidden="true">
                <path d="M14.5 6 8.5 12l6 6" fill="none" stroke="currentColor" strokeWidth="2.4" strokeLinecap="round" strokeLinejoin="round" />
              </svg>
            </button>
            <strong>{displayYear}</strong>
            <button className="quiet-btn month-year-nav" type="button" onClick={() => setDisplayYear((year) => year + 1)}>
              <svg viewBox="0 0 24 24" aria-hidden="true">
                <path d="M9.5 6 15.5 12l-6 6" fill="none" stroke="currentColor" strokeWidth="2.4" strokeLinecap="round" strokeLinejoin="round" />
              </svg>
            </button>
          </div>

          <div className="month-grid">
            {MONTH_LABELS.map((label, index) => {
              const monthNumber = index + 1
              const monthValue = `${displayYear}-${String(monthNumber).padStart(2, '0')}`
              const isSelected = selectedYear === displayYear && selectedMonth === monthNumber
              return (
                <button
                  key={`${displayYear}-${label}`}
                  type="button"
                  className={`month-chip ${isSelected ? 'is-selected' : ''}`}
                  onClick={() => {
                    onChange(monthValue)
                    setIsOpen(false)
                  }}
                >
                  {label}
                </button>
              )
            })}
          </div>
        </div>
      ) : null}
    </div>
  )
}

function App() {
  const [dashboardTheme, setDashboardTheme] = useState<'light' | 'dark'>(() => {
    const storedTheme = window.localStorage.getItem('calm-cash-dashboard-theme')
    return storedTheme === 'dark' ? 'dark' : 'light'
  })
  const [authMode, setAuthMode] = useState<AuthMode>('login')
  const [authLoading, setAuthLoading] = useState(false)
  const [authError, setAuthError] = useState('')
  const [activeModal, setActiveModal] = useState<DashboardModal>(null)
  const [loginEmail, setLoginEmail] = useState('')
  const [loginPassword, setLoginPassword] = useState('')
  const [registerEmail, setRegisterEmail] = useState('')
  const [registerPassword, setRegisterPassword] = useState('')
  const [registerDisplayName, setRegisterDisplayName] = useState('')

  const [createBudgetName, setCreateBudgetName] = useState('')
  const [createBudgetPeriodType, setCreateBudgetPeriodType] = useState<BudgetPeriodType>('MONTHLY')
  const [createBudgetStartDate, setCreateBudgetStartDate] = useState('')
  const [createBudgetTotalDollars, setCreateBudgetTotalDollars] = useState('')
  const [createBudgetError, setCreateBudgetError] = useState('')
  const [draftCategoryLimits, setDraftCategoryLimits] = useState<DraftCategoryLimit[]>([])

  const { isAuthenticated, me, setMe, authenticate, logout, withValidAccess, clearSession } = useAuth()

  const {
    appLoading,
    globalError,
    selectedMonth,
    setSelectedMonth,
    selectedBudgetId,
    setSelectedBudgetId,
    categories,
    budgets,
    transactions,
    summary,
    alerts,
    categoryMap,
    unreadAlertsCount,
    newCategoryName,
    setNewCategoryName,
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
    onCreateBudgetWithLimits,
    onAddCategoryByName,
    onAddTransaction,
    onMarkAlertRead,
  } = useDashboard({
    isAuthenticated,
    setMe,
    withValidAccess,
    clearSession,
  })

  const hasBudget = Boolean(summary && selectedBudgetId)
  const budgetSelectValue = budgets.length > 0 ? selectedBudgetId || budgets[0].id : ''

  function openCreateBudgetModal() {
    setCreateBudgetName(`Budget for ${new Date(`${selectedMonth}-01`).toLocaleDateString('en-US', { month: 'long', year: 'numeric' })}`)
    setCreateBudgetPeriodType('MONTHLY')
    setCreateBudgetStartDate(firstDayOfMonth(selectedMonth))
    setCreateBudgetTotalDollars('')
    setCreateBudgetError('')
    setDraftCategoryLimits([buildDraftRow(categories[0]?.id ?? '', BUDGET_COLOR_OPTIONS[0])])
    setActiveModal('create-budget')
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
      setActiveModal(null)
    }
  }

  async function onAuthSubmit(event: FormEvent) {
    event.preventDefault()
    setAuthLoading(true)
    setAuthError('')

    try {
      if (authMode === 'login') {
        await authenticate('login', {
          email: loginEmail,
          password: loginPassword,
          displayName: '',
        })
      } else {
        await authenticate('register', {
          email: registerEmail,
          password: registerPassword,
          displayName: registerDisplayName,
        })
      }
    } catch (error) {
      setAuthError(
        toErrorMessage(
          error,
          authMode === 'register'
            ? 'We could not create your account. Please review your details and try again.'
            : 'We could not sign you in. Please check your credentials and try again.',
        ),
      )
    } finally {
      setAuthLoading(false)
    }
  }

  function handleAuthModeChange(mode: AuthMode) {
    setAuthMode(mode)
    setAuthError('')
  }

  useEffect(() => {
    if (isAuthenticated) {
      document.title = 'Calm Cash | Dashboard'
      return
    }
    document.title = authMode === 'register' ? 'Calm Cash | Create Account' : 'Calm Cash | Sign In'
  }, [authMode, isAuthenticated])

  useEffect(() => {
    window.localStorage.setItem('calm-cash-dashboard-theme', dashboardTheme)
  }, [dashboardTheme])

  useEffect(() => {
    const enableGlobalDark = isAuthenticated && dashboardTheme === 'dark'
    document.documentElement.classList.toggle('dashboard-theme-dark', enableGlobalDark)
    document.body.classList.toggle('dashboard-theme-dark', enableGlobalDark)

    return () => {
      document.documentElement.classList.remove('dashboard-theme-dark')
      document.body.classList.remove('dashboard-theme-dark')
    }
  }, [dashboardTheme, isAuthenticated])

  if (!isAuthenticated) {
    return (
      <AuthScreen
        authMode={authMode}
        authLoading={authLoading}
        authError={authError}
        loginEmail={loginEmail}
        loginPassword={loginPassword}
        registerEmail={registerEmail}
        registerPassword={registerPassword}
        registerDisplayName={registerDisplayName}
        onAuthModeChange={handleAuthModeChange}
        onLoginEmailChange={setLoginEmail}
        onLoginPasswordChange={setLoginPassword}
        onRegisterEmailChange={setRegisterEmail}
        onRegisterPasswordChange={setRegisterPassword}
        onRegisterDisplayNameChange={setRegisterDisplayName}
        onSubmit={onAuthSubmit}
      />
    )
  }

  return (
    <main className={`dashboard-page bg-white p-5 text-[var(--ink-900)] max-[980px]:p-3 ${dashboardTheme === 'dark' ? 'theme-dark' : ''}`}>
      <div className="dashboard-shell mx-auto max-w-[1180px]">
        <TopBar
          me={me}
          alerts={alerts}
          unreadAlertsCount={unreadAlertsCount}
          onMarkAlertRead={onMarkAlertRead}
          onLogout={logout}
          isDarkMode={dashboardTheme === 'dark'}
          onToggleTheme={() => setDashboardTheme((theme) => (theme === 'dark' ? 'light' : 'dark'))}
        />

        {globalError ? <p className="error-banner my-3 rounded-[var(--radius-sm)] border border-[#eab8b9] bg-[#fff5f5] px-3 py-2 font-semibold text-[var(--danger)]">{globalError}</p> : null}

        {!hasBudget ? (
          <section className="dashboard-empty-state panel mt-4 rounded-[var(--radius-md)] border border-[var(--line)] bg-[var(--surface-1)] px-4 py-8 text-center shadow-[0_8px_18px_rgba(28,69,21,0.04)]">
            <div className="dashboard-empty-controls mb-4 ml-auto flex w-max items-end justify-end gap-3 max-[980px]:ml-0 max-[980px]:w-full max-[980px]:items-stretch max-[980px]:gap-2">
              <MonthPicker value={selectedMonth} onChange={setSelectedMonth} />

              <select className="dashboard-control-select budget-control" value={budgetSelectValue} onChange={(event) => setSelectedBudgetId(event.target.value)}>
                {budgets.length === 0 ? <option value="">No budgets available</option> : null}
                {budgets.map((budget) => (
                  <option key={budget.id} value={budget.id}>
                    {budget.name}
                  </option>
                ))}
              </select>
            </div>

            <h2 className="m-0">No budget created yet</h2>
            <p className="mx-auto mb-4 mt-2 max-w-[56ch] text-[var(--ink-500)]">
              Create your first budget to start tracking category spending and transaction flow.
            </p>
            <button className="primary-btn" type="button" onClick={openCreateBudgetModal}>
              Create Budget
            </button>
          </section>
        ) : (
          <>
            <BudgetPanel
              summary={summary}
              onCreateBudgetClick={openCreateBudgetModal}
              controls={
                <>
                  <MonthPicker value={selectedMonth} onChange={setSelectedMonth} />
                  <select
                    className="dashboard-control-select budget-control"
                    value={budgetSelectValue}
                    onChange={(event) => setSelectedBudgetId(event.target.value)}
                  >
                    {budgets.map((budget) => (
                      <option key={budget.id} value={budget.id}>
                        {budget.name}
                      </option>
                    ))}
                  </select>
                </>
              }
            />

            <SummaryCards summary={summary} unreadAlertsCount={unreadAlertsCount} />

            <section className="dashboard-content mt-4 grid grid-cols-[minmax(0,0.95fr)_minmax(0,1.45fr)] gap-4 items-start max-[980px]:grid-cols-1">
              <div className="dashboard-current-col min-w-0">
                <TransactionFormPanel
                  hasBudget={Boolean(summary)}
                  budgetName={summary?.budgetName ?? 'No selected budget'}
                  budgetPeriodLabel={
                    summary ? formatPeriodRange(summary.startDate, summary.endDate) : 'Choose an active budget for this month'
                  }
                  totalSpentCents={summary?.totalSpentCents ?? 0}
                  totalRemainingCents={summary?.totalRemainingCents ?? 0}
                  onAddTransactionClick={() => setActiveModal('add-transaction')}
                />
              </div>
              <div className="dashboard-transactions-col min-w-0">
                <TransactionsPanel transactions={transactions} categoryNameById={categoryMap} />
              </div>
            </section>
          </>
        )}

        {appLoading ? <p className="loading-overlay fixed bottom-4 right-4 rounded-full bg-[#2a6e21] px-3 py-2 text-[0.85rem] text-[#f2ffe7]">Syncing latest data...</p> : null}
      </div>

      {activeModal === 'create-budget' ? (
        <DashboardModal title="Create Budget" onClose={() => setActiveModal(null)}>
          <form className="stack-form" onSubmit={(event) => void handleCreateBudgetSubmit(event)}>
            <label>
              Budget Name
              <input value={createBudgetName} onChange={(event) => setCreateBudgetName(event.target.value)} maxLength={150} required />
            </label>

            <div className="form-two-col">
              <label>
                Period Type
                <select
                  value={createBudgetPeriodType}
                  onChange={(event) => setCreateBudgetPeriodType(event.target.value as BudgetPeriodType)}
                >
                  <option value="MONTHLY">Monthly</option>
                  <option value="WEEKLY">Weekly</option>
                </select>
              </label>
              <label>
                Start Date
                <input
                  type="date"
                  value={createBudgetStartDate}
                  onChange={(event) => setCreateBudgetStartDate(event.target.value)}
                  required
                />
              </label>
            </div>

            <label>
              Total Budget ($)
              <input
                type="number"
                inputMode="decimal"
                step="0.01"
                min="0"
                value={createBudgetTotalDollars}
                onChange={(event) => setCreateBudgetTotalDollars(event.target.value)}
                required
              />
            </label>

            <div className="modal-section-head">
              <h4>Category Limits</h4>
              <button className="quiet-btn" type="button" onClick={addDraftCategoryLimitRow}>
                Add Category Limit
              </button>
            </div>

            <div className="draft-limits-list">
              {draftCategoryLimits.map((row) => (
                <div key={row.id} className="draft-limit-row">
                  <select
                    value={row.categoryId}
                    onChange={(event) => updateDraftCategoryLimit(row.id, { categoryId: event.target.value })}
                  >
                    <option value="">Select category</option>
                    {categories.map((category) => (
                      <option key={category.id} value={category.id}>
                        {category.name}
                      </option>
                    ))}
                  </select>

                  <input
                    type="number"
                    inputMode="decimal"
                    step="0.01"
                    min="0"
                    placeholder="Limit $"
                    value={row.limitDollars}
                    onChange={(event) => updateDraftCategoryLimit(row.id, { limitDollars: event.target.value })}
                  />

                  <div className="color-picker-grid" role="radiogroup" aria-label="Category color">
                    {BUDGET_COLOR_OPTIONS.map((color) => (
                      <button
                        key={`${row.id}-${color}`}
                        type="button"
                        className={`color-swatch ${row.colorHex === color ? 'is-active' : ''}`}
                        style={{ background: color }}
                        aria-label={`Select color ${color}`}
                        aria-pressed={row.colorHex === color}
                        onClick={() => updateDraftCategoryLimit(row.id, { colorHex: color })}
                      />
                    ))}
                  </div>

                  <button
                    className="quiet-btn"
                    type="button"
                    onClick={() => removeDraftCategoryLimitRow(row.id)}
                    disabled={draftCategoryLimits.length <= 1}
                  >
                    Remove
                  </button>
                </div>
              ))}
            </div>

            <div className="modal-section-head">
              <h4>Add New Category</h4>
            </div>

            <div className="inline-form">
              <input
                value={newCategoryName}
                onChange={(event) => setNewCategoryName(event.target.value)}
                placeholder="Create category without closing this modal"
                maxLength={100}
              />
              <button className="quiet-btn" type="button" onClick={() => void onAddCategoryByName(newCategoryName)}>
                Add
              </button>
            </div>

            {createBudgetError ? <p className="inline-error">{createBudgetError}</p> : null}

            <button className="primary-btn" type="submit">
              Create Budget
            </button>
          </form>
        </DashboardModal>
      ) : null}

      {activeModal === 'add-transaction' ? (
        <DashboardModal title="Add Transaction" onClose={() => setActiveModal(null)}>
          <form className="stack-form" onSubmit={(event) => void onAddTransaction(event)}>
            <label>
              Merchant
              <input
                value={newTxnMerchant}
                onChange={(event) => setNewTxnMerchant(event.target.value)}
                placeholder="Ex: Trader Joe's"
                maxLength={255}
                required
              />
            </label>
            <label>
              Description
              <input
                value={newTxnDescription}
                onChange={(event) => setNewTxnDescription(event.target.value)}
                placeholder="Optional note"
                maxLength={1000}
              />
            </label>
            <div className="form-two-col">
              <label>
                Amount ($)
                <input
                  type="number"
                  inputMode="decimal"
                  step="0.01"
                  value={newTxnAmountDollars}
                  onChange={(event) => setNewTxnAmountDollars(event.target.value)}
                  required
                />
              </label>
              <label>
                Date
                <input type="date" value={newTxnDate} onChange={(event) => setNewTxnDate(event.target.value)} required />
              </label>
            </div>
            <div className="form-two-col">
              <label>
                Source
                <select value={newTxnSource} onChange={(event) => setNewTxnSource(event.target.value as typeof newTxnSource)}>
                  <option value="MANUAL">Manual</option>
                  <option value="PLAID">Plaid</option>
                  <option value="IMPORT">Import</option>
                </select>
              </label>
              <label>
                Category
                <select value={newTxnCategoryId} onChange={(event) => setNewTxnCategoryId(event.target.value)}>
                  <option value="">Uncategorized</option>
                  {categories.map((category) => (
                    <option key={category.id} value={category.id}>
                      {category.name}
                    </option>
                  ))}
                </select>
              </label>
            </div>
            <button className="primary-btn" type="submit" disabled={!selectedBudgetId}>
              Save Transaction
            </button>
          </form>
        </DashboardModal>
      ) : null}
    </main>
  )
}

export default App
