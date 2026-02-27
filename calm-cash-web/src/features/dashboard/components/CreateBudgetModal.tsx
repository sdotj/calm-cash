import type { FormEvent } from 'react'
import type { BudgetPeriodType, Category } from '../../../types'
import { DashboardModal } from './DashboardModal'

export type DraftCategoryLimit = {
  id: string
  categoryId: string
  limitDollars: string
  colorHex: string
}

type CreateBudgetModalProps = {
  isOpen: boolean
  onClose: () => void
  createBudgetName: string
  onCreateBudgetNameChange: (value: string) => void
  createBudgetPeriodType: BudgetPeriodType
  onCreateBudgetPeriodTypeChange: (value: BudgetPeriodType) => void
  createBudgetStartDate: string
  onCreateBudgetStartDateChange: (value: string) => void
  createBudgetTotalDollars: string
  onCreateBudgetTotalDollarsChange: (value: string) => void
  draftCategoryLimits: DraftCategoryLimit[]
  categories: Category[]
  onAddDraftCategoryLimitRow: () => void
  onUpdateDraftCategoryLimit: (rowId: string, patch: Partial<DraftCategoryLimit>) => void
  onRemoveDraftCategoryLimitRow: (rowId: string) => void
  newCategoryName: string
  onNewCategoryNameChange: (value: string) => void
  onAddCategoryByName: (name: string) => Promise<boolean>
  createBudgetError: string
  onSubmit: (event: FormEvent) => Promise<void>
}

const BUDGET_COLOR_OPTIONS = ['#F25F5C', '#3A86FF', '#FF9F1C', '#2EC4B6', '#8E7DBE', '#E76F51', '#43AA8B', '#FF006E']

export function CreateBudgetModal({
  isOpen,
  onClose,
  createBudgetName,
  onCreateBudgetNameChange,
  createBudgetPeriodType,
  onCreateBudgetPeriodTypeChange,
  createBudgetStartDate,
  onCreateBudgetStartDateChange,
  createBudgetTotalDollars,
  onCreateBudgetTotalDollarsChange,
  draftCategoryLimits,
  categories,
  onAddDraftCategoryLimitRow,
  onUpdateDraftCategoryLimit,
  onRemoveDraftCategoryLimitRow,
  newCategoryName,
  onNewCategoryNameChange,
  onAddCategoryByName,
  createBudgetError,
  onSubmit,
}: CreateBudgetModalProps) {
  if (!isOpen) {
    return null
  }

  return (
    <DashboardModal title="Create Budget" onClose={onClose}>
      <form className="stack-form" onSubmit={(event) => void onSubmit(event)}>
        <label>
          Budget Name
          <input value={createBudgetName} onChange={(event) => onCreateBudgetNameChange(event.target.value)} maxLength={150} required />
        </label>

        <div className="form-two-col">
          <label>
            Period Type
            <select value={createBudgetPeriodType} onChange={(event) => onCreateBudgetPeriodTypeChange(event.target.value as BudgetPeriodType)}>
              <option value="MONTHLY">Monthly</option>
              <option value="WEEKLY">Weekly</option>
            </select>
          </label>
          <label>
            Start Date
            <input type="date" value={createBudgetStartDate} onChange={(event) => onCreateBudgetStartDateChange(event.target.value)} required />
          </label>
        </div>

        <label>
          Total Budget ($)
          <input
            type="number"
            inputMode="decimal"
            step="0.01"
            min="0"
            value={createBudgetTotalDollars}
            onChange={(event) => onCreateBudgetTotalDollarsChange(event.target.value)}
            required
          />
        </label>

        <div className="modal-section-head">
          <h4>Category Limits</h4>
          <button className="quiet-btn" type="button" onClick={onAddDraftCategoryLimitRow}>
            Add Category Limit
          </button>
        </div>

        <div className="draft-limits-list">
          {draftCategoryLimits.map((row) => (
            <div key={row.id} className="draft-limit-row">
              <select value={row.categoryId} onChange={(event) => onUpdateDraftCategoryLimit(row.id, { categoryId: event.target.value })}>
                <option value="">Select category</option>
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
                placeholder="Limit $"
                value={row.limitDollars}
                onChange={(event) => onUpdateDraftCategoryLimit(row.id, { limitDollars: event.target.value })}
              />

              <div className="color-picker-grid" role="radiogroup" aria-label="Category color">
                {BUDGET_COLOR_OPTIONS.map((color) => (
                  <button
                    key={`${row.id}-${color}`}
                    type="button"
                    className={`color-swatch ${row.colorHex === color ? 'is-active' : ''}`}
                    style={{ background: color }}
                    aria-label={`Select color ${color}`}
                    aria-pressed={row.colorHex === color}
                    onClick={() => onUpdateDraftCategoryLimit(row.id, { colorHex: color })}
                  />
                ))}
              </div>

              <button
                className="quiet-btn"
                type="button"
                onClick={() => onRemoveDraftCategoryLimitRow(row.id)}
                disabled={draftCategoryLimits.length <= 1}
              >
                Remove
              </button>
            </div>
          ))}
        </div>

        <div className="modal-section-head">
          <h4>Add New Category</h4>
        </div>

        <div className="inline-form">
          <input
            value={newCategoryName}
            onChange={(event) => onNewCategoryNameChange(event.target.value)}
            placeholder="Create category without closing this modal"
            maxLength={100}
          />
          <button className="quiet-btn" type="button" onClick={() => void onAddCategoryByName(newCategoryName)}>
            Add
          </button>
        </div>

        {createBudgetError ? <p className="inline-error">{createBudgetError}</p> : null}

        <button className="primary-btn" type="submit">
          Create Budget
        </button>
      </form>
    </DashboardModal>
  )
}
