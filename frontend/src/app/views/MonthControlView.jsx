import { Panel } from "../components/ui/Panel.jsx";
import { money } from "../domain/formatters.js";

export function MonthControlView({ categoryStatus, isHistorical, monthControl }) {
  return (
    <Panel title={isHistorical ? "Kontrola ostatniego miesiąca" : "Do wydania i prognoza miesiąca"}>
      <div className="monthControl">
        <div className="monthCards">
          <div>
            <span>{monthControl.month}</span>
            <strong>{money(monthControl.spendToDate)}</strong>
            <p>wydane po {monthControl.elapsedDays} dniach</p>
          </div>
          <div>
            <span>{isHistorical ? "Wydatki miesiąca" : "Prognoza końca miesiąca"}</span>
            <strong>{money(monthControl.projectedSpend)}</strong>
            <p>{monthControl.projectedDelta >= 0 ? "pod targetem" : "ponad target"}: {money(Math.abs(monthControl.projectedDelta))}</p>
          </div>
          <div>
            <span>{isHistorical ? "Różnica do targetu" : "Do wydania"}</span>
            <strong>{money(monthControl.remainingBudget)}</strong>
            <p>{isHistorical ? "po faktycznych wydatkach" : `${money(monthControl.dailyAllowed)} dziennie`}</p>
          </div>
          <div>
            <span>Dochód miesiąca</span>
            <strong>{money(monthControl.incomeToDate)}</strong>
            <p>rozpoznane pensje</p>
          </div>
        </div>

        <div className="insightGrid">
          <div className="alerts">
            <h3>Alerty</h3>
            {(monthControl.alerts || []).length ? (
              monthControl.alerts.slice(0, 5).map((alert) => (
                <div className={`alert ${alert.severity}`} key={`${alert.type}-${alert.message}`}>
                  <strong>{alert.type}</strong>
                  <span>{alert.message}</span>
                </div>
              ))
            ) : (
              <div className="empty">Brak ostrych alertów dla wybranego okresu.</div>
            )}
          </div>

          <div className="limitProgress">
            <h3>Status limitów</h3>
            {categoryStatus.map((row) => {
              const pct = Math.min(1.35, Math.max(0, row.currentMonthProjection / row.limit || 0));
              return (
                <div className="progressRow" key={row.category}>
                  <div>
                    <span>{row.category}</span>
                    <em>{money(row.currentMonthProjection)} / {money(row.limit)}</em>
                  </div>
                  <div className="barTrack">
                    <i className={pct > 1 ? "over" : pct > 0.8 ? "warn" : ""} style={{ width: `${Math.min(100, pct * 100)}%` }} />
                  </div>
                </div>
              );
            })}
          </div>
        </div>
      </div>
    </Panel>
  );
}
