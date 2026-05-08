import { buildLimitGaugeOption } from "../../domain/chartOptions.js";
import { EChart } from "./EChart.jsx";

export function LimitGaugeChart({ data = [], onSelect }) {
  return (
    <EChart
      className="chart gauge"
      empty={!data.length}
      emptyMessage="Brak aktywnego ryzyka limitów."
      exportName="najwieksze-ryzyko-limitu"
      onClick={onSelect}
      option={buildLimitGaugeOption(data)}
    />
  );
}
