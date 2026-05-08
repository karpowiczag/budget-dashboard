import { useEffect, useState } from "react";
import { FireAllocationChart } from "../components/charts/FireAllocationChart.jsx";
import { FireProjectionChart } from "../components/charts/FireProjectionChart.jsx";
import { ReportDataTable } from "../components/tables/ReportDataTable.jsx";
import { Panel } from "../components/ui/Panel.jsx";
import { money, percent } from "../domain/formatters.js";

export function FireView({ fireSettings, fireSummary, onSaveSettings, saving = false, settingsStatus }) {
  const summary = fireSummary || {};
  const [draft, setDraft] = useState(fireSettings || null);

  useEffect(() => {
    if (fireSettings) setDraft(fireSettings);
  }, [fireSettings]);

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
  const budget = summary.budgetLink || {};
  const hasSpendTarget = summary.spendTargetConfigured === true;
  const setupTarget = "Ustaw cel";
  const saveDraft = () => {
    if (draft) onSaveSettings?.(draft);
  };
  return (
    <section className="viewStack">
      <Panel
        title="Plan FIRE"
        action={
          <button className="primaryButton" disabled={!draft || saving} onClick={saveDraft} type="button">
            {saving ? "Zapisuję..." : "Zapisz FIRE"}
          </button>
        }
      >
        <div className="dataQualityBanner neutral">
          <strong>Model planistyczny</strong>
          <span>To projekcja w realnych złotych na podstawie celu FIRE, raportów MyFund i tempa inwestowania z budżetu. Nie jest poradą inwestycyjną ani podatkową.</span>
        </div>
        <div className="metricGrid four">
          <Metric label="Kapitał dziś" value={money(summary.currentPortfolioValue)} detail={`${summary.positionCount} pozycji`} />
          <SpendTargetCard
            draft={draft}
            hasSpendTarget={hasSpendTarget}
            saving={saving}
            value={hasSpendTarget ? money(summary.fireNumber) : setupTarget}
            onChange={(value) => update(draft, setDraft, "monthlySpendOverride", value)}
            onSave={saveDraft}
          />
          <Metric label="Brakuje" value={hasSpendTarget ? money(summary.gapToFireNumber) : setupTarget} detail={hasSpendTarget ? `do wieku ${summary.targetAge}` : "nie liczę bez celu"} warn={hasSpendTarget && Number(summary.gapToFireNumber) > 0} />
          <Metric label="Wymagane / mies." value={hasSpendTarget ? money(baseScenario?.requiredMonthlyContribution) : setupTarget} detail={hasSpendTarget ? "scenariusz bazowy" : "najpierw cel wydatków"} />
        </div>
        {settingsStatus && <div className={`inlineStatus ${settingsStatus.type}`}>{settingsStatus.message}</div>}
      </Panel>

      <Panel title="Budżet domowy -> FIRE">
        <div className="dataQualityBanner neutral">
          <strong>{budget.linked ? `Źródło: budżet ${budget.budgetYear}` : "Brak połączenia z budżetem"}</strong>
          <span>{budget.note || "Odbuduj budżet domowy, żeby FIRE używał realnych przepływów."}</span>
        </div>
        <div className="metricGrid four">
          <Metric label="Cel wydatków FIRE" value={hasSpendTarget ? money(summary.monthlySpendTarget) : setupTarget} detail={hasSpendTarget ? "z ustawień FIRE" : "nie zgaduję tej liczby"} />
          <Metric label="Wpłata FIRE" value={money(budget.firePortfolioMonthlyContribution || summary.currentMonthlyWealthContribution)} detail={budget.contributionOverrideUsed ? "override" : "z budżetu: inwestycje + oszcz. netto"} />
          <Metric label="Nadpłata kredytu" value={money(budget.loanOverpaymentMonthly)} detail="osobno od portfela FIRE" />
          <Metric label="Poduszka zostaje" value={money(summary.emergencyReserveTarget || budget.emergencyReserveTarget)} detail="nie liczę jej do pomostu" />
        </div>
        <div className="metricGrid four">
          <Metric label="Wydatki teraz" value={money(budget.currentMonthlyLivingSpend)} detail={`${budget.activeMonths || 0} mies. danych`} />
          <Metric label="Okres budżetu" value={`${budget.activeMonths || 0} mies.`} detail={budget.budgetYear ? `rok ${budget.budgetYear}` : "brak danych"} />
          <Metric label="Inwestycje" value={money(budget.actualMonthlyInvestments)} detail="średnio / mies." />
          <Metric label="Konto oszcz. netto" value={money(budget.savingsAccountMonthlyNet)} detail={`brutto ${money(budget.savingsAccountMonthlyGrossDeposits)}`} />
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
        <Panel title="Alokacja portfela inwestycyjnego">
          <FireAllocationChart data={summary.allocation || []} />
        </Panel>
      </section>

      <section className="gridTwo">
        <Panel title="Akcje do wykonania">
          <ReportDataTable
            exportName="fire-actions"
            rows={summary.actionItems || []}
            columns={[
              { key: "priority", header: "Priorytet" },
              { key: "title", header: "Decyzja" },
              { key: "amount", header: "Kwota", className: "num", render: (row) => money(row.amount) },
              { key: "detail", header: "Dlaczego" },
            ]}
          />
        </Panel>
        <Panel title="Plan wpłat">
          <div className="planCards compactCards">
            <Metric label="Obecnie" value={money(summary.contributionPlan?.currentMonthly)} detail="miesięcznie" />
            <Metric label="Wymagane" value={money(summary.contributionPlan?.requiredMonthlyBase)} detail="scenariusz bazowy" />
            <Metric label="Brakuje / mies." value={money(summary.contributionPlan?.additionalMonthlyNeeded)} detail="do celu 50" warn={Number(summary.contributionPlan?.additionalMonthlyNeeded || 0) > 0} />
            <Metric label="Limit IKE+IKZE" value={money(summary.contributionPlan?.monthlyRetirementWrapperCapacity)} detail="2 osoby / mies." />
          </div>
          <p className="mutedText">{summary.contributionPlan?.recommendation}</p>
        </Panel>
      </section>

      <section className="gridTwo">
        <Panel title="Pomost i dostępność kapitału">
          <div className="planCards compactCards">
            <Metric label="Luka 50-60" value={money(summary.liquidBridgeGapToAge60)} detail="płynny kapitał" warn={Number(summary.liquidBridgeGapToAge60 || 0) > 0} />
            <Metric label="Luka 50-65" value={money(summary.liquidBridgeGapToAge65)} detail="konserwatywnie" warn={Number(summary.liquidBridgeGapToAge65 || 0) > 0} />
            <Metric label="Kapitał pomostowy" value={money(summary.bridgeableLiquidCapital)} detail="po zostawieniu poduszki" />
            <Metric label="Rezerwa podatku" value={money(summary.withdrawalPlan?.estimatedTaxReserve)} detail="Belka od zysków opod." />
          </div>
          <p className="mutedText">{summary.withdrawalPlan?.sequence}</p>
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

      <Panel title="Ustawienia produkcyjne">
        <FireSettingsForm draft={draft} onChange={setDraft} />
      </Panel>

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
        <Panel title="Jakość danych FIRE">
          <div className="planCards compactCards">
            <Metric label="Status" value={summary.dataQuality?.status || "brak"} detail={summary.dataQuality?.newestReportDate || "bez daty"} />
            <Metric label="Stare źródła" value={summary.dataQuality?.staleSourceCount || 0} detail=">45 dni" warn={Number(summary.dataQuality?.staleSourceCount || 0) > 0} />
            <Metric label="Nieznane aktywa" value={summary.dataQuality?.unknownAssetClassCount || 0} detail={money(summary.dataQuality?.unknownAssetClassValue)} warn={Number(summary.dataQuality?.unknownAssetClassCount || 0) > 0} />
            <Metric label="Nieznane segmenty" value={summary.dataQuality?.unknownWrapperCount || 0} detail={money(summary.dataQuality?.unknownWrapperValue)} warn={Number(summary.dataQuality?.unknownWrapperCount || 0) > 0} />
          </div>
          <p className="mutedText">{summary.dataQuality?.note}</p>
        </Panel>
      </section>

      <section className="gridTwo">
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

function SpendTargetCard({ draft, hasSpendTarget, onChange, onSave, saving, value }) {
  const rawValue = draft?.monthlySpendOverride ?? "";
  const numericValue = Number(rawValue || 0);
  const canSave = !!draft && numericValue > 0 && !saving;
  return (
    <div className={`metricCard editableMetric ${!hasSpendTarget ? "warn" : ""}`}>
      <span>Cel FIRE</span>
      <strong>{value}</strong>
      <small>{hasSpendTarget ? "na podstawie docelowych wydatków" : "wpisz miesięczne wydatki FIRE"}</small>
      <div className="inlineEdit">
        <label>
          <span>Cel wydatków FIRE miesięcznie</span>
          <input
            aria-label="Cel wydatków FIRE miesięcznie"
            disabled={!draft || saving}
            min="0"
            onChange={(event) => onChange(event.target.value === "" ? null : Number(event.target.value))}
            placeholder="np. 10000"
            step="100"
            type="number"
            value={rawValue}
          />
        </label>
        <button className="secondaryButton compact" disabled={!canSave} onClick={onSave} type="button">
          {saving ? "Zapisuję..." : "Zapisz cel"}
        </button>
      </div>
    </div>
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

function FireSettingsForm({ draft, onChange }) {
  if (!draft) {
    return <div className="emptyState">Ładuję ustawienia FIRE...</div>;
  }
  const allocationSum = ["targetEquityShare", "targetBondShare", "targetCashShare", "targetAlternativeShare"]
    .reduce((sum, key) => sum + Number(draft[key] || 0), 0);
  return (
    <div className="fireSettingsGrid">
      <NumberField label="Wiek teraz" value={draft.currentAge} min={18} max={90} step={1} onChange={(value) => update(draft, onChange, "currentAge", value)} />
      <NumberField label="Wiek FIRE" value={draft.targetAge} min={19} max={90} step={1} onChange={(value) => update(draft, onChange, "targetAge", value)} />
      <MoneyField label="Docelowe wydatki FIRE / mies." value={draft.monthlySpendOverride} placeholder="np. 10000" onChange={(value) => update(draft, onChange, "monthlySpendOverride", value)} />
      <MoneyField label="Wpłata FIRE override / mies." value={draft.monthlyContributionOverride} placeholder="z budżetu" onChange={(value) => update(draft, onChange, "monthlyContributionOverride", value)} />
      <PercentField label="SWR" value={draft.safeWithdrawalRate} onChange={(value) => update(draft, onChange, "safeWithdrawalRate", value)} />
      <PercentField label="Zwrot ostrożny" value={draft.pessimisticRealReturn} onChange={(value) => update(draft, onChange, "pessimisticRealReturn", value)} />
      <PercentField label="Zwrot bazowy" value={draft.expectedRealReturn} onChange={(value) => update(draft, onChange, "expectedRealReturn", value)} />
      <PercentField label="Zwrot dobry rynek" value={draft.optimisticRealReturn} onChange={(value) => update(draft, onChange, "optimisticRealReturn", value)} />
      <PercentField label="Cel akcje" value={draft.targetEquityShare} onChange={(value) => update(draft, onChange, "targetEquityShare", value)} />
      <PercentField label="Cel obligacje" value={draft.targetBondShare} onChange={(value) => update(draft, onChange, "targetBondShare", value)} />
      <PercentField label="Cel gotówka" value={draft.targetCashShare} onChange={(value) => update(draft, onChange, "targetCashShare", value)} />
      <PercentField label="Cel alternatywne" value={draft.targetAlternativeShare} onChange={(value) => update(draft, onChange, "targetAlternativeShare", value)} />
      <PercentField label="Pasmo rebalancingu" value={draft.rebalanceBand} onChange={(value) => update(draft, onChange, "rebalanceBand", value)} />
      <div className={`allocationCheck ${Math.abs(allocationSum - 1) <= 0.01 ? "ok" : "warn"}`}>
        Suma alokacji: {(allocationSum * 100).toFixed(1)}%
      </div>
    </div>
  );
}

function NumberField({ label, max, min, onChange, step, value }) {
  return (
    <label className="field">
      <span>{label}</span>
      <input type="number" min={min} max={max} step={step} value={value ?? ""} onChange={(event) => onChange(Number(event.target.value || 0))} />
    </label>
  );
}

function MoneyField({ label, onChange, placeholder = "z budżetu", value }) {
  return (
    <label className="field">
      <span>{label}</span>
      <input type="number" min="0" step="100" value={value ?? ""} placeholder={placeholder} onChange={(event) => onChange(event.target.value === "" ? null : Number(event.target.value))} />
    </label>
  );
}

function PercentField({ label, onChange, value }) {
  return (
    <label className="field">
      <span>{label}</span>
      <input type="number" step="0.1" value={fractionToPercent(value)} onChange={(event) => onChange(Number(event.target.value || 0) / 100)} />
    </label>
  );
}

function update(draft, onChange, field, value) {
  onChange({ ...draft, [field]: value });
}

function fractionToPercent(value) {
  return Number.isFinite(Number(value)) ? Number((Number(value) * 100).toFixed(3)) : "";
}
