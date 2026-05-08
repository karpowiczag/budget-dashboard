import { describe, expect, it } from "vitest";
import {
  buildDashboardViews,
  buildSidebarNavigation,
  selectBudgetBurnDown,
  selectBuckets,
  selectCategoryLimitChart,
  selectCategoryExamples,
  selectCategoryShare,
  selectCategorySubcategories,
  selectCashflowSankey,
  selectDataQualityChart,
  selectDailyCalendarHeatmap,
  selectFinancialFlows,
  selectFixednessChart,
  selectHierarchySunburst,
  selectImportHealth,
  selectMerchantFunnel,
  selectMerchantShare,
  selectMonthDashboard,
  selectMonthFinancialFlow,
  selectMonthlyParentLimitStatus,
  selectMonthlyDimensionTrends,
  selectModuleHeader,
  selectParentPlanRows,
  selectPlanRows,
  selectRecurringSummary,
  selectRecommendedCuts,
  selectReportsSections,
  selectReportsWorkspace,
  selectSavingsFocus,
  selectSavingsRadar,
  selectSpendingPlanSections,
  selectLocalTimeScope,
  selectSavingsWaterfall,
  selectTransactionFilterOptions,
  selectTransactionPresets,
  selectWealthDashboard,
} from "./budgetSelectors.js";

describe("buildDashboardViews", () => {
  it("builds the sidebar order around current month control and removes the old summary tab", () => {
    expect(buildDashboardViews(false).map((view) => view.id)).toEqual([
      "control",
      "plan",
      "reports",
      "wealth",
      "obligations",
      "transactions",
      "import",
    ]);
    expect(buildSidebarNavigation(false).map((view) => view.label)).toEqual([
      "Kontrola",
      "Plan",
      "Raporty",
      "Majątek",
      "Zobowiązania",
      "Transakcje",
      "Import",
    ]);
    expect(buildDashboardViews(false).map((view) => view.label)).not.toContain("Podsumowanie");
  });

  it("labels a closed year plan as a simulation", () => {
    expect(buildDashboardViews(true).find((view) => view.id === "plan")).toMatchObject({ label: "Symulacja" });
  });
});

