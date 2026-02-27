import { useEffect, useMemo, useRef, useState } from 'react'

type MonthPickerProps = {
  value: string
  onChange: (value: string) => void
}

const MONTH_LABELS = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec']

export function MonthPicker({ value, onChange }: MonthPickerProps) {
  const [isOpen, setIsOpen] = useState(false)
  const [displayYear, setDisplayYear] = useState(Number(value.slice(0, 4)))
  const rootRef = useRef<HTMLDivElement | null>(null)

  const selectedYear = Number(value.slice(0, 4))
  const selectedMonth = Number(value.slice(5, 7))

  useEffect(() => {
    if (!isOpen) {
      return
    }

    function handleOutsideClick(event: MouseEvent | TouchEvent) {
      const target = event.target as Node | null
      if (!target) {
        return
      }
      if (!rootRef.current?.contains(target)) {
        setIsOpen(false)
      }
    }

    function handleEscape(event: KeyboardEvent) {
      if (event.key === 'Escape') {
        setIsOpen(false)
      }
    }

    document.addEventListener('mousedown', handleOutsideClick)
    document.addEventListener('touchstart', handleOutsideClick)
    document.addEventListener('keydown', handleEscape)

    return () => {
      document.removeEventListener('mousedown', handleOutsideClick)
      document.removeEventListener('touchstart', handleOutsideClick)
      document.removeEventListener('keydown', handleEscape)
    }
  }, [isOpen])

  useEffect(() => {
    setDisplayYear(selectedYear)
  }, [selectedYear])

  const triggerLabel = useMemo(() => {
    const monthIndex = Math.max(0, Math.min(11, selectedMonth - 1))
    return `${MONTH_LABELS[monthIndex]} ${selectedYear}`
  }, [selectedMonth, selectedYear])

  return (
    <div className="month-picker" ref={rootRef}>
      <button className="dashboard-control-btn" type="button" onClick={() => setIsOpen((open) => !open)} aria-expanded={isOpen}>
        <span>{triggerLabel}</span>
        <svg viewBox="0 0 24 24" aria-hidden="true" className="month-trigger-icon">
          <path d="M7 2a1 1 0 0 1 1 1v1h8V3a1 1 0 1 1 2 0v1h1a2 2 0 0 1 2 2v12a3 3 0 0 1-3 3H6a3 3 0 0 1-3-3V6a2 2 0 0 1 2-2h1V3a1 1 0 0 1 1-1Zm12 8H5v8a1 1 0 0 0 1 1h12a1 1 0 0 0 1-1v-8ZM6 6a1 1 0 0 0-1 1v1h14V7a1 1 0 0 0-1-1H6Z" />
        </svg>
      </button>

      {isOpen ? (
        <div className="month-picker-popover">
          <div className="month-picker-head">
            <button className="quiet-btn month-year-nav" type="button" onClick={() => setDisplayYear((year) => year - 1)}>
              <svg viewBox="0 0 24 24" aria-hidden="true">
                <path d="M14.5 6 8.5 12l6 6" fill="none" stroke="currentColor" strokeWidth="2.4" strokeLinecap="round" strokeLinejoin="round" />
              </svg>
            </button>
            <strong>{displayYear}</strong>
            <button className="quiet-btn month-year-nav" type="button" onClick={() => setDisplayYear((year) => year + 1)}>
              <svg viewBox="0 0 24 24" aria-hidden="true">
                <path d="M9.5 6 15.5 12l-6 6" fill="none" stroke="currentColor" strokeWidth="2.4" strokeLinecap="round" strokeLinejoin="round" />
              </svg>
            </button>
          </div>

          <div className="month-grid">
            {MONTH_LABELS.map((label, index) => {
              const monthNumber = index + 1
              const monthValue = `${displayYear}-${String(monthNumber).padStart(2, '0')}`
              const isSelected = selectedYear === displayYear && selectedMonth === monthNumber
              return (
                <button
                  key={`${displayYear}-${label}`}
                  type="button"
                  className={`month-chip ${isSelected ? 'is-selected' : ''}`}
                  onClick={() => {
                    onChange(monthValue)
                    setIsOpen(false)
                  }}
                >
                  {label}
                </button>
              )
            })}
          </div>
        </div>
      ) : null}
    </div>
  )
}
