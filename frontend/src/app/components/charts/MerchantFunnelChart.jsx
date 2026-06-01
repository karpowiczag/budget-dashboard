import { buildMerchantFunnelOption } from "../../domain/chartOptions.js";
import { EChart } from "./EChart.jsx";

export function MerchantFunnelChart({ data = [], onSelect }) {
  return (
    <EChart
      className="chart tall"
      empty={!data.length}
      emptyMessage="Brak sprzedawców dla wybranego zakresu."
      exportName="koncentracja-sprzedawcow"
      onClick={onSelect}
      option={buildMerchantFunnelOption(data)}
    />
  );
}
