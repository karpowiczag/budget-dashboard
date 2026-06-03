import { useEffect, useMemo, useState } from "react";
import { FireAllocationChart } from "../components/charts/FireAllocationChart.jsx";
import { FireProjectionChart } from "../components/charts/FireProjectionChart.jsx";
import { ReportDataTable } from "../components/tables/ReportDataTable.jsx";
import { Panel } from "../components/ui/Panel.jsx";
import { money, percent } from "../domain/formatters.js";

export function FireView({ fireSettings, fireSummary, loading = false, onSaveSettings, saving = false, settingsStatus }) {
  const summary = fireSummary || {};
  const [draft, setDraft] = useState(fireSettings || null);

  useEffect(() => {
    if (fireSettings) setDraft(fireSettings);
  }, [fireSettings]);

  if (!summary.reportsLoaded) {
    return (
      <section className="viewStack fireView">
        <Panel title="FIRE tracking">
          {loading ? (
            <div className="emptyState">Ładuję dane FIRE...</div>
          ) : (
            <div className="fireOnboarding">
              <h3>Połącz portfel inwestycyjny</h3>
              <p>Moduł FIRE prognozuje niezależność finansową na podstawie eksportów MyFund. Aby go uruchomić:</p>
              <ol>
                <li>Pobierz z MyFund eksport składu portfela (<code>portfelSklad</code>).</li>
                <li>Umieść pliki w katalogu <code>{summary.reportsPath || "fire/investments_reports"}</code>.</li>
                <li>Odśwież aplikację — prognoza, alokacja i ryzyka pojawią się automatycznie.</li>
              </ol>
            </div>
          )}
        </Panel>
      </section>
    );
  }

  const budget = summary.budgetLink || {};
  const risks = summary.risks || [];
  const hasSpendTarget = summary.spendTargetConfigured === true;
  const saveDraft = () => {
    if (draft) onSaveSettings?.(draft);
  };

  return (
    <section className="viewStack fireView">
      <Panel
        title="Decyzja FIRE"
        action={
          <button className="primaryButton" disabled={!draft || saving} onClick={saveDraft} type="button">
            {saving ? "Zapisuję..." : "Zapisz FIRE"}
          </button>
        }
      >
        <div className="fireDecisionGrid">
          <SpendTargetEditor
            draft={draft}
            hasSpendTarget={hasSpendTarget}
            monthlySpendTarget={summary.monthlySpendTarget}
            fireNumber={summary.fireNumber}
            saving={saving}
            onChange={(value) => update(draft, setDraft, "monthlySpendOverride", value)}
            onSave={saveDraft}
          />
          <DecisionSummary actionItems={summary.actionItems || []} risks={risks} hasSpendTarget={hasSpendTarget} />
          <ContributionSummary budget={budget} contributionPlan={summary.contributionPlan} currentMonthlyWealthContribution={summary.currentMonthlyWealthContribution} />
        </div>
        {settingsStatus && <div className={`inlineStatus ${settingsStatus.type}`}>{settingsStatus.message}</div>}
      </Panel>

      <Panel title="Ryzyka inwestycyjne" action={<RiskCounter risks={risks} />}>
        <div className="dataQualityBanner neutral compactBanner">
          <strong>Aktualny portfel MyFund</strong>
          <span>Najpierw ryzyka, potem prognoza. Ocena obejmuje alokację, koncentrację, płynność, walutę, pomost i podatek.</span>
        </div>
        <RiskCards risks={risks} />
        <details className="fireDisclosure">
          <summary>Pełna tabela ryzyk</summary>
          <ReportDataTable
            className="smallRows fireRiskTable"
            emptyMessage="Brak istotnych ryzyk dla aktualnych danych."
            exportName="fire-risks"
            rows={risks}
            columns={[
              { key: "level", header: "Poziom", render: (row) => <RiskLevel level={row.level} /> },
              { key: "area", header: "Obszar" },
              { key: "title", header: "Ryzyko" },
              { key: "metric", header: "Metryka" },
              { key: "value", header: "Wartość", className: "num" },
              { key: "threshold", header: "Próg", className: "num" },
              { key: "recommendation", header: "Co zrobić" },
            ]}
          />
        </details>
      </Panel>

      <section className="gridTwo">
        <Panel title="Prognoza i luka do celu">
          <FireProjectionChart
            currentAge={summary.currentAge}
            currentValue={summary.currentPortfolioValue}
            scenarios={summary.scenarios}
            target={summary.fireNumber}
            targetAge={summary.targetAge}
          />
          <ScenarioStrip scenarios={summary.scenarios || []} />
          <div className="fireMiniMetrics">
            <Metric label="Szansa sukcesu (Monte-Carlo)" value={hasSpendTarget ? percent(summary.monteCarloSuccessRate) : "Ustaw cel"} detail="dojście do celu w symulacji" warn={hasSpendTarget && Number(summary.monteCarloSuccessRate || 0) < 0.7} />
            <Metric label="Sugerowane akcje (glidepath)" value={percent(summary.suggestedEquityShare)} detail="de-risking przy zbliżaniu do celu" />
            <Metric label="Cel FIRE nominalnie" value={hasSpendTarget ? money(summary.fireNumberNominalAtTarget) : "Ustaw cel"} detail={`ceny roku celu · infl. ${percent(summary.assumedInflation)}`} />
          </div>
          {summary.ppkRecommendation ? <p className="mutedText">PPK: {summary.ppkRecommendation}</p> : null}
        </Panel>
        <Panel title="Pomost 50-60/65">
          <div className="fireMiniMetrics">
            <Metric label="Luka 50-60" value={hasSpendTarget ? money(summary.liquidBridgeGapToAge60) : "Ustaw cel"} detail="płynny kapitał" warn={hasSpendTarget && Number(summary.liquidBridgeGapToAge60 || 0) > 0} />
            <Metric label="Luka 50-65" value={hasSpendTarget ? money(summary.liquidBridgeGapToAge65) : "Ustaw cel"} detail="konserwatywnie" warn={hasSpendTarget && Number(summary.liquidBridgeGapToAge65 || 0) > 0} />
            <Metric label="Kapitał pomostowy" value={money(summary.bridgeableLiquidCapital)} detail="po zostawieniu poduszki" />
            <Metric label="Rezerwa podatku" value={money(summary.withdrawalPlan?.estimatedTaxReserve)} detail="Belka od zysków opod." />
          </div>
          <p className="mutedText">{summary.withdrawalPlan?.sequence}</p>
          <details className="fireDisclosure">
            <summary>Kamienie milowe pomostu</summary>
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
          </details>
        </Panel>
      </section>

      <section className="gridTwo">
        <Panel title="Alokacja portfela">
          <FireAllocationChart data={summary.allocation || []} />
          <details className="fireDisclosure">
            <summary>Szczegóły rebalancingu</summary>
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
              ]}
            />
          </details>
        </Panel>
        <Panel title="Portfele MyFund">
          <ReportDataTable
            exportName="fire-portfele"
            rows={summary.portfolios || []}
            columns={[
              { key: "portfolio", header: "Portfel" },
              { key: "role", header: "Rola" },
              { key: "value", header: "Wartość", className: "num", render: (row) => money(row.value) },
              { key: "share", header: "Udział", className: "num", render: (row) => percent(row.share) },
              { key: "investmentValue", header: "Inwest.", className: "num", render: (row) => money(row.investmentValue) },
              { key: "retirementLockedValue", header: "Emeryt.", className: "num", render: (row) => money(row.retirementLockedValue) },
              { key: "note", header: "Wniosek" },
            ]}
          />
        </Panel>
      </section>

      <section className="gridTwo">
        <Panel title="Płynność i opakowania">
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
        <Panel title="Dane MyFund">
          <DataQualitySummary dataQuality={summary.dataQuality} />
          <details className="fireDisclosure">
            <summary>Źródła MyFund</summary>
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
          </details>
        </Panel>
      </section>

      <Panel title="Analiza walorów">
        <PositionAnalysisWorkspace portfolios={summary.portfolios || []} rows={summary.positionAnalyses || []} />
      </Panel>

      <section className="gridTwo">
        <Panel title="Dane i reguły">
          <details className="fireDisclosure">
            <summary>Polskie reguły modelu</summary>
            <ReportDataTable
              exportName="fire-reguly"
              rows={summary.legalRules || []}
              columns={[
                { key: "label", header: "Reguła" },
                { key: "value", header: "Wartość" },
                { key: "note", header: "Komentarz" },
              ]}
            />
          </details>
        </Panel>
        <Panel
          title="Ustawienia modelu"
          action={
            <button className="secondaryButton compact" disabled={!draft || saving} onClick={saveDraft} type="button">
              {saving ? "Zapisuję..." : "Zapisz"}
            </button>
          }
        >
          <FireSettingsForm draft={draft} onChange={setDraft} />
        </Panel>
      </section>
    </section>
  );
}

