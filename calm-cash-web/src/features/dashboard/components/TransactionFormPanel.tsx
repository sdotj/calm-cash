import { formatCents } from '../../../utils/format'

type TransactionFormPanelProps = {
  hasBudget: boolean
  budgetName: string
  budgetPeriodLabel: string
  totalSpentCents: number
  totalRemainingCents: number
  onAddTransactionClick: () => void
}

export function TransactionFormPanel({
  hasBudget,
  budgetName,
  budgetPeriodLabel,
  totalSpentCents,
  totalRemainingCents,
  onAddTransactionClick,
}: TransactionFormPanelProps) {
  return (
    <article className="panel panel-emphasis rounded-[var(--radius-md)] border border-[var(--line)] p-4 shadow-[0_8px_18px_rgba(28,69,21,0.04)]">
      <div className="panel-head mb-3">
        <h3 className="m-0">Current Budget</h3>
        <p className="mt-1 text-[0.86rem] text-[var(--ink-500)]">{hasBudget ? budgetPeriodLabel : 'Select or create a budget to get started.'}</p>
      </div>

      {hasBudget ? (
        <div className="current-budget-meta">
          <h4 className="mb-2 text-[1.04rem]">{budgetName}</h4>
          <div className="current-budget-stats mb-4 grid grid-cols-2 gap-2.5">
            <div className="rounded-[10px] border border-[#dcebd2] bg-[#fcfffa] px-2.5 py-2">
              <span className="mb-0.5 block text-xs text-[var(--ink-500)]">Spent</span>
              <strong>{formatCents(totalSpentCents)}</strong>
            </div>
            <div className="rounded-[10px] border border-[#dcebd2] bg-[#fcfffa] px-2.5 py-2">
              <span className="mb-0.5 block text-xs text-[var(--ink-500)]">Remaining</span>
              <strong className={totalRemainingCents < 0 ? 'money-negative' : 'money-positive'}>
                {formatCents(totalRemainingCents)}
              </strong>
            </div>
          </div>
        </div>
      ) : (
        <p className="empty-state">No budget currently selected.</p>
      )}

      <button className="primary-btn" type="button" onClick={onAddTransactionClick} disabled={!hasBudget}>
        Add Transaction
      </button>
    </article>
  )
}
