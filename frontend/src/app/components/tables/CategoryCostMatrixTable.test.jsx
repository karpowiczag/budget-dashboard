import { cleanup, render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, describe, expect, it } from "vitest";
import { CategoryCostMatrixTable } from "./CategoryCostMatrixTable.jsx";

afterEach(() => cleanup());

describe("CategoryCostMatrixTable", () => {
  it("uses a readable low-to-high cost heat scale with a legend", () => {
    render(
      <CategoryCostMatrixTable
        matrix={{
          grandTotal: 17300,
          maxCell: 10000,
          months: [
            { key: "2026-01", label: "styczeń" },
            { key: "2026-02", label: "luty" },
            { key: "2026-03", label: "marzec" },
            { key: "2026-04", label: "kwiecień" },
          ],
          rows: [
            {
              filter: { category: "Podróże" },
              label: "Podróże",
              level: 0,
              months: {
                "2026-01": 100,
                "2026-02": 2200,
                "2026-03": 5000,
                "2026-04": 10000,
              },
              total: 17300,
            },
          ],
          totalsByMonth: [],
        }}
      />
    );

    expect(screen.getByLabelText("Skala koloru kosztu")).toHaveTextContent("niski");
    expect(screen.getByLabelText("Skala koloru kosztu")).toHaveTextContent("bardzo wysoki");
    expect(screen.getByRole("button", { name: /100\s*zł/ }).closest("td")).toHaveStyle({ backgroundColor: "#dcfce7" });
    expect(screen.getByRole("button", { name: /2200\s*zł/ }).closest("td")).toHaveStyle({ backgroundColor: "#fef3c7" });
    expect(screen.getByRole("button", { name: /5000\s*zł/ }).closest("td")).toHaveStyle({ backgroundColor: "#fed7aa" });
    expect(screen.getByRole("button", { name: /10\s*000\s*zł/ }).closest("td")).toHaveStyle({ backgroundColor: "#fecaca" });
  });

  it("collapses grouped rows by default and expands hierarchy on demand", async () => {
    render(
      <CategoryCostMatrixTable
        matrix={{
          grandTotal: 1000,
          maxCell: 1000,
          months: [{ key: "2026-01", label: "styczeń" }],
          rows: [
            { key: "area:style", label: "Styl życia", level: 0, months: { "2026-01": 1000 }, total: 1000, filter: { area: "Styl życia" } },
            { key: "category:travel", label: "Podróże", level: 1, parent: "Styl życia", months: { "2026-01": 1000 }, total: 1000, filter: { area: "Styl życia", category: "Podróże" } },
            { key: "subcategory:hotels", label: "Noclegi", level: 2, parent: "Styl życia / Podróże", months: { "2026-01": 1000 }, total: 1000, filter: { area: "Styl życia", category: "Podróże", subcategory: "Noclegi" } },
          ],
          totalsByMonth: [{ key: "2026-01", label: "styczeń", spend: 1000 }],
        }}
      />
    );

    expect(screen.getByText("Styl życia")).toBeInTheDocument();
    expect(screen.queryByText("Podróże")).not.toBeInTheDocument();
    expect(screen.queryByText("Noclegi")).not.toBeInTheDocument();

    await userEvent.click(screen.getByRole("button", { name: "Rozwiń Styl życia" }));
    expect(screen.getByText("Podróże")).toBeInTheDocument();
    expect(screen.queryByText("Noclegi")).not.toBeInTheDocument();

    await userEvent.click(screen.getByRole("button", { name: "Rozwiń Podróże" }));
    expect(screen.getByText("Noclegi")).toBeInTheDocument();

    await userEvent.click(screen.getByRole("button", { name: "Zwiń wszystko" }));
    expect(screen.queryByText("Podróże")).not.toBeInTheDocument();
  });
});
