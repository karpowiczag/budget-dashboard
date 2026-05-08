import { Info } from "lucide-react";
import { SavingsRadarChart } from "../components/charts/SavingsRadarChart.jsx";
import { SavingsWaterfallChart } from "../components/charts/SavingsWaterfallChart.jsx";
import { ReportDataTable } from "../components/tables/ReportDataTable.jsx";
import { Panel } from "../components/ui/Panel.jsx";
import { bucketAliasesForLimit } from "../domain/budgetSelectors.js";
import { money } from "../domain/formatters.js";

export function SavingsPlanView({
  data,
  financialFlows = { rows: [], total: 0, count: 0 },
  categoryExamples = {},
  categorySubcategories = {},
  isHistorical,
  plan,
  parentPlanRows = [],
  primaryPlanRows = [],
  planRows,
  planSummary,
  planTitle,
  plannedInvestmentAfterCuts,
  plannedSpendAfterCuts,
  recommendedCuts,
  savingsRadar,
  savingsWaterfall,
  settings,
  settingsStatus,
  bucketOptions = [],
  onBucketOverrideChange,
  onLimitChange,
  onSaveSettings,
  onSettingChange,
}) {
  const minMonths = Number(settings?.emergencyFundMinMonths || 3);
  const comfortMonths = Number(settings?.emergencyFundComfortMonths || 6);
  const coreMonthlyCost = Number(plan.coreMonthlyCost || 0);

  return (
    <Panel
      title={planTitle}
      action={
        <button className="primaryButton" onClick={onSaveSettings} type="button">
          Zapisz ustawienia
        </button>
      }
    >
      <div className="savingPlan">
        {isHistorical && (
          <div className="historicalNotice">
            To jest rok zamknięty. Ten moduł nie jest planem działania na {data.year}, tylko pokazuje, ile dałyby limity i które nawyki przenieść do aktualnego budżetu.
          </div>
        )}
        <div className="planCards">
          <div>
            <span>{isHistorical ? "Hipotetyczny target" : "Target wydatków"}</span>
            <input
              className="planNumberInput"
              type="number"
              min="0"
              step="100"
              value={Math.round(Number(settings?.targetMonthlySpend || plan.targetMonthlySpend))}
              onChange={(event) => onSettingChange("targetMonthlySpend", Number(event.target.value || 0))}
              aria-label="Target wydatków"
            />
            <p>ambitnie: {money(settings?.aggressiveMonthlySpend || plan.aggressiveMonthlySpend)}</p>
          </div>
          <div>
            <span>Ambitny target</span>
            <input
              className="planNumberInput"
              type="number"
              min="0"
              step="100"
              value={Math.round(Number(settings?.aggressiveMonthlySpend || plan.aggressiveMonthlySpend))}
              onChange={(event) => onSettingChange("aggressiveMonthlySpend", Number(event.target.value || 0))}
              aria-label="Ambitny target wydatków"
            />
            <p>ustawienie globalne</p>
          </div>
          <div>
            <span>{isHistorical ? "Możliwy przelew wtedy" : "Wolne środki po target spend"}</span>
            <strong>{money(plan.targetInvestmentTransfer)}</strong>
            <p>to nie miesza inwestycji z nadpłatą kredytu</p>
          </div>
          <div>
            <span>{isHistorical ? "Utracony potencjał limitów" : "Potencjał z limitów"}</span>
            <strong>{money(planSummary.potentialMonthly)}</strong>
            <p>{money(planSummary.potentialYearly)} rocznie</p>
          </div>
          <div>
            <span>{isHistorical ? "Scenariusz po limitach" : "Po limitach"}</span>
            <strong>{money(plannedSpendAfterCuts)}</strong>
            <p>wolne środki: {money(plannedInvestmentAfterCuts)} / mies.</p>
          </div>
          <div>
            <span>Fundusz awaryjny</span>
            <div className="emergencyInputs">
              <label>
                min
                <input
                  className="monthInput"
                  type="number"
                  min="1"
                  step="1"
                  value={minMonths}
                  onChange={(event) => onSettingChange("emergencyFundMinMonths", Number(event.target.value || 1))}
                  aria-label="Minimalny fundusz awaryjny w miesiącach"
                />
              </label>
              <label>
                komfort
                <input
                  className="monthInput"
                  type="number"
                  min="1"
                  step="1"
                  value={comfortMonths}
                  onChange={(event) => onSettingChange("emergencyFundComfortMonths", Number(event.target.value || 1))}
                  aria-label="Komfortowy fundusz awaryjny w miesiącach"
                />
              </label>
            </div>
            <p>{minMonths} mies.: {money(coreMonthlyCost * minMonths)} · komfort: {money(coreMonthlyCost * comfortMonths)}</p>
          </div>
          <div>
            <span>Przepływy majątkowe</span>
            <strong>{money(financialFlows.total || 0)}</strong>
            <p>szczegóły są w module Majątek, tutaj liczy się tylko cel planu</p>
          </div>
        </div>
        {settingsStatus && <div className={`inlineStatus ${settingsStatus.type}`}>{settingsStatus.message}</div>}

        <RecommendedCutsSummary recommendedCuts={recommendedCuts} isHistorical={isHistorical} />

        <div className="planTable">
          <h3>Główne limity</h3>
          <MainLimitsList
            categoryRows={planRows}
            categoryExamples={categoryExamples}
            categorySubcategories={categorySubcategories}
            isHistorical={isHistorical}
            rows={primaryPlanRows.length ? primaryPlanRows : parentPlanRows}
            bucketOptions={bucketOptions}
            onBucketOverrideChange={onBucketOverrideChange}
            onLimitChange={onLimitChange}
          />
        </div>

        <details className="planChartsDisclosure">
          <summary>Wizualizacja scenariusza</summary>
          <section className="gridTwo embeddedGrid">
            <div className="planChart">
              <h3>Scenariusz oszczędzania</h3>
              <SavingsWaterfallChart data={savingsWaterfall || []} />
            </div>
            <div className="planChart">
              <h3>Mapa decyzji oszczędnościowych</h3>
              <SavingsRadarChart data={savingsRadar || []} />
            </div>
          </section>
        </details>
      </div>
    </Panel>
  );
}

