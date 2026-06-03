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
    cell: ({ row }) => (
      <span className="stackedCell">
        <strong>{row.original.correctedCategory || row.original.category}</strong>
        {row.original.subcategory && <small>{row.original.subcategory}</small>}
      </span>
    ),
    meta: { sortField: "category", csvValue: (row) => row.correctedCategory || row.category },
  },
  {
    accessorKey: "bucket",
    header: "Koszyk",
    cell: ({ row }) => (
      <span className="stackedCell">
        <strong>{row.original.bucket}</strong>
        {row.original.fixedness && <small>{row.original.fixedness}</small>}
      </span>
    ),
    meta: { sortField: "bucket" },
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
  {
    accessorKey: "reviewStatus",
    header: "Review",
    cell: ({ row }) => reviewLabel(row.original.reviewStatus),
    meta: { sortField: "reviewStatus" },
  },
];

export function TransactionsTable({ transactions, sort, onSort, exportName = "transakcje", categoryOptions, onRecategorize }) {
  const tableColumns = onRecategorize
    ? [...columns, recategorizeColumn(categoryOptions || [], onRecategorize)]
    : columns;
  return (
    <DataTable
      className="transactions"
      columns={tableColumns}
      data={transactions}
      emptyMessage="Brak transakcji dla bieżących filtrów."
      exportName={exportName}
      onSort={onSort}
      sort={sort}
    />
  );
}

function recategorizeColumn(categoryOptions, onRecategorize) {
  return {
    id: "recategorize",
    header: "Akcje",
    enableSorting: false,
    cell: ({ row }) => (
      <select
        className="recategorizeSelect"
        aria-label={`Zmień kategorię: ${row.original.merchant || row.original.correctedCategory || row.original.id}`}
        value=""
        onChange={(event) => {
          if (event.target.value) {
            onRecategorize(row.original.id, event.target.value);
          }
        }}
      >
        <option value="">Zmień kategorię…</option>
        {categoryOptions.map((option) => (
          <option key={option.id} value={option.id}>{option.label}</option>
        ))}
      </select>
    ),
    meta: { disableCsv: true },
  };
}

function reviewLabel(value) {
  if (value === "needsReview") return "Do sprawdzenia";
  if (value === "needsSplit") return "Do rozbicia";
  return "OK";
}
