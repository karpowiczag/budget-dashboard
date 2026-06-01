import { useState } from "react";
import { BudgetMixDonutChart } from "../components/charts/BudgetMixDonutChart.jsx";
import { CashflowSankeyChart } from "../components/charts/CashflowSankeyChart.jsx";
import { CategoryParetoChart } from "../components/charts/CategoryParetoChart.jsx";
import { CategoryShareDonutChart } from "../components/charts/CategoryShareDonutChart.jsx";
import { CategoryTrendChart } from "../components/charts/CategoryTrendChart.jsx";
import { DataQualityChart } from "../components/charts/DataQualityChart.jsx";
import { FixednessBreakdownChart } from "../components/charts/FixednessBreakdownChart.jsx";
import { HierarchySunburstChart } from "../components/charts/HierarchySunburstChart.jsx";
import { MerchantFunnelChart } from "../components/charts/MerchantFunnelChart.jsx";
import { MerchantShareDonutChart } from "../components/charts/MerchantShareDonutChart.jsx";
import { MerchantTrendChart } from "../components/charts/MerchantTrendChart.jsx";
import { MonthlyCashflowComboChart } from "../components/charts/MonthlyCashflowComboChart.jsx";
import { OutlierTimelineChart } from "../components/charts/OutlierTimelineChart.jsx";
import { ReportWorkspace } from "../components/layout/ReportWorkspace.jsx";
import { CategoryCostMatrixTable } from "../components/tables/CategoryCostMatrixTable.jsx";
import { ReportDataTable } from "../components/tables/ReportDataTable.jsx";
import { Panel } from "../components/ui/Panel.jsx";
import { CHART_COLORS } from "../domain/charts.js";
import { money, percent } from "../domain/formatters.js";