describe("module view-model selectors", () => {
  it("keeps local time scopes independent and exposes filter chips", () => {
    expect(selectLocalTimeScope({
      calendarStats: { month: "05.2026" },
      time: { scope: "day", month: "05.2026", day: "4", drillFilter: { label: "Restauracje" } },
    })).toMatchObject({
      activeTimeLabel: "Dzień 04.05.2026",
      scope: "day",
      selectedDay: "4",
      chips: [
        { label: "Zakres", value: "Dzień 04.05.2026" },
        { label: "Drilldown", value: "Restauracje" },
      ],
    });
  });

  it("builds the spending plan rail from parent monthly limits", () => {
    expect(selectSpendingPlanSections({
      financialFlowTotal: 700,
      monthControl: { incomeToDate: 5000, remainingBudget: 1200, dailyAllowed: 60 },
      parentStatus: [
        { name: "Obowiązkowe stałe", currentMonthSpend: 1000 },
        { name: "Obowiązkowe zmienne", currentMonthSpend: 900 },
        { name: "Do rozbicia", currentMonthSpend: 300 },
        { name: "Nieobowiązkowe", currentMonthSpend: 600 },
      ],
    })).toMatchObject([
      { label: "Dochód", value: 5000, filter: { flow: "income" } },
      { label: "Rachunki i zobowiązania", value: 1000, filter: { bucket: "Obowiązkowe stałe" } },
      { label: "Planowane zmienne", value: 1200 },
      { label: "Elastyczne wydatki", value: 600 },
      { label: "Oszczędności i nadpłaty", value: 700, filter: { flow: "financial" } },
      { label: "Zostaje w miesiącu", value: 1200 },
    ]);
  });

  it("groups reports into mutually exclusive report workspaces", () => {
    expect(selectReportsWorkspace({ activeTimeLabel: "Miesiąc 05.2026", yearLabel: "Cały 2026" })).toMatchObject({
      scope: { activeTimeLabel: "Miesiąc 05.2026", yearLabel: "Cały 2026" },
      reports: [
        { id: "cashflow", label: "Cashflow", modes: ["breakdown", "trends"] },
        { id: "spending", label: "Wydatki", modes: ["breakdown", "trends"] },
        { id: "income", label: "Dochód", modes: ["breakdown", "trends"] },
        { id: "quality", label: "Jakość danych", modes: ["breakdown", "trends"] },
      ],
    });
  });

  it("separates wealth-building flows from live account balances", () => {
    expect(selectWealthDashboard({
      financialFlows: {
        rows: [
          { category: "Inwestycje", outgoing: 500, count: 1 },
          { category: "Konto oszczędnościowe", outgoing: 300, count: 2 },
          { category: "Nadpłata kredytu", outgoing: 700, count: 1 },
        ],
        investmentTotal: 500,
        investmentCount: 1,
        savingsAccountTotal: 300,
        savingsAccountInflows: 400,
        savingsAccountOutflows: 100,
        savingsAccountCount: 2,
        loanOverpaymentTotal: 700,
        loanOverpaymentCount: 1,
        total: 1500,
      },
      monthly: [{ month: "05.2026", monthKey: "2026-05", savingsInvestments: 1500, transactions: 4 }],
      reportsSections: { yearLabel: "Cały 2026" },
    })).toMatchObject({
      cards: [
        { label: "Inwestycje", value: 500, filter: { category: "Inwestycje" } },
        { label: "Konto oszczędnościowe", value: 300, filter: { category: "Konto oszczędnościowe" } },
        { label: "Nadpłaty kredytu", value: 700, filter: { category: "Nadpłata kredytu" } },
        { label: "Razem przepływy", value: 1500, detail: "transakcyjnie, bez sald kont" },
      ],
      monthlyTrend: [{ month: "05.2026", filter: { month: "2026-05", flow: "financial" } }],
      scopeLabel: "Cały 2026",
    });
  });

  it("returns context-specific module headers instead of one global KPI strip", () => {
    expect(selectModuleHeader({
      view: "control",
      activeTimeLabel: "Miesiąc 05.2026",
      data: { monthControl: { remainingBudget: 500, dailyAllowed: 25, spendToDate: 2000 }, kpis: { toCheck: 1, toCheckAmount: 80 } },
      monthDashboard: { categoryStatus: [{ currentMonthSpend: 600, limit: 500 }] },
    })).toMatchObject({
      eyebrow: "Kontrola miesiąca",
      cards: [
        { label: "Zostaje", value: 500 },
        { label: "Wydane", value: 2000 },
        { label: "Ryzyka limitów", value: 1 },
        { label: "Do sprawdzenia", value: 80 },
      ],
    });

    expect(selectModuleHeader({
      view: "wealth",
      wealthDashboard: { cards: [{ label: "Inwestycje", value: 500 }] },
    })).toMatchObject({
      eyebrow: "Majątek",
      cards: [{ label: "Inwestycje", value: 500 }],
    });
  });
});

describe("selectBuckets", () => {
  it("includes the transaction bucket for savings and investment transfers", () => {
    expect(selectBuckets({
      budgetMix: [
        { bucket: "Potrzeby", sum: 100 },
        { bucket: "Oszczędzanie/inwestycje wykonane", sum: 200 },
      ],
      savingsPlan: { categoryLimits: [] },
    })).toContain("Oszczędzanie/inwestycje");
  });
});

describe("selectFinancialFlows", () => {
  it("summarizes savings and mortgage overpayments without mixing them into spend", () => {
    expect(selectFinancialFlows({
      categories: [
        { category: "Żywność i chemia", excluded: 0, count: 5 },
        { category: "Inwestycje", excluded: 300, count: 2 },
        { category: "Konto oszczędnościowe", excluded: 400, count: 3 },
        { category: "Nadpłata kredytu", excluded: 700, count: 1 },
      ],
    })).toEqual({
      rows: [
        { category: "Nadpłata kredytu", outgoing: 700, count: 1, kind: "loanOverpayment" },
        { category: "Konto oszczędnościowe", outgoing: 400, count: 3, kind: "savingsAccount" },
        { category: "Inwestycje", outgoing: 300, count: 2, kind: "investment" },
      ],
      investments: [{ category: "Inwestycje", outgoing: 300, count: 2, kind: "investment" }],
      savingsAccounts: [{ category: "Konto oszczędnościowe", outgoing: 400, count: 3, kind: "savingsAccount" }],
      loanOverpayments: [{ category: "Nadpłata kredytu", outgoing: 700, count: 1, kind: "loanOverpayment" }],
      investmentTotal: 300,
      investmentCount: 2,
      savingsAccountTotal: 400,
      savingsAccountGrossTotal: 400,
      savingsAccountInflows: 400,
      savingsAccountOutflows: 0,
      savingsAccountCount: 3,
      loanOverpaymentTotal: 700,
      loanOverpaymentCount: 1,
      total: 1400,
      count: 6,
    });
  });

  it("uses backend net movement for savings account totals when available", () => {
    expect(selectFinancialFlows({
      kpis: { savingsAccountNetChange: 150 },
      categories: [
        { category: "Inwestycje", excluded: 300, count: 2 },
        { category: "Konto oszczędnościowe", excluded: 400, count: 3 },
        { category: "Nadpłata kredytu", excluded: 700, count: 1 },
      ],
    })).toMatchObject({
      savingsAccountTotal: 150,
      savingsAccountGrossTotal: 400,
      savingsAccountInflows: 400,
      savingsAccountOutflows: 250,
      total: 1150,
    });
  });

  it("exposes bank-like savings account inflows and outflows when backend provides turnover", () => {
    expect(selectFinancialFlows({
      kpis: {
        savingsAccountNetChange: 5859.72,
        savingsAccountInflows: 30656.42,
        savingsAccountOutflows: 24796.7,
      },
      categories: [
        { category: "Konto oszczędnościowe", excluded: 26322.78, count: 10 },
      ],
    })).toMatchObject({
      savingsAccountTotal: 5859.72,
      savingsAccountGrossTotal: 26322.78,
      savingsAccountInflows: 30656.42,
      savingsAccountOutflows: 24796.7,
      total: 5859.72,
    });
  });
});

