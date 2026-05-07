import { buildSavingsWaterfallOption } from "../../domain/chartOptions.js";
import { EChart } from "./EChart.jsx";

export function SavingsWaterfallChart({ data = [] }) {
  return (
    <EChart
      className="chart compact"
      empty={!data.length}
      emptyMessage="Brak danych planu oszczędzania."
      exportName="scenariusz-oszczedzania"
      option={buildSavingsWaterfallOption(data)}
    />
  );
}
