import { buildMerchantShareDonutOption } from "../../domain/chartOptions.js";
import { EChart } from "./EChart.jsx";

export function MerchantShareDonutChart({ data = [], onSelect }) {
  return (
    <EChart
      className="chart compact"
      empty={!data.length}
      emptyMessage="Brak danych udziału sprzedawców."
      exportName="udzial-sprzedawcow"
      onClick={onSelect}
      option={buildMerchantShareDonutOption(data)}
    />
  );
}
