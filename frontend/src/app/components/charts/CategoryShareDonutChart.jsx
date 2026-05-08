import { buildCategoryShareDonutOption } from "../../domain/chartOptions.js";
import { EChart } from "./EChart.jsx";

export function CategoryShareDonutChart({ data = [], onSelect }) {
  return (
    <EChart
      className="chart compact"
      empty={!data.length}
      emptyMessage="Brak danych udziału kategorii."
      exportName="udzial-kategorii"
      onClick={onSelect}
      option={buildCategoryShareDonutOption(data)}
    />
  );
}
