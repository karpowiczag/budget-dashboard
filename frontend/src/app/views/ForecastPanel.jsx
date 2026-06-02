import { useState } from "react";
import { Panel } from "../components/ui/Panel.jsx";
import { money } from "../domain/formatters.js";
import { selectForecast } from "../domain/budgetSelectors.js";

export function ForecastPanel({ startingLiquid = 0, monthlyIncome = 0, monthlySpend = 0 }) {
  const [incomeAdj, setIncomeAdj] = useState("0");
  const [expenseAdj, setExpenseAdj] = useState("0");
  const forecast = selectForecast({
    startingLiquid,
    monthlyIncome,
    monthlySpend,
    months: 12,
    incomeAdjustmentPct: Number(incomeAdj) || 0,
    expenseAdjustmentPct: Number(expenseAdj) || 0,
  });
  return (
    <Panel title="Prognoza przepływów (12 mies.)">
      <p className="nwMuted">Projekcja płynnych środków przy obecnym tempie. Suwaki testują scenariusze: spadek dochodu, wzrost wydatków.</p>
      <div className="nwNewLiability">
        <label className="nwInlineField">
          Zmiana dochodu %
          <input type="number" step="5" value={incomeAdj} aria-label="Zmiana dochodu w procentach" onChange={(event) => setIncomeAdj(event.target.value)} />
        </label>
        <label className="nwInlineField">
          Zmiana wydatków %
          <input type="number" step="5" value={expenseAdj} aria-label="Zmiana wydatków w procentach" onChange={(event) => setExpenseAdj(event.target.value)} />
        </label>
      </div>
      {forecast.negative ? (
        <div className="dataQualityBanner warn">
          <span>
            Ujemny miesięczny bilans ({money(forecast.monthlyNet)}).{" "}
            {forecast.runwayMonths != null
              ? `Płynne środki wystarczą na ~${forecast.runwayMonths} mies.`
              : "Brak płynnej poduszki — saldo spada."}
          </span>
        </div>
      ) : null}
      <div className="nwSummary">
        <div>
          <span className="nwMuted">Miesięczny bilans</span>
          <strong className={forecast.negative ? "warn" : "good"}>{money(forecast.monthlyNet)}</strong>
          <span className="nwMuted">dochód {money(forecast.adjustedIncome)} − wydatki {money(forecast.adjustedSpend)}</span>
        </div>
        <div>
          <span className="nwMuted">Płynne dziś</span>
          <strong>{money(forecast.startingLiquid)}</strong>
        </div>
        <div>
          <span className="nwMuted">Za 12 mies.</span>
          <strong className={forecast.endingBalance < 0 ? "warn" : "good"}>{money(forecast.endingBalance)}</strong>
          <span className="nwMuted">3 mies. {money(forecast.checkpoints.m3)} · 6 mies. {money(forecast.checkpoints.m6)}</span>
        </div>
      </div>
      {forecast.startingLiquid === 0 ? (
        <p className="nwMuted">Ustaw salda kont w module Majątek, aby prognoza startowała od realnego salda (teraz pokazuje skumulowane oszczędności).</p>
      ) : null}
    </Panel>
  );
}
