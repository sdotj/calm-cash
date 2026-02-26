import type { MonthlySummary } from '../types'
import { formatCents } from '../utils/format'

type SummaryCardsProps = {
  summary: MonthlySummary | null
  unreadAlertsCount: number
}

export function SummaryCards({ summary, unreadAlertsCount }: SummaryCardsProps) {
  return (
    <section className="summary-grid">
      <article className="stat-card">
        <p>Income</p>
        <h2>{formatCents(summary?.incomeCents ?? 0)}</h2>
      </article>
      <article className="stat-card">
        <p>Expenses</p>
        <h2>{formatCents(summary?.expenseCents ?? 0)}</h2>
      </article>
      <article className="stat-card">
        <p>Net</p>
        <h2>{formatCents(summary?.netCents ?? 0)}</h2>
      </article>
      <article className="stat-card">
        <p>Unread Alerts</p>
        <h2>{unreadAlertsCount}</h2>
      </article>
    </section>
  )
}
