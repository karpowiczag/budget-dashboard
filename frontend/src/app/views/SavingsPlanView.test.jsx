import { fireEvent, render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import { SavingsPlanView } from "./SavingsPlanView.jsx";

vi.mock("../components/charts/SavingsWaterfallChart.jsx", () => ({
  SavingsWaterfallChart: () => <div data-testid="savings-waterfall-chart" />,
}));

vi.mock("../components/charts/SavingsRadarChart.jsx", () => ({
  SavingsRadarChart: () => <div data-testid="savings-radar-chart" />,
}));

const plan = {
  currentMonthlySpend: 15000,
  currentMonthlyIncome: 22000,
  coreMonthlyCost: 9000,
  targetMonthlySpend: 14000,
  aggressiveMonthlySpend: 13000,
  targetInvestmentTransfer: 8000,
  aggressiveInvestmentTransfer: 9000,
  emergencyFundMin: 27000,
  emergencyFundComfort: 54000,
};

describe("SavingsPlanView", () => {
  it("edits and saves persisted budget settings", async () => {
    const onSettingChange = vi.fn();
    const onLimitChange = vi.fn();
    const onBucketOverrideChange = vi.fn();
    const onSaveSettings = vi.fn();

    render(
      <SavingsPlanView
        data={{ year: 2026 }}
        categoryExamples={{
          "Jedzenie poza domem": ["RESTAURACJA TEST", "KAWIARNIA TEST"],
        }}
        categorySubcategories={{
          "Jedzenie poza domem": ["Restauracje", "Kawa i przekąski"],
        }}
        financialFlows={{
          investmentTotal: 5000,
          investmentCount: 2,
          savingsAccountTotal: 1500,
          savingsAccountCount: 1,
          loanOverpaymentTotal: 1000,
          loanOverpaymentCount: 1,
          total: 7500,
          count: 4,
        }}
        isHistorical={false}
        plan={plan}
        primaryPlanRows={[
          {
            scope: "bucket",
            name: "Nieobowiązkowe",
            category: "Nieobowiązkowe",
            bucket: "Nieobowiązkowe",
            currentMonthly: 1000,
            limit: 700,
            potentialMonthly: 300,
            priority: "Średni",
            action: "Główny limit",
          },
        ]}
        planRows={[
          {
            category: "Jedzenie poza domem",
            bucket: "Nieobowiązkowe",
            originalBucket: "Nieobowiązkowe",
            currentMonthly: 1000,
            limit: 600,
            potentialMonthly: 400,
            priority: "Średni",
            action: "Limit restauracji",
          },
        ]}
        planSummary={{ potentialMonthly: 400, potentialYearly: 4800 }}
        planTitle="Plan oszczędzania"
        plannedInvestmentAfterCuts={7400}
        plannedSpendAfterCuts={14600}
        recommendedCuts={{
          realisticCut: 300,
          aggressiveCut: 400,
          realisticSpend: 14700,
          aggressiveSpend: 14600,
          realisticInvestable: 7300,
          aggressiveInvestable: 7400,
          groups: [
            {
              label: "Uznaniowe",
              rows: [
                {
                  category: "Jedzenie poza domem",
                  bucket: "Zachcianki",
                  currentMonthly: 1000,
                  limit: 600,
                  potentialMonthly: 400,
                  priority: "Średni",
                  action: "Limit restauracji",
                },
              ],
            },
          ],
        }}
        savingsRadar={[{ label: "Uznaniowe", value: 400 }]}
        savingsWaterfall={[{ label: "Do inwestowania", amount: 7400 }]}
        settings={{
          targetMonthlySpend: 14000,
          aggressiveMonthlySpend: 13000,
          emergencyFundMinMonths: 3,
          emergencyFundComfortMonths: 6,
          categoryLimits: [],
        }}
        settingsStatus={{ type: "success", message: "Ustawienia zapisane" }}
        bucketOptions={["Obowiązkowe stałe", "Obowiązkowe zmienne", "Do rozbicia", "Nieobowiązkowe", "Inwestycje", "Konto oszczędnościowe", "Nadpłata kredytu"]}
        onBucketOverrideChange={onBucketOverrideChange}
        onLimitChange={onLimitChange}
        onSaveSettings={onSaveSettings}
        onSettingChange={onSettingChange}
      />
    );

    fireEvent.change(screen.getByLabelText("Target wydatków"), { target: { value: "13500" } });
    expect(onSettingChange).toHaveBeenLastCalledWith("targetMonthlySpend", 13500);

    fireEvent.change(screen.getByLabelText("Limit bucket Nieobowiązkowe"), { target: { value: "800" } });
    expect(onLimitChange).toHaveBeenLastCalledWith("bucket", "Nieobowiązkowe", 800);

    await userEvent.click(screen.getByText("Kategorie (1)"));
    expect(screen.getByText("Podkategorie")).toBeInTheDocument();
    expect(screen.getByText("Restauracje")).toBeInTheDocument();
    expect(screen.getByText("Kawa i przekąski")).toBeInTheDocument();
    expect(screen.getByText("Przykłady transakcji")).toBeInTheDocument();
    expect(screen.getByText("RESTAURACJA TEST")).toBeInTheDocument();
    expect(screen.getByText("KAWIARNIA TEST")).toBeInTheDocument();
    await userEvent.selectOptions(screen.getByLabelText("Główny limit Jedzenie poza domem"), "Do rozbicia");
    expect(onBucketOverrideChange).toHaveBeenLastCalledWith("Jedzenie poza domem", "Do rozbicia");
    fireEvent.change(screen.getByLabelText("Limit Jedzenie poza domem"), { target: { value: "500" } });
    expect(onLimitChange).toHaveBeenLastCalledWith("category", "Jedzenie poza domem", 500);

    fireEvent.change(screen.getByLabelText("Komfortowy fundusz awaryjny w miesiącach"), { target: { value: "9" } });
    expect(onSettingChange).toHaveBeenLastCalledWith("emergencyFundComfortMonths", 9);

    await userEvent.click(screen.getByRole("button", { name: "Zapisz ustawienia" }));
    expect(onSaveSettings).toHaveBeenCalledOnce();
    expect(screen.getByText("Ustawienia zapisane")).toBeInTheDocument();
    expect(screen.getByText("Realistyczne cięcie")).toBeInTheDocument();
    expect(screen.getByText("Główne limity")).toBeInTheDocument();
    expect(screen.queryByText("Override'y kategorii")).not.toBeInTheDocument();
    expect(screen.getByText("Przepływy majątkowe")).toBeInTheDocument();
    expect(screen.getByText("szczegóły są w module Majątek, tutaj liczy się tylko cel planu")).toBeInTheDocument();
    expect(screen.queryByText("Faktycznie inwestowane")).not.toBeInTheDocument();
    expect(screen.queryByText("Faktyczne nadpłaty kredytu")).not.toBeInTheDocument();
    expect(screen.getByTestId("savings-waterfall-chart")).toBeInTheDocument();
    expect(screen.getByTestId("savings-radar-chart")).toBeInTheDocument();
  });
});
