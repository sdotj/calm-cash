import type { FormEvent } from 'react'
import type { Category, MonthlySummary } from '../../../types'
import { formatCents } from '../../../utils/format'

type BudgetPanelProps = {
  summary: MonthlySummary | null
  categories: Category[]
  newCategoryName: string
  newBudgetCategoryId: string
  newBudgetLimitDollars: string
  onNewCategoryNameChange: (value: string) => void
  onNewBudgetCategoryIdChange: (value: string) => void
  onNewBudgetLimitDollarsChange: (value: string) => void
  onAddCategory: (event: FormEvent) => Promise<void>
  onSetBudget: (event: FormEvent) => Promise<void>
}

export function BudgetPanel({
  summary,
  categories,
  newCategoryName,
  newBudgetCategoryId,
  newBudgetLimitDollars,
  onNewCategoryNameChange,
  onNewBudgetCategoryIdChange,
  onNewBudgetLimitDollarsChange,
  onAddCategory,
  onSetBudget,
}: BudgetPanelProps) {
  return (
    <article className="panel">
      <div className="panel-head">
        <h3>Budgets</h3>
        <p>Set category caps and check utilization at a glance.</p>
      </div>

      <div className="budget-actions">
        <form onSubmit={(event) => void onAddCategory(event)} className="inline-form">
          <input
            value={newCategoryName}
            onChange={(event) => onNewCategoryNameChange(event.target.value)}
            placeholder="New category"
            maxLength={100}
          />
          <button className="primary-btn" type="submit">
            Add
          </button>
        </form>

        <form onSubmit={(event) => void onSetBudget(event)} className="inline-form inline-form-3">
          <select value={newBudgetCategoryId} onChange={(event) => onNewBudgetCategoryIdChange(event.target.value)}>
            <option value="">Category</option>
            {categories.map((category) => (
              <option key={category.id} value={category.id}>
                {category.name}
              </option>
            ))}
          </select>

          <input
            type="number"
            inputMode="decimal"
            step="0.01"
            min="0"
            placeholder="Budget $"
            value={newBudgetLimitDollars}
            onChange={(event) => onNewBudgetLimitDollarsChange(event.target.value)}
          />

          <button className="primary-btn" type="submit">
            Set
          </button>
        </form>
      </div>

      <div className="table-wrap">
        <table>
          <thead>
            <tr>
              <th>Category</th>
              <th>Budget</th>
              <th>Spent</th>
              <th>Usage</th>
            </tr>
          </thead>
          <tbody>
            {summary?.categories.map((row) => (
              <tr key={row.categoryId}>
                <td>{row.categoryName}</td>
                <td>{row.budgetLimitCents ? formatCents(row.budgetLimitCents) : 'Not set'}</td>
                <td>{formatCents(row.spentCents)}</td>
                <td>
                  {typeof row.utilizationPct === 'number' ? `${Math.round(row.utilizationPct)}%` : '-'}
                  {row.budgetLimitCents ? (
                    <div className="util-track">
                      <span
                        style={{
                          width: `${Math.max(0, Math.min(100, row.utilizationPct ?? 0))}%`,
                        }}
                      />
                    </div>
                  ) : null}
                </td>
              </tr>
            ))}

            {!summary?.categories.length ? (
              <tr>
                <td colSpan={4} className="empty-cell">
                  No categories yet. Add one to get started.
                </td>
              </tr>
            ) : null}
          </tbody>
        </table>
      </div>
    </article>
  )
}
