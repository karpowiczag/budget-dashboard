import { useMemo } from "react";
import {
  buildDashboardViews,
  emptyAnalytics,
  selectBuckets,
  selectCalendarStats,
  selectCategoryExamples,
  selectCategorySubcategories,
  selectDataQualityChart,
  selectFinancialFlows,
  selectImportHealth,
  selectMonthDashboard,
  selectMonthFinancialFlow,
  selectParentPlanRows,
  selectMonthStats,
  selectPlanRows,
  selectPlanSummary,
  selectPrimaryPlanRows,
  selectRecommendedCuts,
  selectRecurringSummary,
  selectReportsSections,
  selectSavingsFocus,
  selectSavingsRadar,
  selectSavingsWaterfall,
  selectTransactionPresets,
  selectTransactionFilterOptions,
  selectVisibleSpend,
} from "../domain/budgetSelectors.js";

export function useDashboardModel({
  data,
  years,
  selectedMonth,
  selectedDay,
  timeScope,
  analytics,
  yearAnalytics,
  calendar,
  transactionPage,
  customLimits,
  bucketOverrides,
  importRuns,
}) {
  const buckets = useMemo(() => selectBuckets(data), [data]);
  const planRows = useMemo(() => selectPlanRows(data, customLimits, bucketOverrides), [data, customLimits, bucketOverrides]);
  const parentPlanRows = useMemo(() => selectParentPlanRows(data, customLimits, planRows), [data, customLimits, planRows]);
  const primaryPlanRows = useMemo(() => selectPrimaryPlanRows(parentPlanRows), [parentPlanRows]);
  const categorySubcategories = useMemo(() => selectCategorySubcategories(data), [data]);
  const categoryExamples = useMemo(() => selectCategoryExamples(data), [data]);
  const planSummary = useMemo(() => selectPlanSummary(primaryPlanRows), [primaryPlanRows]);
  const savingsFocus = useMemo(() => selectSavingsFocus({ planRows: primaryPlanRows }), [primaryPlanRows]);
  const recommendedCuts = useMemo(() => selectRecommendedCuts({ plan: data?.savingsPlan, planRows: primaryPlanRows, savingsFocus }), [data, primaryPlanRows, savingsFocus]);
  const monthStats = useMemo(() => selectMonthStats(data, selectedMonth), [data, selectedMonth]);
  const calendarStats = useMemo(() => selectCalendarStats(calendar, selectedDay), [calendar, selectedDay]);
  const scopedStats = useMemo(() => analytics || emptyAnalytics(), [analytics]);
  const yearStats = useMemo(
    () => yearAnalytics || (timeScope === "all" ? scopedStats : emptyAnalytics()),
    [yearAnalytics, scopedStats, timeScope],
  );
  const filteredTransactions = useMemo(() => transactionPage?.items || [], [transactionPage]);
  const visibleSpend = useMemo(() => selectVisibleSpend(filteredTransactions), [filteredTransactions]);
  const financialFlows = useMemo(() => selectFinancialFlows(data), [data]);
  const transactionFilterOptions = useMemo(() => selectTransactionFilterOptions(data), [data]);
  const transactionPresets = useMemo(() => selectTransactionPresets(), []);
  const importHealth = useMemo(() => selectImportHealth({ data, importRuns, years }), [data, importRuns, years]);

  if (!data) {
    return {
      buckets,
      transactionFilterOptions,
      transactionPresets,
      planRows,
      parentPlanRows,
      primaryPlanRows,
      categorySubcategories,
      categoryExamples,
      planSummary,
      savingsFocus,
      recommendedCuts,
      monthStats,
      calendarStats,
      scopedStats,
      filteredTransactions,
      visibleSpend,
      transactionPage,
      financialFlows,
      importHealth,
    };
  }

  const kpis = data.kpis;
  const monthly = data.monthly.filter((row) => row.transactions > 0);
  const budgetMix = data.budgetMix.filter((row) => Math.abs(Number(row.sum)) > 0);
  const recurring = data.recurring;
  const latestYear = Math.max(...years.map((row) => Number(row.year)));
  const isHistorical = Number(data.year) < latestYear;
  const savingsTarget = Number(kpis.income) * 0.2;
  const wantsTarget = Number(kpis.income) * 0.3;
  const needs = Number(budgetMix.find((row) => row.bucket === "Obowiązkowe stałe")?.sum || budgetMix.find((row) => row.bucket === "Potrzeby")?.sum || 0);
  const mixedNeeds = Number(budgetMix.find((row) => row.bucket === "Obowiązkowe zmienne")?.sum || budgetMix.find((row) => row.bucket === "Potrzeby mieszane")?.sum || 0);
  const wants = Number(budgetMix.find((row) => row.bucket === "Nieobowiązkowe")?.sum || budgetMix.find((row) => row.bucket === "Zachcianki")?.sum || 0);
  const plan = data.savingsPlan;
  const monthControl = data.monthControl;
  const monthControlFinancialFlow = selectMonthFinancialFlow(data, monthControl?.monthKey);
  const savingsScenarioCut = Number(recommendedCuts?.realisticCut || planSummary.potentialMonthly || 0);
  const plannedSpendAfterCuts = Math.max(0, Number(plan.currentMonthlySpend) - savingsScenarioCut);
  const plannedInvestmentAfterCuts = Math.max(0, Number(plan.currentMonthlyIncome) - plannedSpendAfterCuts);
  const savingsWaterfall = selectSavingsWaterfall({ plan, planSummary, plannedSpendAfterCuts, plannedInvestmentAfterCuts, recommendedCuts });
  const savingsRadar = selectSavingsRadar(recommendedCuts);
  const categoryStatus = (monthControl?.categoryStatus || []).slice(0, 10);
  const oneoffs = (scopedStats.oneoffs?.length ? scopedStats.oneoffs : data.largeOneoffs).slice(0, 14);
  const recurringCalendar = [...(data.recurring || [])]
    .filter((row) => row.avgDay)
    .sort((a, b) => a.avgDay - b.avgDay || Number(b.monthlyAverage) - Number(a.monthlyAverage));
  const activeTimeLabel = buildActiveTimeLabel(timeScope, selectedMonth, calendarStats);
  const monthDashboard = selectMonthDashboard({
    bucketOverrides,
    calendarStats,
    financialFlowTotal: monthControlFinancialFlow,
    isHistorical,
    monthControl,
    primaryPlanRows,
  });
  const reportsSections = selectReportsSections({
    activeTimeLabel,
    year: data.year,
    budgetMix,
    financialFlows,
    fixedness: data.fixedness,
    kpis,
    monthly,
    needs,
    mixedNeeds,
    oneoffs,
    savingsTarget,
    scopedStats,
    yearStats,
    savingsFocus,
    wants,
    wantsTarget,
  });
  const recurringSummary = selectRecurringSummary({ monthControl, recurring, recurringCalendar });
  const dataQualityChart = selectDataQualityChart({ kpis, scopedStats });

  return {
    buckets,
    transactionFilterOptions,
    transactionPresets,
    planRows,
    parentPlanRows,
    primaryPlanRows,
    categorySubcategories,
    categoryExamples,
    planSummary,
    savingsFocus,
    recommendedCuts,
    monthStats,
    calendarStats,
    scopedTransactions: filteredTransactions,
    drillFilteredTransactions: filteredTransactions,
    scopedStats,
    filteredTransactions,
    visibleSpend,
    transactionPage,
    financialFlows,
    importHealth,
    monthControlFinancialFlow,
    monthDashboard,
    reportsSections,
    recurringSummary,
    kpis,
    monthly,
    budgetMix,
    recurring,
    latestYear,
    isHistorical,
    savingsTarget,
    wantsTarget,
    needs,
    mixedNeeds,
    wants,
    plan,
    monthControl,
    plannedSpendAfterCuts,
    plannedInvestmentAfterCuts,
    savingsWaterfall,
    savingsRadar,
    categoryStatus,
    oneoffs,
    recurringCalendar,
    dataQualityChart,
    activeTimeLabel,
    views: buildDashboardViews(isHistorical),
  };
}

function buildActiveTimeLabel(timeScope, selectedMonth, calendarStats) {
  if (timeScope === "all") return "Cały rok";
  if (timeScope === "month") return `Miesiąc ${selectedMonth}`;
  return `Dzień ${String(calendarStats?.selected || "").padStart(2, "0")}.${selectedMonth}`;
}
