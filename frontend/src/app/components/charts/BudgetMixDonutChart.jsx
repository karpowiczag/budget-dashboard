import { buildBudgetMixDonutOption } from "../../domain/chartOptions.js";
import { EChart } from "./EChart.jsx";

export function BudgetMixDonutChart({ data = [], onSelect }) {
  return (
    <EChart
      className="chart compact"
      empty={!data.length}
      emptyMessage="Brak danych udziału koszyków."
      exportName="udzial-koszykow"
      onClick={onSelect}
      option={buildBudgetMixDonutOption(data)}
    />
  );
}
