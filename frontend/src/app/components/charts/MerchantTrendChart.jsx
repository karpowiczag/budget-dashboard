import { buildMerchantTrendOption } from "../../domain/chartOptions.js";
import { EChart } from "./EChart.jsx";

export function MerchantTrendChart({ data = [], onSelect }) {
  return (
    <EChart
      className="chart tall"
      empty={!data.length}
      emptyMessage="Brak miesięcznych trendów sprzedawców."
      exportName="trend-sprzedawcow"
      onClick={onSelect}
      option={buildMerchantTrendOption(data)}
    />
  );
}
