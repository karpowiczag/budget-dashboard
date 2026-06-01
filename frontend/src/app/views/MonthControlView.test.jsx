import { cleanup, render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, describe, expect, it, vi } from "vitest";
import { MonthControlView } from "./MonthControlView.jsx";

vi.mock("../components/charts/BudgetBurnDownChart.jsx", () => ({
  BudgetBurnDownChart: () => <div data-testid="burn-down-chart" />,
}));

vi.mock("../components/charts/CategoryLimitProjectionChart.jsx", () => ({
  CategoryLimitProjectionChart: () => <div data-testid="limit-projection-chart" />,
}));

vi.mock("../components/charts/DailyCalendarHeatmapChart.jsx", () => ({
  DailyCalendarHeatmapChart: () => <div data-testid="daily-heatmap-chart" />,
}));

vi.mock("../components/charts/LimitGaugeChart.jsx", () => ({
  LimitGaugeChart: () => <div data-testid="limit-gauge-chart" />,
}));

afterEach(() => cleanup());

describe("MonthControlView", () => {
  it("shows monthly control metrics and opens category drilldown", async () => {
    const onInspect = vi.fn();

    render(
      <MonthControlView
        onInspect={onInspect}
        monthDashboard={{
          title: "Kontrola bieżącego miesiąca",
          cards: [
            { label: "05.2026", value: 4000, detail: "wydane po 7 dniach", filter: { flow: "spend" } },
            { label: "Do wydania", value: 1200, detail: "120 dziennie", filter: { flow: "spend" } },
          ],
          selectedDay: { day: 7, spend: 250, income: 0, transactions: 3 },
          alerts: [{ type: "Limit", severity: "Wysoki", message: "Restauracje ponad plan" }],
          burnDown: [{ day: 1, cumulativeSpend: 100, targetPace: 200 }],
          dailyHeatmap: [{ date: "2026-05-07", spend: 250 }],
          limitChart: [{ category: "Jedzenie poza domem", projected: 1600, limit: 1000 }],
          categoryStatus: [
            {
              scope: "bucket",
              name: "Nieobowiązkowe",
              category: "Nieobowiązkowe",
              currentMonthProjection: 1600,
              currentMonthSpend: 400,
              limit: 1000,
              filter: { bucket: "Nieobowiązkowe" },
              children: [
                {
                  category: "Jedzenie poza domem",
                  currentMonthProjection: 1200,
                  currentMonthSpend: 300,
                  limit: 800,
                  remainingThisMonth: 500,
                  potentialMonthly: 400,
                  filter: { category: "Jedzenie poza domem" },
                },
              ],
            },
          ],
        }}
        savingsFocus={{
          cutNowPotential: 600,
          reviewPotential: 0,
          topActions: [
            {
              category: "Jedzenie poza domem",
              decision: "Do cięcia",
              current: 1600,
              limit: 1000,
              potentialMonthly: 600,
              note: "kontrolowalny wydatek",
              tone: "cut",
              filter: { category: "Jedzenie poza domem" },
            },
          ],
        }}
      />
    );

    expect(screen.getByText("Kontrola bieżącego miesiąca")).toBeInTheDocument();
    expect(screen.getByText("Co ciąć teraz")).toBeInTheDocument();
    expect(screen.getByText("Restauracje ponad plan")).toBeInTheDocument();
    expect(screen.getByText("Dzień 07")).toBeInTheDocument();
    expect(screen.getByTestId("burn-down-chart")).toBeInTheDocument();
    expect(screen.getByTestId("limit-gauge-chart")).toBeInTheDocument();
    expect(screen.queryByTestId("daily-heatmap-chart")).not.toBeInTheDocument();
    expect(screen.queryByTestId("limit-projection-chart")).not.toBeInTheDocument();
    expect(screen.getByText("Status głównych limitów")).toBeInTheDocument();
    expect(screen.getByText(/Kategorie \(1\)/)).toBeInTheDocument();
    expect(screen.getByText("400 zł / 1000 zł")).toBeInTheDocument();

    await userEvent.click(screen.getByText(/Kategorie \(1\)/));
    expect(screen.getByText("300 zł / 800 zł")).toBeInTheDocument();
    expect(screen.getByText("Zapas: 500 zł")).toBeInTheDocument();
    expect(screen.queryByText("1200 zł / 800 zł")).not.toBeInTheDocument();
    await userEvent.click(screen.getByRole("button", { name: /Pokaż transakcje limitu/ }));
    expect(onInspect).toHaveBeenCalledWith({
      title: "Transakcje: Nieobowiązkowe",
      filters: { bucket: "Nieobowiązkowe" },
      useTimeScope: true,
    });

    await userEvent.click(screen.getAllByRole("button", { name: /Jedzenie poza domem/ })[0]);
    expect(onInspect).toHaveBeenCalledWith({
      title: "Transakcje: Jedzenie poza domem",
      filters: { category: "Jedzenie poza domem" },
      useTimeScope: true,
    });
  });
});
