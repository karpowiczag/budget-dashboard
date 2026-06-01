import { buildBudgetMixTreemapOption } from "../../domain/chartOptions.js";
import { EChart } from "./EChart.jsx";

export function BudgetMixTreemap({ data = [], onSelect }) {
  return (
    <EChart
      empty={!data.length}
      emptyMessage="Brak danych modelu budżetu."
      exportName="model-budzetu"
      onClick={onSelect}
      option={buildBudgetMixTreemapOption(data)}
    />
  );
}
