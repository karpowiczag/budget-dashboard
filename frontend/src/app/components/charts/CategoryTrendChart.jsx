import { buildCategoryTrendOption } from "../../domain/chartOptions.js";
import { EChart } from "./EChart.jsx";

export function CategoryTrendChart({ data = [], onSelect }) {
  return (
    <EChart
      className="chart tall"
      empty={!data.length}
      emptyMessage="Brak trendów kategorii dla wybranego zakresu."
      exportName="trend-kategorii"
      onClick={onSelect}
      option={buildCategoryTrendOption(data)}
    />
  );
}