function PositionAnalysisWorkspace({ portfolios, rows }) {
  const [activePortfolio, setActivePortfolio] = useState("all");
  const [activeFocus, setActiveFocus] = useState("all");
  const model = useMemo(() => buildPositionAnalysisModel(rows, portfolios), [rows, portfolios]);
  const selectedRows = useMemo(
    () => rows.filter((row) => matchesPortfolio(row, activePortfolio) && matchesFocus(row, activeFocus)),
    [rows, activePortfolio, activeFocus],
  );
  const selectedModel = useMemo(() => buildPositionAnalysisModel(selectedRows, portfolios), [selectedRows, portfolios]);

  useEffect(() => {
    if (activePortfolio !== "all" && !model.groups.some((group) => group.name === activePortfolio)) {
      setActivePortfolio("all");
    }
  }, [activePortfolio, model.groups]);

  const activePortfolioLabel = activePortfolio === "all" ? "Wszystkie portfele" : activePortfolio;

  return (
    <div className="positionAnalysisWorkspace">
      <div className="dataQualityBanner neutral compactBanner">
        <strong>Przegląd pozycji z MyFund</strong>
        <span>Najpierw wybierz portfel i typ decyzji, potem schodź do waloru. To diagnostyka portfelowa, nie rekomendacja kupna ani sprzedaży.</span>
      </div>

      <div className="positionAnalysisKpis">
        <Metric label="Zakres" value={selectedModel.positionCount} detail={`${activePortfolioLabel} · ${focusLabel(activeFocus)}`} />
        <Metric label="Wartość" value={money(selectedModel.totalValue)} detail={`${selectedModel.portfolioCount || 0} portfele`} />
        <Metric label="Wysokie ryzyko" value={selectedModel.highRiskCount} detail="pierwsze do kontroli" warn={selectedModel.highRiskCount > 0} />
        <Metric label="Look-through" value={selectedModel.lookThroughCount} detail="produkty mieszane" warn={selectedModel.lookThroughCount > 0} />
      </div>

      <PortfolioSelector activePortfolio={activePortfolio} model={model} onChange={setActivePortfolio} />
      <ScopeSummary activePortfolio={activePortfolioLabel} activeFocus={activeFocus} model={selectedModel} />

      <div className="positionAnalysisGrid">
        <aside className="positionPriorityPanel">
          <h3>Filtry analizy</h3>
          <p>Filtry są lokalne dla analizy walorów. Nie zmieniają alokacji ani prognozy FIRE.</p>
          <FocusFilter activeFocus={activeFocus} model={model} onChange={setActiveFocus} />
          <h3 className="priorityHeading">Priorytety decyzji</h3>
          <p>Najpierw ryzyko i wartość. To lista tematów do ręcznej kontroli przed rebalancingiem.</p>
          <div className="priorityPositionList">
            {selectedModel.priorities.length ? selectedModel.priorities.map((row) => <PriorityPosition key={positionKey(row)} row={row} />) : <div className="emptyState compact">Brak pilnych pozycji w tym zakresie.</div>}
          </div>
        </aside>

        <div className="portfolioPositionGroups">
          {selectedModel.groups.length ? selectedModel.groups.map((group) => <PortfolioPositionGroup group={group} key={group.name} />) : <div className="emptyState compact">Brak pozycji dla wybranego filtra.</div>}
        </div>
      </div>

      <details className="fireDisclosure">
        <summary>Tabela techniczna aktualnego zakresu i eksport CSV</summary>
        <PositionAnalysisTable rows={selectedRows} />
      </details>
    </div>
  );
}

