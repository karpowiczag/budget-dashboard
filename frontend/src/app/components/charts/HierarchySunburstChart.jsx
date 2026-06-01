import { buildHierarchySunburstOption } from "../../domain/chartOptions.js";
import { EChart } from "./EChart.jsx";

export function HierarchySunburstChart({ data = [], onSelect }) {
  return (
    <EChart
      className="chart tall"
      empty={!data.length}
      emptyMessage="Brak hierarchii kategorii dla wybranego zakresu."
      exportName="hierarchia-wydatkow"
      onClick={onSelect}
      option={buildHierarchySunburstOption(data)}
    />
  );
}