describe("selectCategorySubcategories", () => {
  it("builds sorted specific subcategory lists per category from hierarchy rows", () => {
    expect(selectCategorySubcategories({
      hierarchy: [
        { category: "Jedzenie poza domem", subcategory: "Restauracje" },
        { category: "Jedzenie poza domem", subcategory: "Kawa" },
        { category: "Jedzenie poza domem", subcategory: "Ogólne" },
        { category: "Jedzenie poza domem", subcategory: "Restauracje" },
        { category: "Transport", subcategory: "Parking" },
      ],
    })).toEqual({
      "Jedzenie poza domem": ["Kawa", "Restauracje"],
      Transport: ["Parking"],
    });
  });
});

describe("selectMonthlyParentLimitStatus", () => {
  it("aggregates category month status into high-level budget limits", () => {
    expect(selectMonthlyParentLimitStatus([
      { category: "Restauracje", bucket: "Zachcianki", currentMonthSpend: 300, currentMonthProjection: 900 },
      { category: "Vinted", bucket: "Nieobowiązkowe", currentMonthSpend: 100, currentMonthProjection: 200 },
      { category: "Zdrowie", bucket: "Potrzeby mieszane", currentMonthSpend: 400, currentMonthProjection: 600 },
    ], [
      { scope: "bucket", name: "Nieobowiązkowe", currentMonthly: 800, limit: 700 },
      { scope: "bucket", name: "Do rozbicia", currentMonthly: 600, limit: 500 },
    ])).toMatchObject([
      {
        scope: "bucket",
        category: "Nieobowiązkowe",
        currentMonthSpend: 400,
        currentMonthProjection: 1100,
        limit: 700,
        potentialMonthly: 400,
        filter: { bucket: "Nieobowiązkowe" },
        children: [
          { category: "Restauracje", currentMonthProjection: 900, potentialMonthly: 900, filter: { category: "Restauracje" } },
          { category: "Vinted", currentMonthProjection: 200, potentialMonthly: 200, filter: { category: "Vinted" } },
        ],
      },
      {
        scope: "bucket",
        category: "Do rozbicia",
        currentMonthSpend: 400,
        currentMonthProjection: 600,
        limit: 500,
        potentialMonthly: 100,
        filter: { bucket: "Do rozbicia" },
      },
    ]);
  });

  it("uses manual bucket overrides when aggregating month limit status", () => {
    expect(selectMonthlyParentLimitStatus([
      { category: "Marketplace i zakupy online", bucket: "Do rozbicia", currentMonthSpend: 400, currentMonthProjection: 600 },
    ], [
      { scope: "bucket", name: "Obowiązkowe zmienne", currentMonthly: 0, limit: 700 },
      { scope: "bucket", name: "Do rozbicia", currentMonthly: 0, limit: 700 },
    ], {
      "Marketplace i zakupy online": "Obowiązkowe zmienne",
    })).toMatchObject([
      {
        category: "Obowiązkowe zmienne",
        currentMonthProjection: 600,
        children: [{ category: "Marketplace i zakupy online", bucket: "Obowiązkowe zmienne", bucketOverride: "Obowiązkowe zmienne" }],
      },
      {
        category: "Do rozbicia",
        currentMonthProjection: 0,
        children: [],
      },
    ]);
  });
});

