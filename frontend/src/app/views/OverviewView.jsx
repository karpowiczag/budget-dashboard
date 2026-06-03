import { Panel } from "../components/ui/Panel.jsx";
import { money, percent } from "../domain/formatters.js";

export function OverviewView({ data, financialFlows = {}, fireSummary, onNavigate, safeToSpend = null }) {
  const monthControl = data?.monthControl || {};
  const kpis = data?.kpis || {};
  const plan = data?.savingsPlan || {};
  const remaining = safeToSpend ? Number(safeToSpend.safeToSpend || 0) : Number(monthControl.remainingBudget || 0);
  const daily = safeToSpend ? Number(safeToSpend.dailyAllowed || 0) : Number(monthControl.dailyAllowed || 0);
  const recurringCount = (data?.recurring || []).length;

  const cards = [
    {
      label: "Stopa oszczędzania",
      value: percent(Number(kpis.savingsRate || 0)),
      detail: `wpływy ${money(Number(kpis.income || 0))}`,
      to: "control",
    },
    {
      label: "Wydatki miesiąca",
      value: money(Number(monthControl.spendToDate || 0)),
      detail: `cel ${money(Number(plan.targetMonthlySpend || 0))}`,
      to: "reports",
    },
    {
      label: "Przepływy majątkowe",
      value: money(Number(financialFlows.total || 0)),
      detail: "oszczędności, inwestycje, dług",
      to: "wealth",
    },
    {
      label: "Cykliczne",
      value: String(recurringCount),
      detail: "rozpoznane pozycje",
      to: "obligations",
    },
    {
      label: "Do sprawdzenia",
      value: String(Number(kpis.toCheck || 0)),
      detail: money(Number(kpis.toCheckAmount || 0)),
      to: "transactions",
      tone: Number(kpis.toCheck || 0) > 0 ? "warn" : "",
    },
    {
      label: "FIRE",
      value: fireSummary?.currentPortfolioValue ? money(Number(fireSummary.currentPortfolioValue)) : "Otwórz",
      detail: "portfel i prognoza",
      to: "fire",
    },
  ];

  return (
    <section className="viewStack overview">
      <Panel title="Ile możemy bezpiecznie wydać?">
        <div className="overviewHero">
          <div className={`overviewHeroMain ${remaining < 0 ? "warn" : "good"}`}>
            <span>Bezpiecznie do wydania</span>
            <strong>{money(remaining)}</strong>
            <p>{money(daily)} dziennie · wydane {money(Number(monthControl.spendToDate || 0))}</p>
          </div>
          <button type="button" className="primaryButton" onClick={() => onNavigate?.("control")}>
            Otwórz budżet
          </button>
        </div>
      </Panel>

      <Panel title="Skrót">
        <div className="overviewCards">
          {cards.map((card) => (
            <button
              type="button"
              className={`moduleCard inspectable ${card.tone || ""}`.trim()}
              key={card.label}
              onClick={() => onNavigate?.(card.to)}
            >
              <span>{card.label}</span>
              <strong>{card.value}</strong>
              <p>{card.detail}</p>
            </button>
          ))}
        </div>
      </Panel>
    </section>
  );
}