function PortfolioSelector({ activePortfolio, model, onChange }) {
  const options = [
    {
      name: "all",
      label: "Wszystkie",
      role: "pełny portfel",
      value: model.totalValue,
      positions: model.positionCount,
      riskLevel: model.riskLevel,
    },
    ...model.groups.map((group) => ({
      name: group.name,
      label: group.name,
      role: group.role,
      value: group.value,
      positions: group.positions.length,
      riskLevel: group.riskLevel,
    })),
  ];
  return (
    <div className="portfolioScopeRail" aria-label="Wybór portfela MyFund">
      {options.map((option) => (
        <button
          aria-pressed={activePortfolio === option.name}
          className={`portfolioScopeTile ${activePortfolio === option.name ? "active" : ""} ${option.riskLevel || "info"}`}
          key={option.name}
          onClick={() => onChange(option.name)}
          type="button"
        >
          <span>{option.label}</span>
          <strong>{money(option.value)}</strong>
          <small>{option.role} · {option.positions} walorów</small>
        </button>
      ))}
    </div>
  );
}

function ScopeSummary({ activeFocus, activePortfolio, model }) {
  return (
    <div className="positionScopeSummary">
      <div>
        <span>Wybrany zakres</span>
        <strong>{activePortfolio}</strong>
        <small>{focusLabel(activeFocus)} · {model.positionCount} walorów · {money(model.totalValue)}</small>
      </div>
      <AssetMixBar items={model.assetMix} />
    </div>
  );
}

