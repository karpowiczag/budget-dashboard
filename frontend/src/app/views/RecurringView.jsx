import { RecurringTimelineChart } from "../components/charts/RecurringTimelineChart.jsx";
import { Panel } from "../components/ui/Panel.jsx";
import { money } from "../domain/formatters.js";

export function RecurringView({ recurringSummary, onInspect }) {
  const summary = recurringSummary || { cards: [], recurringCalendar: [], sinkingFunds: [] };

  return (
    <section className="viewStack">
      <Panel title="Cykliczne zobowiązania miesiąca">
        <div className="monthCards">
          {summary.cards.map((card) => (
            <div key={card.label}>
              <span>{card.label}</span>
              <strong>{card.textValue ? card.value : money(card.value)}</strong>
              <p>{card.detail}</p>
            </div>
          ))}
        </div>
      </Panel>
      <section className="gridTwo">
        <Panel title="Kalendarz cyklicznych płatności (Top 24)">
          <RecurringTimelineChart
            data={summary.recurringTimeline || []}
            onSelect={(entry) => {
              const row = entry?.payload || entry;
              onInspect?.({ title: `Transakcje: ${row.merchant}`, filters: row.filter || { query: row.merchant }, useTimeScope: false });
            }}
          />
        </Panel>

        <Panel title="Cykliczne płatności (Top 16)">
          <div className="recurringCalendar">
            {summary.recurringCalendar.map((row) => (
              <button
                type="button"
                className="recurringItem recurringButton"
                key={`${row.merchant}-${row.category}`}
                onClick={() => onInspect?.({ title: `Transakcje: ${row.merchant}`, filters: { query: row.merchant }, useTimeScope: false })}
              >
                <span>{row.avgDay}</span>
                <div>
                  <strong>{row.merchant}</strong>
                  <p>{row.category} · {row.months} mies.</p>
                </div>
                <em>{money(row.monthlyAverage)}</em>
              </button>
            ))}
          </div>
        </Panel>

        <Panel title="Fundusze celowe">
          <div className="fundGrid">
            {summary.sinkingFunds.map((fund) => (
              <div className="fundItem" key={fund.category}>
                <span>{fund.name}</span>
                <strong>{money(fund.monthlySetAside)}</strong>
                <p>{money(fund.yearlyNeed)} rocznie</p>
              </div>
            ))}
          </div>
        </Panel>
      </section>
    </section>
  );
}
