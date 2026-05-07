export function downloadCsv(filename, rows, columns) {
  const safeRows = rows || [];
  const safeColumns = (columns || []).filter((column) => column?.header && column?.value);
  const csv = [
    safeColumns.map((column) => csvCell(column.header)).join(","),
    ...safeRows.map((row) => safeColumns.map((column) => csvCell(column.value(row))).join(",")),
  ].join("\n");
  const blob = new Blob([csv], { type: "text/csv;charset=utf-8" });
  const url = URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = url;
  link.download = `${filename || "export"}.csv`;
  link.click();
  URL.revokeObjectURL(url);
}

function csvCell(value) {
  const text = String(value ?? "");
  if (!/[",\n\r]/.test(text)) return text;
  return `"${text.replaceAll('"', '""')}"`;
}
