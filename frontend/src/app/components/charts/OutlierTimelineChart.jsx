import { buildOutlierTimelineOption } from "../../domain/chartOptions.js";
import { EChart } from "./EChart.jsx";

export function OutlierTimelineChart({ data = [], onSelect }) {
  return (
    <EChart
      className="chart compact"
      empty={!data.length}
      emptyMessage="Brak dużych jednorazowych wydatków w zakresie."
      exportName="duze-jednorazowe"
      onClick={onSelect}
      option={buildOutlierTimelineOption(data)}
    />
  );
}
