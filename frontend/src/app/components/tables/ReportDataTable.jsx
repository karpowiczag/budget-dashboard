import { DataTable } from "./DataTable.jsx";

export function ReportDataTable({ className = "smallRows", columns, emptyMessage, exportName, onRowClick, rows }) {
  const tableColumns = columns.map((column) => ({
    accessorKey: column.key,
    header: column.header,
    cell: ({ row }) => column.render ? column.render(row.original) : row.original[column.key],
    meta: {
      className: column.className,
      csvValue: column.csvValue || ((row) => row[column.key]),
      disableCsv: column.disableCsv,
      sortValue: column.sortValue,
      sortField: column.sortField,
    },
  }));

  return (
    <DataTable
      className={className}
      columns={tableColumns}
      data={rows || []}
      emptyMessage={emptyMessage}
      exportName={exportName}
      onRowClick={onRowClick}
    />
  );
}
