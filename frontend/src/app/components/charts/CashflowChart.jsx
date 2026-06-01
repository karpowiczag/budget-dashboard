import { buildMonthlyCashflowComboOption } from "../../domain/chartOptions.js";
import { EChart } from "./EChart.jsx";

export function CashflowChart({ data }) {
  return (
    <EChart
      empty={!data?.length}
      emptyMessage="Brak danych cashflow."
      exportName="cashflow"
      option={buildMonthlyCashflowComboOption(data || [])}
    />
  );
}