function AssetMixBar({ items }) {
  if (!items.length) {
    return <div className="emptyState compact">Brak danych alokacji dla zakresu.</div>;
  }
  return (
    <div className="assetMixSummary">
      <div className="assetMixBar" aria-label="Struktura klas aktywów">
        {items.map((item) => (
          <span
            className={`assetSegment ${assetClassToken(item.assetClass)}`}
            key={item.assetClass}
            style={{ width: `${Math.max(item.share * 100, 2)}%` }}
            title={`${item.assetClass}: ${percent(item.share)}`}
          />
        ))}
      </div>
      <div className="assetMixLegend">
        {items.map((item) => (
          <span key={item.assetClass}>
            <i className={assetClassToken(item.assetClass)} /> {item.assetClass} <strong>{percent(item.share)}</strong>
          </span>
        ))}
      </div>
    </div>
  );
}

function FocusFilter({ activeFocus, model, onChange }) {
  const options = [
    { id: "all", label: "Wszystkie", count: model.positionCount },
    { id: "attention", label: "Do decyzji", count: model.attentionCount },
    { id: "high", label: "Wysokie", count: model.highRiskCount },
    { id: "lookThrough", label: "Look-through", count: model.lookThroughCount },
    { id: "taxable", label: "Opodatkowane", count: model.taxableCount },
  ];
  return (
    <div className="focusFilterStack">
      {options.map((option) => (
        <button className={activeFocus === option.id ? "active" : ""} key={option.id} onClick={() => onChange(option.id)} type="button">
          <span>{option.label}</span>
          <strong>{option.count}</strong>
        </button>
      ))}
    </div>
  );
}

function PriorityPosition({ row }) {
  return (
    <article className={`priorityPosition ${row.riskLevel || "info"}`}>
      <div>
        <RiskLevel level={row.riskLevel} />
        <strong>{row.instrument}</strong>
        <small>{[row.portfolio, row.assetClass, money(row.value)].filter(Boolean).join(" · ")}</small>
      </div>
      <p>{row.decision || row.action || row.reviewFocus}</p>
    </article>
  );
}

