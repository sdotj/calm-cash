import type { ReactNode } from 'react'
import { useMemo, useState } from 'react'
import type { MonthlySummary } from '../../../types'
import { formatCents } from '../../../utils/format'

type BudgetPanelProps = {
  summary: MonthlySummary | null
  onCreateBudgetClick: () => void
  controls?: ReactNode
}

const FALLBACK_COLORS = ['#F25F5C', '#3A86FF', '#FF9F1C', '#2EC4B6', '#8E7DBE', '#E76F51', '#43AA8B', '#FF006E']

function hexToRgba(hex: string, alpha: number): string {
  const sanitized = hex.replace('#', '')
  if (sanitized.length !== 6) {
    return `rgba(58, 134, 255, ${alpha})`
  }

  const red = Number.parseInt(sanitized.slice(0, 2), 16)
  const green = Number.parseInt(sanitized.slice(2, 4), 16)
  const blue = Number.parseInt(sanitized.slice(4, 6), 16)
  return `rgba(${red}, ${green}, ${blue}, ${alpha})`
}

export function BudgetPanel({ summary, onCreateBudgetClick, controls }: BudgetPanelProps) {
  const hasBudget = Boolean(summary)
  const total = summary?.totalLimitCents ?? 0
  const [hoveredCategoryId, setHoveredCategoryId] = useState<string | null>(null)

  const hoveredCategory = useMemo(() => {
    if (!summary || !hoveredCategoryId) {
      return null
    }
    return summary.categories.find((category) => category.categoryId === hoveredCategoryId) ?? null
  }, [hoveredCategoryId, summary])

  const hoveredCategoryPositionPct = useMemo(() => {
    if (!summary || !hoveredCategory) {
      return null
    }

    let cumulativeLimit = 0
    for (const category of summary.categories) {
      const limit = Math.max(0, category.limitCents ?? 0)
      if (category.categoryId === hoveredCategory.categoryId) {
        const midpoint = cumulativeLimit + limit / 2
        return total > 0 ? (midpoint / total) * 100 : 50
      }
      cumulativeLimit += limit
    }

    return 50
  }, [hoveredCategory, summary, total])

  return (
    <article className="panel rounded-[var(--radius-md)] border border-[var(--line)] bg-[var(--surface-1)] p-4 shadow-[0_8px_18px_rgba(28,69,21,0.04)]">
      <div className="panel-head panel-head-row flex items-start justify-between gap-3 max-[980px]:flex-col">
        <div>
          <h3 className="m-0">Budget Overview</h3>
          <p className="mt-1 text-[0.86rem] text-[var(--ink-500)]">
            {hasBudget ? 'Track category usage at a glance.' : 'Create a budget to start tracking spending.'}
          </p>
        </div>

        <div className="quick-actions-inline flex flex-wrap items-center justify-end gap-1.5 max-[980px]:justify-start">
          {controls ? <div className="budget-panel-controls flex items-center gap-2 max-[980px]:w-full">{controls}</div> : null}
          <button className="primary-btn" type="button" onClick={onCreateBudgetClick}>
            {hasBudget ? 'Create Another Budget' : 'Create Budget'}
          </button>
        </div>
      </div>

      {!hasBudget ? (
        <p className="empty-state">No active budget selected for this month.</p>
      ) : (
        <>
          <div className="budget-composition-wrap">
            <div className="budget-composition" role="img" aria-label="Budget category composition">
              {summary?.categories.map((category, index) => {
                const limit = category.limitCents ?? 0
                const widthPct = total > 0 ? (limit / total) * 100 : 0
                const color = category.colorHex ?? FALLBACK_COLORS[index % FALLBACK_COLORS.length]
                const fillPct = Math.max(0, Math.min(100, category.utilizationPct ?? 0))
                const fadedColor = hexToRgba(color, 0.28)

                return (
                  <button
                    key={category.categoryId}
                    className={`budget-segment ${hoveredCategoryId === category.categoryId ? 'is-active' : ''}`}
                    type="button"
                    onMouseEnter={() => setHoveredCategoryId(category.categoryId)}
                    onMouseLeave={() => setHoveredCategoryId(null)}
                    onFocus={() => setHoveredCategoryId(category.categoryId)}
                    onBlur={() => setHoveredCategoryId(null)}
                    style={{
                      width: `${Math.max(widthPct, 2)}%`,
                      background: fadedColor,
                    }}
                    aria-label={`${category.categoryName}: ${Math.round(fillPct)} percent used`}
                  >
                    <span className="budget-segment-fill" style={{ width: `${fillPct}%`, background: color }} />
                  </button>
                )
              })}
            </div>
            {hoveredCategory && hoveredCategoryPositionPct !== null ? (
              <div className="budget-segment-tooltip" role="status" aria-live="polite">
                <div
                  className="budget-segment-tooltip-bubble"
                  style={{
                    left: `${hoveredCategoryPositionPct}%`,
                  }}
                >
                  <strong>{hoveredCategory.categoryName}</strong>
                  <span>
                    {formatCents(hoveredCategory.spentCents)} of {formatCents(hoveredCategory.limitCents ?? 0)} spent
                  </span>
                  <span>
                    {formatCents(hoveredCategory.remainingCents ?? 0)} remaining ({Math.round(hoveredCategory.utilizationPct ?? 0)}%)
                  </span>
                </div>
              </div>
            ) : null}
          </div>

          <div className="budget-legend">
            {summary?.categories.map((category, index) => {
              const color = category.colorHex ?? FALLBACK_COLORS[index % FALLBACK_COLORS.length]

              return (
                <div key={category.categoryId} className="budget-legend-row">
                  <span className="budget-legend-dot" style={{ background: color }} aria-hidden="true" />
                  <div className="budget-legend-text">
                    <strong>{category.categoryName}</strong>
                    <small>
                      {formatCents(category.spentCents)} / {formatCents(category.limitCents ?? 0)}
                    </small>
                  </div>
                  <span className="budget-legend-pct">{Math.round(category.utilizationPct ?? 0)}%</span>
                </div>
              )
            })}
          </div>
        </>
      )}
    </article>
  )
}
