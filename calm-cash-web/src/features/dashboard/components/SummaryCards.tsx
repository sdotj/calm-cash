import type { MonthlySummary } from '../../../types'
import { formatCents } from '../../../utils/format'

type SummaryCardsProps = {
  summary: MonthlySummary | null
  unreadAlertsCount: number
}

export function SummaryCards({ summary, unreadAlertsCount }: SummaryCardsProps) {
  const net = summary?.netCents ?? 0

  return (
    <section className="summary-grid" aria-label="Monthly highlights">
      <article className="stat-card">
        <p>Income</p>
        <h2>{formatCents(summary?.incomeCents ?? 0)}</h2>
      </article>

      <article className="stat-card">
        <p>Expenses</p>
        <h2>{formatCents(summary?.expenseCents ?? 0)}</h2>
      </article>

      <article className="stat-card">
        <p>Net Position</p>
        <h2 className={net < 0 ? 'money-negative' : 'money-positive'}>{formatCents(net)}</h2>
      </article>

      <article className="stat-card">
        <p>Unread Alerts</p>
        <h2>{unreadAlertsCount}</h2>
      </article>
    </section>
  )
}
