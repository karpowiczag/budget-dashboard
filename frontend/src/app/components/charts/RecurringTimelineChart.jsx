import { buildRecurringTimelineOption } from "../../domain/chartOptions.js";
import { EChart } from "./EChart.jsx";

export function RecurringTimelineChart({ data = [], onSelect }) {
  return (
    <EChart
      className="chart compact"
      empty={!data.length}
      emptyMessage="Brak rozpoznanych cyklicznych płatności."
      exportName="cykliczne-platnosci"
      onClick={onSelect}
      option={buildRecurringTimelineOption(data)}
    />
  );
}
