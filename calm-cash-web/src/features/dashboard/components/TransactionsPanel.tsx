import type { Txn } from '../../../types'
import { formatCents } from '../../../utils/format'

type TransactionsPanelProps = {
  transactions: Txn[]
  categoryNameById: Map<string, string>
}

export function TransactionsPanel({ transactions, categoryNameById }: TransactionsPanelProps) {
  return (
    <article className="panel">
      <h3>Recent Transactions</h3>
      <div className="table-wrap">
        <table>
          <thead>
            <tr>
              <th>Date</th>
              <th>Merchant</th>
              <th>Category</th>
              <th>Amount</th>
            </tr>
          </thead>
          <tbody>
            {transactions.map((txn) => (
              <tr key={txn.id}>
                <td>{new Date(txn.transactionDate).toLocaleDateString()}</td>
                <td>{txn.merchant}</td>
                <td>{txn.categoryId ? categoryNameById.get(txn.categoryId) ?? 'Unknown' : 'Uncategorized'}</td>
                <td className={txn.amountCents < 0 ? 'money-negative' : 'money-positive'}>{formatCents(txn.amountCents)}</td>
              </tr>
            ))}
            {!transactions.length ? (
              <tr>
                <td colSpan={4}>No transactions for this month.</td>
              </tr>
            ) : null}
          </tbody>
        </table>
      </div>
    </article>
  )
}
