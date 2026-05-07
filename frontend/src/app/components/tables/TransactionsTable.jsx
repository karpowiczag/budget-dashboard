import { moneyDec } from "../../domain/formatters.js";
import { DataTable } from "./DataTable.jsx";

const columns = [
  {
    accessorKey: "postedDate",
    header: "Data",
    cell: ({ row }) => row.original.postedDate || row.original.date,
    meta: { sortField: "postedDate", csvValue: (row) => row.postedDate || row.date },
  },
  {
    accessorKey: "merchant",
    header: "Sprzedawca",
    meta: { sortField: "merchant" },
  },
  {
    accessorKey: "correctedCategory",
    header: "Kategoria",
    cell: ({ row }) => row.original.correctedCategory || row.original.category,
    meta: { sortField: "category", csvValue: (row) => row.correctedCategory || row.category },
  },
  {
    accessorKey: "subcategory",
    header: "Podkategoria",
    meta: { sortField: "subcategory" },
  },
  {
    accessorKey: "bucket",
    header: "Koszyk",
    meta: { sortField: "bucket" },
  },
  {
    accessorKey: "fixedness",
    header: "Stałość",
    meta: { sortField: "fixedness" },
  },
  {
    accessorKey: "amount",
    header: "Kwota",
    cell: ({ row }) => (
      <span className={Number(row.original.amount) < 0 ? "neg" : "pos"}>
        {moneyDec(row.original.amount)}
      </span>
    ),
    meta: { className: "num", sortField: "amount" },
  },
  {
    accessorKey: "confidence",
    header: "Pewność",
    cell: ({ row }) => <span className={`badge ${row.original.confidence}`}>{row.original.confidence}</span>,
    meta: { sortField: "confidence" },
  },
];

export function TransactionsTable({ transactions, sort, onSort, exportName = "transakcje" }) {
  return (
    <DataTable
      className="transactions"
      columns={columns}
      data={transactions}
      emptyMessage="Brak transakcji dla bieżących filtrów."
      exportName={exportName}
      onSort={onSort}
      sort={sort}
    />
  );
}