describe("manual category bucket overrides", () => {
  it("moves category plan rows and recomputes parent bucket totals", () => {
    const planRows = selectPlanRows({
      savingsPlan: {
        categoryLimits: [
          { category: "Marketplace i zakupy online", bucket: "Do rozbicia", currentMonthly: 1000, limit: 900 },
        ],
      },
    }, {}, {
      "Marketplace i zakupy online": "Obowiązkowe zmienne",
    });

    expect(planRows).toMatchObject([
      { category: "Marketplace i zakupy online", originalBucket: "Do rozbicia", bucket: "Obowiązkowe zmienne", bucketOverride: "Obowiązkowe zmienne" },
    ]);
    expect(selectParentPlanRows({
      savingsPlan: {
        parentLimits: [
          { scope: "bucket", name: "Obowiązkowe zmienne", currentMonthly: 0, limit: 800 },
          { scope: "bucket", name: "Do rozbicia", currentMonthly: 1000, limit: 800 },
        ],
      },
    }, {}, planRows)).toMatchObject([
      { name: "Obowiązkowe zmienne", currentMonthly: 1000, potentialMonthly: 200 },
      { name: "Do rozbicia", currentMonthly: 0, potentialMonthly: 0 },
    ]);
  });
});

describe("selectCategoryExamples", () => {
  it("builds transaction example lists per category from category summaries", () => {
    expect(selectCategoryExamples({
      categories: [
        { category: "Jedzenie poza domem", merchantExamples: ["RESTAURACJA", "KAWIARNIA"] },
        { category: "Transport", merchantExamples: ["PARKING"] },
      ],
    })).toEqual({
      "Jedzenie poza domem": ["RESTAURACJA", "KAWIARNIA"],
      Transport: ["PARKING"],
    });
  });
});

describe("selectMonthFinancialFlow", () => {
  it("reads the monthly savings flow from the selected month", () => {
    expect(selectMonthFinancialFlow({
      monthly: [
        { monthKey: "2026-01", savingsInvestments: 100 },
        { monthKey: "2026-02", savingsInvestments: 200 },
      ],
    }, "2026-02")).toBe(200);
  });
});

describe("selectMonthDashboard", () => {
  it("builds action cards and prioritizes categories over budget", () => {
    expect(selectMonthDashboard({
      financialFlowTotal: 300,
      monthControl: {
        month: "05.2026",
        spendToDate: 4000,
        elapsedDays: 7,
        projectedSpend: 19000,
        projectedDelta: -1000,
        remainingBudget: -500,
        dailyAllowed: 0,
        incomeToDate: 18000,
        alerts: [{ type: "Limit", severity: "Wysoki", message: "Za dużo" }],
        categoryStatus: [
          { category: "Transport", projectedDelta: 200, usage: 0.4 },
          { category: "Opłaty bankowe", projectedDelta: 1, usage: 0, currentMonthProjection: 0 },
          { category: "Jedzenie poza domem", projectedDelta: -800, usage: 1.2 },
        ],
      },
    })).toMatchObject({
      title: "Kontrola bieżącego miesiąca",
      cards: [
        { label: "05.2026", filter: { flow: "spend" } },
        { label: "Prognoza końca miesiąca", filter: { flow: "spend" } },
        { label: "Do wydania", tone: "warn" },
        { label: "Dochód miesiąca", filter: { flow: "income" } },
        { label: "Oszczędności/nadpłaty", value: 300, filter: { flow: "financial" } },
      ],
      categoryStatus: [
        { category: "Jedzenie poza domem" },
        { category: "Transport" },
      ],
    });
  });

  it("uses high-level parent limits when they are available", () => {
    expect(selectMonthDashboard({
      monthControl: {
        month: "05.2026",
        spendToDate: 4000,
        elapsedDays: 7,
        projectedSpend: 19000,
        projectedDelta: -1000,
        remainingBudget: -500,
        dailyAllowed: 0,
        incomeToDate: 18000,
        alerts: [],
        categoryStatus: [
          { category: "Restauracje", bucket: "Zachcianki", currentMonthSpend: 300, currentMonthProjection: 900 },
          { category: "Vinted", bucket: "Nieobowiązkowe", currentMonthSpend: 100, currentMonthProjection: 200 },
        ],
      },
      primaryPlanRows: [
        { scope: "bucket", name: "Nieobowiązkowe", currentMonthly: 800, limit: 700 },
      ],
    })).toMatchObject({
      categoryStatus: [
        { scope: "bucket", category: "Nieobowiązkowe", currentMonthProjection: 1100, filter: { bucket: "Nieobowiązkowe" } },
      ],
      savingsFocus: {
        topActions: [
          { category: "Nieobowiązkowe", decision: "Do cięcia", potentialMonthly: 400, filter: { bucket: "Nieobowiązkowe" } },
        ],
      },
      limitChart: [
        { category: "Nieobowiązkowe", projected: 1100, limit: 700, filter: { bucket: "Nieobowiązkowe" } },
      ],
    });
  });
});

