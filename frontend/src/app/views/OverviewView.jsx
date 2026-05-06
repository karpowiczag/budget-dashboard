import { CashflowChart } from "../components/charts/CashflowChart.jsx";
import { Panel } from "../components/ui/Panel.jsx";
import { CHART_COLORS } from "../domain/charts.js";
import { money, percent } from "../domain/formatters.js";

export function OverviewView({ budgetMix, kpis, monthly, needs, mixedNeeds, savingsTarget, wants, wantsTarget }) {
  return (
    <section className="viewStack">
      <section className="gridTwo">
        <Panel title="Cashflow miesięczny">
          <CashflowChart data={monthly} />
        </Panel>

        <Panel title="Model budżetu">
          <div className="mixList mixListFull">
            {budgetMix.map((row, index) => (
              <div className="mixRow" key={row.bucket}>
                <i style={{ background: CHART_COLORS[index % CHART_COLORS.length] }} />
                <span>{row.bucket}</span>
                <strong>{money(row.sum)}</strong>
                <em>{percent(row.incomeShare)}</em>
              </div>
            ))}
          </div>
        </Panel>
      </section>

      <Panel title="Benchmark i presja na budżet">
        <div className="benchmark">
          <div>
            <span>Potrzeby + mieszane</span>
            <strong>{money(needs + mixedNeeds)}</strong>
            <p>limit 50%: {money(kpis.income * 0.5)}</p>
          </div>
          <div>
            <span>Zachcianki</span>
            <strong>{money(wants)}</strong>
            <p>punkt 30%: {money(wantsTarget)}</p>
          </div>
          <div>
            <span>Oszczędzanie operacyjne</span>
            <strong>{money(kpis.operatingSurplus)}</strong>
            <p>minimum 20%: {money(savingsTarget)}</p>
          </div>
          <div>
            <span>Nadwyżka po inwestycjach</span>
            <strong>{money(kpis.unassignedSurplus)}</strong>
            <p>do decyzji lub dalszego inwestowania</p>
          </div>
        </div>
      </Panel>
    </section>
  );
}
