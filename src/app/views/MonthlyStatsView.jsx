import { SpendBarChart } from "../components/charts/SpendBarChart.jsx";
import { TransactionsTable } from "../components/tables/TransactionsTable.jsx";
import { Panel } from "../components/ui/Panel.jsx";
import { money, percent } from "../domain/formatters.js";

export function MonthlyStatsView({ activeTimeLabel, scopedStats, scopedTransactions }) {
  return (
    <section className="viewStack">
      <Panel title={`Statystyki: ${activeTimeLabel}`}>
        <div className="monthCards">
          <div>
            <span>Wydatki</span>
            <strong>{money(scopedStats.spend)}</strong>
            <p>{scopedTransactions.length} transakcji w zakresie</p>
          </div>
          <div>
            <span>Wpływy</span>
            <strong>{money(scopedStats.income)}</strong>
            <p>rozpoznane pensje</p>
          </div>
          <div>
            <span>Nadwyżka</span>
            <strong>{money(scopedStats.income - scopedStats.spend)}</strong>
            <p>{scopedStats.income ? percent((scopedStats.income - scopedStats.spend) / scopedStats.income) : "brak wpływów"}</p>
          </div>
          <div>
            <span>Duże wydatki</span>
            <strong>{scopedStats.oneoffs.length}</strong>
            <p>wydatki {"≥"} 500 zł</p>
          </div>
        </div>
      </Panel>

      <section className="gridTwo">
        <Panel title="Kategorie w zakresie">
          <SpendBarChart data={scopedStats.categoryTop.slice(0, 10)} dataKey="category" yAxisWidth={126} left={94} />
        </Panel>

        <Panel title="Top kategorie">
          <div className="tableWrap smallRows">
            <table>
              <thead>
                <tr>
                  <th>Kategoria</th>
                  <th>Wydatki</th>
                  <th>Udział</th>
                </tr>
              </thead>
              <tbody>
                {scopedStats.categoryTop.slice(0, 12).map((row) => (
                  <tr key={row.category}>
                    <td>{row.category}</td>
                    <td className="num">{money(row.spend)}</td>
                    <td className="num">{percent(row.spend / (scopedStats.spend || 1))}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </Panel>
      </section>

      <section className="gridTwo">
        <Panel title="Top sprzedawcy">
          <div className="tableWrap smallRows">
            <table>
              <thead>
                <tr>
                  <th>Sprzedawca</th>
                  <th>Wydatki</th>
                </tr>
              </thead>
              <tbody>
                {scopedStats.merchants.slice(0, 12).map((row) => (
                  <tr key={row.merchant}>
                    <td>{row.merchant}</td>
                    <td className="num">{money(row.sum)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </Panel>

        <Panel title="Duże wydatki">
          <div className="tableWrap smallRows">
            <table>
              <thead>
                <tr>
                  <th>Data</th>
                  <th>Sprzedawca</th>
                  <th>Kategoria</th>
                  <th>Kwota</th>
                </tr>
              </thead>
              <tbody>
                {scopedStats.oneoffs.slice(0, 12).map((tx) => (
                  <tr key={tx.Lp}>
                    <td>{tx.Data}</td>
                    <td>{tx.Sprzedawca}</td>
                    <td>{tx["Kategoria skorygowana"]}</td>
                    <td className="num">{money(tx["Wydatek analizy"])}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </Panel>
      </section>

      <Panel title={`Transakcje: ${activeTimeLabel}`}>
        <TransactionsTable transactions={scopedTransactions.slice(0, 160)} />
      </Panel>
    </section>
  );
}
