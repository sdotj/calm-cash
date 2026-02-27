import type { ReactNode } from 'react'

type DashboardModalProps = {
  title: string
  onClose: () => void
  children: ReactNode
}

export function DashboardModal({ title, onClose, children }: DashboardModalProps) {
  return (
    <div className="dashboard-modal-backdrop" role="presentation" onClick={onClose}>
      <section
        className="dashboard-modal"
        role="dialog"
        aria-modal="true"
        aria-label={title}
        onClick={(event) => event.stopPropagation()}
      >
        <header className="dashboard-modal-header">
          <h3>{title}</h3>
          <button className="quiet-btn" type="button" onClick={onClose}>
            Close
          </button>
        </header>
        {children}
      </section>
    </div>
  )
}
