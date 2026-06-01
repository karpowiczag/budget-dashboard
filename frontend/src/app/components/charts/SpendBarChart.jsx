import { buildSpendBarOption } from "../../domain/chartOptions.js";
import { EChart } from "./EChart.jsx";

export function SpendBarChart({
  data,
  dataKey,
  color = "#0f766e",
  onSelect,
}) {
  return (
    <EChart
      className="chart compact"
      empty={!data?.length}
      emptyMessage="Brak danych w wybranym zakresie."
      exportName={`wydatki-${dataKey}`}
      onClick={onSelect}
      option={buildSpendBarOption({ color, data, dataKey })}
    />
  );
}
