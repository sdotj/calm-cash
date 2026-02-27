import type { Txn } from '../../../types'
import { formatCents } from '../../../utils/format'

type TransactionsPanelProps = {
  transactions: Txn[]
  categoryNameById: Map<string, string>
}

export function TransactionsPanel({ transactions, categoryNameById }: TransactionsPanelProps) {
  return (
    <article className="panel rounded-[var(--radius-md)] border border-[var(--line)] bg-[var(--surface-1)] p-4 shadow-[0_8px_18px_rgba(28,69,21,0.04)]">
      <div className="panel-head mb-3">
        <h3 className="m-0">Recent Transactions</h3>
        <p className="mt-1 text-[0.86rem] text-[var(--ink-500)]">Latest activity for the selected month.</p>
      </div>

      <div className="table-wrap overflow-auto rounded-[10px] border border-[#e2f0d9]">
        <table className="min-w-[520px] w-full border-collapse">
          <thead>
            <tr>
              <th className="border-b border-[#e7f2e1] bg-[#f8fcf5] p-2.5 text-left text-[0.9rem] font-bold text-[var(--ink-700)]">Date</th>
              <th className="border-b border-[#e7f2e1] bg-[#f8fcf5] p-2.5 text-left text-[0.9rem] font-bold text-[var(--ink-700)]">Details</th>
              <th className="border-b border-[#e7f2e1] bg-[#f8fcf5] p-2.5 text-left text-[0.9rem] font-bold text-[var(--ink-700)]">Category</th>
              <th className="border-b border-[#e7f2e1] bg-[#f8fcf5] p-2.5 text-left text-[0.9rem] font-bold text-[var(--ink-700)]">Amount</th>
            </tr>
          </thead>
          <tbody>
            {transactions.map((txn) => (
              <tr key={txn.id}>
                <td className="border-b border-[#e7f2e1] p-2.5 text-[0.9rem]">{new Date(txn.transactionDate).toLocaleDateString()}</td>
                <td className="border-b border-[#e7f2e1] p-2.5 text-[0.9rem]">
                  <div className="txn-merchant font-bold">{txn.merchant}</div>
                  {txn.description ? <div className="txn-description mt-0.5 max-w-[280px] truncate text-[0.78rem] text-[var(--ink-500)]">{txn.description}</div> : null}
                </td>
                <td className="border-b border-[#e7f2e1] p-2.5 text-[0.9rem]">{txn.categoryId ? categoryNameById.get(txn.categoryId) ?? 'Unknown' : 'Uncategorized'}</td>
                <td className={`border-b border-[#e7f2e1] p-2.5 text-[0.9rem] ${txn.amountCents < 0 ? 'money-negative' : 'money-positive'}`}>
                  {formatCents(txn.amountCents)}
                </td>
              </tr>
            ))}

            {!transactions.length ? (
              <tr>
                <td colSpan={4} className="empty-cell">
                  No transactions for this month.
                </td>
              </tr>
            ) : null}
          </tbody>
        </table>
      </div>
    </article>
  )
}
