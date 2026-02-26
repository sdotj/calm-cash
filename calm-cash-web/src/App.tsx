import { useState } from 'react'
import type { FormEvent } from 'react'
import './App.css'
import { AlertsPanel } from './components/AlertsPanel'
import { AuthScreen } from './components/AuthScreen'
import { BudgetPanel } from './components/BudgetPanel'
import { SummaryCards } from './components/SummaryCards'
import { TopBar } from './components/TopBar'
import { TransactionFormPanel } from './components/TransactionFormPanel'
import { TransactionsPanel } from './components/TransactionsPanel'
import { useAuth } from './hooks/useAuth'
import { useDashboard } from './hooks/useDashboard'
import type { AuthMode } from './types'
import { toErrorMessage } from './utils/errors'

function App() {
  const [authMode, setAuthMode] = useState<AuthMode>('login')
  const [authLoading, setAuthLoading] = useState(false)
  const [authError, setAuthError] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [displayName, setDisplayName] = useState('')

  const { isAuthenticated, me, setMe, authenticate, logout, withValidAccess, clearSession } = useAuth()

  const {
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
  } = useDashboard({
    isAuthenticated,
    setMe,
    withValidAccess,
    clearSession,
  })

  async function onAuthSubmit(event: FormEvent) {
    event.preventDefault()
    setAuthLoading(true)
    setAuthError('')

    try {
      await authenticate(authMode, { email, password, displayName })
    } catch (error) {
      setAuthError(toErrorMessage(error, 'Unable to authenticate. Please try again.'))
    } finally {
      setAuthLoading(false)
    }
  }

  if (!isAuthenticated) {
    return (
      <AuthScreen
        authMode={authMode}
        authLoading={authLoading}
        authError={authError}
        email={email}
        password={password}
        displayName={displayName}
        onAuthModeChange={setAuthMode}
        onEmailChange={setEmail}
        onPasswordChange={setPassword}
        onDisplayNameChange={setDisplayName}
        onSubmit={onAuthSubmit}
      />
    )
  }

  return (
    <main className="dashboard-page">
      <TopBar me={me} selectedMonth={selectedMonth} onSelectedMonthChange={setSelectedMonth} onLogout={logout} />

      {globalError ? <p className="error-banner">{globalError}</p> : null}

      <SummaryCards summary={summary} unreadAlertsCount={unreadAlertsCount} />

      <section className="dashboard-grid">
        <BudgetPanel
          summary={summary}
          categories={categories}
          newCategoryName={newCategoryName}
          newBudgetCategoryId={newBudgetCategoryId}
          newBudgetLimitDollars={newBudgetLimitDollars}
          onNewCategoryNameChange={setNewCategoryName}
          onNewBudgetCategoryIdChange={setNewBudgetCategoryId}
          onNewBudgetLimitDollarsChange={setNewBudgetLimitDollars}
          onAddCategory={onAddCategory}
          onSetBudget={onSetBudget}
        />

        <TransactionFormPanel
          categories={categories}
          newTxnMerchant={newTxnMerchant}
          newTxnDescription={newTxnDescription}
          newTxnAmountDollars={newTxnAmountDollars}
          newTxnDate={newTxnDate}
          newTxnSource={newTxnSource}
          newTxnCategoryId={newTxnCategoryId}
          onNewTxnMerchantChange={setNewTxnMerchant}
          onNewTxnDescriptionChange={setNewTxnDescription}
          onNewTxnAmountDollarsChange={setNewTxnAmountDollars}
          onNewTxnDateChange={setNewTxnDate}
          onNewTxnSourceChange={setNewTxnSource}
          onNewTxnCategoryIdChange={setNewTxnCategoryId}
          onAddTransaction={onAddTransaction}
        />
      </section>

      <section className="dashboard-grid lower-grid">
        <TransactionsPanel transactions={transactions} categoryNameById={categoryMap} />
        <AlertsPanel alerts={alerts} onMarkAlertRead={onMarkAlertRead} />
      </section>

      {appLoading ? <p className="loading-overlay">Syncing latest data...</p> : null}

      <footer className="app-footer">
        <p>
          Signed in as <strong>{me?.email}</strong>
        </p>
        <p>
          {budgets.length} budgets configured across {categories.length} categories.
        </p>
      </footer>
    </main>
  )
}

export default App
