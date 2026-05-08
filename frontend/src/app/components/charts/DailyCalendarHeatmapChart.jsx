import { buildDailyCalendarHeatmapOption } from "../../domain/chartOptions.js";
import { EChart } from "./EChart.jsx";

export function DailyCalendarHeatmapChart({ data = [], onSelect }) {
  return (
    <EChart
      className="chart calendar"
      empty={!data.length}
      emptyMessage="Brak dziennych danych wydatków dla wybranego miesiąca."
      exportName="wydatki-dzienne-heatmap"
      onClick={onSelect}
      option={buildDailyCalendarHeatmapOption(data)}
    />
  );
}
