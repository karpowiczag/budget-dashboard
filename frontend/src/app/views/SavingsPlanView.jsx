import { Panel } from "../components/ui/Panel.jsx";
import { money } from "../domain/formatters.js";

export function SavingsPlanView({
  data,
  isHistorical,
  plan,
  planRows,
  planSummary,
  planTitle,
  plannedInvestmentAfterCuts,
  plannedSpendAfterCuts,
  onLimitChange,
}) {
  return (
    <Panel title={planTitle}>
      <div className="savingPlan">
        {isHistorical && (
          <div className="historicalNotice">
            To jest rok zamknięty. Ten moduł nie jest planem działania na {data.year}, tylko pokazuje, ile dałyby limity i które nawyki przenieść do aktualnego budżetu.
          </div>
        )}
        <div className="planCards">
          <div>
            <span>{isHistorical ? "Hipotetyczny target" : "Target wydatków"}</span>
            <strong>{money(plan.targetMonthlySpend)}</strong>
            <p>ambitnie: {money(plan.aggressiveMonthlySpend)}</p>
          </div>
          <div>
            <span>{isHistorical ? "Możliwy przelew wtedy" : "Przelew inwestycyjny po pensji"}</span>
            <strong>{money(plan.targetInvestmentTransfer)}</strong>
            <p>agresywnie: {money(plan.aggressiveInvestmentTransfer)}</p>
          </div>
          <div>
            <span>{isHistorical ? "Utracony potencjał limitów" : "Potencjał z limitów"}</span>
            <strong>{money(planSummary.potentialMonthly)}</strong>
            <p>{money(planSummary.potentialYearly)} rocznie</p>
          </div>
          <div>
            <span>{isHistorical ? "Scenariusz po limitach" : "Po limitach"}</span>
            <strong>{money(plannedSpendAfterCuts)}</strong>
            <p>inwestycje: {money(plannedInvestmentAfterCuts)} / mies.</p>
          </div>
          <div>
            <span>Fundusz awaryjny 3 mies.</span>
            <strong>{money(plan.emergencyFundMin)}</strong>
            <p>komfort 6 mies.: {money(plan.emergencyFundComfort)}</p>
          </div>
        </div>

        <div className="planTable">
          <div className="tableWrap">
            <table>
              <thead>
                <tr>
                  <th>Kategoria</th>
                  <th>Koszyk</th>
                  <th>{isHistorical ? "Było / mies." : "Teraz / mies."}</th>
                  <th>{isHistorical ? "Limit do symulacji" : "Limit"}</th>
                  <th>{isHistorical ? "Można było" : "Potencjał"}</th>
                  <th>Priorytet</th>
                  <th>{isHistorical ? "Wniosek" : "Co robić"}</th>
                </tr>
              </thead>
              <tbody>
                {planRows.map((row) => (
                  <tr key={row.category}>
                    <td>{row.category}</td>
                    <td>{row.bucket}</td>
                    <td className="num">{money(row.currentMonthly)}</td>
                    <td>
                      <input
                        className="limitInput"
                        type="number"
                        min="0"
                        step="50"
                        value={Math.round(row.limit)}
                        onChange={(event) => onLimitChange(row.category, Number(event.target.value || 0))}
                        aria-label={`Limit ${row.category}`}
                      />
                    </td>
                    <td className="num strong">{money(row.potentialMonthly)}</td>
                    <td>
                      <span className={`priority ${row.priority}`}>{row.priority}</span>
                    </td>
                    <td>{row.action}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </Panel>
  );
}
