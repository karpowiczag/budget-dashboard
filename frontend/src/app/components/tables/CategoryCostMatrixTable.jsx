import { useMemo, useState } from "react";
import { ChevronDown, ChevronRight, Download } from "lucide-react";
import { money } from "../../domain/formatters.js";

export function CategoryCostMatrixTable({ matrix, onInspect }) {
  const months = matrix?.months || [];
  const rows = matrix?.rows || [];
  const totalsByMonth = matrix?.totalsByMonth || [];
  const maxCell = Number(matrix?.maxCell || 1);
  const [expandedRows, setExpandedRows] = useState(() => new Set());
  const expandableRows = useMemo(() => rows.filter((row, index) => hasChildRows(rows, index)), [rows]);
  const visibleRows = useMemo(() => visibleMatrixRows(rows, expandedRows), [rows, expandedRows]);

  if (!rows.length) {
    return <div className="empty">Brak wydatków do pokazania w macierzy kosztów.</div>;
  }

  const allExpanded = expandableRows.length > 0 && expandableRows.every((row) => expandedRows.has(rowId(row)));

  return (
    <div className="costMatrixShell">
      <div className="costMatrixToolbar">
        <span>Kliknij kwotę, żeby zobaczyć transakcje dla miesiąca i kategorii.</span>
        <div className="costMatrixLegend" aria-label="Skala koloru kosztu">
          <span><i className="low" /> niski</span>
          <span><i className="medium" /> średni</span>
          <span><i className="high" /> wysoki</span>
          <span><i className="peak" /> bardzo wysoki</span>
        </div>
        <button
          type="button"
          className="secondaryButton"
          onClick={() => setExpandedRows(allExpanded ? new Set() : new Set(expandableRows.map(rowId)))}
        >
          {allExpanded ? "Zwiń wszystko" : "Rozwiń wszystko"}
        </button>
        <button type="button" className="secondaryButton" onClick={() => exportMatrixCsv(matrix)}>
          <Download size={16} />
          CSV
        </button>
      </div>
      <div className="costMatrixScroller">
        <table className="costMatrix">
          <thead>
            <tr>
              <th className="costMatrixLabel">Grupa / kategoria kosztów</th>
              {months.map((month) => (
                <th key={month.key}>{month.label}</th>
              ))}
              <th>Suma</th>
            </tr>
          </thead>
          <tbody>
            {visibleRows.map(({ row, hasChildren }) => (
              <tr key={rowId(row)} className={`matrixRow matrixLevel${row.level || 0}`}>
                <th className="costMatrixLabel">
                  <MatrixRowLabel
                    expanded={expandedRows.has(rowId(row))}
                    hasChildren={hasChildren}
                    row={row}
                    onToggle={() => toggleRow(row, setExpandedRows)}
                  />
                </th>
                {months.map((month) => (
                  <td key={month.key} className="num" style={heatStyle(row.months[month.key], maxCell)}>
                    <MatrixAmount
                      amount={row.months[month.key]}
                      label={row.label}
                      onClick={() => inspectMatrixCell(row, month.key, onInspect)}
                    />
                  </td>
                ))}
                <td className="num totalCell">{money(row.total)}</td>
              </tr>
            ))}
          </tbody>
          <tfoot>
            <tr>
              <th className="costMatrixLabel">Suma</th>
              {totalsByMonth.map((month) => (
                <td key={month.key} className="num totalCell">{money(month.spend)}</td>
              ))}
              <td className="num totalCell">{money(matrix?.grandTotal)}</td>
            </tr>
          </tfoot>
        </table>
      </div>
    </div>
  );
}

function MatrixRowLabel({ expanded, hasChildren, onToggle, row }) {
  return (
    <div className="matrixLabelContent">
      {hasChildren ? (
        <button
          type="button"
          className="matrixToggle"
          aria-expanded={expanded}
          aria-label={`${expanded ? "Zwiń" : "Rozwiń"} ${row.label}`}
          onClick={onToggle}
        >
          {expanded ? <ChevronDown size={14} /> : <ChevronRight size={14} />}
        </button>
      ) : (
        <span className="matrixToggleSpacer" />
      )}
      <span className="matrixLabelText">
        <span>{row.label}</span>
        {row.level > 0 && <small>{row.parent}</small>}
      </span>
    </div>
  );
}

