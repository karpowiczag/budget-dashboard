import { Download } from "lucide-react";
import { money } from "../../domain/formatters.js";

export function CategoryCostMatrixTable({ matrix, onInspect }) {
  const months = matrix?.months || [];
  const rows = matrix?.rows || [];
  const totalsByMonth = matrix?.totalsByMonth || [];
  const maxCell = Number(matrix?.maxCell || 1);

  if (!rows.length) {
    return <div className="empty">Brak wydatków do pokazania w macierzy kosztów.</div>;
  }

  return (
    <div className="costMatrixShell">
      <div className="costMatrixToolbar">
        <span>Kliknij kwotę, żeby zobaczyć transakcje dla miesiąca i kategorii.</span>
        <button type="button" className="secondaryButton" onClick={() => exportMatrixCsv(matrix)}>
          <Download size={16} />
          CSV
        </button>
      </div>
      <div className="costMatrixScroller">
        <table className="costMatrix">
          <thead>
            <tr>
              <th className="costMatrixLabel">Kategoria kosztów</th>
              {months.map((month) => (
                <th key={month.key}>{month.label}</th>
              ))}
              <th>Suma</th>
            </tr>
          </thead>
          <tbody>
            {rows.map((row) => (
              <tr key={`${row.parent || ""}-${row.label}`} className={row.level > 0 ? "subcategoryRow" : "categoryRow"}>
                <th className="costMatrixLabel">
                  <span>{row.level > 0 ? row.label : row.label}</span>
                  {row.level > 0 && <small>{row.parent}</small>}
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

function heatStyle(amount, maxCell) {
  const value = Number(amount || 0);
  if (value <= 0) return {};
  const ratio = Math.min(1, Math.max(0.08, value / Math.max(1, Number(maxCell || 1))));
  const hue = 145 - ratio * 95;
  const saturation = 62 + ratio * 12;
  const lightness = 92 - ratio * 26;
  return {
    backgroundColor: `hsl(${hue} ${saturation}% ${lightness}%)`,
  };
}

function exportMatrixCsv(matrix) {
  const months = matrix?.months || [];
  const rows = matrix?.rows || [];
  const header = ["Kategoria kosztów", ...months.map((month) => month.label), "Suma"];
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
