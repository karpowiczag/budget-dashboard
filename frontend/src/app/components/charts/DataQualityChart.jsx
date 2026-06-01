import { buildAmountBandsOption, buildConfidenceOption } from "../../domain/chartOptions.js";
import { money } from "../../domain/formatters.js";
import { EChart } from "./EChart.jsx";

export function DataQualityChart({ data, onInspect }) {
  const confidence = data?.confidence || [];
  const amountBands = data?.amountBands || [];
  const cards = data?.cards || [];

  return (
    <section className="dataQuality">
      <div className="qualityCards">
        {cards.map((card) => {
          const content = (
            <>
              <span>{card.label}</span>
              <strong>{card.value}</strong>
              {card.amount != null && <em>{money(card.amount)}</em>}
            </>
          );
          if (card.filter === null) {
            return <div key={card.label} className="qualityCard passive">{content}</div>;
          }
          return (
            <button
              type="button"
              key={card.label}
              className="qualityCard"
              onClick={() => onInspect?.({
                title: `Transakcje: ${card.label}`,
                filters: card.filter || {},
                useTimeScope: card.useTimeScope ?? true,
              })}
            >
              {content}
            </button>
          );
        })}
      </div>
      <section className="qualityCharts">
        <EChart
          className="chart compact"
          empty={!confidence.length}
          emptyMessage="Brak danych jakości kategoryzacji."
          exportName="pewnosc-kategoryzacji"
          onClick={(row) => onInspect?.({ title: `Transakcje: pewność ${row.confidence}`, filters: row.filter, useTimeScope: true })}
          option={buildConfidenceOption(confidence)}
        />
        <EChart
          className="chart compact"
          empty={!amountBands.length}
          emptyMessage="Brak rozkładu kwot transakcji."
          exportName="histogram-kwot"
          onClick={(row) => {
            const detail = row.maxAmount == null ? `od ${money(row.minAmount)}` : `${money(row.minAmount)} - ${money(row.maxAmount)}`;
            onInspect?.({ title: `Transakcje: kwoty ${detail}`, filters: { flow: "spend" }, useTimeScope: true });
          }}
          option={buildAmountBandsOption(amountBands)}
        />
      </section>
    </section>
  );
}
