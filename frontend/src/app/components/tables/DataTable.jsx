import { ArrowDown, ArrowUp, ArrowUpDown, Download } from "lucide-react";
import { useMemo, useState } from "react";
import { flexRender, getCoreRowModel, getSortedRowModel, useReactTable } from "@tanstack/react-table";
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
  const [sorting, setSorting] = useState([]);
  const enhancedColumns = useMemo(() => columns.map((column) => ({
    enableSorting: column.enableSorting !== false,
    sortingFn: column.sortingFn || ((left, right, columnId) => compareValues(
      sortableValue(column, left.original, columnId),
      sortableValue(column, right.original, columnId),
    )),
    ...column,
  })), [columns]);
  const table = useReactTable({
    columns: enhancedColumns,
    data,
    getCoreRowModel: getCoreRowModel(),
    getSortedRowModel: getSortedRowModel(),
    manualSorting: !!onSort,
    onSortingChange: onSort ? undefined : setSorting,
    state: onSort ? undefined : { sorting },
  });

  const exportColumns = table
    .getAllLeafColumns()
    .filter((column) => !column.columnDef.meta?.disableCsv)
    .map((column) => ({
      header: headerText(column.columnDef.header),
      value: (row) => column.columnDef.meta?.csvValue?.(row) ?? valueByAccessor(row, column.columnDef.accessorKey),
    }));

  function handleExport() {
    downloadCsv(exportName, table.getRowModel().rows.map((row) => row.original), exportColumns);
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
  if (onSort) {
    if (!sortField) return label;
    const [activeField, direction = "desc"] = String(sort || "").split(",");
    const active = activeField === sortField;
    const nextDirection = active && direction === "desc" ? "asc" : "desc";
    return (
      <button type="button" className={`sortHeader ${active ? "active" : ""}`} onClick={() => onSort(`${sortField},${nextDirection}`)}>
        <span className="sortHeaderLabel">{label}</span>
        <SortIcon direction={active ? direction : false} />
      </button>
    );
  }

  if (!column.getCanSort()) return label;
  const direction = column.getIsSorted();
  return (
    <button type="button" className={`sortHeader ${direction ? "active" : ""}`} onClick={column.getToggleSortingHandler()}>
      <span className="sortHeaderLabel">{label}</span>
      <SortIcon direction={direction} />
    </button>
  );
}

function SortIcon({ direction }) {
  if (direction === "asc") return <ArrowUp aria-hidden="true" className="sortHeaderIcon active" size={14} />;
  if (direction === "desc") return <ArrowDown aria-hidden="true" className="sortHeaderIcon active" size={14} />;
  return <ArrowUpDown aria-hidden="true" className="sortHeaderIcon" size={14} />;
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

function sortableValue(column, row, columnId) {
  return column.meta?.sortValue?.(row) ?? valueByAccessor(row, column.accessorKey || columnId);
}

function compareValues(left, right) {
  const leftEmpty = left == null || left === "";
  const rightEmpty = right == null || right === "";
  if (leftEmpty && rightEmpty) return 0;
  if (leftEmpty) return 1;
  if (rightEmpty) return -1;

  const leftNumber = typeof left === "number" ? left : Number(String(left).replace(/\s/g, "").replace(",", "."));
  const rightNumber = typeof right === "number" ? right : Number(String(right).replace(/\s/g, "").replace(",", "."));
  if (Number.isFinite(leftNumber) && Number.isFinite(rightNumber)) {
    return leftNumber === rightNumber ? 0 : leftNumber > rightNumber ? 1 : -1;
  }

  return String(left).localeCompare(String(right), "pl", { numeric: true, sensitivity: "base" });
}