function PortfolioPositionGroup({ group }) {
  return (
    <details className="portfolioPositionGroup" open>
      <summary>
        <div>
          <strong>{group.name}</strong>
          <span>{group.role} · {group.positions.length} walorów · {money(group.value)}</span>
        </div>
        <RiskLevel level={group.riskLevel} />
      </summary>
      {group.note && <p className="portfolioGroupNote">{group.note}</p>}
      <div className="portfolioAssetMix">
        {group.assetMix.map((item) => (
          <span key={item.assetClass}>
            {item.assetClass} <strong>{percent(item.share)}</strong>
          </span>
        ))}
      </div>
      <div className="positionCardGrid">
        {group.positions.map((row) => <PositionAnalysisCard key={positionKey(row)} row={row} />)}
      </div>
    </details>
  );
}

function PositionAnalysisCard({ row }) {
  return (
    <article className={`positionAnalysisCard ${row.riskLevel || "info"}`}>
      <header>
        <div>
          <strong>{row.instrument}</strong>
          <small>{[row.isin || row.account || "brak ISIN", row.currency].filter(Boolean).join(" · ")}</small>
        </div>
        <RiskLevel level={row.riskLevel} />
      </header>
      <div className="positionCardMetrics">
        <div className="positionMetric">
          <span>Wartość</span>
          <strong>{money(row.value)}</strong>
          <small>{percent(row.shareOfInvestments)} portfela inwest.</small>
        </div>
        <div className={`positionMetric ${Number(row.returnPct || 0) < 0 ? "warn" : ""}`}>
          <span>Zwrot</span>
          <strong>{percent(row.returnPct)}</strong>
          <small>{row.priceDate || "brak daty"}</small>
        </div>
      </div>
      <div className="fireRoleChips">
        <span>{row.assetClass || "Klasa do ustalenia"}</span>
        <span>{row.instrumentType || "Typ do ustalenia"}</span>
        <span>{row.fireRole || "Rola do ustalenia"}</span>
        <span>{row.wrapper || "Segment do ustalenia"}</span>
      </div>
      <div className="positionDecisionBlock">
        <span>{row.reviewFocus || "Decyzja"}</span>
        <strong>{row.decision || row.action || "Sprawdź ręcznie"}</strong>
        {row.decisionReason && <p>{row.decisionReason}</p>}
      </div>
      {!!(row.riskDrivers || []).length && (
        <div className="riskDriverChips">
          {(row.riskDrivers || []).map((item) => <span key={item}>{item}</span>)}
        </div>
      )}
      <details className="inlineDisclosure">
        <summary>Perspektywa i checklista</summary>
        <p>{row.perspective}</p>
        <ul className="checklistList">
          {(row.checklist || []).map((item) => <li key={item}>{item}</li>)}
        </ul>
      </details>
    </article>
  );
}

