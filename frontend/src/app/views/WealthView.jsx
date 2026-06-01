import { CashflowSankeyChart } from "../components/charts/CashflowSankeyChart.jsx";
import { CategoryTrendChart } from "../components/charts/CategoryTrendChart.jsx";
import { SavingsWaterfallChart } from "../components/charts/SavingsWaterfallChart.jsx";
import { ReportDataTable } from "../components/tables/ReportDataTable.jsx";
import { Panel } from "../components/ui/Panel.jsx";
import { money } from "../domain/formatters.js";

export function WealthView({ wealthDashboard, onInspect }) {
  const dashboard = wealthDashboard || {};
  return (
    <section className="viewStack">
      <Panel title="Przepływy majątkowe">
        <div className="dataQualityBanner neutral">
          <strong>Transakcyjnie, nie saldo kont</strong>
          <span>Ten widok pokazuje sumę importowanych ruchów pieniędzy. Realne saldo/net worth wymaga osobnego modelu stanów kont.</span>
        </div>
      </Panel>

      <section className="gridTwo">
        <Panel title="Sankey: gdzie przesuwamy pieniądze">
          <CashflowSankeyChart
            data={dashboard.sankey}
            onSelect={(entry) => {
              const row = entry?.payload || entry;
              if (row?.filter) {
                onInspect?.({ title: `Transakcje: ${row.target || row.name}`, filters: row.filter, useTimeScope: false });
              }
            }}
          />
        </Panel>
        <Panel title="Wkład w majątek i dług">
          <SavingsWaterfallChart data={dashboard.waterfall || []} />
        </Panel>
      </section>

      <Panel title="Trend miesięczny przepływów majątkowych">
        <CategoryTrendChart
          data={dashboard.monthlyTrend || []}
          onSelect={(entry) => {
            const row = entry?.payload || entry;
            onInspect?.({ title: `Transakcje: majątek ${row.month}`, filters: row.filter || { flow: "financial", month: row.monthKey }, useTimeScope: false });
          }}
        />
      </Panel>

      <Panel title="Kategorie przepływów majątkowych">
        <ReportDataTable
          exportName="majątek-przepływy"
          rows={dashboard.flows || []}
          onRowClick={(row) => onInspect?.({ title: `Transakcje: ${row.category}`, filters: { category: row.category }, useTimeScope: false })}
          columns={[
            { key: "category", header: "Kategoria" },
            { key: "outgoing", header: "Wypływy (brutto)", className: "num", render: (row) => money(row.outgoing) },
            { key: "count", header: "Transakcje", className: "num" },
          ]}
        />
      </Panel>
    </section>
  );
}
