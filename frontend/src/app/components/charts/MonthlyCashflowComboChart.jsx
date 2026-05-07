import { buildMonthlyCashflowComboOption } from "../../domain/chartOptions.js";
import { EChart } from "./EChart.jsx";

export function MonthlyCashflowComboChart({ data = [], onSelect }) {
  return (
    <EChart
      empty={!data.length}
      emptyMessage="Brak miesięcznych danych cashflow."
      exportName="cashflow-miesieczny"
      onClick={onSelect}
      option={buildMonthlyCashflowComboOption(data)}
    />
  );
}