describe("decision-focused savings selectors", () => {
  it("separates controllable cuts, review categories and protected obligations", () => {
    const focus = selectSavingsFocus({
      planRows: [
        { category: "Jedzenie poza domem", bucket: "Zachcianki", currentMonthly: 900, limit: 600, potentialMonthly: 300 },
        { category: "Marketplace i zakupy online", bucket: "Do rozbicia", currentMonthly: 1700, limit: 1400, potentialMonthly: 300 },
        { category: "Czynsz i wynajem", bucket: "Potrzeby", currentMonthly: 5000, limit: 5000, potentialMonthly: 0 },
      ],
    });

    expect(focus.cutNow).toMatchObject([{ category: "Jedzenie poza domem", decision: "Do cięcia" }]);
    expect(focus.review).toMatchObject([{ category: "Marketplace i zakupy online", decision: "Do rozbicia" }]);
    expect(focus.protectedRows).toMatchObject([{ category: "Czynsz i wynajem", decision: "Nie ciąć automatycznie" }]);
  });

  it("builds realistic and aggressive cut scenarios from decision groups", () => {
    expect(selectRecommendedCuts({
      plan: { currentMonthlySpend: 18000, currentMonthlyIncome: 30000 },
      planRows: [
        { category: "Jedzenie poza domem", bucket: "Zachcianki", currentMonthly: 900, limit: 600, potentialMonthly: 300 },
        { category: "Marketplace i zakupy online", bucket: "Do rozbicia", currentMonthly: 1700, limit: 1400, potentialMonthly: 300 },
      ],
    })).toMatchObject({
      realisticCut: 300,
      aggressiveCut: 600,
      realisticSpend: 17700,
      aggressiveInvestable: 12600,
      groups: [
        { label: "Uznaniowe" },
        { label: "Do rozbicia" },
      ],
    });
  });
});

describe("chart selectors", () => {
  it("builds monthly burn-down and limit projection series", () => {
    expect(selectBudgetBurnDown({
      calendar: {
        days: [
          { day: 1, date: "2026-05-01", spend: 100, income: 0, transactions: 1 },
          { day: 2, date: "2026-05-02", spend: 50, income: 0, transactions: 1 },
        ],
      },
      monthControl: { targetSpend: 3000, daysInMonth: 30 },
    })).toMatchObject([
      { day: 1, cumulativeSpend: 100, targetPace: 100 },
      { day: 2, cumulativeSpend: 150, targetPace: 200 },
    ]);

    expect(selectDailyCalendarHeatmap({
      days: [{ day: 1, date: "2026-05-01", spend: 100, income: 0, transactions: 1 }],
    })).toMatchObject([
      { date: "2026-05-01", spend: 100, filter: { date: "2026-05-01" } },
    ]);

    expect(selectCategoryLimitChart([
      { category: "Restauracje", limit: 1000, currentMonthSpend: 800, currentMonthProjection: 1300, remainingThisMonth: -300 },
    ])).toMatchObject([
      { category: "Restauracje", current: 800, projected: 1300, limit: 1000, usage: 1.3, filter: { category: "Restauracje" } },
    ]);
  });

  it("builds plan, fixedness and data quality chart models", () => {
    expect(selectSavingsWaterfall({
      plan: { currentMonthlyIncome: 20000, currentMonthlySpend: 18000 },
      planSummary: { potentialMonthly: 2500 },
      plannedSpendAfterCuts: 15500,
      plannedInvestmentAfterCuts: 4500,
    })).toMatchObject([
      { label: "Średni dochód", amount: 20000, kind: "total" },
      { label: "Koszt życia teraz", amount: -18000, absAmount: 18000, kind: "delta" },
      { label: "Cięcia limitów", amount: 2500, absAmount: 2500, kind: "delta" },
      { label: "Wolne środki po limitach", amount: 4500, kind: "total" },
    ]);

    expect(selectSavingsRadar({
      groups: [
        { label: "Uznaniowe", rows: [{ potentialMonthly: 300 }, { potentialMonthly: 200 }] },
      ],
    })).toEqual([{ label: "Uznaniowe", value: 500, count: 2 }]);

    expect(selectFixednessChart([
      { type: "Stałe", spend: 3000, excludedOutgoing: 500, count: 4 },
    ])).toMatchObject([
      { fixedness: "Stałe", total: 3500, filter: { fixedness: "Stałe" } },
    ]);

    expect(selectDataQualityChart({
      kpis: { toCheck: 2, toCheckAmount: 300, lowConfidence: 1, corrections: 4 },
      scopedStats: {
        categoryTop: [{ category: "Marketplace i zakupy online", spend: 250, count: 2 }],
        confidenceBreakdown: [{ confidence: "Niska", count: 1, spend: 100, income: 0, excluded: 0 }],
        amountBands: [{ label: "100-250", minAmount: 100, maxAmount: 250, count: 3, spend: 500 }],
      },
    })).toMatchObject({
      cards: [
        { label: "Do sprawdzenia", value: 2, amount: 300, filter: { reviewStatus: "needsReview" } },
        { label: "Do rozbicia", value: 2, filter: { reviewStatus: "needsSplit" } },
        { label: "Niska pewność", value: 1 },
        { label: "Korekty roku", value: 4, filter: null, useTimeScope: false },
      ],
      confidence: [{ confidence: "Niska", count: 1, filter: { confidence: "Niska" } }],
      amountBands: [{ label: "100-250", count: 3 }],
    });
  });
});

