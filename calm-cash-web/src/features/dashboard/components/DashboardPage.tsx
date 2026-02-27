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
          <section className="dashboard-empty-state panel mt-4 rounded-[var(--radius-md)] border border-[var(--line)] bg-[var(--surface-1)] px-4 py-8 text-center shadow-[0_8px_18px_rgba(28,69,21,0.04)]">
            <div className="dashboard-empty-controls mb-4 ml-auto flex w-max items-end justify-end gap-3 max-[980px]:ml-0 max-[980px]:w-full max-[980px]:items-stretch max-[980px]:gap-2">
              <BudgetControls
                selectedMonth={selectedMonth}
                onMonthChange={onSelectedMonthChange}
                budgetSelectValue={budgetSelectValue}
                budgets={budgets}
                onBudgetChange={onSelectedBudgetIdChange}
              />
            </div>

            <h2 className="m-0">No budget created yet</h2>
            <p className="mx-auto mb-4 mt-2 max-w-[56ch] text-[var(--ink-500)]">
              Create your first budget to start tracking category spending and transaction flow.
            </p>
            <button className="primary-btn" type="button" onClick={onOpenCreateBudgetModal}>
              Create Budget
            </button>
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
