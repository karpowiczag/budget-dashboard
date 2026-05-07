import { cleanup, render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, describe, expect, it, vi } from "vitest";
import { DataTable } from "./DataTable.jsx";

afterEach(() => {
  cleanup();
  vi.restoreAllMocks();
});

describe("DataTable", () => {
  it("keeps server-side sorting callbacks on headers", async () => {
    const onSort = vi.fn();

    render(
      <DataTable
        columns={[
          { accessorKey: "name", header: "Nazwa", meta: { sortField: "name" } },
          { accessorKey: "amount", header: "Kwota", meta: { className: "num", sortField: "amount" } },
        ]}
        data={[{ name: "A", amount: 10 }]}
        sort="amount,desc"
        onSort={onSort}
      />
    );

    await userEvent.click(screen.getByRole("button", { name: /Kwota/ }));
    expect(onSort).toHaveBeenCalledWith("amount,asc");
  });

  it("exports visible rows as CSV", async () => {
    const createObjectURL = vi.fn(() => "blob:test");
    const revokeObjectURL = vi.fn();
    vi.stubGlobal("URL", { createObjectURL, revokeObjectURL });
    vi.spyOn(HTMLAnchorElement.prototype, "click").mockImplementation(() => {});

    render(
      <DataTable
        exportName="test-export"
        columns={[
          { accessorKey: "name", header: "Nazwa" },
          { accessorKey: "amount", header: "Kwota" },
        ]}
        data={[{ name: "A, B", amount: 10 }]}
      />
    );

    await userEvent.click(screen.getByRole("button", { name: /CSV/ }));

    expect(createObjectURL).toHaveBeenCalledOnce();
    expect(revokeObjectURL).toHaveBeenCalledWith("blob:test");
  });
});
