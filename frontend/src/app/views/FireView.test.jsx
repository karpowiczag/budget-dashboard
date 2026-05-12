import { cleanup, render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
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
          monthlySpendTarget: 14000,
          spendTargetConfigured: true,
          fireNumber: 4800000,
          gapToFireNumber: 4600000,
          safeWithdrawalRate: 0.035,
          scenarios: [{ id: "base", label: "Bazowy", requiredMonthlyContribution: 12000, projectedAtFire: 1800000 }],
          allocation: [{ assetClass: "Akcje", value: 160000, share: 0.8, targetShare: 0.8 }],
          wrappers: [{ wrapper: "Rachunek opodatkowany", value: 100000, share: 0.5, positions: 2, liquidity: "płynne" }],
          portfolios: [{ portfolio: "Giełda", value: 100000, share: 0.5, investmentValue: 100000, emergencyValue: 0, retirementLockedValue: 0, taxableValue: 100000, positions: 2, role: "Płynny inwestycyjny", note: "Może finansować pomost." }],
          rebalancing: [{ assetClass: "Akcje", currentShare: 0.8, targetShare: 0.8, drift: 0, amountToTarget: 0, action: "Bez zmian", priority: "Normalny" }],
          risks: [{ id: "equityConcentration", level: "medium", area: "Alokacja", title: "Portfel jest mocno akcyjny", metric: "Akcje", value: "80,0%", threshold: "85,0%", detail: "Akcje są powyżej celu.", recommendation: "Doważ obligacje." }],
          positionAnalyses: [{
            instrument: "Global Equity ETF",
            isin: "IE00FAKEETF1",
            portfolio: "Giełda",
            assetClass: "Akcje",
            instrumentType: "ETF akcyjny szerokiego rynku",
            fireRole: "Rdzeń wzrostowy FIRE",
            wrapper: "Rachunek opodatkowany",
            account: "Makler",
            currency: "USD",
            priceDate: "2026-05-08",
            value: 160000,
            costBasis: 120000,
            unrealizedGain: 40000,
            returnPct: 0.3333,
            shareOfPortfolio: 0.8,
            shareOfInvestments: 0.8,
            riskLevel: "high",
            reviewFocus: "Koncentracja",
            decision: "Nie zwiększaj ekspozycji. Nowe wpłaty kieruj w niedoważone klasy albo płynny pomost.",
            decisionReason: "Rdzeń wzrostowy FIRE · udział 80,0% portfela inwestycyjnego",
            action: "Nie dokupuj automatycznie; rozcieńczaj koncentrację nowymi wpłatami.",
            perspective: "Silnik wzrostu FIRE: wysoka zmienność.",
            riskDrivers: ["Koncentracja pozycji powyżej 25% portfela inwestycyjnego", "Waluta instrumentu: USD"],
            checklist: ["Sprawdź KID/PRIIP, TER/opłaty, walutę i replikację.", "Sprawdź, czy walor nie dubluje ekspozycji z innymi ETF/funduszami."],
          }],
          milestones: [{ age: 50, label: "FIRE target", description: "Cel", requiredCapital: 4800000 }],
          legalRules: [{ id: "ike-limit", label: "Limit IKE 2026", value: "28 260 zł / osoba", note: "Reguła", sourceUrl: "https://example.com" }],
          sources: [{ portfolio: "Test", asOf: "2026-05-08", positions: 4, value: 200000 }],
        }}
      />
    );

    expect(screen.getByText("Decyzja FIRE")).toBeInTheDocument();
    expect(screen.getByText("Cel wydatków FIRE / mies.")).toBeInTheDocument();
    expect(screen.getByText("Budżet zasila FIRE")).toBeInTheDocument();
    expect(screen.getByText("Ryzyka inwestycyjne")).toBeInTheDocument();
    expect(screen.getByText("Pełna tabela ryzyk")).toBeInTheDocument();
    expect(screen.getAllByText("Portfel jest mocno akcyjny").length).toBeGreaterThan(0);
    expect(screen.getByText("Prognoza i luka do celu")).toBeInTheDocument();
    expect(screen.getByText("Alokacja portfela")).toBeInTheDocument();
    expect(screen.getByText("Portfele MyFund")).toBeInTheDocument();
    expect(screen.getAllByText("Giełda").length).toBeGreaterThan(0);
    expect(screen.getByText("Płynny inwestycyjny")).toBeInTheDocument();
    expect(screen.getByText("Szczegóły rebalancingu")).toBeInTheDocument();
    expect(screen.getByText("Analiza walorów")).toBeInTheDocument();
    expect(screen.getByText("Wybrany zakres")).toBeInTheDocument();
    expect(screen.getByText("Wszystkie portfele")).toBeInTheDocument();
    expect(screen.getByText("Filtry analizy")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /Do decyzji/i })).toBeInTheDocument();
    expect(screen.getAllByText("Global Equity ETF").length).toBeGreaterThan(0);
    expect(screen.getAllByText("ETF akcyjny szerokiego rynku").length).toBeGreaterThan(0);
    expect(screen.getAllByText("Rdzeń wzrostowy FIRE").length).toBeGreaterThan(0);
    expect(screen.getAllByText("Koncentracja pozycji powyżej 25% portfela inwestycyjnego").length).toBeGreaterThan(0);
    expect(screen.getAllByText("Sprawdź KID/PRIIP, TER/opłaty, walutę i replikację.").length).toBeGreaterThan(0);
    expect(screen.getByText("Dane i reguły")).toBeInTheDocument();
    expect(screen.getByText("Polskie reguły modelu")).toBeInTheDocument();
    expect(screen.getByTestId("fire-projection-chart")).toBeInTheDocument();
    expect(screen.getByTestId("fire-allocation-chart")).toBeInTheDocument();
  });

  it("does not show a guessed 14k FIRE spending target when target is not configured", () => {
    render(
      <FireView
        fireSettings={{ monthlySpendOverride: null }}
        fireSummary={{
          reportsLoaded: true,
          currentAge: 36,
          targetAge: 50,
          positionCount: 4,
          currentPortfolioValue: 200000,
          monthlySpendTarget: 0,
          spendTargetConfigured: false,
          fireNumber: 0,
          gapToFireNumber: 0,
          safeWithdrawalRate: 0.035,
          scenarios: [],
          allocation: [],
          wrappers: [],
          rebalancing: [],
          actionItems: [{ priority: "P1", title: "Ustaw miesięczny cel wydatków FIRE", detail: "Bez celu nie liczę.", amount: 0 }],
          milestones: [],
          legalRules: [],
          sources: [],
          budgetLink: { firePortfolioMonthlyContribution: 9000, targetMonthlySpend: 14000 },
        }}
      />
    );

    expect(screen.getAllByText("Ustaw cel").length).toBeGreaterThan(0);
    expect(screen.getByText("Nie liczę celu z obecnego budżetu.")).toBeInTheDocument();
    expect(screen.getByRole("spinbutton", { name: "Cel wydatków FIRE miesięcznie" })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Zapisz cel" })).toBeDisabled();
    expect(screen.queryByText("Target budżetu")).not.toBeInTheDocument();
    expect(screen.queryByText(/14\s*000/)).not.toBeInTheDocument();
    expect(screen.queryByText("domyślny cel FIRE")).not.toBeInTheDocument();
  });

  it("saves the FIRE spending target from the main card", async () => {
    const user = userEvent.setup();
    const onSaveSettings = vi.fn();

    render(
      <FireView
        fireSettings={{ monthlySpendOverride: null }}
        fireSummary={{
          reportsLoaded: true,
          currentAge: 36,
          targetAge: 50,
          positionCount: 4,
          currentPortfolioValue: 200000,
          monthlySpendTarget: 0,
          spendTargetConfigured: false,
          fireNumber: 0,
          gapToFireNumber: 0,
          safeWithdrawalRate: 0.035,
          scenarios: [],
          allocation: [],
          wrappers: [],
          rebalancing: [],
          actionItems: [],
          milestones: [],
          legalRules: [],
          sources: [],
          budgetLink: { firePortfolioMonthlyContribution: 9000 },
        }}
        onSaveSettings={onSaveSettings}
      />
    );

    await user.type(screen.getByRole("spinbutton", { name: "Cel wydatków FIRE miesięcznie" }), "10000");
    await user.click(screen.getByRole("button", { name: "Zapisz cel" }));

    expect(onSaveSettings).toHaveBeenCalledWith(expect.objectContaining({ monthlySpendOverride: 10000 }));
  });

  it("renders portfolio risk levels and recommendations", () => {
    render(
      <FireView
        fireSummary={{
          reportsLoaded: true,
          currentAge: 36,
          targetAge: 50,
          positionCount: 4,
          currentPortfolioValue: 260000,
          monthlySpendTarget: 10000,
          spendTargetConfigured: true,
          fireNumber: 3428571,
          gapToFireNumber: 3000000,
          safeWithdrawalRate: 0.035,
          scenarios: [],
          allocation: [],
          wrappers: [],
          rebalancing: [],
          risks: [
            {
              id: "singlePositionConcentration",
              level: "high",
              area: "Dywersyfikacja",
              title: "Duża koncentracja pojedynczej pozycji",
              metric: "Największa pozycja",
              value: "32,0%",
              threshold: "15,0%",
              detail: "Największy instrument ma 32,0% portfela.",
              recommendation: "Rozcieńczaj koncentrację nowymi wpłatami.",
            },
          ],
          actionItems: [],
          milestones: [],
          legalRules: [],
          sources: [],
        }}
      />
    );

    expect(screen.getAllByText("Wysokie").length).toBeGreaterThan(0);
    expect(screen.getAllByText("Duża koncentracja pojedynczej pozycji").length).toBeGreaterThan(0);
    expect(screen.getAllByText("Rozcieńczaj koncentrację nowymi wpłatami.").length).toBeGreaterThan(0);
  });

  it("explains where local MyFund reports are expected when data is missing", () => {
    render(<FireView fireSummary={{ reportsLoaded: false, reportsPath: "fire/investments_reports" }} />);

    expect(screen.getByText("Brak raportów inwestycyjnych")).toBeInTheDocument();
    expect(screen.getByText(/fire\/investments_reports/)).toBeInTheDocument();
  });
});
