import { FireAllocationChart } from "../components/charts/FireAllocationChart.jsx";
import { FireProjectionChart } from "../components/charts/FireProjectionChart.jsx";
import { ReportDataTable } from "../components/tables/ReportDataTable.jsx";
import { Panel } from "../components/ui/Panel.jsx";
import { money, percent } from "../domain/formatters.js";

export function FireView({ fireSummary }) {
  const summary = fireSummary || {};
  if (!summary.reportsLoaded) {
    return (
      <section className="viewStack">
        <Panel title="FIRE tracking">
          <div className="dataQualityBanner warn">
            <strong>Brak raportów inwestycyjnych</strong>
            <span>Włóż eksporty MyFund `portfelSklad` do `{summary.reportsPath || "fire/investments_reports"}` i odśwież aplikację.</span>
          </div>
        </Panel>
      </section>
    );
  }

  const baseScenario = summary.scenarios?.find((scenario) => scenario.id === "base") || summary.scenarios?.[0];
  return (
    <section className="viewStack">
      <Panel title="Plan FIRE">
        <div className="dataQualityBanner neutral">
          <strong>Model planistyczny</strong>
          <span>To projekcja w realnych złotych na podstawie raportów MyFund i budżetu życia. Nie jest poradą inwestycyjną ani podatkową.</span>
        </div>
        <div className="metricGrid four">
          <Metric label="Kapitał dziś" value={money(summary.currentPortfolioValue)} detail={`${summary.positionCount} pozycji`} />
          <Metric label="Cel FIRE" value={money(summary.fireNumber)} detail={`${percent(summary.safeWithdrawalRate)} SWR`} />
          <Metric label="Brakuje" value={money(summary.gapToFireNumber)} detail={`do wieku ${summary.targetAge}`} warn={Number(summary.gapToFireNumber) > 0} />
          <Metric label="Dopłata bazowa" value={money(baseScenario?.requiredMonthlyContribution)} detail="miesięcznie do celu" />
        </div>
      </Panel>

      <section className="gridTwo">
        <Panel title="Prognoza do wieku 50">
          <FireProjectionChart
            currentAge={summary.currentAge}
            currentValue={summary.currentPortfolioValue}
            scenarios={summary.scenarios}
            target={summary.fireNumber}
            targetAge={summary.targetAge}
          />
        </Panel>
        <Panel title="Alokacja aktywów">
          <FireAllocationChart data={summary.allocation || []} />
        </Panel>
      </section>

      <section className="gridTwo">
        <Panel title="Pomost i dostępność kapitału">
          <ReportDataTable
            exportName="fire-milestones"
            rows={summary.milestones || []}
            columns={[
              { key: "age", header: "Wiek", className: "num", render: (row) => row.age || "-" },
              { key: "label", header: "Kamień milowy" },
              { key: "requiredCapital", header: "Kapitał", className: "num", render: (row) => money(row.requiredCapital) },
              { key: "description", header: "Sens" },
            ]}
          />
        </Panel>
        <Panel title="Opakowania i płynność">
          <ReportDataTable
            exportName="fire-wrappers"
            rows={summary.wrappers || []}
            columns={[
              { key: "wrapper", header: "Segment" },
              { key: "value", header: "Wartość", className: "num", render: (row) => money(row.value) },
              { key: "share", header: "Udział", className: "num", render: (row) => percent(row.share) },
              { key: "liquidity", header: "Płynność" },
            ]}
          />
        </Panel>
      </section>

      <Panel title="Rebalancing">
        <ReportDataTable
          exportName="fire-rebalancing"
          rows={summary.rebalancing || []}
          columns={[
            { key: "assetClass", header: "Klasa" },
            { key: "currentShare", header: "Teraz", className: "num", render: (row) => percent(row.currentShare) },
            { key: "targetShare", header: "Cel", className: "num", render: (row) => percent(row.targetShare) },
            { key: "drift", header: "Odchylenie", className: "num", render: (row) => percent(row.drift) },
            { key: "amountToTarget", header: "Kwota do celu", className: "num", render: (row) => money(row.amountToTarget) },
            { key: "action", header: "Akcja" },
            { key: "priority", header: "Priorytet" },
          ]}
        />
      </Panel>

      <section className="gridTwo">
        <Panel title="Polskie reguły w modelu">
          <ReportDataTable
            exportName="fire-reguly"
            rows={summary.legalRules || []}
            columns={[
              { key: "label", header: "Reguła" },
              { key: "value", header: "Wartość" },
              { key: "note", header: "Komentarz" },
            ]}
          />
        </Panel>
        <Panel title="Źródła MyFund">
          <ReportDataTable
            exportName="fire-zrodla"
            rows={summary.sources || []}
            columns={[
              { key: "portfolio", header: "Portfel" },
              { key: "asOf", header: "Data" },
              { key: "positions", header: "Pozycje", className: "num" },
              { key: "value", header: "Wartość", className: "num", render: (row) => money(row.value) },
            ]}
          />
        </Panel>
      </section>
    </section>
  );
}

function Metric({ detail, label, value, warn = false }) {
  return (
    <div className={`metricCard ${warn ? "warn" : ""}`}>
      <span>{label}</span>
      <strong>{value}</strong>
      <small>{detail}</small>
    </div>
  );
}
