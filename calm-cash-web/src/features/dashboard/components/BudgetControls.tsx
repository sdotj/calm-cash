import type { Budget } from '../../../types'
import { MonthPicker } from './MonthPicker'

type BudgetControlsProps = {
  selectedMonth: string
  onMonthChange: (value: string) => void
  budgetSelectValue: string
  budgets: Budget[]
  onBudgetChange: (value: string) => void
  showEmptyOption?: boolean
}

export function BudgetControls({
  selectedMonth,
  onMonthChange,
  budgetSelectValue,
  budgets,
  onBudgetChange,
  showEmptyOption = true,
}: BudgetControlsProps) {
  return (
    <>
      <MonthPicker value={selectedMonth} onChange={onMonthChange} />
      <div className="dashboard-select-wrap">
        <select className="dashboard-control-select budget-control" value={budgetSelectValue} onChange={(event) => onBudgetChange(event.target.value)}>
          {showEmptyOption && budgets.length === 0 ? <option value="">No budgets available</option> : null}
          {budgets.map((budget) => (
            <option key={budget.id} value={budget.id}>
              {budget.name}
            </option>
          ))}
        </select>
        <span className="dashboard-select-chevron" aria-hidden="true">
          <svg viewBox="0 0 24 24">
            <path d="m7 10 5 5 5-5" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round" />
          </svg>
        </span>
      </div>
    </>
  )
}
