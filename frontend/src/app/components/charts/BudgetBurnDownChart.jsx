import { buildBudgetBurnDownOption } from "../../domain/chartOptions.js";
import { EChart } from "./EChart.jsx";

export function BudgetBurnDownChart({ data = [], onSelect }) {
  return (
    <EChart
      empty={!data.length}
      emptyMessage="Brak danych dziennych dla wybranego miesiąca."
      exportName="tempo-wydatkow"
      onClick={onSelect}
      option={buildBudgetBurnDownOption(data)}
    />
  );
}
