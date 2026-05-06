import { Panel } from "../components/ui/Panel.jsx";
import { money } from "../domain/formatters.js";

export function RecurringView({ monthControl, oneoffs, recurring, recurringCalendar }) {
  return (
    <section className="gridTwo">
      <Panel title="Cykliczne płatności">
        <div className="recurringCalendar">
          {recurringCalendar.map((row) => (
            <div className="recurringItem" key={`${row.merchant}-${row.category}`}>
              <span>{row.avgDay}</span>
              <div>
                <strong>{row.merchant}</strong>
                <p>{row.category} · {row.months} mies.</p>
              </div>
              <em>{money(row.monthlyAverage)}</em>
            </div>
          ))}
        </div>
      </Panel>

      <Panel title="Fundusze celowe">
        <div className="fundGrid">
          {(monthControl.sinkingFunds || []).map((fund) => (
            <div className="fundItem" key={fund.category}>
              <span>{fund.name}</span>
              <strong>{money(fund.monthlySetAside)}</strong>
              <p>{money(fund.yearlyNeed)} rocznie</p>
            </div>
          ))}
        </div>
      </Panel>

      <Panel title="Cykliczne koszty">
        <div className="tableWrap smallRows">
          <table>
            <thead>
              <tr>
                <th>Sprzedawca</th>
                <th>Kategoria</th>
                <th>Mies.</th>
                <th>Suma</th>
              </tr>
            </thead>
            <tbody>
              {recurring.map((row) => (
                <tr key={`${row.merchant}-${row.category}`}>
                  <td>{row.merchant}</td>
                  <td>{row.category}</td>
                  <td className="num">{row.months}</td>
                  <td className="num">{money(row.sum)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </Panel>

      <Panel title="Duże jednorazowe">
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
              {oneoffs.map((row) => (
                <tr key={`${row.date || row.postedDate || row.Data}-${row.merchant || row.Sprzedawca}-${row.amount || row.spend || row.id}`}>
                  <td>{row.date || row.postedDate || row.Data}</td>
                  <td>{row.merchant || row.Sprzedawca}</td>
                  <td>{row.category || row.correctedCategory || row["Kategoria skorygowana"]}</td>
                  <td className="num">{money(row.amount || row.spend || row["Wydatek analizy"])}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </Panel>
    </section>
  );
}
