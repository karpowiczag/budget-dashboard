import { buildCategoryLimitProjectionOption } from "../../domain/chartOptions.js";
import { EChart } from "./EChart.jsx";

export function CategoryLimitProjectionChart({ data = [], onSelect }) {
  const rows = data.slice(0, 10);
  return (
    <EChart
      className="chart tall"
      empty={!rows.length}
      emptyMessage="Brak aktywnych limitów dla wybranego miesiąca."
      exportName="ryzyko-limitow"
      onClick={onSelect}
      option={buildCategoryLimitProjectionOption(rows)}
    />
  );
}