function RecommendedCutsSummary({ recommendedCuts, isHistorical }) {
  if (!recommendedCuts) return null;
  return (
    <div className="recommendedCuts">
      <div>
        <span>{isHistorical ? "Realistycznie można było ciąć" : "Realistyczne cięcie"}</span>
        <strong>{money(recommendedCuts.realisticCut)}</strong>
        <p>wydatki po cięciu: {money(recommendedCuts.realisticSpend)} · inwestycje: {money(recommendedCuts.realisticInvestable)}</p>
      </div>
      <div>
        <span>{isHistorical ? "Agresywny scenariusz wtedy" : "Agresywne cięcie"}</span>
        <strong>{money(recommendedCuts.aggressiveCut)}</strong>
        <p>wydatki po cięciu: {money(recommendedCuts.aggressiveSpend)} · inwestycje: {money(recommendedCuts.aggressiveInvestable)}</p>
      </div>
      <div>
        <span>Logika decyzji</span>
        <strong>{recommendedCuts.groups?.length || 0} grupy</strong>
        <p>uznaniowe tniemy, niejasne rozbijamy ręcznie, rachunków i długu nie ścinamy automatycznie</p>
      </div>
    </div>
  );
}

function MainLimitsList({ bucketOptions, categoryRows = [], categoryExamples = {}, categorySubcategories = {}, isHistorical, rows, onBucketOverrideChange, onLimitChange }) {
  const visibleRows = (rows || [])
    .filter((row) => row.scope === "bucket")
    .filter((row) => !["Przychody", "Transfer techniczny"].includes(row.name || row.category))
    .sort(compareMainLimitRows)
    .slice(0, 7);
  if (!visibleRows.length) {
    return <div className="empty">Brak limitów nadrzędnych do pokazania.</div>;
  }
  return (
    <div className="mainLimitList">
      {visibleRows.map((row) => (
        <MainLimitBlock
          categoryRows={categoryRows}
          categoryExamples={categoryExamples}
          categorySubcategories={categorySubcategories}
          isHistorical={isHistorical}
          key={`${row.scope}:${row.name || row.category}`}
          row={row}
          bucketOptions={bucketOptions}
          onBucketOverrideChange={onBucketOverrideChange}
          onLimitChange={onLimitChange}
        />
      ))}
    </div>
  );
}

