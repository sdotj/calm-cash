import type { CSSProperties } from 'react'
import './segmented-tabs.css'

type SegmentedTabItem<T extends string> = {
  id: T
  label: string
  disabled?: boolean
}

type SegmentedTabsTheme = {
  trackBg?: string
  indicatorBg?: string
  activeText?: string
  inactiveText?: string
  borderColor?: string
  indicatorShadow?: string
}

type SegmentedTabsLayout = {
  padding?: string
  gap?: string
  radius?: string
  transitionMs?: number
}

type SegmentedTabsProps<T extends string> = {
  tabs: SegmentedTabItem<T>[]
  value: T
  onChange: (value: T) => void
  ariaLabel: string
  className?: string
  theme?: SegmentedTabsTheme
  layout?: SegmentedTabsLayout
}

export function SegmentedTabs<T extends string>({
  tabs,
  value,
  onChange,
  ariaLabel,
  className,
  theme,
  layout,
}: SegmentedTabsProps<T>) {
  const currentIndex = Math.max(
    0,
    tabs.findIndex((tab) => tab.id === value),
  )

  const style = {
    '--seg-count': String(tabs.length),
    '--seg-index': String(currentIndex),
    '--seg-pad': layout?.padding ?? '0.2rem',
    '--seg-gap': layout?.gap ?? '0.2rem',
    '--seg-radius': layout?.radius ?? '999px',
    '--seg-ms': `${layout?.transitionMs ?? 220}ms`,
    '--seg-track-bg': theme?.trackBg ?? 'var(--surface-2)',
    '--seg-indicator-bg': theme?.indicatorBg ?? '#ffffff',
    '--seg-active-text': theme?.activeText ?? 'var(--ink-900)',
    '--seg-inactive-text': theme?.inactiveText ?? 'var(--ink-700)',
    '--seg-border': theme?.borderColor ?? 'transparent',
    '--seg-indicator-shadow': theme?.indicatorShadow ?? '0 4px 8px rgba(0, 0, 0, 0.06)',
  } as CSSProperties

  return (
    <div className={`segmented-tabs ${className ?? ''}`.trim()} style={style} role="tablist" aria-label={ariaLabel}>
      <span className="segmented-tabs-indicator" aria-hidden="true" />
      {tabs.map((tab) => (
        <button
          key={tab.id}
          type="button"
          role="tab"
          aria-selected={tab.id === value}
          disabled={tab.disabled}
          className={tab.id === value ? 'is-active' : ''}
          onClick={() => onChange(tab.id)}
        >
          {tab.label}
        </button>
      ))}
    </div>
  )
}
