import type { MeResponse } from '../../../types'

type TopBarProps = {
  me: MeResponse | null
  selectedMonth: string
  onSelectedMonthChange: (value: string) => void
  onLogout: () => Promise<void>
}

export function TopBar({ me, selectedMonth, onSelectedMonthChange, onLogout }: TopBarProps) {
  return (
    <header className="topbar">
      <div>
        <p className="eyebrow">Calm Cash</p>
        <h1>{me ? `Welcome back, ${me.displayName}` : 'Budget Dashboard'}</h1>
      </div>
      <div className="topbar-actions">
        <label>
          Month
          <input type="month" value={selectedMonth} onChange={(event) => onSelectedMonthChange(event.target.value)} />
        </label>
        <button onClick={() => void onLogout()} className="quiet-btn" type="button">
          Logout
        </button>
      </div>
    </header>
  )
}