function PositionAnalysisTable({ rows }) {
  return (
    <ReportDataTable
      className="smallRows firePositionTable"
      emptyMessage="Brak pozycji do analizy. Dodaj raporty MyFund portfelSklad."
      exportName="fire-analiza-walorow"
      rows={rows}
      columns={[
        {
          key: "instrument",
          header: "Walor",
          render: (row) => (
            <div className="firePositionName">
              <strong>{row.instrument}</strong>
              <small>{[row.portfolio, row.isin || row.account || "brak ISIN"].filter(Boolean).join(" · ")}</small>
            </div>
          ),
          csvValue: (row) => row.instrument,
        },
        {
          key: "assetClass",
          header: "Klasyfikacja FIRE",
          render: (row) => (
            <div className="analysisText">
              <strong>{row.assetClass}</strong>
              <small>{row.instrumentType || "Typ do ustalenia"}</small>
              <div className="fireRoleChips">
                <span>{row.fireRole || "Rola do ustalenia"}</span>
                <span>{row.wrapper}</span>
              </div>
            </div>
          ),
          csvValue: (row) => [row.assetClass, row.instrumentType, row.fireRole, row.wrapper].filter(Boolean).join(" | "),
        },
        {
          key: "value",
          header: "Metryki",
          render: (row) => (
            <div className="analysisText compact">
              <strong>{money(row.value)}</strong>
              <small>{percent(row.shareOfInvestments)} portfela inwest. · zwrot {percent(row.returnPct)}</small>
            </div>
          ),
          csvValue: (row) => row.value,
        },
        {
          key: "action",
          header: "Decyzja, ryzyka i checklista",
          render: (row) => (
            <details className="inlineDisclosure">
              <summary><RiskLevel level={row.riskLevel} /> {row.reviewFocus}: {row.decision || row.action}</summary>
              {row.decisionReason && <p className="decisionReason">{row.decisionReason}</p>}
              {!!(row.riskDrivers || []).length && (
                <div className="riskDriverChips">
                  {(row.riskDrivers || []).map((item) => <span key={item}>{item}</span>)}
                </div>
              )}
              <p>{row.perspective}</p>
              <ul className="checklistList">
                {(row.checklist || []).map((item) => <li key={item}>{item}</li>)}
              </ul>
            </details>
          ),
          csvValue: (row) => [row.reviewFocus, row.decision || row.action, row.decisionReason, row.perspective, ...(row.riskDrivers || []), ...(row.checklist || [])].filter(Boolean).join(" | "),
        },
      ]}
    />
  );
}

function buildPositionAnalysisModel(rows = [], portfolios = []) {
  const portfolioByName = new Map(portfolios.map((portfolio) => [portfolio.portfolio, portfolio]));
  const sortedRows = [...rows].sort(comparePositions);
  const groupsByName = new Map();
  let totalValue = 0;
  sortedRows.forEach((row) => {
    const name = row.portfolio || "Nieprzypisane";
    totalValue += Number(row.value || 0);
    const group = groupsByName.get(name) || {
      name,
      positions: [],
      value: 0,
      role: portfolioByName.get(name)?.role || "Nieprzypisany",
      note: portfolioByName.get(name)?.note || "",
      riskLevel: "info",
    };
    group.positions.push(row);
    group.value += Number(row.value || 0);
    group.riskLevel = maxRisk(group.riskLevel, row.riskLevel);
    groupsByName.set(name, group);
  });
  const groups = Array.from(groupsByName.values())
    .map((group) => ({ ...group, assetMix: buildAssetMix(group.positions, group.value) }))
    .sort((a, b) => b.value - a.value);
  const highRiskCount = rows.filter((row) => row.riskLevel === "high").length;
  const lookThroughCount = rows.filter((row) => isLookThrough(row)).length;
  const taxableCount = rows.filter((row) => isTaxable(row)).length;
  const priorityRows = sortedRows.filter(isPriorityPosition);
  return {
    groups,
    totalValue,
    assetMix: buildAssetMix(rows, totalValue),
    riskLevel: groups.reduce((level, group) => maxRisk(level, group.riskLevel), "info"),
    portfolioCount: groups.length,
    positionCount: rows.length,
    highRiskCount,
    lookThroughCount,
    taxableCount,
    attentionCount: priorityRows.length,
    priorities: priorityRows.slice(0, 6),
  };
}

function buildAssetMix(rows, totalValue) {
  const byClass = new Map();
  rows.forEach((row) => {
    const assetClass = row.assetClass || "Inne";
    byClass.set(assetClass, (byClass.get(assetClass) || 0) + Number(row.value || 0));
  });
  return Array.from(byClass.entries())
    .map(([assetClass, value]) => ({ assetClass, value, share: totalValue ? value / totalValue : 0 }))
    .sort((a, b) => b.value - a.value);
}

function comparePositions(a, b) {
  const riskDiff = riskRank(b.riskLevel) - riskRank(a.riskLevel);
  if (riskDiff) return riskDiff;
  return Number(b.value || 0) - Number(a.value || 0);
}