describe("selectReportsSections", () => {
  it("keeps cashflow, budget mix and scoped cards together for reports", () => {
    const sections = selectReportsSections({
      activeTimeLabel: "Miesiąc 01.2026",
      year: 2026,
      budgetMix: [{ bucket: "Potrzeby", sum: 1000 }],
      fixedness: [{ type: "Stałe", spend: 400, excludedOutgoing: 0, count: 2 }],
      financialFlows: { rows: [{ category: "Nadpłata kredytu", outgoing: 200, count: 1 }] },
      kpis: { income: 5000, operatingSurplus: 1200, unassignedSurplus: 300 },
      monthly: [{ month: "01.2026", monthKey: "2026-01", income: 5000, spend: 2000, savingsInvestments: 200, transactions: 10 }],
      needs: 1500,
      mixedNeeds: 250,
      oneoffs: [{ postedDate: "2026-01-10", merchant: "SKLEP", correctedCategory: "Dom", spend: 700 }],
      savingsTarget: 1000,
      scopedStats: {
        spend: 2000,
        income: 5000,
        transactionCount: 10,
        categoryTop: [{ category: "Dom", spend: 1000, count: 2 }],
        hierarchyTop: [{ area: "Dom", group: "Mieszkanie", category: "Dom", subcategory: "Ogólne", spend: 1000, count: 2 }],
        merchants: [{ merchant: "IKEA", sum: 900, count: 1 }],
        fixednessBreakdown: [{ fixedness: "Uznaniowe", spend: 600, excludedOutgoing: 0, count: 3 }],
      },
      yearStats: {
        monthlyCategoryTrends: [{ month: "02.2026", monthKey: "2026-02", category: "Dom", spend: 800, count: 2 }],
        monthlyMerchantTrends: [{ month: "02.2026", monthKey: "2026-02", merchant: "IKEA", spend: 700, count: 1 }],
      },
      wants: 600,
      wantsTarget: 1500,
    });

    expect(sections).toMatchObject({
      activeTimeLabel: "Miesiąc 01.2026",
      yearLabel: "Cały 2026",
      financialFlowTotal: 200,
      financialFlowCount: 1,
      scopeCards: [
        { label: "Wydatki", filter: { flow: "spend" } },
        { label: "Wpływy", filter: { flow: "income" } },
        { label: "Nadwyżka operacyjna", value: 3000 },
        { label: "Oszczędności/nadpłaty", filter: { flow: "financial" } },
      ],
    });
    expect(sections.benchmarkCards).toEqual(expect.arrayContaining([
      expect.objectContaining({ label: "Obowiązkowe", value: 1750 }),
      expect.objectContaining({ label: "Nieobowiązkowe", value: 600 }),
    ]));
    expect(sections.cashflowSeries).toMatchObject([{ month: "01.2026", spend: 2000 }]);
    expect(sections.budgetMixChart).toMatchObject([{ name: "Potrzeby", value: 1000, filter: { bucket: "Potrzeby" } }]);
    expect(sections.categoryPareto).toMatchObject([{ category: "Dom", cumulativeShare: 1 }]);
    expect(sections.categoryShare[0]).toMatchObject({ category: "Dom", share: 0.5 });
    expect(sections.cashflowSankey.links).toEqual(expect.arrayContaining([
      expect.objectContaining({ source: "Wpływy", target: "Dostępne środki" }),
      expect.objectContaining({ source: "Dostępne środki", target: "Koszt życia" }),
    ]));
    expect(sections.cashflowSankey.links).not.toEqual(expect.arrayContaining([
      expect.objectContaining({ source: "Koszt życia", target: "Potrzeby" }),
    ]));
    expect(sections.categoryTrends).toMatchObject([{ category: "Dom", monthKey: "2026-02" }]);
    expect(sections.hierarchySunburst).toMatchObject([{ name: "Dom", children: [{ name: "Mieszkanie" }] }]);
    expect(sections.merchantFunnel).toMatchObject([{ merchant: "IKEA", sum: 900 }]);
    expect(sections.merchantShare[0]).toMatchObject({ merchant: "IKEA", share: 0.45 });
    expect(sections.merchantTrends).toMatchObject([{ merchant: "IKEA", monthKey: "2026-02" }]);
    expect(sections.fixednessChart).toMatchObject([{ fixedness: "Uznaniowe", total: 600 }]);
    expect(sections.outlierTimeline).toMatchObject([{ merchant: "SKLEP", amount: 700 }]);
  });
});

