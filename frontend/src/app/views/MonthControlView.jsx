import { BudgetBurnDownChart } from "../components/charts/BudgetBurnDownChart.jsx";
import { CategoryLimitProjectionChart } from "../components/charts/CategoryLimitProjectionChart.jsx";
import { DailyCalendarHeatmapChart } from "../components/charts/DailyCalendarHeatmapChart.jsx";
import { LimitGaugeChart } from "../components/charts/LimitGaugeChart.jsx";
import { Panel } from "../components/ui/Panel.jsx";
import { money } from "../domain/formatters.js";

export function MonthControlView({ monthDashboard, savingsFocus, spendingPlanSections = [], onInspect }) {
  const alerts = monthDashboard?.alerts || [];
  const categoryStatus = monthDashboard?.categoryStatus || [];
  const focus = monthDashboard?.savingsFocus || savingsFocus || {};
  const burnDown = monthDashboard?.burnDown || [];
  const dailyHeatmap = monthDashboard?.dailyHeatmap || [];
  const limitChart = monthDashboard?.limitChart || [];
  const selectedDay = monthDashboard?.selectedDay;

  return (
    <section className="viewStack">
      <Panel title={monthDashboard?.title || "Kontrola miesiąca"}>
        <div className="monthControl">
          <SavingsFocusPanel focus={focus} onInspect={onInspect} />

          <div className={`monthDecisionGrid ${selectedDay ? "" : "single"}`}>
            {selectedDay && (
              <button
                type="button"
                className="metricCard inspectable selectedDayCard"
                onClick={() => onInspect?.({ title: `Transakcje: dzień ${String(selectedDay.day).padStart(2, "0")}`, filters: { date: selectedDay.date }, useTimeScope: false })}
              >
                <span>Dzień {String(selectedDay.day).padStart(2, "0")}</span>
                <strong>{money(selectedDay.spend)}</strong>
                <p>{selectedDay.transactions} transakcji · wpływy {money(selectedDay.income)}</p>
              </button>
            )}

            {!!spendingPlanSections.length && (
              <div className="spendingPlanRail">
                <h3>Spending plan miesiąca</h3>
                <div className="spendingPlanSteps">
                  {spendingPlanSections.map((section) => (
                    <button
                      type="button"
                      className={`spendingStep ${section.tone || ""}`}
                      key={section.label}
                      onClick={() => onInspect?.({ title: `Transakcje: ${section.label}`, filters: section.filter || {}, useTimeScope: true })}
                    >
                      <span>{section.label}</span>
                      <strong>{money(section.value)}</strong>
                      <p>{formatDetail(section.detail)}</p>
                    </button>
                  ))}
                </div>
              </div>
            )}
          </div>

          <div className="insightGrid">
            <div className="alerts">
              <h3>Alerty</h3>
              {alerts.length ? (
                alerts.slice(0, 5).map((alert) => (
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
              <h3>Status głównych limitów</h3>
              {categoryStatus.map((row) => (
                <LimitProgressGroup key={`${row.scope || "category"}-${row.name || row.category}`} row={row} onInspect={onInspect} />
              ))}
            </div>
          </div>
        </div>
      </Panel>
      <section className="gridTwo">
        <Panel title="Tempo wydatków">
          <BudgetBurnDownChart
            data={burnDown}
            onSelect={(row) => onInspect?.({ title: `Transakcje: dzień ${String(row.day).padStart(2, "0")}`, filters: { date: row.date }, useTimeScope: false })}
          />
        </Panel>
        <Panel title="Mapa dziennych wydatków">
          <DailyCalendarHeatmapChart
            data={dailyHeatmap}
            onSelect={(row) => onInspect?.({ title: `Transakcje: ${row.date}`, filters: { date: row.date }, useTimeScope: false })}
          />
        </Panel>
        <Panel title="Ryzyko limitów">
          <CategoryLimitProjectionChart
            data={limitChart}
            onSelect={(entry) => {
              const row = entry?.payload || entry;
              onInspect?.({ title: `Transakcje: ${row.category}`, filters: row.filter || { category: row.category }, useTimeScope: true });
            }}
          />
        </Panel>
        <Panel title="Największe przekroczenie limitu">
          <LimitGaugeChart
            data={limitChart}
            onSelect={(entry) => {
              const row = entry?.payload || entry;
              onInspect?.({ title: `Transakcje: ${row.category}`, filters: row.filter || { category: row.category }, useTimeScope: true });
            }}
          />
        </Panel>
      </section>
    </section>
  );
}

function LimitProgressGroup({ row, onInspect }) {
  const label = row.name || row.category;
  const pct = progressRatio(row);
  const children = row.children || [];
  const spent = actualMonthSpend(row);
  const overSpend = actualOverLimit(row);
  const title = `Wydane realnie w miesiącu: ${money(spent)}. Limit: ${money(row.limit)}. Prognoza końca miesiąca: ${money(row.currentMonthProjection || spent)}.`;
  return (
    <details className="progressGroup">
      <summary className="progressRow progressSummary">
        <div>
          <span>{label}</span>
          <em title={title}>{money(spent)} / {money(row.limit)}</em>
        </div>
        <div className="barTrack">
          <i className={progressTone(pct)} style={{ width: `${Math.min(100, pct * 100)}%` }} />
        </div>
        <small>
          {children.length ? `Wydane / limit · Kategorie (${children.length})` : "Wydane / limit"}
          {overSpend > 0 ? ` · Ponad: ${money(overSpend)}` : ""}
        </small>
      </summary>
      <div className="progressChildren">
        <button
          type="button"
          className="miniInspectButton"
          onClick={() => onInspect?.({ title: `Transakcje: ${label}`, filters: row.filter || { category: row.category }, useTimeScope: true })}
        >
          Pokaż transakcje limitu
        </button>
        {children.length ? (
          children.map((child) => (
            <button
              type="button"
              className="progressChildRow"
              key={child.category}
              onClick={() => onInspect?.({ title: `Transakcje: ${child.category}`, filters: child.filter || { category: child.category }, useTimeScope: true })}
            >
              <span>{child.category}</span>
              <em title={`Wydane realnie w miesiącu: ${money(actualMonthSpend(child))}. Limit: ${money(child.limit)}. Prognoza końca miesiąca: ${money(child.currentMonthProjection || actualMonthSpend(child))}.`}>
                {money(actualMonthSpend(child))} / {money(child.limit)}
              </em>
              <div className="barTrack compactTrack">
                <i className={progressTone(progressRatio(child))} style={{ width: `${Math.min(100, progressRatio(child) * 100)}%` }} />
              </div>
              <small>{actualOverLimit(child) > 0 ? `Ponad: ${money(actualOverLimit(child))}` : `Zapas: ${money(Math.max(0, child.remainingThisMonth || 0))}`}</small>
            </button>
          ))
        ) : (
          <div className="empty">Brak kategorii z aktywnymi wydatkami w tym limicie.</div>
        )}
      </div>
    </details>
  );
}

function SavingsFocusPanel({ focus, onInspect }) {
  const rows = focus?.topActions || [];
  return (
    <div className="savingsFocus">
      <div className="focusHead">
        <div>
          <h3>Co ciąć teraz</h3>
          <p>Kontrolowalne kategorie, które realnie przesuwają budżet w tym miesiącu.</p>
        </div>
        <div className="focusTotals">
          <span>Do cięcia {money(focus?.cutNowPotential || 0)}</span>
          <span>Do rozbicia {money(focus?.reviewPotential || 0)}</span>
        </div>
      </div>
      {rows.length ? (
        <div className="focusRows">
          {rows.map((row) => (
            <button
              type="button"
              className={`focusRow ${row.tone}`}
              key={`${row.category}-${row.decision}`}
              onClick={() => onInspect?.({ title: `Transakcje: ${row.name || row.category}`, filters: row.filter || { category: row.category }, useTimeScope: true })}
            >
              <span>{row.decision}</span>
              <strong>{row.name || row.category}</strong>
              <em>{money(row.current)} / limit {money(row.limit)}</em>
              <b>{money(row.potentialMonthly)}</b>
              <small>{row.note}</small>
            </button>
          ))}
        </div>
      ) : (
        <div className="empty">Brak kontrolowalnych przekroczeń w wybranym miesiącu.</div>
      )}
    </div>
  );
}

function progressRatio(row) {
  return Math.min(1.35, Math.max(0, actualMonthSpend(row) / Number(row.limit || 0) || 0));
}

function progressTone(pct) {
  return pct > 1 ? "over" : pct > 0.8 ? "warn" : "";
}

function actualMonthSpend(row) {
  return Number(row.currentMonthSpend ?? row.current ?? row.currentMonthly ?? row.currentMonthProjection ?? 0);
}

function actualOverLimit(row) {
  return Math.max(0, actualMonthSpend(row) - Number(row.limit || 0));
}

function formatDetail(detail) {
  if (typeof detail !== "string") return "";
  return detail
    .replace(/(-?\d+(?:\.\d+)?) dziennie/g, (_, amount) => `${money(Number(amount))} dziennie`)
    .replace(/(-?\d+(?:\.\d+)?) PLN/g, (_, amount) => money(Number(amount)));
}
