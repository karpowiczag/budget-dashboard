import { cleanup, render, screen } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { FireView } from "./FireView.jsx";

vi.mock("../components/charts/FireAllocationChart.jsx", () => ({
  FireAllocationChart: () => <div data-testid="fire-allocation-chart" />,
}));

vi.mock("../components/charts/FireProjectionChart.jsx", () => ({
  FireProjectionChart: () => <div data-testid="fire-projection-chart" />,
}));

afterEach(() => cleanup());

describe("FireView", () => {
  it("shows FIRE plan, projections, rebalancing and Polish rule sections", () => {
    render(
      <FireView
        fireSummary={{
          reportsLoaded: true,
          currentAge: 36,
          targetAge: 50,
          positionCount: 4,
          currentPortfolioValue: 200000,
          fireNumber: 4800000,
          gapToFireNumber: 4600000,
          safeWithdrawalRate: 0.035,
          scenarios: [{ id: "base", label: "Bazowy", requiredMonthlyContribution: 12000, projectedAtFire: 1800000 }],
          allocation: [{ assetClass: "Akcje", value: 160000, share: 0.8, targetShare: 0.8 }],
          wrappers: [{ wrapper: "Rachunek opodatkowany", value: 100000, share: 0.5, positions: 2, liquidity: "płynne" }],
          rebalancing: [{ assetClass: "Akcje", currentShare: 0.8, targetShare: 0.8, drift: 0, amountToTarget: 0, action: "Bez zmian", priority: "Normalny" }],
          milestones: [{ age: 50, label: "FIRE target", description: "Cel", requiredCapital: 4800000 }],
          legalRules: [{ id: "ike-limit", label: "Limit IKE 2026", value: "28 260 zł / osoba", note: "Reguła", sourceUrl: "https://example.com" }],
          sources: [{ portfolio: "Test", asOf: "2026-05-08", positions: 4, value: 200000 }],
        }}
      />
    );

    expect(screen.getByText("Model planistyczny")).toBeInTheDocument();
    expect(screen.getByText("Kapitał dziś")).toBeInTheDocument();
    expect(screen.getByText("Cel FIRE")).toBeInTheDocument();
    expect(screen.getByText("Rebalancing")).toBeInTheDocument();
    expect(screen.getByText("Polskie reguły w modelu")).toBeInTheDocument();
    expect(screen.getByTestId("fire-projection-chart")).toBeInTheDocument();
    expect(screen.getByTestId("fire-allocation-chart")).toBeInTheDocument();
  });

  it("explains where local MyFund reports are expected when data is missing", () => {
    render(<FireView fireSummary={{ reportsLoaded: false, reportsPath: "fire/investments_reports" }} />);

    expect(screen.getByText("Brak raportów inwestycyjnych")).toBeInTheDocument();
    expect(screen.getByText(/fire\/investments_reports/)).toBeInTheDocument();
  });
});