function riskRank(level) {
  return { high: 3, medium: 2, low: 1, info: 0 }[level] || 0;
}

function maxRisk(a, b) {
  return riskRank(a) >= riskRank(b) ? a : b || a;
}

function isLookThrough(row) {
  const text = [row.assetClass, row.instrumentType, row.fireRole, row.decision, row.action, row.decisionReason].filter(Boolean).join(" ").toLowerCase();
  return text.includes("look-through") || text.includes("mieszane") || text.includes("mieszany");
}

function isTaxable(row) {
  return String(row.wrapper || "").toLowerCase().includes("opodatk");
}

function isPriorityPosition(row) {
  return row.riskLevel === "high" || isLookThrough(row) || String(row.decision || row.action || "").toLowerCase().includes("nie ");
}

function matchesPortfolio(row, activePortfolio) {
  return activePortfolio === "all" || (row.portfolio || "Nieprzypisane") === activePortfolio;
}

function matchesFocus(row, activeFocus) {
  if (activeFocus === "attention") return isPriorityPosition(row);
  if (activeFocus === "high") return row.riskLevel === "high";
  if (activeFocus === "lookThrough") return isLookThrough(row);
  if (activeFocus === "taxable") return isTaxable(row);
  return true;
}

function focusLabel(activeFocus) {
  return {
    all: "wszystkie walory",
    attention: "do decyzji",
    high: "wysokie ryzyko",
    lookThrough: "look-through",
    taxable: "opodatkowane",
  }[activeFocus] || activeFocus;
}

function assetClassToken(assetClass = "") {
  const normalized = assetClass.toLowerCase();
  if (normalized.includes("akcj")) return "equity";
  if (normalized.includes("oblig")) return "bond";
  if (normalized.includes("got")) return "cash";
  if (normalized.includes("altern")) return "alternative";
  if (normalized.includes("miesz")) return "mixed";
  return "other";
}

function positionKey(row) {
  return [row.portfolio, row.instrument, row.isin, row.account].filter(Boolean).join("|");
}

function SpendTargetEditor({ draft, fireNumber, hasSpendTarget, monthlySpendTarget, onChange, onSave, saving }) {
  const rawValue = draft?.monthlySpendOverride ?? "";
  const numericValue = Number(rawValue || 0);
  const canSave = !!draft && numericValue > 0 && !saving;
  return (
    <article className={`fireTargetEditor ${!hasSpendTarget ? "warn" : ""}`}>
      <span>Cel wydatków FIRE / mies.</span>
      <strong>{hasSpendTarget ? money(monthlySpendTarget) : "Ustaw cel"}</strong>
      <small>{hasSpendTarget ? `FIRE number: ${money(fireNumber)}` : "Nie liczę celu z obecnego budżetu."}</small>
      <div className="inlineEdit">
        <label>
          <span>Docelowe wydatki miesięczne</span>
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
    </article>
  );
}

function DecisionSummary({ actionItems, hasSpendTarget, risks }) {
  const primaryRisk = risks.find((risk) => risk.level === "high") || risks[0];
  const primaryAction = actionItems[0];
  const title = primaryRisk?.title || primaryAction?.title || "Utrzymaj automatyzację";
  const metric = primaryRisk ? `${primaryRisk.metric}: ${primaryRisk.value}` : primaryAction?.amount ? money(primaryAction.amount) : hasSpendTarget ? "plan aktywny" : "brak celu";
  const detail = primaryRisk?.recommendation || primaryAction?.detail || "Najważniejsze są regularne wpłaty, kontrola kosztów życia i rebalancing nowymi środkami.";
  return (
    <article className={`fireDecisionCard ${primaryRisk?.level || "info"}`}>
      <span>Najważniejsza decyzja</span>
      <strong>{title}</strong>
      <small>{metric}</small>
      <p>{detail}</p>
    </article>
  );
}

