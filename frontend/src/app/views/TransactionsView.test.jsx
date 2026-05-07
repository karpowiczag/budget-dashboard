import { cleanup, render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, describe, expect, it, vi } from "vitest";
import { TransactionsView } from "./TransactionsView.jsx";

afterEach(() => cleanup());

describe("TransactionsView", () => {
  it("shows current server page counts and changes pages", async () => {
    const onPageChange = vi.fn();
    const onInspect = vi.fn();

    render(
      <TransactionsView
        activeTimeLabel="Miesiąc 01.2026"
        bucket="Wszystkie"
        buckets={["Wszystkie", "Potrzeby"]}
        filterOptions={filterOptions}
        dataQualityChart={{
          cards: [{ label: "Do sprawdzenia", value: 2, amount: 300, filter: { category: "Do sprawdzenia" } }],
          confidence: [],
          amountBands: [],
        }}
        filteredTransactions={[
          {
            id: 1,
            postedDate: "2026-01-04",
            merchant: "BIEDRONKA",
            correctedCategory: "Żywność i chemia",
            subcategory: "Market spożywczy",
            bucket: "Potrzeby",
            fixedness: "Zmienne konieczne",
            confidence: "Wysoka",
            amount: -500,
            spend: 500,
            income: 0,
          },
        ]}
        presets={presets}
        query=""
        transactionFilters={defaultFilters}
        transactionPage={{ page: 1, size: 1, totalItems: 3, totalPages: 3, items: [] }}
        visibleSpend={500}
        yearTransactionTotal={12}
        onBucketChange={vi.fn()}
        onFilterChange={vi.fn()}
        onPageChange={onPageChange}
        onInspect={onInspect}
        onPreset={vi.fn()}
        onQueryChange={vi.fn()}
        onResetFilters={vi.fn()}
        onShowFullYear={vi.fn()}
      />
    );

    expect(screen.getByText("W zakresie: 3 z 12 transakcji roku")).toBeInTheDocument();
    expect(screen.getByText("Widoczna strona: 1")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Pokaż cały rok" })).toBeInTheDocument();
    expect(screen.getByText("Do sprawdzenia")).toBeInTheDocument();
    expect(screen.getByText("Strona 2 / 3")).toBeInTheDocument();

    await userEvent.click(screen.getByRole("button", { name: "Następna" }));
    expect(onPageChange).toHaveBeenCalledWith(2);

    await userEvent.click(screen.getByRole("button", { name: "Poprzednia" }));
    expect(onPageChange).toHaveBeenCalledWith(0);

    await userEvent.click(screen.getByRole("button", { name: /Do sprawdzenia/ }));
    expect(onInspect).toHaveBeenCalledWith({
      title: "Transakcje: Do sprawdzenia",
      filters: { category: "Do sprawdzenia" },
      useTimeScope: true,
    });
  });

  it("applies server-side filters and sort choices", async () => {
    const onFilterChange = vi.fn();
    const onBucketChange = vi.fn();
    const onPreset = vi.fn();
    const onResetFilters = vi.fn();

    render(
      <TransactionsView
        activeTimeLabel="Cały rok"
        bucket="Wszystkie"
        buckets={["Wszystkie", "Potrzeby", "Zachcianki"]}
        filterOptions={filterOptions}
        filteredTransactions={[]}
        presets={presets}
        query=""
        transactionFilters={defaultFilters}
        transactionPage={{ page: 0, size: 50, totalItems: 0, totalPages: 0, items: [] }}
        visibleSpend={0}
        onBucketChange={onBucketChange}
        onFilterChange={onFilterChange}
        onPageChange={vi.fn()}
        onPreset={onPreset}
        onQueryChange={vi.fn()}
        onResetFilters={onResetFilters}
      />
    );

    await userEvent.selectOptions(screen.getByLabelText("Przepływ"), "income");
    expect(onFilterChange).toHaveBeenCalledWith("flow", "income");

    await userEvent.click(screen.getByRole("button", { name: "Niska pewność" }));
    expect(onPreset).toHaveBeenCalledWith({ label: "Niska pewność", filters: { confidence: "Niska" } });

    await userEvent.selectOptions(screen.getByLabelText("Koszyk"), "Zachcianki");
    expect(onBucketChange).toHaveBeenCalledWith("Zachcianki");

    await userEvent.click(screen.getByRole("button", { name: /Filtry zaawansowane/ }));

    await userEvent.selectOptions(screen.getByLabelText("Stałość"), "Zmienne konieczne");
    expect(onFilterChange).toHaveBeenCalledWith("fixedness", "Zmienne konieczne");

    await userEvent.selectOptions(screen.getByLabelText("Pewność"), "Niska");
    expect(onFilterChange).toHaveBeenCalledWith("confidence", "Niska");

    await userEvent.selectOptions(screen.getByLabelText("Sortowanie"), "amount,desc");
    expect(onFilterChange).toHaveBeenCalledWith("sort", "amount,desc");

    await userEvent.selectOptions(screen.getByLabelText("Na stronie"), "100");
    expect(onFilterChange).toHaveBeenCalledWith("pageSize", 100);

    await userEvent.click(screen.getByRole("button", { name: /Kwota/ }));
    expect(onFilterChange).toHaveBeenCalledWith("sort", "amount,desc");

    await userEvent.click(screen.getByRole("button", { name: /Wyczyść/ }));
    expect(onResetFilters).toHaveBeenCalled();
  });
});

const defaultFilters = {
  flow: "Wszystkie",
  area: "Wszystkie",
  group: "Wszystkie",
  category: "Wszystkie",
  subcategory: "Wszystkie",
  fixedness: "Wszystkie",
  confidence: "Wszystkie",
  sort: "postedDate,desc",
  pageSize: 50,
};

const presets = [
  { label: "Niska pewność", filters: { confidence: "Niska" } },
];

const filterOptions = {
  flows: [
    { value: "Wszystkie", label: "Wszystkie przepływy" },
    { value: "income", label: "Dochód" },
  ],
  areas: ["Wszystkie", "Koszty codzienne"],
  groups: ["Wszystkie", "Potrzeby podstawowe"],
  categories: ["Wszystkie", "Żywność i chemia"],
  subcategories: ["Wszystkie", "Żywność i chemia · Market spożywczy"],
  fixedness: ["Wszystkie", "Zmienne konieczne"],
  confidence: ["Wszystkie", "Wysoka", "Niska"],
  sort: [
    { value: "postedDate,desc", label: "Data: najnowsze" },
    { value: "amount,desc", label: "Kwota: najwyższa" },
  ],
  pageSizes: [50, 100],
};