function MainLimitBlock({ bucketOptions, categoryRows, categoryExamples, categorySubcategories, isHistorical, row, onBucketOverrideChange, onLimitChange }) {
  const name = row.name || row.category;
  const children = categoryRowsForMainLimit(name, categoryRows);
  const averageLabel = isHistorical ? "Średnia wtedy" : "Średnia / mies.";
  return (
    <details className="mainLimitBlock">
      <summary className="mainLimitSummary">
        <span className="mainLimitName">{name}</span>
        <span className="mainLimitMetric">
          <span>{averageLabel}</span>
          <strong>{money(row.currentMonthly || 0)}</strong>
        </span>
        <label className="mainLimitInput" onClick={(event) => event.stopPropagation()}>
          Limit
          <input
            className="limitInput"
            type="number"
            min="0"
            step="100"
            value={Math.round(row.limit)}
            onChange={(event) => onLimitChange(row.scope, name, Number(event.target.value || 0))}
            aria-label={`Limit ${row.scope} ${name}`}
          />
        </label>
        <span className="mainLimitMetric strong">
          <span>Potencjał</span>
          <strong>{money(row.potentialMonthly)}</strong>
        </span>
        <span className="mainLimitChildrenCount">Kategorie ({children.length})</span>
      </summary>

      <div className="mainLimitChildren">
        {children.length ? (
          <ReportDataTable
            className="smallRows planLimitTable"
            rows={children}
            columns={[
              {
                key: "category",
                header: "Kategoria",
                render: (child) => (
                  <CategoryNameWithTooltip
                    category={child.category}
                    examples={categoryExamples[child.category] || []}
                    subcategories={categorySubcategories[child.category] || []}
                  />
                ),
              },
              {
                key: "bucket",
                header: "Główny limit",
                render: (child) => (
                  <select
                    className="bucketSelect"
                    value={child.bucket}
                    onChange={(event) => onBucketOverrideChange?.(child.category, event.target.value === child.originalBucket ? "" : event.target.value)}
                    aria-label={`Główny limit ${child.category}`}
                  >
                    {bucketOptions.map((option) => (
                      <option key={option} value={option}>{option}</option>
                    ))}
                  </select>
                ),
              },
              {
                key: "currentMonthly",
                header: averageLabel,
                className: "num",
                render: (child) => money(child.currentMonthly || child.current),
                sortValue: (child) => Number(child.currentMonthly || child.current || 0),
              },
              {
                key: "limit",
                header: "Override limitu",
                sortValue: (child) => Number(child.limit || 0),
                render: (child) => (
                  <input
                    className="limitInput"
                    type="number"
                    min="0"
                    step="50"
                    value={Math.round(child.limit)}
                    onChange={(event) => onLimitChange(child.scope || "category", child.name || child.category, Number(event.target.value || 0))}
                    aria-label={`Limit ${child.category}`}
                  />
                ),
              },
              {
                key: "potentialMonthly",
                header: "Ponad override",
                className: "num strong",
                render: (child) => money(child.potentialMonthly),
                sortValue: (child) => Number(child.potentialMonthly || 0),
              },
              { key: "action", header: "Co robić" },
            ]}
          />
        ) : (
          <div className="empty">Brak kategorii w tym koszyku.</div>
        )}
      </div>
    </details>
  );
}

function CategoryNameWithTooltip({ category, examples, subcategories }) {
  const subcategoryItems = (subcategories || []).filter(Boolean);
  const exampleItems = (examples || []).filter(Boolean);
  if (!subcategoryItems.length && !exampleItems.length) return <span>{category}</span>;
  return (
    <span className="categoryTooltipWrap" tabIndex={0}>
      <span>{category}</span>
      <Info aria-hidden="true" size={13} strokeWidth={2.4} />
      <span className="categoryTooltip" role="tooltip">
        {!!subcategoryItems.length && (
          <span className="tooltipSection">
            <strong>Podkategorie</strong>
            {subcategoryItems.map((item) => (
              <span key={item}>{item}</span>
            ))}
          </span>
        )}
        {!!exampleItems.length && (
          <span className="tooltipSection">
            <strong>Przykłady transakcji</strong>
            {exampleItems.map((item) => (
              <span key={item}>{item}</span>
            ))}
          </span>
        )}
      </span>
    </span>
  );
}

function categoryRowsForMainLimit(name, categoryRows) {
  const aliases = bucketAliasesForLimit(name);
  return (categoryRows || [])
    .filter((row) => aliases.has(row.bucket))
    .sort((left, right) => Number(right.currentMonthly || 0) - Number(left.currentMonthly || 0) || left.category.localeCompare(right.category, "pl"));
}

function compareMainLimitRows(left, right) {
  const rank = {
    "Obowiązkowe stałe": 0,
    "Obowiązkowe zmienne": 1,
    "Do rozbicia": 2,
    Nieobowiązkowe: 3,
    Inwestycje: 4,
    "Konto oszczędnościowe": 5,
    "Nadpłata kredytu": 6,
    Potrzeby: 0,
    "Potrzeby mieszane": 2,
    Zachcianki: 3,
    "Zachcianki do rozbicia": 2,
  };
  const leftName = left.name || left.category || "";
  const rightName = right.name || right.category || "";
  return (rank[leftName] ?? 20) - (rank[rightName] ?? 20)
    || Number(right.currentMonthly || 0) - Number(left.currentMonthly || 0)
    || leftName.localeCompare(rightName, "pl");
}