function MatrixAmount({ amount, label, onClick }) {
  const value = Number(amount || 0);
  if (value <= 0) return <span className="zeroAmount">0 zł</span>;
  return (
    <button type="button" className="matrixAmount" onClick={onClick} title={`Pokaż transakcje: ${label}`}>
      {money(value)}
    </button>
  );
}

function inspectMatrixCell(row, monthKey, onInspect) {
  if (!row || !monthKey) return;
  onInspect?.({
    title: `Transakcje: ${row.parent ? `${row.parent} / ${row.label}` : row.label}`,
    filters: { ...row.filter, month: monthKey },
    subtitle: monthKey,
    useTimeScope: false,
  });
}

function hasChildRows(rows, index) {
  return Number(rows[index + 1]?.level || 0) > Number(rows[index]?.level || 0);
}

function rowId(row) {
  return `${row.parent || ""}\u001F${row.key || row.label}`;
}

function toggleRow(row, setExpandedRows) {
  setExpandedRows((current) => {
    var next = new Set(current);
    var id = rowId(row);
    if (next.has(id)) {
      next.delete(id);
    } else {
      next.add(id);
    }
    return next;
  });
}

function visibleMatrixRows(rows, expandedRows) {
  var ancestors = [];
  var visible = [];
  rows.forEach((row, index) => {
    var level = Number(row.level || 0);
    while (ancestors.length && ancestors[ancestors.length - 1].level >= level) {
      ancestors.pop();
    }
    var hidden = ancestors.some((ancestor) => !expandedRows.has(ancestor.id));
    var hasChildren = hasChildRows(rows, index);
    if (!hidden) {
      visible.push({ row, hasChildren });
    }
    if (hasChildren) {
      ancestors.push({ id: rowId(row), level });
    }
  });
  return visible;
}

function heatStyle(amount, maxCell) {
  const value = Number(amount || 0);
  if (value <= 0) return {};
  const ratio = Math.min(1, Math.max(0.05, Math.sqrt(value / Math.max(1, Number(maxCell || 1)))));
  if (ratio >= 0.86) {
    return { "--matrix-cell-text": "#7f1d1d", backgroundColor: "#fecaca" };
  }
  if (ratio >= 0.62) {
    return { "--matrix-cell-text": "#7c2d12", backgroundColor: "#fed7aa" };
  }
  if (ratio >= 0.38) {
    return { "--matrix-cell-text": "#78350f", backgroundColor: "#fef3c7" };
  }
  return { "--matrix-cell-text": "#14532d", backgroundColor: "#dcfce7" };
}

function exportMatrixCsv(matrix) {
  const months = matrix?.months || [];
  const rows = matrix?.rows || [];
  const header = ["Grupa / kategoria kosztów", ...months.map((month) => month.label), "Suma"];
  const body = rows.map((row) => [
    row.parent ? `${row.parent} / ${row.label}` : row.label,
    ...months.map((month) => Number(row.months?.[month.key] || 0).toFixed(2)),
    Number(row.total || 0).toFixed(2),
  ]);
  const total = ["Suma", ...months.map((month) => Number((matrix?.totalsByMonth || []).find((item) => item.key === month.key)?.spend || 0).toFixed(2)), Number(matrix?.grandTotal || 0).toFixed(2)];
  const csv = [header, ...body, total]
    .map((line) => line.map((value) => `"${String(value).replaceAll('"', '""')}"`).join(","))
    .join("\n");
  const blob = new Blob([csv], { type: "text/csv;charset=utf-8" });
  const url = URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = url;
  link.download = "koszty-total.csv";
  link.click();
  URL.revokeObjectURL(url);
}
