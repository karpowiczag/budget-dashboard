import { Download } from "lucide-react";
import { flexRender, getCoreRowModel, useReactTable } from "@tanstack/react-table";
import { downloadCsv } from "../../domain/exporters.js";

export function DataTable({
  className = "",
  columns,
  data = [],
  emptyMessage = "Brak danych.",
  exportName,
  onRowClick,
  onSort,
  sort,
}) {
  const table = useReactTable({
    columns,
    data,
    getCoreRowModel: getCoreRowModel(),
    manualSorting: true,
  });

  const exportColumns = table
    .getAllLeafColumns()
    .filter((column) => !column.columnDef.meta?.disableCsv)
    .map((column) => ({
      header: headerText(column.columnDef.header),
      value: (row) => column.columnDef.meta?.csvValue?.(row) ?? valueByAccessor(row, column.columnDef.accessorKey),
    }));

  function handleExport() {
    downloadCsv(exportName, data, exportColumns);
  }

  return (
    <div className="dataTableShell">
      {exportName && data.length > 0 && (
        <button type="button" className="tableExport" onClick={handleExport} title="Eksportuj widoczne dane CSV">
          <Download size={14} /> CSV
        </button>
      )}
      <div className={`tableWrap ${className}`.trim()}>
        <table>
          <thead>
            {table.getHeaderGroups().map((headerGroup) => (
              <tr key={headerGroup.id}>
                {headerGroup.headers.map((header) => (
                  <th key={header.id} className={header.column.columnDef.meta?.className || ""}>
                    {header.isPlaceholder ? null : (
                      <HeaderContent
                        column={header.column}
                        label={flexRender(header.column.columnDef.header, header.getContext())}
                        onSort={onSort}
                        sort={sort}
                      />
                    )}
                  </th>
                ))}
              </tr>
            ))}
          </thead>
          <tbody>
            {table.getRowModel().rows.map((row) => (
              <tr
                key={row.id}
                className={onRowClick ? "clickableRow" : ""}
                onClick={onRowClick ? () => onRowClick(row.original) : undefined}
              >
                {row.getVisibleCells().map((cell) => (
                  <td key={cell.id} className={cell.column.columnDef.meta?.className || ""}>
                    {flexRender(cell.column.columnDef.cell, cell.getContext())}
                  </td>
                ))}
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      {!data.length && <div className="tableEmpty">{emptyMessage}</div>}
    </div>
  );
}

function HeaderContent({ column, label, onSort, sort }) {
  const sortField = column.columnDef.meta?.sortField;
  if (!onSort || !sortField) {
    return label;
  }
  const [activeField, direction = "desc"] = String(sort || "").split(",");
  const active = activeField === sortField;
  const nextDirection = active && direction === "desc" ? "asc" : "desc";
  return (
    <button type="button" className={`sortHeader ${active ? "active" : ""}`} onClick={() => onSort(`${sortField},${nextDirection}`)}>
      {label}
      <span>{active ? (direction === "asc" ? "↑" : "↓") : ""}</span>
    </button>
  );
}

function headerText(header) {
  return typeof header === "string" ? header : "Kolumna";
}

function valueByAccessor(row, accessorKey) {
  if (!accessorKey) return "";
  return String(accessorKey)
    .split(".")
    .reduce((value, key) => (value == null ? undefined : value[key]), row);
}
