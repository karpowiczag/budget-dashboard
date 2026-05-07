import { cleanup, render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, describe, expect, it, vi } from "vitest";
import { ReportsView } from "./ReportsView.jsx";

vi.mock("../components/charts/MonthlyCashflowComboChart.jsx", () => ({
  MonthlyCashflowComboChart: () => <div data-testid="cashflow-chart" />,
}));

vi.mock("../components/charts/BudgetMixDonutChart.jsx", () => ({
  BudgetMixDonutChart: () => <div data-testid="budget-mix-chart" />,
}));

vi.mock("../components/charts/CashflowSankeyChart.jsx", () => ({
  CashflowSankeyChart: () => <div data-testid="cashflow-sankey-chart" />,
}));

vi.mock("../components/charts/CategoryTrendChart.jsx", () => ({
  CategoryTrendChart: () => <div data-testid="category-trend-chart" />,
}));

vi.mock("../components/charts/CategoryParetoChart.jsx", () => ({
  CategoryParetoChart: ({ onSelect }) => (
    <button type="button" data-testid="pareto-chart" onClick={() => onSelect?.({ category: "Żywność i chemia" })}>
      pareto
    </button>
  ),
}));

vi.mock("../components/charts/CategoryShareDonutChart.jsx", () => ({
  CategoryShareDonutChart: () => <div data-testid="category-share-chart" />,
}));

vi.mock("../components/charts/FixednessBreakdownChart.jsx", () => ({
  FixednessBreakdownChart: () => <div data-testid="fixedness-chart" />,
}));

vi.mock("../components/charts/HierarchySunburstChart.jsx", () => ({
  HierarchySunburstChart: () => <div data-testid="hierarchy-sunburst-chart" />,
}));

vi.mock("../components/charts/MerchantFunnelChart.jsx", () => ({
  MerchantFunnelChart: () => <div data-testid="merchant-funnel-chart" />,
}));

vi.mock("../components/charts/MerchantShareDonutChart.jsx", () => ({
  MerchantShareDonutChart: () => <div data-testid="merchant-share-chart" />,
}));

vi.mock("../components/charts/MerchantTrendChart.jsx", () => ({
  MerchantTrendChart: () => <div data-testid="merchant-trend-chart" />,
}));

vi.mock("../components/charts/OutlierTimelineChart.jsx", () => ({
  OutlierTimelineChart: () => <div data-testid="outlier-chart" />,
}));

vi.mock("../components/charts/SpendBarChart.jsx", () => ({
  SpendBarChart: ({ dataKey, onSelect }) => (
    <button type="button" data-testid={`spend-chart-${dataKey}`} onClick={() => onSelect?.({ [dataKey]: "Żywność i chemia" })}>
      chart
    </button>
  ),
}));

afterEach(() => cleanup());

describe("ReportsView", () => {
  it("contains cashflow, budget mix and keeps drilldown from report charts", async () => {
    const onDrill = vi.fn();
    const onInspect = vi.fn();

    render(
      <ReportsView
        onDrill={onDrill}
        onInspect={onInspect}
        reportsSections={{
          activeTimeLabel: "Cały rok",
          yearLabel: "Cały 2026",
          monthly: [{ month: "01.2026", income: 1000, spend: 500 }],
          cashflowSeries: [{ month: "01.2026", income: 1000, spend: 500 }],
          cashflowSankey: { nodes: [{ name: "Wpływy" }], links: [{ source: "Wpływy", target: "Koszt życia", value: 500 }] },
          budgetMix: [{ bucket: "Potrzeby", sum: 500, incomeShare: 0.5 }],
          budgetMixChart: [{ name: "Potrzeby", value: 500 }],
          categoryShare: [{ category: "Żywność i chemia", spend: 500, share: 1 }],
          categoryTrends: [{ month: "01.2026", monthKey: "2026-01", category: "Żywność i chemia", spend: 500 }],
          categoryPareto: [{ category: "Żywność i chemia", spend: 500, cumulativeShare: 1 }],
          fixednessChart: [{ fixedness: "Zmienne konieczne", total: 500 }],
          hierarchySunburst: [{ name: "Koszty", value: 500, children: [] }],
          merchantFunnel: [{ merchant: "BIEDRONKA", sum: 500, count: 1 }],
          merchantShare: [{ merchant: "BIEDRONKA", sum: 500, share: 1 }],
          merchantTrends: [{ month: "01.2026", monthKey: "2026-01", merchant: "BIEDRONKA", spend: 500 }],
          outlierTimeline: [],
          benchmarkCards: [{ label: "Zachcianki", value: 100, detail: "punkt 30%: 300" }],
          scopeCards: [{ label: "Wydatki", value: 500, detail: "2 transakcje", filter: { flow: "spend" } }],
          scopedStats: {
            spend: 500,
            income: 1000,
            areaTop: [{ area: "Koszty codzienne", spend: 500 }],
            categoryTop: [{ category: "Żywność i chemia", spend: 500 }],
            subcategoryTop: [],
            merchants: [],
            oneoffs: [],
          },
          financialFlows: [],
          financialFlowTotal: 0,
          financialFlowCount: 0,
          controllable: {
            cutNowPotential: 150,
            reviewPotential: 50,
            topActions: [{ category: "Jedzenie poza domem", decision: "Do cięcia", potentialMonthly: 150, note: "kontrolowalny wydatek", tone: "cut", filter: { category: "Jedzenie poza domem" } }],
          },
        }}
      />
    );

    expect(screen.getByText("Kontrolowalne wydatki")).toBeInTheDocument();
    expect(screen.getByText("Jedzenie poza domem")).toBeInTheDocument();
    expect(screen.getByText("Cashflow miesięczny")).toBeInTheDocument();
    expect(screen.getByText("Udział koszyków budżetu")).toBeInTheDocument();
    expect(screen.getByText("Raporty")).toBeInTheDocument();
    expect(screen.getAllByText("Zakres: Cały rok").length).toBeGreaterThan(0);
    expect(screen.getAllByText("Rok: Cały 2026").length).toBeGreaterThan(0);
    expect(screen.getByTestId("cashflow-sankey-chart")).toBeInTheDocument();
    expect(screen.getByTestId("budget-mix-chart")).toBeInTheDocument();

    await userEvent.click(screen.getByRole("button", { name: "Kategorie" }));
    expect(screen.getByTestId("category-share-chart")).toBeInTheDocument();
    expect(screen.getByTestId("category-trend-chart")).toBeInTheDocument();
    expect(screen.getByTestId("hierarchy-sunburst-chart")).toBeInTheDocument();
    expect(screen.getByTestId("fixedness-chart")).toBeInTheDocument();

    await userEvent.click(screen.getByTestId("pareto-chart"));
    expect(onDrill).toHaveBeenCalledWith({
      type: "category",
      value: "Żywność i chemia",
      label: "Kategoria: Żywność i chemia",
    });
    expect(onInspect).toHaveBeenCalledWith({
      title: "Transakcje: Kategoria: Żywność i chemia",
      filters: { category: "Żywność i chemia" },
      useTimeScope: true,
    });

    await userEvent.click(screen.getByRole("button", { name: "Sprzedawcy" }));
    expect(screen.getByTestId("merchant-share-chart")).toBeInTheDocument();
    expect(screen.getByTestId("merchant-funnel-chart")).toBeInTheDocument();
  });
});
