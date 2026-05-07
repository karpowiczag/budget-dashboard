import { buildCashflowSankeyOption } from "../../domain/chartOptions.js";
import { EChart } from "./EChart.jsx";

export function CashflowSankeyChart({ data, onSelect }) {
  const links = data?.links || [];
  return (
    <EChart
      className="chart sankey"
      empty={!links.length}
      emptyMessage="Brak danych cashflow do Sankey."
      exportName="cashflow-sankey"
      onClick={onSelect}
      option={buildCashflowSankeyOption(data)}
    />
  );
}
