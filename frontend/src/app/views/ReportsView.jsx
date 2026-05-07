import { useState } from "react";
import { BudgetMixDonutChart } from "../components/charts/BudgetMixDonutChart.jsx";
import { CashflowSankeyChart } from "../components/charts/CashflowSankeyChart.jsx";
import { CategoryParetoChart } from "../components/charts/CategoryParetoChart.jsx";
import { CategoryShareDonutChart } from "../components/charts/CategoryShareDonutChart.jsx";
import { CategoryTrendChart } from "../components/charts/CategoryTrendChart.jsx";
import { FixednessBreakdownChart } from "../components/charts/FixednessBreakdownChart.jsx";
import { HierarchySunburstChart } from "../components/charts/HierarchySunburstChart.jsx";
import { MerchantFunnelChart } from "../components/charts/MerchantFunnelChart.jsx";
import { MerchantShareDonutChart } from "../components/charts/MerchantShareDonutChart.jsx";
import { MerchantTrendChart } from "../components/charts/MerchantTrendChart.jsx";
import { MonthlyCashflowComboChart } from "../components/charts/MonthlyCashflowComboChart.jsx";
import { OutlierTimelineChart } from "../components/charts/OutlierTimelineChart.jsx";
import { FinancialFlowsPanel } from "../components/finance/FinancialFlowsPanel.jsx";
import { ReportDataTable } from "../components/tables/ReportDataTable.jsx";
import { Panel } from "../components/ui/Panel.jsx";
import { CHART_COLORS } from "../domain/charts.js";
import { money, percent } from "../domain/formatters.js";

