import { buildFireAllocationOption } from "../../domain/chartOptions.js";
import { EChart } from "./EChart.jsx";

export function FireAllocationChart({ data = [] }) {
  return (
    <EChart
      empty={!data.length}
      emptyMessage="Brak alokacji z raportów MyFund."
      exportName="fire-alokacja"
      option={buildFireAllocationOption(data)}
    />
  );
}
