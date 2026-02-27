import type { FormEvent } from 'react'
import type { Category, TransactionSource } from '../../../types'
import { DashboardModal } from './DashboardModal'

type AddTransactionModalProps = {
  isOpen: boolean
  onClose: () => void
  onSubmit: (event: FormEvent) => Promise<void>
  newTxnMerchant: string
  onNewTxnMerchantChange: (value: string) => void
  newTxnDescription: string
  onNewTxnDescriptionChange: (value: string) => void
  newTxnAmountDollars: string
  onNewTxnAmountDollarsChange: (value: string) => void
  newTxnDate: string
  onNewTxnDateChange: (value: string) => void
  newTxnSource: TransactionSource
  onNewTxnSourceChange: (value: TransactionSource) => void
  newTxnCategoryId: string
  onNewTxnCategoryIdChange: (value: string) => void
  categories: Category[]
  selectedBudgetId: string
}

export function AddTransactionModal({
  isOpen,
  onClose,
  onSubmit,
  newTxnMerchant,
  onNewTxnMerchantChange,
  newTxnDescription,
  onNewTxnDescriptionChange,
  newTxnAmountDollars,
  onNewTxnAmountDollarsChange,
  newTxnDate,
  onNewTxnDateChange,
  newTxnSource,
  onNewTxnSourceChange,
  newTxnCategoryId,
  onNewTxnCategoryIdChange,
  categories,
  selectedBudgetId,
}: AddTransactionModalProps) {
  if (!isOpen) {
    return null
  }

  return (
    <DashboardModal title="Add Transaction" onClose={onClose}>
      <form className="stack-form" onSubmit={(event) => void onSubmit(event)}>
        <label>
          Merchant
          <input
            value={newTxnMerchant}
            onChange={(event) => onNewTxnMerchantChange(event.target.value)}
            placeholder="Ex: Trader Joe's"
            maxLength={255}
            required
          />
        </label>
        <label>
          Description
          <input
            value={newTxnDescription}
            onChange={(event) => onNewTxnDescriptionChange(event.target.value)}
            placeholder="Optional note"
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
        <button className="primary-btn" type="submit" disabled={!selectedBudgetId}>
          Save Transaction
        </button>
      </form>
    </DashboardModal>
  )
}