export function ReportsView({ reportsSections, onDrill, onInspect }) {
  const [section, setSection] = useState("cashflow");
  const sections = reportsSections || {};
  const scopedStats = {
    areaTop: [],
    categoryTop: [],
    subcategoryTop: [],
    merchants: [],
    oneoffs: [],
    ...(sections.scopedStats || {}),
  };

  function inspectDrill(filter) {
    onDrill(filter);
    onInspect?.({ title: `Transakcje: ${filter.label}`, filters: { [filter.type]: filter.value }, useTimeScope: true });
  }

  function inspectChartRow(entry, fallback = {}) {
    return entry?.payload || entry || fallback;
  }

  return (
    <section className="viewStack">
      <Panel title="Kontrolowalne wydatki" action={<ScopeBadge type="context" value="Aktywny miesiąc planu" />}>
        <div className="reportFocus">
          <div>
            <span>Do cięcia</span>
            <strong>{money(sections.controllable?.cutNowPotential || 0)}</strong>
            <p>wydatki uznaniowe z potencjałem limitów</p>
          </div>
          <div>
            <span>Do rozbicia</span>
            <strong>{money(sections.controllable?.reviewPotential || 0)}</strong>
            <p>kategorie mieszane, zdrowie/uroda i marketplace</p>
          </div>
          {(sections.controllable?.topActions || []).slice(0, 5).map((row) => (
            <button
              type="button"
              className={`reportFocusAction ${row.tone}`}
              key={row.category}
              onClick={() => onInspect?.({ title: `Transakcje: ${row.category}`, filters: row.filter || { category: row.category }, useTimeScope: true })}
            >
              <span>{row.decision}</span>
              <strong>{row.category}</strong>
              <p>{money(row.potentialMonthly)} potencjału · {row.note}</p>
            </button>
          ))}
        </div>
      </Panel>

      <Panel title="Raporty">
        <div className="reportTabs segmented compact" aria-label="Sekcja raportów">
          {[
            ["cashflow", "Cashflow"],
            ["categories", "Kategorie"],
            ["merchants", "Sprzedawcy"],
            ["oneoffs", "Jednorazowe"],
          ].map(([id, label]) => (
            <button type="button" key={id} className={section === id ? "active" : ""} onClick={() => setSection(id)}>
              {label}
            </button>
          ))}
        </div>
        <div className="reportScopeLegend">
          <ScopeBadge type="scope" value={sections.activeTimeLabel || "Zakres"} />
          <ScopeBadge type="year" value={sections.yearLabel || "Cały rok"} />
        </div>
      </Panel>

      {section === "cashflow" && (
        <>
          <section className="gridTwo">
            <Panel title="Sankey przepływu pieniędzy" className="fullSpan" action={<ScopeBadge type="scope" value={sections.activeTimeLabel} />}>
              <CashflowSankeyChart
                data={sections.cashflowSankey}
                onSelect={(entry) => {
                  const row = inspectChartRow(entry);
                  if (row.filter) {
                    onInspect?.({ title: `Transakcje: ${row.target || row.name}`, filters: row.filter, useTimeScope: true });
                  }
                }}
              />
            </Panel>
            <Panel title="Cashflow miesięczny" action={<ScopeBadge type="year" value={sections.yearLabel} />}>
              <MonthlyCashflowComboChart
                data={sections.cashflowSeries || sections.monthly || []}
                onSelect={(row) => onInspect?.({ title: `Transakcje: ${row.month}`, filters: { month: row.monthKey }, useTimeScope: false })}
              />
            </Panel>
            <Panel title="Udział koszyków budżetu" action={<ScopeBadge type="year" value={sections.yearLabel} />}>
              <BudgetMixDonutChart
                data={sections.budgetMixChart || []}
                onSelect={(entry) => {
                  const row = inspectChartRow(entry);
                  if (row.filter) {
                    onInspect?.({ title: `Transakcje: ${row.name}`, filters: row.filter, useTimeScope: true });
                  }
                }}
              />
              <div className="mixList mixListFull compactMix">
                {(sections.budgetMix || []).map((row, index) => (
                  <div className="mixRow" key={row.bucket}>
                    <i style={{ background: CHART_COLORS[index % CHART_COLORS.length] }} />
                    <span>{row.bucket}</span>
                    <strong>{money(row.sum)}</strong>
                    <em>{percent(row.incomeShare)}</em>
                  </div>
                ))}
              </div>
            </Panel>
          </section>
          <Panel title="Benchmark i presja na budżet" action={<ScopeBadge type="year" value={sections.yearLabel} />}>
            <div className="benchmark">
              {(sections.benchmarkCards || []).map((card) => (
                <div key={card.label}>
                  <span>{card.label}</span>
                  <strong>{money(card.value)}</strong>
                  <p>{formatBenchmarkDetail(card.detail)}</p>
                </div>
              ))}
            </div>
          </Panel>
          <Panel title="Zakres raportu" action={<ScopeBadge type="scope" value={sections.activeTimeLabel} />}>
            <div className="monthCards">
              {(sections.scopeCards || []).map((card) => (
                <MetricCard
                  key={card.label}
                  label={card.label}
                  value={money(card.value)}
                  detail={formatScopeDetail(card.detail)}
                  onInspect={card.filter ? () => onInspect?.({ title: `Transakcje: ${card.label.toLowerCase()}`, filters: card.filter, useTimeScope: true }) : undefined}
                />
              ))}
            </div>
          </Panel>
        </>
      )}

      {section === "categories" && (
        <>
          <section className="gridTwo">
            <Panel title="Udział kategorii w wydatkach" action={<ScopeBadge type="scope" value={sections.activeTimeLabel} />}>
              <CategoryShareDonutChart
                data={sections.categoryShare || []}
                onSelect={(entry) => {
                  const row = inspectChartRow(entry);
                  if (row.filter) {
                    onInspect?.({ title: `Transakcje: ${row.category}`, filters: row.filter, useTimeScope: true });
                  }
                }}
              />
            </Panel>
            <Panel title="Pareto kategorii (Top 12)" action={<ScopeBadge type="scope" value={sections.activeTimeLabel} />}>
              <CategoryParetoChart
                data={sections.categoryPareto || []}
                onSelect={(entry) => {
                  const row = inspectChartRow(entry);
                  inspectDrill({ type: "category", value: row.category, label: `Kategoria: ${row.category}` });
                }}
              />
            </Panel>
          </section>
          <section className="gridTwo">
            <Panel title="Trend kategorii miesiąc po miesiącu" action={<ScopeBadge type="year" value={sections.yearLabel} />}>
              <CategoryTrendChart
                data={sections.categoryTrends || []}
                onSelect={(entry) => {
                  const row = inspectChartRow(entry);
                  if (row.category) {
                    onInspect?.({ title: `Transakcje: ${row.category}`, filters: { category: row.category, month: row.monthKey }, useTimeScope: false });
                  }
                }}
              />
            </Panel>
            <Panel title="Hierarchia: obszar → grupa → kategoria" action={<ScopeBadge type="scope" value={sections.activeTimeLabel} />}>
              <HierarchySunburstChart
                data={sections.hierarchySunburst || []}
                onSelect={(entry) => {
                  const row = inspectChartRow(entry);
                  if (row.filter) {
                    onInspect?.({ title: `Transakcje: ${row.name}`, filters: row.filter, useTimeScope: true });
                  }
                }}
              />
            </Panel>
          </section>
          <Panel title="Stałe i zmienne koszty" action={<ScopeBadge type="scope" value={sections.activeTimeLabel} />}>
            <FixednessBreakdownChart
              data={sections.fixednessChart || []}
              onSelect={(entry) => {
                const row = inspectChartRow(entry);
                onInspect?.({ title: `Transakcje: ${row.fixedness}`, filters: row.filter || { fixedness: row.fixedness }, useTimeScope: true });
              }}
            />
          </Panel>
          <Panel title="Podkategorie (Top 14)" action={<ScopeBadge type="scope" value={sections.activeTimeLabel} />}>
            <ReportDataTable
              exportName="podkategorie"
              rows={scopedStats.subcategoryTop.slice(0, 14)}
              onRowClick={(row) => inspectDrill({ type: "subcategory", value: row.subcategory, label: `Podkategoria: ${row.subcategory}` })}
              columns={[
                { key: "subcategory", header: "Podkategoria" },
                { key: "spend", header: "Kwota", className: "num", render: (row) => money(row.spend) },
                {
                  key: "share",
                  header: "Udział",
                  className: "num",
                  render: (row) => percent(row.spend / (Number(scopedStats.spend) || 1)),
                  csvValue: (row) => row.spend / (Number(scopedStats.spend) || 1),
                },
              ]}
            />
          </Panel>
        </>
      )}

      {section === "merchants" && (
        <>
          <section className="gridTwo">
            <Panel title="Udział sprzedawców w wydatkach" action={<ScopeBadge type="scope" value={sections.activeTimeLabel} />}>
              <MerchantShareDonutChart
                data={sections.merchantShare || []}
                onSelect={(entry) => {
                  const row = inspectChartRow(entry);
                  if (row.filter) {
                    onInspect?.({ title: `Transakcje: ${row.merchant}`, filters: row.filter, useTimeScope: true });
                  }
                }}
              />
            </Panel>
            <Panel title="Trend sprzedawców miesiąc po miesiącu" action={<ScopeBadge type="year" value={sections.yearLabel} />}>
              <MerchantTrendChart
                data={sections.merchantTrends || []}
                onSelect={(entry) => {
                  const row = inspectChartRow(entry);
                  if (row.merchant) {
                    onInspect?.({ title: `Transakcje: ${row.merchant}`, filters: { query: row.merchant, month: row.monthKey }, useTimeScope: false });
                  }
                }}
              />
            </Panel>
          </section>
          <Panel title="Koncentracja sprzedawców" action={<ScopeBadge type="scope" value={sections.activeTimeLabel} />}>
            <MerchantFunnelChart
              data={sections.merchantFunnel || []}
              onSelect={(entry) => {
                const row = inspectChartRow(entry);
                onInspect?.({ title: `Transakcje: ${row.merchant}`, filters: row.filter || { query: row.merchant }, useTimeScope: true });
              }}
            />
          </Panel>
          <Panel title="Sprzedawcy (Top 14)" action={<ScopeBadge type="scope" value={sections.activeTimeLabel} />}>
            <ReportDataTable
              exportName="sprzedawcy"
              rows={scopedStats.merchants.slice(0, 14)}
              onRowClick={onInspect ? (row) => onInspect({ title: `Transakcje: ${row.merchant}`, filters: { query: row.merchant }, useTimeScope: true }) : undefined}
              columns={[
                { key: "merchant", header: "Sprzedawca" },
                { key: "sum", header: "Wydatki", className: "num", render: (row) => money(row.sum) },
                { key: "count", header: "Liczba", className: "num" },
              ]}
            />
          </Panel>
        </>
      )}

      {section === "oneoffs" && (
        <section className="gridTwo">
          <FinancialFlowsPanel
            action={<ScopeBadge type="scope" value={sections.activeTimeLabel} />}
            flows={sections.financialFlows || []}
            total={sections.financialFlowTotal}
            count={sections.financialFlowCount}
            onInspect={(config) => onInspect?.({ ...config, useTimeScope: true })}
          />
          <Panel title="Duże jednorazowe (Top 10)" action={<ScopeBadge type="scope" value={sections.activeTimeLabel} />}>
            <OutlierTimelineChart
              data={sections.outlierTimeline || []}
              onSelect={(entry) => {
                const row = inspectChartRow(entry);
                onInspect?.({ title: `Transakcje: ${row.merchant}`, filters: row.filter || { query: row.merchant }, useTimeScope: true });
              }}
            />
            <ReportDataTable
              exportName="duze-jednorazowe"
              rows={scopedStats.oneoffs.slice(0, 10)}
              onRowClick={onInspect ? (tx) => onInspect({ title: `Transakcje: ${tx.merchant}`, filters: { query: tx.merchant }, useTimeScope: true }) : undefined}
              columns={[
                { key: "postedDate", header: "Data", render: (tx) => tx.postedDate || tx.date, csvValue: (tx) => tx.postedDate || tx.date },
                { key: "merchant", header: "Sprzedawca" },
                { key: "correctedCategory", header: "Kategoria", render: (tx) => tx.correctedCategory || tx.category, csvValue: (tx) => tx.correctedCategory || tx.category },
                { key: "spend", header: "Kwota", className: "num", render: (tx) => money(tx.spend || tx.amount), csvValue: (tx) => tx.spend || tx.amount },
              ]}
            />
          </Panel>
        </section>
      )}
    </section>
  );
}

