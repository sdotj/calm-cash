import type { FormEvent } from 'react'
import { AddTransactionModal } from './AddTransactionModal'
import { BudgetControls } from './BudgetControls'
import { BudgetPanel } from './BudgetPanel'
import { CreateBudgetModal, type DraftCategoryLimit } from './CreateBudgetModal'
import { SummaryCards } from './SummaryCards'
import { TopBar } from './TopBar'
import { TransactionFormPanel } from './TransactionFormPanel'
import { TransactionsPanel } from './TransactionsPanel'
import type { Alert, Budget, Category, MeResponse, MonthlySummary, TransactionSource, Txn } from '../../../types'

type DashboardPageProps = {
  dashboardTheme: 'light' | 'dark'
  me: MeResponse | null
  alerts: Alert[]
  unreadAlertsCount: number
  onMarkAlertRead: (alertId: string) => Promise<void>
  onLogout: () => Promise<void>
  onToggleTheme: () => void
  globalError: string
  hasBudget: boolean
  selectedMonth: string
  onSelectedMonthChange: (value: string) => void
  budgetSelectValue: string
  budgets: Budget[]
  onSelectedBudgetIdChange: (value: string) => void
  onOpenCreateBudgetModal: () => void
  onOpenAddTransactionModal: () => void
  summary: MonthlySummary | null
  transactions: Txn[]
  categoryMap: Map<string, string>
  appLoading: boolean
  showCreateBudgetModal: boolean
  showAddTransactionModal: boolean
  onCloseCreateBudgetModal: () => void
  onCloseAddTransactionModal: () => void
  createBudgetName: string
  onCreateBudgetNameChange: (value: string) => void
  createBudgetPeriodType: 'WEEKLY' | 'MONTHLY'
  onCreateBudgetPeriodTypeChange: (value: 'WEEKLY' | 'MONTHLY') => void
  createBudgetStartDate: string
  onCreateBudgetStartDateChange: (value: string) => void
  createBudgetTotalDollars: string
  onCreateBudgetTotalDollarsChange: (value: string) => void
  draftCategoryLimits: DraftCategoryLimit[]
  categories: Category[]
  onAddDraftCategoryLimitRow: () => void
  onUpdateDraftCategoryLimit: (rowId: string, patch: Partial<DraftCategoryLimit>) => void
  onRemoveDraftCategoryLimitRow: (rowId: string) => void
  newCategoryName: string
  onNewCategoryNameChange: (value: string) => void
  onAddCategoryByName: (name: string) => Promise<boolean>
  createBudgetError: string
  onCreateBudgetSubmit: (event: FormEvent) => Promise<void>
  onAddTransactionSubmit: (event: FormEvent) => Promise<void>
  newTxnMerchant: string
  onNewTxnMerchantChange: (value: string) => void
  newTxnDescription: string
  onNewTxnDescriptionChange: (value: string) => void
  newTxnAmountDollars: string
  onNewTxnAmountDollarsChange: (value: string) => void
  newTxnDate: string
  onNewTxnDateChange: (value: string) => void
  newTxnSource: TransactionSource
  onNewTxnSourceChange: (value: TransactionSource) => void
  newTxnCategoryId: string
  onNewTxnCategoryIdChange: (value: string) => void
  selectedBudgetId: string
  budgetPeriodLabel: string
}

