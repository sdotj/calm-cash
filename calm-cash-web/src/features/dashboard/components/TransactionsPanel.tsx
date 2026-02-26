import type { Txn } from '../../../types'
import { formatCents } from '../../../utils/format'

type TransactionsPanelProps = {
  transactions: Txn[]
  categoryNameById: Map<string, string>
}

export function TransactionsPanel({ transactions, categoryNameById }: TransactionsPanelProps) {
  return (
    <article className="panel">
      <div className="panel-head">
        <h3>Recent Transactions</h3>
        <p>Latest activity for the selected month.</p>
      </div>

      <div className="table-wrap">
        <table>
          <thead>
            <tr>
              <th>Date</th>
              <th>Details</th>
              <th>Category</th>
              <th>Amount</th>
            </tr>
          </thead>
          <tbody>
            {transactions.map((txn) => (
              <tr key={txn.id}>
                <td>{new Date(txn.transactionDate).toLocaleDateString()}</td>
                <td>
                  <div className="txn-merchant">{txn.merchant}</div>
                  {txn.description ? <div className="txn-description">{txn.description}</div> : null}
                </td>
                <td>{txn.categoryId ? categoryNameById.get(txn.categoryId) ?? 'Unknown' : 'Uncategorized'}</td>
                <td className={txn.amountCents < 0 ? 'money-negative' : 'money-positive'}>{formatCents(txn.amountCents)}</td>
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
