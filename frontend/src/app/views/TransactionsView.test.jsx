import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import { TransactionsView } from "./TransactionsView.jsx";

describe("TransactionsView", () => {
  it("shows current server page counts and changes pages", async () => {
    const onPageChange = vi.fn();

    render(
      <TransactionsView
        activeTimeLabel="Miesiąc 01.2026"
        bucket="Wszystkie"
        buckets={["Wszystkie", "Potrzeby"]}
        filteredTransactions={[
          {
            id: 1,
            postedDate: "2026-01-04",
            merchant: "BIEDRONKA",
            correctedCategory: "Żywność i chemia",
            subcategory: "Market spożywczy",
            bucket: "Potrzeby",
            spend: 500,
            income: 0,
          },
        ]}
        query=""
        transactionPage={{ page: 1, size: 1, totalItems: 3, totalPages: 3, items: [] }}
        visibleSpend={500}
        onBucketChange={vi.fn()}
        onPageChange={onPageChange}
        onQueryChange={vi.fn()}
      />
    );

    expect(screen.getByText("Strona: 1 z 3")).toBeInTheDocument();
    expect(screen.getByText("Zakres po filtrach: 3")).toBeInTheDocument();
    expect(screen.getByText("Strona 2 / 3")).toBeInTheDocument();

    await userEvent.click(screen.getByRole("button", { name: "Następna" }));
    expect(onPageChange).toHaveBeenCalledWith(2);

    await userEvent.click(screen.getByRole("button", { name: "Poprzednia" }));
    expect(onPageChange).toHaveBeenCalledWith(0);
  });
});