describe("advanced analytical selectors", () => {
  it("builds Sankey, sunburst and top dimension trend models", () => {
    expect(selectCashflowSankey({
      financialFlows: { rows: [{ category: "Nadpłata kredytu", outgoing: 200 }] },
      scopedStats: { income: 1500, spend: 1000 },
    }).links).toEqual(expect.arrayContaining([
      expect.objectContaining({ source: "Wpływy", target: "Dostępne środki", value: 1500 }),
      expect.objectContaining({ source: "Dostępne środki", target: "Koszt życia", value: 1000 }),
      expect.objectContaining({ source: "Dostępne środki", target: "Nadpłata kredytu", value: 200 }),
      expect.objectContaining({ source: "Dostępne środki", target: "Wolne środki po przepływach", value: 300 }),
    ]));
    expect(selectCashflowSankey({
      financialFlows: { rows: [{ category: "Inwestycje", outgoing: 8000 }] },
      scopedStats: { income: 5000, spend: 2000 },
    }).links).toEqual(expect.arrayContaining([
      expect.objectContaining({ source: "Wpływy", target: "Dostępne środki", value: 5000 }),
      expect.objectContaining({ source: "Środki z salda", target: "Dostępne środki", value: 5000 }),
      expect.objectContaining({ source: "Dostępne środki", target: "Inwestycje", value: 8000 }),
    ]));
    expect(selectCashflowSankey({
      financialFlows: { rows: [] },
      scopedStats: { income: 300, spend: 0 },
    }).links).not.toEqual(expect.arrayContaining([
      expect.objectContaining({ source: "Koszt życia", target: "Nadwyżka operacyjna po oszczędnościach" }),
    ]));

    expect(selectHierarchySunburst([
      { area: "Dom", group: "Mieszkanie", category: "Czynsz", subcategory: "Ogólne", spend: 1200, count: 1 },
    ])).toMatchObject([
      { name: "Dom", value: 1200, children: [{ name: "Mieszkanie", children: [{ name: "Czynsz" }] }] },
    ]);

    expect(selectMonthlyDimensionTrends([
      { month: "01.2026", monthKey: "2026-01", merchant: "IKEA", spend: 500, count: 1 },
      { month: "02.2026", monthKey: "2026-02", merchant: "IKEA", spend: 300, count: 1 },
    ], "merchant", { type: "merchant" })).toMatchObject([
      { merchant: "IKEA", filter: { query: "IKEA" } },
      { merchant: "IKEA", filter: { query: "IKEA" } },
    ]);

    expect(selectMerchantFunnel([{ merchant: "IKEA", sum: 500, count: 1 }])).toEqual([
      { merchant: "IKEA", sum: 500, count: 1, filter: { query: "IKEA" } },
    ]);

    expect(selectCategoryShare([{ category: "Dom", spend: 500, count: 1 }], 1000)).toEqual([
      { category: "Dom", spend: 500, count: 1, share: 0.5, filter: { category: "Dom" } },
      { category: "Pozostałe", spend: 500, count: 0, share: 0.5, filter: null },
    ]);
    expect(selectMerchantShare([{ merchant: "IKEA", sum: 250, count: 1 }], 1000)).toEqual([
      { merchant: "IKEA", sum: 250, count: 1, share: 0.25, filter: { query: "IKEA" } },
      { merchant: "Pozostali", sum: 750, count: 0, share: 0.75, filter: null },
    ]);
  });
});

