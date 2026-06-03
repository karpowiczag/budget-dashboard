import { cleanup, render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, describe, expect, it, vi } from "vitest";
import { TransactionsTable } from "./TransactionsTable.jsx";

afterEach(() => cleanup());

const transactions = [
  {
    id: 7,
    postedDate: "2026-01-03",
    merchant: "BIEDRONKA",
    correctedCategory: "Żywność i chemia",
    bucket: "Obowiązkowe zmienne",
    amount: -200,
    confidence: "Wysoka",
    reviewStatus: "ok",
  },
];

describe("TransactionsTable recategorize action", () => {
  it("omits the Akcje column when no recategorize handler is provided", () => {
    render(<TransactionsTable transactions={transactions} />);
    expect(screen.queryByText("Akcje")).not.toBeInTheDocument();
  });

  it("recategorizes a row by its id when a category is picked", async () => {
    const onRecategorize = vi.fn();
    render(
      <TransactionsTable
        transactions={transactions}
        categoryOptions={[{ id: "investments", label: "Inwestycje" }]}
        onRecategorize={onRecategorize}
      />
    );

    await userEvent.selectOptions(screen.getByLabelText(/Zmień kategorię/), "investments");

    expect(onRecategorize).toHaveBeenCalledWith(7, "investments");
  });
});
