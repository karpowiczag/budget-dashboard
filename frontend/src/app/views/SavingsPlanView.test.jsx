import { fireEvent, render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import { SavingsPlanView } from "./SavingsPlanView.jsx";

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
    const onSaveSettings = vi.fn();

    render(
      <SavingsPlanView
        data={{ year: 2026 }}
        isHistorical={false}
        plan={plan}
        planRows={[
          {
            category: "Jedzenie poza domem",
            bucket: "Zachcianki",
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
        settings={{
          targetMonthlySpend: 14000,
          aggressiveMonthlySpend: 13000,
          emergencyFundMinMonths: 3,
          emergencyFundComfortMonths: 6,
          categoryLimits: [],
        }}
        settingsStatus={{ type: "success", message: "Ustawienia zapisane" }}
        onLimitChange={onLimitChange}
        onSaveSettings={onSaveSettings}
        onSettingChange={onSettingChange}
      />
    );

    fireEvent.change(screen.getByLabelText("Target wydatków"), { target: { value: "13500" } });
    expect(onSettingChange).toHaveBeenLastCalledWith("targetMonthlySpend", 13500);

    fireEvent.change(screen.getByLabelText("Limit Jedzenie poza domem"), { target: { value: "500" } });
    expect(onLimitChange).toHaveBeenLastCalledWith("Jedzenie poza domem", 500);

    fireEvent.change(screen.getByLabelText("Komfortowy fundusz awaryjny w miesiącach"), { target: { value: "9" } });
    expect(onSettingChange).toHaveBeenLastCalledWith("emergencyFundComfortMonths", 9);

    await userEvent.click(screen.getByRole("button", { name: "Zapisz ustawienia" }));
    expect(onSaveSettings).toHaveBeenCalledOnce();
    expect(screen.getByText("Ustawienia zapisane")).toBeInTheDocument();
  });
});
