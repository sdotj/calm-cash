import type { MonthlySummary } from '../../../types'
import { formatCents } from '../../../utils/format'

type SummaryCardsProps = {
  summary: MonthlySummary | null
  unreadAlertsCount: number
}

export function SummaryCards({ summary, unreadAlertsCount }: SummaryCardsProps) {
  const net = summary?.netCents ?? 0
  const utilization = typeof summary?.utilizationPct === 'number' ? `${Math.round(summary.utilizationPct)}% used` : null

  return (
    <section className="summary-grid mt-4 grid grid-cols-4 gap-3 max-[980px]:grid-cols-1" aria-label="Budget highlights">
      <article className="stat-card relative rounded-[var(--radius-md)] border border-[var(--line)] px-4 py-4">
        <p className="pl-2 text-[0.82rem] text-[var(--ink-500)]">Total Budget</p>
        <h2>{formatCents(summary?.totalLimitCents ?? 0)}</h2>
      </article>

      <article className="stat-card relative rounded-[var(--radius-md)] border border-[var(--line)] px-4 py-4">
        <p className="pl-2 text-[0.82rem] text-[var(--ink-500)]">Total Spent</p>
        <h2>{formatCents(summary?.totalSpentCents ?? 0)}</h2>
        {utilization ? <small>{utilization}</small> : null}
      </article>

      <article className="stat-card relative rounded-[var(--radius-md)] border border-[var(--line)] px-4 py-4">
        <p className="pl-2 text-[0.82rem] text-[var(--ink-500)]">Remaining</p>
        <h2 className={(summary?.totalRemainingCents ?? 0) < 0 ? 'money-negative' : 'money-positive'}>
          {formatCents(summary?.totalRemainingCents ?? 0)}
        </h2>
      </article>

      <article className="stat-card relative rounded-[var(--radius-md)] border border-[var(--line)] px-4 py-4">
        <p className="pl-2 text-[0.82rem] text-[var(--ink-500)]">Net Cashflow</p>
        <h2 className={net < 0 ? 'money-negative' : 'money-positive'}>{formatCents(net)}</h2>
        <small>{unreadAlertsCount} unread alerts</small>
      </article>
    </section>
  )
}
