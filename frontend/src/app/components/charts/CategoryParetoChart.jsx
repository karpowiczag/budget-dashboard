import { buildCategoryParetoOption } from "../../domain/chartOptions.js";
import { EChart } from "./EChart.jsx";

export function CategoryParetoChart({ data = [], onSelect }) {
  return (
    <EChart
      className="chart tall"
      empty={!data.length}
      emptyMessage="Brak kategorii wydatków dla wybranego zakresu."
      exportName="pareto-kategorii"
      onClick={onSelect}
      option={buildCategoryParetoOption(data)}
    />
  );
}
