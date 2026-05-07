import { buildFixednessBreakdownOption } from "../../domain/chartOptions.js";
import { EChart } from "./EChart.jsx";

export function FixednessBreakdownChart({ data = [], onSelect }) {
  return (
    <EChart
      className="chart compact"
      empty={!data.length}
      emptyMessage="Brak rozbicia kosztów stałych i zmiennych."
      exportName="koszty-stale-zmienne"
      onClick={onSelect}
      option={buildFixednessBreakdownOption(data)}
    />
  );
}
