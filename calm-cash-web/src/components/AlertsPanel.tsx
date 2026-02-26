import type { Alert } from '../types'

type AlertsPanelProps = {
  alerts: Alert[]
  onMarkAlertRead: (alertId: string) => Promise<void>
}

export function AlertsPanel({ alerts, onMarkAlertRead }: AlertsPanelProps) {
  return (
    <article className="panel">
      <h3>Alerts</h3>
      <div className="alerts-list">
        {alerts.map((alert) => (
          <div key={alert.id} className={`alert-row ${alert.readAt ? 'read' : ''}`}>
            <div>
              <p>{alert.message}</p>
              <small>{new Date(alert.createdAt).toLocaleString()}</small>
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
        {!alerts.length ? <p>No alerts. You are on track.</p> : null}
      </div>
    </article>
  )
}
