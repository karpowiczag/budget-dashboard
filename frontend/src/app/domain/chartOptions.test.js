import { describe, expect, it } from "vitest";
import {
  buildBudgetBurnDownOption,
  buildBudgetMixDonutOption,
  buildCashflowSankeyOption,
  buildCategoryLimitProjectionOption,
  buildCategoryShareDonutOption,
  buildCategoryTrendOption,
  buildDailyCalendarHeatmapOption,
  buildFixednessBreakdownOption,
  buildFireAllocationOption,
  buildFireProjectionOption,
  buildHierarchySunburstOption,
  buildMerchantFunnelOption,
  buildMerchantShareDonutOption,
  buildMonthlyCashflowComboOption,
  buildSavingsRadarOption,
  buildSavingsWaterfallOption,
  buildSpendBarOption,
} from "./chartOptions.js";

describe("ECharts option builders", () => {
  it("builds burn-down line series with drilldown payload data", () => {
    const option = buildBudgetBurnDownOption([
      { day: 1, date: "2026-05-01", cumulativeSpend: 100, targetPace: 200 },
    ]);

    expect(option.series).toHaveLength(2);
    expect(option.series[0]).toMatchObject({ name: "Wydane narastająco", type: "line" });
    expect(option.series[0].data[0]).toMatchObject({
      date: "2026-05-01",
      day: 1,
      value: 100,
    });
  });

  it("builds category limit projection bars", () => {
    const option = buildCategoryLimitProjectionOption([
      { category: "Restauracje", projected: 1300, limit: 1000, filter: { category: "Restauracje" } },
    ]);

    expect(option.yAxis.data).toEqual(["Restauracje"]);
    expect(option.series.map((series) => series.name)).toEqual(["Limit", "Prognoza"]);
    expect(option.series[1].data[0]).toMatchObject({
      category: "Restauracje",
      value: 1300,
      filter: { category: "Restauracje" },
    });
  });

  it("builds cashflow combo chart with money and savings-rate axes", () => {
    const option = buildMonthlyCashflowComboOption([
      { month: "01.2026", income: 1000, spend: 700, savingsInvestments: 100, savingsRate: 0.2 },
    ]);

    expect(option.series.map((series) => series.name)).toEqual([
      "Wpływy",
      "Wydatki",
      "Inwestycje/nadpłaty",
      "Stopa oszczędzania",
    ]);
    expect(option.yAxis).toHaveLength(2);
  });

  it("builds generic spend bars without losing original row fields", () => {
    const option = buildSpendBarOption({
      dataKey: "area",
      data: [{ area: "Koszty codzienne", spend: 500, count: 2 }],
    });

    expect(option.series[0].data[0]).toMatchObject({
      area: "Koszty codzienne",
      count: 2,
      value: 500,
    });
  });

  it("builds calendar heatmap and Sankey options for decision charts", () => {
    const heatmap = buildDailyCalendarHeatmapOption([
      { date: "2026-05-01", spend: 100, income: 0, transactions: 1 },
    ]);
    expect(heatmap.series[0]).toMatchObject({ type: "heatmap", coordinateSystem: "calendar" });
    expect(heatmap.series[0].data[0]).toMatchObject({ date: "2026-05-01", value: ["2026-05-01", 100] });

    const sankey = buildCashflowSankeyOption({
      nodes: [{ name: "Wpływy" }, { name: "Dostępne środki" }],
      links: [{ source: "Wpływy", target: "Dostępne środki", value: 500 }],
    });
    expect(sankey.series[0]).toMatchObject({ type: "sankey" });
    expect(sankey.series[0].label).toMatchObject({ overflow: "truncate", width: 156 });
    expect(sankey.series[0].links[0]).toMatchObject({ value: 500 });
  });

  it("builds sunburst, funnel, trend, donut and radar options", () => {
    expect(buildBudgetMixDonutOption([{ name: "Potrzeby", value: 500, share: 0.5 }]).series[0].type).toBe("pie");
    expect(buildCategoryShareDonutOption([{ category: "Dom", spend: 500, share: 0.5 }]).series[0].type).toBe("pie");
    expect(buildMerchantShareDonutOption([{ merchant: "IKEA", sum: 500, share: 0.5 }]).series[0].type).toBe("pie");
    expect(buildHierarchySunburstOption([{ name: "Dom", value: 100, children: [] }]).series[0].type).toBe("sunburst");
    expect(buildMerchantFunnelOption([{ merchant: "IKEA", sum: 500, count: 1 }]).series[0].type).toBe("funnel");
    expect(buildFixednessBreakdownOption([{ fixedness: "Stałe", total: 700, count: 2 }]).series[0].type).toBe("pie");
    expect(buildSavingsRadarOption([{ label: "Uznaniowe", value: 300 }]).series[0].type).toBe("radar");

    const trend = buildCategoryTrendOption([
      { month: "01.2026", monthKey: "2026-01", category: "Dom", spend: 100 },
      { month: "02.2026", monthKey: "2026-02", category: "Dom", spend: 200 },
    ]);
    expect(trend.series[0]).toMatchObject({ type: "line", stack: "Trend kategorii" });
    expect(trend.xAxis.data).toEqual(["2026-01", "2026-02"]);
  });

  it("builds a real income-to-surplus savings waterfall", () => {
    const option = buildSavingsWaterfallOption([
      { label: "Średni dochód", amount: 20000, kind: "total", tone: "income" },
      { label: "Koszt życia teraz", amount: -18000, absAmount: 18000, kind: "delta", tone: "expense" },
      { label: "Cięcia limitów", amount: 2500, absAmount: 2500, kind: "delta", tone: "good" },
      { label: "Wolne środki po limitach", amount: 4500, kind: "total", tone: "saving" },
    ]);

    expect(option.xAxis.data).toEqual([
      "Średni dochód",
      "Koszt życia teraz",
      "Cięcia limitów",
      "Wolne środki po limitach",
    ]);
    expect(option.series[0].data.map((row) => row.value)).toEqual([0, 2000, 2000, 0]);
    expect(option.series[1].data.map((row) => row.value)).toEqual([20000, 18000, 2500, 4500]);
  });

  it("builds FIRE projection and allocation charts", () => {
    const projection = buildFireProjectionOption(
      [{ id: "base", label: "Bazowy", projectedAtFire: 900000 }],
      1000000,
      36,
      50,
      200000,
    );
    expect(projection.series.map((series) => series.name)).toEqual(["Bazowy", "Cel FIRE"]);
    expect(projection.series[0].data[0]).toMatchObject({ age: 36, value: 200000 });

    const allocation = buildFireAllocationOption([
      { assetClass: "Akcje", value: 800, share: 0.8, targetShare: 0.75 },
      { assetClass: "Obligacje", value: 200, share: 0.2, targetShare: 0.25 },
    ]);
    expect(allocation.series[0]).toMatchObject({ type: "pie" });
    expect(allocation.series[0].data[0]).toMatchObject({ name: "Akcje", value: 800 });
  });
});
