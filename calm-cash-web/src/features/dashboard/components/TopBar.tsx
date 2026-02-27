import { useEffect, useRef, useState } from 'react'
import type { CSSProperties } from 'react'
import type { Alert, MeResponse } from '../../../types'
import calmCashLogoIcon from '../../../assets/calm-cash-full-no-text.png'

function MoonIcon() {
  return (
    <svg viewBox="0 0 24 24" aria-hidden="true">
      <path d="M14.45 3.25a1 1 0 0 0-1.13 1.29 7.95 7.95 0 0 1 .44 2.63A8 8 0 0 1 5.75 15.2a7.95 7.95 0 0 1-2.63-.44 1 1 0 0 0-1.29 1.13A10 10 0 1 0 14.45 3.25Z" />
    </svg>
  )
}

function SunIcon() {
  return (
    <svg viewBox="0 0 24 24" aria-hidden="true">
      <path d="M12 7a5 5 0 1 0 5 5 5 5 0 0 0-5-5Zm0-5a1 1 0 0 1 1 1v2a1 1 0 0 1-2 0V3a1 1 0 0 1 1-1Zm0 17a1 1 0 0 1 1 1v2a1 1 0 0 1-2 0v-2a1 1 0 0 1 1-1Zm9-8a1 1 0 0 1 0 2h-2a1 1 0 0 1 0-2Zm-16 0a1 1 0 0 1 0 2H3a1 1 0 0 1 0-2Zm12.36-6.36a1 1 0 0 1 1.42 1.42l-1.41 1.41a1 1 0 1 1-1.42-1.42Zm-10.31 10.3a1 1 0 0 1 1.42 1.42l-1.41 1.42a1 1 0 0 1-1.42-1.42Zm10.31 3.13a1 1 0 1 1-1.42 1.42l-1.41-1.42a1 1 0 0 1 1.42-1.42ZM8.47 8.47a1 1 0 0 1-1.42 0L5.64 7.06a1 1 0 0 1 1.42-1.42l1.41 1.41a1 1 0 0 1 0 1.42Z" />
    </svg>
  )
}

type TopBarProps = {
  me: MeResponse | null
  alerts: Alert[]
  unreadAlertsCount: number
  onMarkAlertRead: (alertId: string) => Promise<void>
  onLogout: () => Promise<void>
  isDarkMode: boolean
  onToggleTheme: () => void
}

const TAGLINES = [
  "You're doing great. Let's check in on your budget.",
  'Ready for a quick money check-in?',
  "Nice to see you. Here's where things stand.",
  "Every dollar has a job. You've got this.",
  "Let's keep your spending calm and on track.",
  'How are we doing against your goals today?',
  'Small updates now make money feel easier later.',
  'Want to take a quick look at this month?',
  "You're in control. Let's make a smart move next.",
  'A clear plan today, less stress tomorrow.',
]
const SESSION_TAGLINE = TAGLINES[Math.floor(Math.random() * TAGLINES.length)]