export function DashboardPage({
  dashboardTheme,
  me,
  alerts,
  unreadAlertsCount,
  onMarkAlertRead,
  onLogout,
  onToggleTheme,
  globalError,
  hasBudget,
  selectedMonth,
  onSelectedMonthChange,
  budgetSelectValue,
  budgets,
  onSelectedBudgetIdChange,
  onOpenCreateBudgetModal,
  onOpenAddTransactionModal,
  summary,
  transactions,
  categoryMap,
  appLoading,
  showCreateBudgetModal,
  showAddTransactionModal,
  onCloseCreateBudgetModal,
  onCloseAddTransactionModal,
  createBudgetName,
  onCreateBudgetNameChange,
  createBudgetPeriodType,
  onCreateBudgetPeriodTypeChange,
  createBudgetStartDate,
  onCreateBudgetStartDateChange,
  createBudgetTotalDollars,
  onCreateBudgetTotalDollarsChange,
  draftCategoryLimits,
  categories,
  onAddDraftCategoryLimitRow,
  onUpdateDraftCategoryLimit,
  onRemoveDraftCategoryLimitRow,
  newCategoryName,
  onNewCategoryNameChange,
  onAddCategoryByName,
  createBudgetError,
  onCreateBudgetSubmit,
  onAddTransactionSubmit,
  newTxnMerchant,
  onNewTxnMerchantChange,
  newTxnDescription,
  onNewTxnDescriptionChange,
  newTxnAmountDollars,
  onNewTxnAmountDollarsChange,
  newTxnDate,
  onNewTxnDateChange,
  newTxnSource,
  onNewTxnSourceChange,
  newTxnCategoryId,
  onNewTxnCategoryIdChange,
  selectedBudgetId,
  budgetPeriodLabel,
}: DashboardPageProps) {
  return (
    <main className={`dashboard-page bg-white p-5 text-[var(--ink-900)] max-[980px]:p-3 ${dashboardTheme === 'dark' ? 'theme-dark' : ''}`}>
      <div className="dashboard-shell mx-auto max-w-[1180px]">
        <TopBar
          me={me}
          alerts={alerts}
          unreadAlertsCount={unreadAlertsCount}
          onMarkAlertRead={onMarkAlertRead}
          onLogout={onLogout}
          isDarkMode={dashboardTheme === 'dark'}
          onToggleTheme={onToggleTheme}
        />

        {globalError ? <p className="error-banner my-3 rounded-[var(--radius-sm)] border border-[#eab8b9] bg-[#fff5f5] px-3 py-2 font-semibold text-[var(--danger)]">{globalError}</p> : null}

        {!hasBudget ? (
          <section className="dashboard-empty-state panel mt-4 rounded-[var(--radius-md)] border border-[var(--line)] bg-[var(--surface-1)] p-0 shadow-[0_8px_18px_rgba(28,69,21,0.04)]">
            <div className="flex items-center justify-between gap-4 border-b border-[var(--line)] px-5 py-4 max-[980px]:flex-col max-[980px]:items-stretch max-[980px]:gap-3 max-[980px]:px-4">
              <div>
                <h2 className="m-0 text-[1.05rem] font-semibold text-[var(--ink-900)]">Budget Dashboard</h2>
                <p className="mt-1 text-[0.84rem] text-[var(--ink-500)]">Pick a month and budget to continue.</p>
              </div>

              <div className="dashboard-empty-controls ml-auto flex w-max items-end justify-end gap-3 max-[980px]:ml-0 max-[980px]:w-full max-[980px]:items-stretch max-[980px]:gap-2">
                <BudgetControls
                  selectedMonth={selectedMonth}
                  onMonthChange={onSelectedMonthChange}
                  budgetSelectValue={budgetSelectValue}
                  budgets={budgets}
                  onBudgetChange={onSelectedBudgetIdChange}
                />
              </div>
            </div>

            <div className="mx-auto flex max-w-[560px] flex-col items-center px-5 py-9 text-center max-[980px]:px-4 max-[980px]:py-7">
              <div className="mb-4 flex h-14 w-14 items-center justify-center rounded-full border border-[#cfe6c0] bg-[#f3faed] text-[var(--brand)] shadow-[0_6px_14px_rgba(79,152,40,0.15)]">
                <svg viewBox="0 0 24 24" aria-hidden="true" className="h-7 w-7 fill-current">
                  <path d="M4 5.5A1.5 1.5 0 0 1 5.5 4h13A1.5 1.5 0 0 1 20 5.5V8H4V5.5Zm0 4h16v9A1.5 1.5 0 0 1 18.5 20h-13A1.5 1.5 0 0 1 4 18.5v-9Zm9.75 4.25a.75.75 0 1 1 0 1.5h-3v1a.75.75 0 0 1-1.5 0v-1h-1a.75.75 0 0 1 0-1.5h1v-1a.75.75 0 0 1 1.5 0v1h3Z" />
                </svg>
              </div>

              <h3 className="m-0 text-[1.22rem] font-semibold text-[var(--ink-900)]">No budget for this timeframe</h3>
              <p className="mt-2 max-w-[52ch] text-[0.92rem] leading-relaxed text-[var(--ink-500)]">
                Create a budget for the selected month to unlock category insights, spending breakdowns, and transaction tracking.
              </p>

              <button className="primary-btn mt-6 inline-flex h-[42px] min-w-[180px] items-center justify-center px-4 text-[0.92rem]" type="button" onClick={onOpenCreateBudgetModal}>
                Create Budget
              </button>
            </div>
          </section>
        ) : (
          <>
            <BudgetPanel
              summary={summary}
              onCreateBudgetClick={onOpenCreateBudgetModal}
              controls={
                <BudgetControls
                  selectedMonth={selectedMonth}
                  onMonthChange={onSelectedMonthChange}
                  budgetSelectValue={budgetSelectValue}
                  budgets={budgets}
                  onBudgetChange={onSelectedBudgetIdChange}
                  showEmptyOption={false}
                />
              }
            />

            <SummaryCards summary={summary} unreadAlertsCount={unreadAlertsCount} />

            <section className="dashboard-content mt-4 grid grid-cols-[minmax(0,0.95fr)_minmax(0,1.45fr)] gap-4 items-start max-[980px]:grid-cols-1">
              <div className="dashboard-current-col min-w-0">
                <TransactionFormPanel
                  hasBudget={Boolean(summary)}
                  budgetName={summary?.budgetName ?? 'No selected budget'}
                  budgetPeriodLabel={budgetPeriodLabel}
                  totalSpentCents={summary?.totalSpentCents ?? 0}
                  totalRemainingCents={summary?.totalRemainingCents ?? 0}
                  onAddTransactionClick={onOpenAddTransactionModal}
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

      <CreateBudgetModal
        isOpen={showCreateBudgetModal}
        onClose={onCloseCreateBudgetModal}
        createBudgetName={createBudgetName}
        onCreateBudgetNameChange={onCreateBudgetNameChange}
        createBudgetPeriodType={createBudgetPeriodType}
        onCreateBudgetPeriodTypeChange={onCreateBudgetPeriodTypeChange}
        createBudgetStartDate={createBudgetStartDate}
        onCreateBudgetStartDateChange={onCreateBudgetStartDateChange}
        createBudgetTotalDollars={createBudgetTotalDollars}
        onCreateBudgetTotalDollarsChange={onCreateBudgetTotalDollarsChange}
        draftCategoryLimits={draftCategoryLimits}
        categories={categories}
        onAddDraftCategoryLimitRow={onAddDraftCategoryLimitRow}
        onUpdateDraftCategoryLimit={onUpdateDraftCategoryLimit}
        onRemoveDraftCategoryLimitRow={onRemoveDraftCategoryLimitRow}
        newCategoryName={newCategoryName}
        onNewCategoryNameChange={onNewCategoryNameChange}
        onAddCategoryByName={onAddCategoryByName}
        createBudgetError={createBudgetError}
        onSubmit={onCreateBudgetSubmit}
      />

      <AddTransactionModal
        isOpen={showAddTransactionModal}
        onClose={onCloseAddTransactionModal}
        onSubmit={onAddTransactionSubmit}
        newTxnMerchant={newTxnMerchant}
        onNewTxnMerchantChange={onNewTxnMerchantChange}
        newTxnDescription={newTxnDescription}
        onNewTxnDescriptionChange={onNewTxnDescriptionChange}
        newTxnAmountDollars={newTxnAmountDollars}
        onNewTxnAmountDollarsChange={onNewTxnAmountDollarsChange}
        newTxnDate={newTxnDate}
        onNewTxnDateChange={onNewTxnDateChange}
        newTxnSource={newTxnSource}
        onNewTxnSourceChange={onNewTxnSourceChange}
        newTxnCategoryId={newTxnCategoryId}
        onNewTxnCategoryIdChange={onNewTxnCategoryIdChange}
        categories={categories}
        selectedBudgetId={selectedBudgetId}
      />
    </main>
  )
}
