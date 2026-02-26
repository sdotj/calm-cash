import type { MeResponse } from '../../../types'
import calmCashLogoIcon from '../../../assets/calm-cash-full-no-text.png'

function MoonIcon() {
  return (
    <svg viewBox="0 0 24 24" aria-hidden="true">
      <path d="M14.45 3.25a1 1 0 0 0-1.13 1.29 7.95 7.95 0 0 1 .44 2.63A8 8 0 0 1 5.75 15.2a7.95 7.95 0 0 1-2.63-.44 1 1 0 0 0-1.29 1.13A10 10 0 1 0 14.45 3.25Z" />
    </svg>
  )
}

function SunIcon() {
  return (
    <svg viewBox="0 0 24 24" aria-hidden="true">
      <path d="M12 7a5 5 0 1 0 5 5 5 5 0 0 0-5-5Zm0-5a1 1 0 0 1 1 1v2a1 1 0 0 1-2 0V3a1 1 0 0 1 1-1Zm0 17a1 1 0 0 1 1 1v2a1 1 0 0 1-2 0v-2a1 1 0 0 1 1-1Zm9-8a1 1 0 0 1 0 2h-2a1 1 0 0 1 0-2Zm-16 0a1 1 0 0 1 0 2H3a1 1 0 0 1 0-2Zm12.36-6.36a1 1 0 0 1 1.42 1.42l-1.41 1.41a1 1 0 1 1-1.42-1.42Zm-10.31 10.3a1 1 0 0 1 1.42 1.42l-1.41 1.42a1 1 0 0 1-1.42-1.42Zm10.31 3.13a1 1 0 1 1-1.42 1.42l-1.41-1.42a1 1 0 0 1 1.42-1.42ZM8.47 8.47a1 1 0 0 1-1.42 0L5.64 7.06a1 1 0 0 1 1.42-1.42l1.41 1.41a1 1 0 0 1 0 1.42Z" />
    </svg>
  )
}

type TopBarProps = {
  me: MeResponse | null
  selectedMonth: string
  onSelectedMonthChange: (value: string) => void
  onLogout: () => Promise<void>
  isDarkMode: boolean
  onToggleTheme: () => void
}

export function TopBar({
  me,
  selectedMonth,
  onSelectedMonthChange,
  onLogout,
  isDarkMode,
  onToggleTheme,
}: TopBarProps) {
  return (
    <header className="dashboard-topbar panel-surface">
      <div className="dashboard-topbar-main">
        <div className="dashboard-title-row">
          <img src={calmCashLogoIcon} alt="Calm Cash logo" className="dashboard-logo-mark" />
          <div>
            <p className="eyebrow">Calm Cash Dashboard</p>
            <h1>{me ? `Hi, ${me.displayName}` : 'Budget Dashboard'}</h1>
          </div>
        </div>
        <p className="topbar-subtitle">Track spending, stay under budget, and keep this month stress-free.</p>
      </div>

      <div className="dashboard-topbar-controls">
        <label className="month-control">
          <span>Month</span>
          <input type="month" value={selectedMonth} onChange={(event) => onSelectedMonthChange(event.target.value)} />
        </label>

        <button onClick={onToggleTheme} className="quiet-btn theme-toggle-btn" type="button" aria-pressed={isDarkMode}>
          <span className="sr-only">{isDarkMode ? 'Switch to light mode' : 'Switch to dark mode'}</span>
          {isDarkMode ? <SunIcon /> : <MoonIcon />}
        </button>

        <button onClick={() => void onLogout()} className="quiet-btn" type="button">
          Logout
        </button>
      </div>
    </header>
  )
}
