import type { FormEvent } from 'react'
import type { Category, TransactionSource } from '../../../types'

type TransactionFormPanelProps = {
  categories: Category[]
  newTxnMerchant: string
  newTxnDescription: string
  newTxnAmountDollars: string
  newTxnDate: string
  newTxnSource: TransactionSource
  newTxnCategoryId: string
  onNewTxnMerchantChange: (value: string) => void
  onNewTxnDescriptionChange: (value: string) => void
  onNewTxnAmountDollarsChange: (value: string) => void
  onNewTxnDateChange: (value: string) => void
  onNewTxnSourceChange: (value: TransactionSource) => void
  onNewTxnCategoryIdChange: (value: string) => void
  onAddTransaction: (event: FormEvent) => Promise<void>
}

export function TransactionFormPanel({
  categories,
  newTxnMerchant,
  newTxnDescription,
  newTxnAmountDollars,
  newTxnDate,
  newTxnSource,
  newTxnCategoryId,
  onNewTxnMerchantChange,
  onNewTxnDescriptionChange,
  onNewTxnAmountDollarsChange,
  onNewTxnDateChange,
  onNewTxnSourceChange,
  onNewTxnCategoryIdChange,
  onAddTransaction,
}: TransactionFormPanelProps) {
  return (
    <article className="panel">
      <h3>Quick Transaction</h3>
      <form onSubmit={(event) => void onAddTransaction(event)} className="stack-form">
        <label>
          Merchant
          <input
            value={newTxnMerchant}
            onChange={(event) => onNewTxnMerchantChange(event.target.value)}
            maxLength={255}
            required
          />
        </label>

        <label>
          Description
          <input
            value={newTxnDescription}
            onChange={(event) => onNewTxnDescriptionChange(event.target.value)}
            maxLength={1000}
          />
        </label>

        <div className="form-two-col">
          <label>
            Amount ($)
            <input
              type="number"
              inputMode="decimal"
              step="0.01"
              value={newTxnAmountDollars}
              onChange={(event) => onNewTxnAmountDollarsChange(event.target.value)}
              required
            />
          </label>

          <label>
            Date
            <input type="date" value={newTxnDate} onChange={(event) => onNewTxnDateChange(event.target.value)} required />
          </label>
        </div>

        <div className="form-two-col">
          <label>
            Source
            <select value={newTxnSource} onChange={(event) => onNewTxnSourceChange(event.target.value as TransactionSource)}>
              <option value="MANUAL">Manual</option>
              <option value="PLAID">Plaid</option>
              <option value="IMPORT">Import</option>
            </select>
          </label>

          <label>
            Category
            <select value={newTxnCategoryId} onChange={(event) => onNewTxnCategoryIdChange(event.target.value)}>
              <option value="">Uncategorized</option>
              {categories.map((category) => (
                <option key={category.id} value={category.id}>
                  {category.name}
                </option>
              ))}
            </select>
          </label>
        </div>

        <button className="primary-btn" type="submit">
          Add Transaction
        </button>
      </form>
    </article>
  )
}