export function TopBar({
  me,
  alerts,
  unreadAlertsCount,
  onMarkAlertRead,
  onLogout,
  isDarkMode,
  onToggleTheme,
}: TopBarProps) {
  const [showAlerts, setShowAlerts] = useState(false)
  const [showLogoutConfirm, setShowLogoutConfirm] = useState(false)
  const [alertsCaretRight, setAlertsCaretRight] = useState(120)
  const tagline = SESSION_TAGLINE
  const topbarRef = useRef<HTMLElement | null>(null)
  const alertsButtonRef = useRef<HTMLButtonElement | null>(null)
  const alertsPopoverRef = useRef<HTMLDivElement | null>(null)

  useEffect(() => {
    if (!showAlerts) {
      return
    }

    function updateCaretPosition() {
      const topbarRect = topbarRef.current?.getBoundingClientRect()
      const bellRect = alertsButtonRef.current?.getBoundingClientRect()
      if (!topbarRect || !bellRect) {
        return
      }
      const bellCenterFromRight = topbarRect.right - (bellRect.left + bellRect.width / 2)
      setAlertsCaretRight(Math.max(24, bellCenterFromRight))
    }

    updateCaretPosition()
    window.addEventListener('resize', updateCaretPosition)
    return () => window.removeEventListener('resize', updateCaretPosition)
  }, [showAlerts])

  useEffect(() => {
    if (!showAlerts) {
      return
    }

    function onPointerDown(event: MouseEvent | TouchEvent) {
      const target = event.target as Node | null
      if (!target) {
        return
      }

      if (alertsButtonRef.current?.contains(target)) {
        return
      }

      if (alertsPopoverRef.current?.contains(target)) {
        return
      }

      setShowAlerts(false)
    }

    function onKeyDown(event: KeyboardEvent) {
      if (event.key === 'Escape') {
        setShowAlerts(false)
      }
    }

    document.addEventListener('mousedown', onPointerDown)
    document.addEventListener('touchstart', onPointerDown)
    document.addEventListener('keydown', onKeyDown)

    return () => {
      document.removeEventListener('mousedown', onPointerDown)
      document.removeEventListener('touchstart', onPointerDown)
      document.removeEventListener('keydown', onKeyDown)
    }
  }, [showAlerts])

  return (
    <header className="dashboard-topbar panel-surface relative flex items-center justify-between gap-5" ref={topbarRef}>
      <div className="dashboard-topbar-main min-w-0 flex-1 text-left">
        <h1 className="m-0 text-[clamp(1.35rem,2.8vw,1.9rem)]">{me ? `Hi, ${me.displayName}` : 'Budget Dashboard'}</h1>
        <p className="topbar-subtitle mt-1 text-[0.84rem] font-bold text-[var(--ink-500)]">{tagline}</p>
      </div>

      <img
        src={calmCashLogoIcon}
        alt="Calm Cash logo"
        className="dashboard-logo-mark h-[72px] w-[72px] shrink-0 object-contain"
      />

      <div className="dashboard-topbar-controls flex flex-1 items-center justify-end gap-3">
        <button onClick={onToggleTheme} className="quiet-btn theme-toggle-btn" type="button" aria-pressed={isDarkMode}>
          <span className="sr-only">{isDarkMode ? 'Switch to light mode' : 'Switch to dark mode'}</span>
          {isDarkMode ? <SunIcon /> : <MoonIcon />}
        </button>

        <div className="alerts-menu-wrap">
          <button
            ref={alertsButtonRef}
            className="quiet-btn theme-toggle-btn alerts-bell-btn"
            type="button"
            onClick={() => setShowAlerts((value) => !value)}
            aria-expanded={showAlerts}
            aria-label="Open alerts"
          >
            <svg viewBox="0 0 24 24" aria-hidden="true">
              <path d="M12 2a6 6 0 0 0-6 6v3.84A2.5 2.5 0 0 1 5.2 13.6l-1 1A2 2 0 0 0 5.6 18h12.8a2 2 0 0 0 1.4-3.4l-1-1a2.5 2.5 0 0 1-.8-1.76V8a6 6 0 0 0-6-6Zm0 20a3 3 0 0 0 2.83-2H9.17A3 3 0 0 0 12 22Z" />
            </svg>
            {unreadAlertsCount > 0 ? <span className="alerts-badge">{unreadAlertsCount}</span> : null}
          </button>

          {showAlerts ? (
            <div
              className="alerts-menu-popover absolute right-0 top-[calc(100%+0.45rem)] z-20 w-[min(360px,86vw)] rounded-xl border border-[var(--line)] bg-[var(--surface-1)] p-2.5 shadow-[0_14px_28px_rgba(22,54,18,0.22)]"
              ref={alertsPopoverRef}
              style={{ '--alerts-caret-right': `${alertsCaretRight}px` } as CSSProperties}
            >
              <h4 className="mb-2 text-[0.92rem]">Alerts</h4>
              <div className="alerts-menu-list grid max-h-[290px] gap-2 overflow-auto">
                {alerts.map((alert) => (
                  <div
                    key={alert.id}
                    className={`alerts-menu-item grid grid-cols-[1fr_auto] gap-2 rounded-[10px] border border-[#d8eacd] p-2 ${alert.readAt ? 'read bg-[#fcfffa]' : 'bg-[#f9fdf6]'}`}
                  >
                    <div>
                      <p className="m-0 text-[0.82rem]">{alert.message}</p>
                      <small className="text-[0.72rem] text-[var(--ink-500)]">{new Date(alert.createdAt).toLocaleString()}</small>
                    </div>
                    {!alert.readAt ? (
                      <button className="quiet-btn" type="button" onClick={() => void onMarkAlertRead(alert.id)}>
                        Mark Read
                      </button>
                    ) : null}
                  </div>
                ))}
                {!alerts.length ? <p className="empty-state">No alerts right now.</p> : null}
              </div>
            </div>
          ) : null}
        </div>

        <button
          onClick={() => setShowLogoutConfirm(true)}
          className="quiet-btn theme-toggle-btn"
          type="button"
          aria-label="Logout"
        >
          <svg viewBox="0 0 24 24" aria-hidden="true">
            <path d="M10 3a1 1 0 0 1 1 1v2a1 1 0 1 1-2 0V5H5v14h4v-1a1 1 0 1 1 2 0v2a1 1 0 0 1-1 1H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2Zm5.29 4.29a1 1 0 0 1 1.42 0l3.99 4a1 1 0 0 1 0 1.42l-3.99 4a1 1 0 1 1-1.42-1.42L17.59 13H9a1 1 0 1 1 0-2h8.59l-2.3-2.29a1 1 0 0 1 0-1.42Z" />
          </svg>
        </button>
      </div>

      {showLogoutConfirm ? (
        <div
          className="logout-confirm-backdrop fixed inset-0 z-40 grid place-items-center p-4"
          role="presentation"
          onClick={() => setShowLogoutConfirm(false)}
        >
          <section
            className="logout-confirm-dialog w-[min(360px,100%)] rounded-xl border border-[var(--line)] bg-[var(--surface-1)] p-3.5"
            role="dialog"
            aria-modal="true"
            aria-label="Logout confirmation"
            onClick={(event) => event.stopPropagation()}
          >
            <p className="m-0 text-center font-bold text-[var(--ink-900)]">Are you sure you want to logout?</p>
            <div className="logout-confirm-actions mt-3 flex justify-between gap-2">
              <button className="quiet-btn" type="button" onClick={() => setShowLogoutConfirm(false)}>
                Cancel
              </button>
              <button
                className="primary-btn"
                type="button"
                onClick={() => {
                  setShowLogoutConfirm(false)
                  void onLogout()
                }}
              >
                Logout
              </button>
            </div>
          </section>
        </div>
      ) : null}
    </header>
  )
}