describe("selectRecurringSummary", () => {
  it("sorts recurring payments by expected day and amount", () => {
    expect(selectRecurringSummary({
      monthControl: { month: "05.2026", sinkingFunds: [{ name: "OC", monthlySetAside: 100 }] },
      recurring: [{ merchant: "NETFLIX", category: "Multimedia", months: 3, monthlyAverage: 60 }],
      recurringCalendar: [
        { merchant: "B", category: "Czynsz i wynajem", avgDay: 10, months: 5, monthlyAverage: 200 },
        { merchant: "A", category: "Multimedia", avgDay: 2, months: 3, monthlyAverage: 50 },
        { merchant: "DELIKATESY", category: "Żywność i chemia", avgDay: 3, months: 5, monthlyAverage: 500 },
        { merchant: "ALLEGRO", category: "Marketplace i zakupy online", avgDay: 5, months: 5, monthlyAverage: 180 },
        { merchant: "ROSSMANN", category: "Uroda i kosmetyki", avgDay: 7, months: 5, monthlyAverage: 140 },
      ],
    })).toMatchObject({
      cards: [
        { label: "Cykliczne koszty", value: 60 },
        { label: "Fundusze celowe", value: 100 },
        { label: "Najbliższy miesiąc", value: "05.2026" },
      ],
      recurringCalendar: [
        { merchant: "A" },
        { merchant: "B" },
      ],
      recurringTimeline: [
        { merchant: "A", day: 2, amount: 50 },
        { merchant: "B", day: 10, amount: 200 },
      ],
    });
  });
});

describe("selectTransactionPresets", () => {
  it("defines audit presets for common transaction investigations", () => {
    expect(selectTransactionPresets()).toEqual(expect.arrayContaining([
      { label: "Do sprawdzenia", filters: { reviewStatus: "needsReview" } },
      { label: "Do rozbicia", filters: { reviewStatus: "needsSplit" } },
      { label: "Niska pewność", filters: { confidence: "Niska" } },
      { label: "Transfery techniczne", filters: { flow: "excluded" } },
      { label: "Duże kwoty", filters: { flow: "spend", sort: "amount,asc" } },
    ]));
  });
});

describe("selectImportHealth", () => {
  it("summarizes imported years, data quality and duplicate audit", () => {
    expect(selectImportHealth({
      data: { year: 2026, kpis: { toCheck: 2, toCheckAmount: 300 } },
      years: [{ year: 2025, transactions: 100 }, { year: 2026, transactions: 50 }],
      importRuns: [{ id: 2, duplicatesRemoved: 3 }, { id: 1, duplicatesRemoved: 1 }],
    })).toMatchObject({
      latestRun: { id: 2 },
      cards: [
        { label: "Lata w bazie", value: 2 },
        { label: "Transakcje w bazie", value: 150 },
        { label: "Do sprawdzenia", value: 2, amount: 300 },
        { label: "Usunięte duplikaty", value: 3 },
      ],
    });
  });
});

describe("selectTransactionFilterOptions", () => {
  it("builds distinct server filter options from dashboard snapshots", () => {
    expect(selectTransactionFilterOptions({
      hierarchy: [
        { area: "Koszty codzienne", group: "Potrzeby", category: "Żywność i chemia", subcategory: "Market" },
        { area: "Koszty codzienne", group: "Potrzeby", category: "Żywność i chemia", subcategory: "Market" },
      ],
      categories: [
        { category: "Żywność i chemia" },
        { category: "Pensja" },
      ],
      fixedness: [
        { type: "Zmienne konieczne" },
      ],
    })).toMatchObject({
      areas: ["Wszystkie", "Koszty codzienne"],
      groups: ["Wszystkie", "Potrzeby"],
      subcategories: [
        { value: "Wszystkie", label: "Wszystkie" },
        { value: "Market", label: "Żywność i chemia · Market" },
      ],
      fixedness: ["Wszystkie", "Zmienne konieczne"],
    });
  });
});
