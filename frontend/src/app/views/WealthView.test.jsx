import { cleanup, render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, describe, expect, it, vi } from "vitest";
import { WealthView } from "./WealthView.jsx";

vi.mock("../components/charts/CashflowSankeyChart.jsx", () => ({
  CashflowSankeyChart: () => <div data-testid="wealth-sankey-chart" />,
}));

vi.mock("../components/charts/CategoryTrendChart.jsx", () => ({
  CategoryTrendChart: () => <div data-testid="wealth-trend-chart" />,
}));

vi.mock("../components/charts/SavingsWaterfallChart.jsx", () => ({
  SavingsWaterfallChart: () => <div data-testid="wealth-waterfall-chart" />,
}));

afterEach(() => cleanup());

describe("WealthView", () => {
  it("shows transaction-driven wealth flows without calling them account balances", async () => {
    const onInspect = vi.fn();

    render(
      <WealthView
        onInspect={onInspect}
        wealthDashboard={{
          cards: [
            { label: "Inwestycje", value: 500, detail: "1 transakcji", filter: { category: "Inwestycje" }, tone: "good" },
            { label: "Konto oszczędnościowe", value: 300, detail: "wpływy 400 · wydatki 100", filter: { category: "Konto oszczędnościowe" }, tone: "good" },
            { label: "Nadpłaty kredytu", value: 700, detail: "1 transakcji", filter: { category: "Nadpłata kredytu" }, tone: "good" },
          ],
          flows: [
            { category: "Inwestycje", outgoing: 500, count: 1 },
            { category: "Konto oszczędnościowe", outgoing: 300, count: 2 },
            { category: "Nadpłata kredytu", outgoing: 700, count: 1 },
          ],
          monthlyTrend: [{ month: "05.2026", monthKey: "2026-05", spend: 1500, category: "Majątek i dług" }],
          sankey: { nodes: [{ name: "Przepływy majątkowe" }], links: [] },
          waterfall: [{ label: "Razem", amount: 1500 }],
        }}
      />
    );

    expect(screen.getByText("Fundusz awaryjny i salda kont")).toBeInTheDocument();
    expect(screen.getByText("Salda wyliczane, inwestycje osobno")).toBeInTheDocument();
    expect(screen.getByText("Inwestycje")).toBeInTheDocument();
    expect(screen.getAllByText("Konto oszczędnościowe").length).toBeGreaterThan(0);
    expect(screen.getByText("Nadpłata kredytu")).toBeInTheDocument();
    expect(screen.getByTestId("wealth-sankey-chart")).toBeInTheDocument();
    expect(screen.getByTestId("wealth-waterfall-chart")).toBeInTheDocument();
    expect(screen.getByTestId("wealth-trend-chart")).toBeInTheDocument();

    await userEvent.click(screen.getByText("Inwestycje"));
    expect(onInspect).toHaveBeenCalledWith({
      title: "Transakcje: Inwestycje",
      filters: { category: "Inwestycje" },
      useTimeScope: false,
    });
  });
});
