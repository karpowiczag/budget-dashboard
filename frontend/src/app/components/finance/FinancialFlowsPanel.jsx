import { Panel } from "../ui/Panel.jsx";
import { money } from "../../domain/formatters.js";
import { ReportDataTable } from "../tables/ReportDataTable.jsx";

export function FinancialFlowsPanel({ title = "Oszczędności, inwestycje i nadpłaty", action, flows = [], total, count, onInspect }) {
  const rows = flows.filter((row) => Number(row.outgoing || 0) > 0);
  const totalOutgoing = Number(total ?? rows.reduce((sum, row) => sum + Number(row.outgoing || 0), 0));
  const totalCount = Number(count ?? rows.reduce((sum, row) => sum + Number(row.count || 0), 0));

  return (
    <Panel title={title} action={action}>
      <div className="monthCards">
        <MetricCard
          label="Razem"
          value={money(totalOutgoing)}
          detail={`${totalCount} transakcji`}
          onInspect={onInspect ? () => onInspect({ title: "Transakcje: oszczędności, inwestycje i nadpłaty", filters: { flow: "financial" } }) : undefined}
        />
        {rows.slice(0, 3).map((row) => (
          <MetricCard
            key={row.category}
            label={row.category}
            value={money(row.outgoing)}
            detail={`${row.count} transakcji`}
            onInspect={onInspect ? () => onInspect({ title: `Transakcje: ${row.category}`, filters: { category: row.category } }) : undefined}
          />
        ))}
      </div>
      {rows.length > 0 && (
        <ReportDataTable
          exportName="przeplywy-finansowe"
          rows={rows}
          onRowClick={onInspect ? (row) => onInspect({ title: `Transakcje: ${row.category}`, filters: { category: row.category } }) : undefined}
          columns={[
            { key: "category", header: "Kategoria" },
            { key: "outgoing", header: "Kwota", className: "num", render: (row) => money(row.outgoing) },
            { key: "count", header: "Liczba", className: "num" },
          ]}
        />
      )}
    </Panel>
  );
}

function MetricCard({ label, value, detail, onInspect }) {
  if (onInspect) {
    return (
      <button type="button" className="metricCard inspectable" onClick={onInspect} title="Pokaż transakcje">
        <span>{label}</span>
        <strong>{value}</strong>
        <p>{detail}</p>
      </button>
    );
  }

  return (
    <div className="metricCard">
      <span>{label}</span>
      <strong>{value}</strong>
      <p>{detail}</p>
    </div>
  );
}