function ContributionSummary({ budget, contributionPlan, currentMonthlyWealthContribution }) {
  return (
    <article className="fireContributionCard">
      <div>
        <span>Budżet zasila FIRE</span>
        <strong>{money(budget.firePortfolioMonthlyContribution || currentMonthlyWealthContribution)}</strong>
        <small>inwestycje + konto oszczędnościowe netto</small>
      </div>
      <div className="fireContributionMetrics">
        <Metric label="Wymagane" value={money(contributionPlan?.requiredMonthlyBase)} detail="scenariusz bazowy" />
        <Metric label="Brakuje / mies." value={money(contributionPlan?.additionalMonthlyNeeded)} detail="do celu 50" warn={Number(contributionPlan?.additionalMonthlyNeeded || 0) > 0} />
        <Metric label="Nadpłata kredytu" value={money(budget.loanOverpaymentMonthly)} detail="osobno od portfela FIRE" />
        <Metric label="Konto oszcz. netto" value={money(budget.savingsAccountMonthlyNet)} detail={`brutto ${money(budget.savingsAccountMonthlyGrossDeposits)}`} />
      </div>
      <p>{contributionPlan?.recommendation}</p>
    </article>
  );
}

function RiskCounter({ risks }) {
  const high = risks.filter((risk) => risk.level === "high").length;
  const medium = risks.filter((risk) => risk.level === "medium").length;
  return <span className={`riskCounter ${high ? "high" : medium ? "medium" : "low"}`}>{high} wysokie · {medium} średnie</span>;
}

function RiskCards({ risks }) {
  const visibleRisks = risks.filter((risk) => ["high", "medium"].includes(risk.level)).slice(0, 4);
  const cards = visibleRisks.length ? visibleRisks : risks.slice(0, 4);
  if (!cards.length) {
    return <div className="emptyState compact">Brak ryzyk do pokazania po aktualnych danych.</div>;
  }
  return (
    <div className="riskCards">
      {cards.map((risk) => (
        <article className={`riskCard ${risk.level}`} key={risk.id}>
          <RiskLevel level={risk.level} />
          <strong>{risk.title}</strong>
          <span>{risk.metric}: {risk.value}</span>
          <p>{risk.detail}</p>
        </article>
      ))}
    </div>
  );
}

function RiskLevel({ level }) {
  const label = {
    high: "Wysokie",
    medium: "Średnie",
    low: "Niskie",
    info: "Info",
  }[level] || level || "Info";
  return <span className={`riskBadge ${level || "info"}`}>{label}</span>;
}

function ScenarioStrip({ scenarios }) {
  if (!scenarios.length) {
    return <p className="mutedText">Ustaw cel wydatków FIRE, żeby pokazać scenariusze dojścia do wieku 50.</p>;
  }
  return (
    <div className="scenarioStrip">
      {scenarios.map((scenario) => (
        <div key={scenario.id}>
          <span>{scenario.label}</span>
          <strong>{money(scenario.projectedAtFire)}</strong>
          <small>{scenario.onTrack ? "domyka cel" : `luka ${money(scenario.gapAtFire)}`}</small>
        </div>
      ))}
    </div>
  );
}

function DataQualitySummary({ dataQuality }) {
  return (
    <div className="fireMiniMetrics">
      <Metric label="Status danych" value={dataQuality?.status || "brak"} detail={dataQuality?.newestReportDate || "bez daty"} />
      <Metric label="Stare źródła" value={dataQuality?.staleSourceCount || 0} detail=">45 dni" warn={Number(dataQuality?.staleSourceCount || 0) > 0} />
      <Metric label="Nieznane aktywa" value={dataQuality?.unknownAssetClassCount || 0} detail={money(dataQuality?.unknownAssetClassValue)} warn={Number(dataQuality?.unknownAssetClassCount || 0) > 0} />
      <Metric label="Nieznane segmenty" value={dataQuality?.unknownWrapperCount || 0} detail={money(dataQuality?.unknownWrapperValue)} warn={Number(dataQuality?.unknownWrapperCount || 0) > 0} />
      <p className="mutedText fullWidth">{dataQuality?.note}</p>
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
