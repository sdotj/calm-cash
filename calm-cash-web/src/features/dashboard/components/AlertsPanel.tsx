import type { Alert } from '../../../types'

type AlertsPanelProps = {
  alerts: Alert[]
  onMarkAlertRead: (alertId: string) => Promise<void>
}

export function AlertsPanel({ alerts, onMarkAlertRead }: AlertsPanelProps) {
  return (
    <article className="panel">
      <div className="panel-head">
        <h3>Alerts</h3>
        <p>Important budget and system notices.</p>
      </div>

      <div className="alerts-list">
        {alerts.map((alert) => (
          <div key={alert.id} className={`alert-row ${alert.readAt ? 'read' : ''}`}>
            <div className="alert-message-wrap">
              <span className="alert-dot" aria-hidden="true" />
              <div>
                <p>{alert.message}</p>
                <small>{new Date(alert.createdAt).toLocaleString()}</small>
              </div>
            </div>

            {!alert.readAt ? (
              <button className="quiet-btn" onClick={() => void onMarkAlertRead(alert.id)} type="button">
                Mark Read
              </button>
            ) : (
              <small>Read</small>
            )}
          </div>
        ))}

        {!alerts.length ? <p className="empty-state">No alerts. You are on track.</p> : null}
      </div>
    </article>
  )
}