function ScopeBadge({ type = "scope", value }) {
  const label = type === "year" ? "Rok" : type === "context" ? "Kontekst" : "Zakres";
  return (
    <span className={`scopeBadge ${type === "year" ? "year" : type === "context" ? "context" : "scope"}`}>
      {label}: {value || (type === "year" ? "Cały rok" : "bieżący")}
    </span>
  );
}

function MetricCard({ label, value, detail, onInspect }) {
  const content = (
    <>
      <span>{label}</span>
      <strong>{value}</strong>
      <p>{detail}</p>
    </>
  );
  if (!onInspect) {
    return <div className="metricCard">{content}</div>;
  }
  return (
    <button type="button" className="metricCard inspectable" onClick={onInspect} title="Pokaż transakcje">
      {content}
    </button>
  );
}

function formatScopeDetail(detail) {
  if (typeof detail === "number") return percent(detail);
  return detail || "brak wpływów";
}

function formatBenchmarkDetail(detail) {
  if (typeof detail !== "string") return detail || "";
  for (const prefix of ["limit 50%: ", "punkt 30%: ", "minimum 20%: "]) {
    if (detail.startsWith(prefix)) {
      return `${prefix}${money(Number(detail.slice(prefix.length)))}`;
    }
  }
  return detail;
}
