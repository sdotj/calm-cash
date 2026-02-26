import { useEffect, useState } from 'react'
import type { FormEvent } from 'react'
import './App.css'
import { AlertsPanel } from './features/dashboard/components/AlertsPanel'
import { AuthScreen } from './features/auth/components/AuthScreen'
import { BudgetPanel } from './features/dashboard/components/BudgetPanel'
import { SummaryCards } from './features/dashboard/components/SummaryCards'
import { TopBar } from './features/dashboard/components/TopBar'
import { TransactionFormPanel } from './features/dashboard/components/TransactionFormPanel'
import { TransactionsPanel } from './features/dashboard/components/TransactionsPanel'
import { useAuth } from './features/auth/hooks/useAuth'
import { useDashboard } from './features/dashboard/hooks/useDashboard'
import type { AuthMode } from './types'
import { toErrorMessage } from './utils/errors'

function App() {
  const [dashboardTheme, setDashboardTheme] = useState<'light' | 'dark'>(() => {
    const storedTheme = window.localStorage.getItem('calm-cash-dashboard-theme')
    return storedTheme === 'dark' ? 'dark' : 'light'
  })
  const [authMode, setAuthMode] = useState<AuthMode>('login')
  const [authLoading, setAuthLoading] = useState(false)
  const [authError, setAuthError] = useState('')
  const [loginEmail, setLoginEmail] = useState('')
  const [loginPassword, setLoginPassword] = useState('')
  const [registerEmail, setRegisterEmail] = useState('')
  const [registerPassword, setRegisterPassword] = useState('')
  const [registerDisplayName, setRegisterDisplayName] = useState('')

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
    <main className={`dashboard-page ${dashboardTheme === 'dark' ? 'theme-dark' : ''}`}>
      <div className="dashboard-shell">
        <TopBar
          me={me}
          selectedMonth={selectedMonth}
          onSelectedMonthChange={setSelectedMonth}
          onLogout={logout}
          isDarkMode={dashboardTheme === 'dark'}
          onToggleTheme={() => setDashboardTheme((theme) => (theme === 'dark' ? 'light' : 'dark'))}
        />

        {globalError ? <p className="error-banner">{globalError}</p> : null}

        <SummaryCards summary={summary} unreadAlertsCount={unreadAlertsCount} />

        <section className="dashboard-content">
          <div className="dashboard-main-column">
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

            <TransactionsPanel transactions={transactions} categoryNameById={categoryMap} />
          </div>

          <aside className="dashboard-side-column">
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

            <AlertsPanel alerts={alerts} onMarkAlertRead={onMarkAlertRead} />
          </aside>
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
      </div>
    </main>
  )
}

export default App
