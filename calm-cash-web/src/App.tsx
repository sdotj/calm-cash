import { useEffect, useState } from 'react'
import './features/dashboard/styles/index.css'
import { AuthScreen } from './features/auth/components/AuthScreen'
import { DashboardPage } from './features/dashboard/components/DashboardPage'
import { useAuth } from './features/auth/hooks/useAuth'
import { useAuthFlowController } from './features/auth/hooks/useAuthFlowController'
import { useCreateBudgetDraft } from './features/dashboard/hooks/useCreateBudgetDraft'
import { useDashboard } from './features/dashboard/hooks/useDashboard'

type DashboardModal = 'create-budget' | 'add-transaction' | null

function formatPeriodRange(startDate: string, endDate: string): string {
  const start = new Date(startDate)
  const end = new Date(endDate)
  const startLabel = start.toLocaleDateString('en-US', { month: 'short', day: 'numeric' })
  const endLabel = end.toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' })
  return `${startLabel} - ${endLabel}`
}

function App() {
  const [dashboardTheme, setDashboardTheme] = useState<'light' | 'dark'>(() => {
    const storedTheme = window.localStorage.getItem('calm-cash-dashboard-theme')
    return storedTheme === 'dark' ? 'dark' : 'light'
  })
  const [activeModal, setActiveModal] = useState<DashboardModal>(null)

  const { isAuthenticated, me, setMe, authenticate, logout, withValidAccess, clearSession } = useAuth()
  const {
    authMode,
    authLoading,
    authError,
    loginEmail,
    setLoginEmail,
    loginPassword,
    setLoginPassword,
    registerEmail,
    setRegisterEmail,
    registerPassword,
    setRegisterPassword,
    registerDisplayName,
    setRegisterDisplayName,
    onAuthSubmit,
    handleAuthModeChange,
  } = useAuthFlowController({ authenticate })

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

  const {
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
  } = useCreateBudgetDraft({
    selectedMonth,
    categories,
    onCreateBudgetWithLimits,
    onCreated: () => setActiveModal(null),
  })

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

  const budgetPeriodLabel = summary ? formatPeriodRange(summary.startDate, summary.endDate) : 'Choose an active budget for this month'

  return (
    <DashboardPage
      dashboardTheme={dashboardTheme}
      me={me}
      alerts={alerts}
      unreadAlertsCount={unreadAlertsCount}
      onMarkAlertRead={onMarkAlertRead}
      onLogout={logout}
      onToggleTheme={() => setDashboardTheme((theme) => (theme === 'dark' ? 'light' : 'dark'))}
      globalError={globalError}
      hasBudget={hasBudget}
      selectedMonth={selectedMonth}
      onSelectedMonthChange={setSelectedMonth}
      budgetSelectValue={budgetSelectValue}
      budgets={budgets}
      onSelectedBudgetIdChange={setSelectedBudgetId}
      onOpenCreateBudgetModal={() => {
        openCreateBudgetModal()
        setActiveModal('create-budget')
      }}
      onOpenAddTransactionModal={() => setActiveModal('add-transaction')}
      summary={summary}
      transactions={transactions}
      categoryMap={categoryMap}
      appLoading={appLoading}
      showCreateBudgetModal={activeModal === 'create-budget'}
      showAddTransactionModal={activeModal === 'add-transaction'}
      onCloseCreateBudgetModal={() => setActiveModal(null)}
      onCloseAddTransactionModal={() => setActiveModal(null)}
      createBudgetName={createBudgetName}
      onCreateBudgetNameChange={setCreateBudgetName}
      createBudgetPeriodType={createBudgetPeriodType}
      onCreateBudgetPeriodTypeChange={setCreateBudgetPeriodType}
      createBudgetStartDate={createBudgetStartDate}
      onCreateBudgetStartDateChange={setCreateBudgetStartDate}
      createBudgetTotalDollars={createBudgetTotalDollars}
      onCreateBudgetTotalDollarsChange={setCreateBudgetTotalDollars}
      draftCategoryLimits={draftCategoryLimits}
      categories={categories}
      onAddDraftCategoryLimitRow={addDraftCategoryLimitRow}
      onUpdateDraftCategoryLimit={updateDraftCategoryLimit}
      onRemoveDraftCategoryLimitRow={removeDraftCategoryLimitRow}
      newCategoryName={newCategoryName}
      onNewCategoryNameChange={setNewCategoryName}
      onAddCategoryByName={onAddCategoryByName}
      createBudgetError={createBudgetError}
      onCreateBudgetSubmit={handleCreateBudgetSubmit}
      onAddTransactionSubmit={onAddTransaction}
      newTxnMerchant={newTxnMerchant}
      onNewTxnMerchantChange={setNewTxnMerchant}
      newTxnDescription={newTxnDescription}
      onNewTxnDescriptionChange={setNewTxnDescription}
      newTxnAmountDollars={newTxnAmountDollars}
      onNewTxnAmountDollarsChange={setNewTxnAmountDollars}
      newTxnDate={newTxnDate}
      onNewTxnDateChange={setNewTxnDate}
      newTxnSource={newTxnSource}
      onNewTxnSourceChange={setNewTxnSource}
      newTxnCategoryId={newTxnCategoryId}
      onNewTxnCategoryIdChange={setNewTxnCategoryId}
      selectedBudgetId={selectedBudgetId}
      budgetPeriodLabel={budgetPeriodLabel}
    />
  )
}

export default App
