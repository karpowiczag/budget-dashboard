import { buildFireProjectionOption } from "../../domain/chartOptions.js";
import { EChart } from "./EChart.jsx";

export function FireProjectionChart({ currentValue = 0, currentAge = 36, targetAge = 50, target = 0, scenarios = [] }) {
  return (
    <EChart
      empty={!scenarios.length}
      emptyMessage="Brak danych do prognozy FIRE."
      exportName="fire-prognoza"
      option={buildFireProjectionOption(scenarios, target, currentAge, targetAge, currentValue)}
    />
  );
}