export function ReportsView({ dataQualityChart, reportsSections, reportsWorkspace, onInspect }) {
  const [report, setReport] = useState("cashflow");
  const [mode, setMode] = useState("breakdown");
  const [advancedOpen, setAdvancedOpen] = useState(false);
  const sections = reportsSections || {};
  const workspace = reportsWorkspace || { reports: [] };
  const scopedStats = {
    areaTop: [],
    categoryTop: [],
    subcategoryTop: [],
    merchants: [],
    oneoffs: [],
    ...(sections.scopedStats || {}),
  };

  return (
    <section className="viewStack">
      <Panel title="Raporty">
        <ReportWorkspace
          activeMode={mode}
          activeReport={report}
          reports={workspace.reports}
          onModeChange={setMode}
          onReportChange={(nextReport) => {
            setReport(nextReport);
            setMode("breakdown");
            setAdvancedOpen(false);
          }}
        >
          <div className="reportScopeLegend">
            <ScopeBadge type="scope" value={sections.activeTimeLabel} />
            <ScopeBadge type="year" value={sections.yearLabel} />
          </div>
        </ReportWorkspace>
      </Panel>

      {report === "cashflow" && mode === "breakdown" && (
        <section className="gridTwo">
          <Panel title="Zakres raportu" className="fullSpan reportContextPanel" action={<ScopeBadge type="scope" value={sections.activeTimeLabel} />}>
            <div className="reportQuickCards">
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
          <Panel title="Sankey przepływu pieniędzy" className="fullSpan compactChartPanel" action={<ScopeBadge type="scope" value={sections.activeTimeLabel} />}>
            <CashflowSankeyChart
              data={sections.cashflowSankey}
              onSelect={(entry) => inspectEntry(entry, onInspect)}
            />
          </Panel>
        </section>
      )}

      {report === "cashflow" && mode === "trends" && (
        <section className="gridTwo">
          <Panel title="Cashflow miesięczny" action={<ScopeBadge type="year" value={sections.yearLabel} />}>
            <MonthlyCashflowComboChart
              data={sections.cashflowSeries || sections.monthly || []}
              onSelect={(row) => onInspect?.({ title: `Transakcje: ${row.month}`, filters: { month: row.monthKey }, useTimeScope: false })}
            />
          </Panel>
          <Panel title="Benchmark budżetu" action={<ScopeBadge type="year" value={sections.yearLabel} />}>
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
        </section>
      )}

      {report === "spending" && mode === "breakdown" && (
        <>
          <section className="gridTwo">
            <Panel title="Udział kategorii w wydatkach" action={<ScopeBadge type="scope" value={sections.activeTimeLabel} />}>
              <CategoryShareDonutChart data={sections.categoryShare || []} onSelect={(entry) => inspectEntry(entry, onInspect)} />
            </Panel>
            <Panel title="Pareto kategorii (Top 12)" action={<ScopeBadge type="scope" value={sections.activeTimeLabel} />}>
              <CategoryParetoChart data={sections.categoryPareto || []} onSelect={(entry) => inspectEntry(entry, onInspect)} />
            </Panel>
          </section>
          <Panel title="Koszty total - obszary, kategorie i podkategorie" action={<ScopeBadge type="year" value={sections.yearLabel} />}>
            <CategoryCostMatrixTable matrix={sections.costMatrix} onInspect={onInspect} />
          </Panel>
          <Panel title="Podkategorie (Top 14)" action={<ScopeBadge type="scope" value={sections.activeTimeLabel} />}>
            <ReportDataTable
              exportName="podkategorie"
              rows={scopedStats.subcategoryTop.slice(0, 14)}
              onRowClick={(row) => onInspect?.({ title: `Transakcje: ${row.subcategory}`, filters: { subcategory: row.subcategory }, useTimeScope: true })}
              columns={[
                { key: "subcategory", header: "Podkategoria" },
                { key: "spend", header: "Kwota", className: "num", render: (row) => money(row.spend) },
                { key: "share", header: "Udział", className: "num", render: (row) => percent(row.spend / (Number(scopedStats.spend) || 1)) },
              ]}
            />
          </Panel>
        </>
      )}

      {report === "spending" && mode === "trends" && (
        <section className="gridTwo">
          <Panel title="Trend kategorii miesiąc po miesiącu" action={<ScopeBadge type="year" value={sections.yearLabel} />}>
            <CategoryTrendChart data={sections.categoryTrends || []} onSelect={(entry) => inspectEntry(entry, onInspect, false)} />
          </Panel>
          <Panel title="Stałe i zmienne koszty" action={<ScopeBadge type="scope" value={sections.activeTimeLabel} />}>
            <FixednessBreakdownChart data={sections.fixednessChart || []} onSelect={(entry) => inspectEntry(entry, onInspect)} />
          </Panel>
        </section>
      )}

      {report === "income" && (
        <section className="gridTwo">
          <Panel title={mode === "breakdown" ? "Udział koszyków budżetu" : "Dochód i net flow miesięcznie"} action={<ScopeBadge type={mode === "breakdown" ? "year" : "scope"} value={mode === "breakdown" ? sections.yearLabel : sections.activeTimeLabel} />}>
            {mode === "breakdown" ? (
              <>
                <BudgetMixDonutChart data={sections.budgetMixChart || []} onSelect={(entry) => inspectEntry(entry, onInspect)} />
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
              </>
            ) : (
              <MonthlyCashflowComboChart
                data={sections.cashflowSeries || []}
                onSelect={(row) => onInspect?.({ title: `Transakcje: ${row.month}`, filters: { month: row.monthKey }, useTimeScope: false })}
              />
            )}
          </Panel>
          <Panel title="Nadwyżka i presja budżetu" action={<ScopeBadge type="year" value={sections.yearLabel} />}>
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
        </section>
      )}

      {report === "quality" && (
        <Panel title="Jakość danych raportu" action={<ScopeBadge type="scope" value={sections.activeTimeLabel} />}>
          <DataQualityChart data={dataQualityChart} onInspect={onInspect} />
        </Panel>
      )}

      <Panel
        title="Więcej wykresów"
        action={<button type="button" className="secondaryButton" onClick={() => setAdvancedOpen((open) => !open)}>{advancedOpen ? "Ukryj" : "Pokaż"}</button>}
      >
        {advancedOpen ? (
          <section className="gridTwo embeddedGrid">
            <div className="embeddedPanel">
              <h3>Hierarchia: obszar → grupa → kategoria</h3>
              <HierarchySunburstChart data={sections.hierarchySunburst || []} onSelect={(entry) => inspectEntry(entry, onInspect)} />
            </div>
            <div className="embeddedPanel">
              <h3>Sprzedawcy: udział</h3>
              <MerchantShareDonutChart data={sections.merchantShare || []} onSelect={(entry) => inspectEntry(entry, onInspect)} />
            </div>
            <div className="embeddedPanel">
              <h3>Sprzedawcy: trend</h3>
              <MerchantTrendChart data={sections.merchantTrends || []} onSelect={(entry) => inspectEntry(entry, onInspect, false)} />
            </div>
            <div className="embeddedPanel">
              <h3>Koncentracja sprzedawców</h3>
              <MerchantFunnelChart data={sections.merchantFunnel || []} onSelect={(entry) => inspectEntry(entry, onInspect)} />
            </div>
            <div className="embeddedPanel">
              <h3>Duże jednorazowe</h3>
              <OutlierTimelineChart data={sections.outlierTimeline || []} onSelect={(entry) => inspectEntry(entry, onInspect)} />
            </div>
          </section>
        ) : (
          <div className="empty">Dodatkowe wykresy są schowane, żeby główny raport pozostał czytelny.</div>
        )}
      </Panel>
    </section>
  );
}

function inspectEntry(entry, onInspect, useTimeScope = true) {
  const row = entry?.payload || entry || {};
  const filters = row.filter || fallbackFilter(row);
  if (!filters) return;
  onInspect?.({
    title: `Transakcje: ${row.category || row.merchant || row.fixedness || row.name || row.target || "raport"}`,
    filters,
    useTimeScope,
  });
}

function fallbackFilter(row) {
  if (row.category) return { category: row.category };
  if (row.merchant) return { query: row.merchant };
  if (row.fixedness) return { fixedness: row.fixedness };
  if (row.bucket || row.name) return { bucket: row.bucket || row.name };
  return null;
}

function ScopeBadge({ type = "scope", value }) {
  const label = type === "year" ? "Rok" : "Zakres";
  return (
    <span className={`scopeBadge ${type === "year" ? "year" : "scope"}`}>
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
  if (!onInspect) return <div className="metricCard">{content}</div>;
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
  for (const prefix of ["limit 50%: ", "punkt odniesienia 50%: ", "punkt odniesienia 30%: ", "minimum 20%: "]) {
    if (detail.startsWith(prefix)) {
      return `${prefix}${money(Number(detail.slice(prefix.length)))}`;
    }
  }
  return detail;
}
