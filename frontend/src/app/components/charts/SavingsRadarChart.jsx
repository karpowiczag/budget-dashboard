import { buildSavingsRadarOption } from "../../domain/chartOptions.js";
import { EChart } from "./EChart.jsx";

export function SavingsRadarChart({ data = [] }) {
  return (
    <EChart
      className="chart compact"
      empty={!data.length}
      emptyMessage="Brak danych decyzji oszczędnościowych."
      exportName="radar-oszczedzania"
      option={buildSavingsRadarOption(data)}
    />
  );
}
