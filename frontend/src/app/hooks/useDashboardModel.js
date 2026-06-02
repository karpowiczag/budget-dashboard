import { useMemo } from "react";
import {
  NET_INCOME_RATIO,
  buildSidebarNavigation,
  emptyAnalytics,
  selectBuckets,
  selectCalendarStats,
  selectCategoryExamples,
  selectCategorySubcategories,
  selectDataQualityChart,
  selectFinancialFlows,
  selectImportHealth,
  selectLocalTimeScope,
  selectModuleHeader,
  selectMonthDashboard,
  selectMonthFinancialFlow,
  selectParentPlanRows,
  selectPlanFeasibilityWarnings,
  selectMonthStats,
  selectPlanRows,
  selectPlanSummary,
  selectPrimaryPlanRows,
  selectRecommendedCuts,
  selectRecurringSummary,
  selectReportsSections,
  selectSafeToSpend,
  selectReportsWorkspace,
  selectSavingsFocus,
  selectSavingsRadar,
  selectSavingsWaterfall,
  selectSpendingPlanSections,
  selectTransactionPresets,
  selectTransactionFilterOptions,
  selectVisibleSpend,
  selectWealthDashboard,
} from "../domain/budgetSelectors.js";

export function useDashboardModel({
  activeView = "control",
  controlTime,
  data,
  years,
  reportsTime,
  transactionsTime,
  reportsAnalytics,
  reportsYearAnalytics,
  transactionsAnalytics,
  calendar,
  transactionPage,
  customLimits,
  bucketOverrides,
  importRuns,
  fireSummary,
  budgetSettings,
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
  const monthStats = useMemo(() => selectMonthStats(data, controlTime?.month), [data, controlTime]);
  const filteredTransactions = useMemo(() => transactionPage?.items || [], [transactionPage]);
  const visibleSpend = useMemo(() => selectVisibleSpend(filteredTransactions), [filteredTransactions]);
  const financialFlows = useMemo(() => selectFinancialFlows(data), [data]);
  const transactionFilterOptions = useMemo(() => selectTransactionFilterOptions(data), [data]);
  const transactionPresets = useMemo(() => selectTransactionPresets(), []);
  const importHealth = useMemo(() => selectImportHealth({ data, importRuns, years }), [data, importRuns, years]);
  const controlCalendarStats = useMemo(() => selectCalendarStats(calendar, controlTime?.day), [calendar, controlTime]);
  const controlTimeScope = useMemo(() => selectLocalTimeScope({ calendarStats: controlCalendarStats, time: controlTime }), [controlCalendarStats, controlTime]);
  const reportsTimeScope = useMemo(() => selectLocalTimeScope({ time: reportsTime }), [reportsTime]);
  const transactionsTimeScope = useMemo(() => selectLocalTimeScope({ time: transactionsTime }), [transactionsTime]);

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
      calendarStats: controlCalendarStats,
      scopedStats: emptyAnalytics(),
      filteredTransactions,
      visibleSpend,
      transactionPage,
      financialFlows,
      importHealth,
      fireSummary,
      views: buildSidebarNavigation(false),
    };
  }

  const kpis = data.kpis;
  const monthly = data.monthly.filter((row) => row.transactions > 0);
  const budgetMix = data.budgetMix.filter((row) => Math.abs(Number(row.sum)) > 0);
  const recurring = data.recurring;
  const latestYear = Math.max(...years.map((row) => Number(row.year)));
  const isHistorical = Number(data.year) < latestYear;
  const netIncomeRatio = Number(budgetSettings?.netIncomeRatio) > 0 ? Number(budgetSettings.netIncomeRatio) : NET_INCOME_RATIO;
  const netIncome = Number(kpis.income) * netIncomeRatio;
  const needsTarget = netIncome * 0.5;
  const savingsTarget = netIncome * 0.2;
  const wantsTarget = netIncome * 0.3;
  const needs = Number(budgetMix.find((row) => row.bucket === "Obowiązkowe stałe")?.sum || budgetMix.find((row) => row.bucket === "Potrzeby")?.sum || 0);
  const mixedNeeds = Number(budgetMix.find((row) => row.bucket === "Obowiązkowe zmienne")?.sum || budgetMix.find((row) => row.bucket === "Potrzeby mieszane")?.sum || 0);
  const wants = Number(budgetMix.find((row) => row.bucket === "Nieobowiązkowe")?.sum || budgetMix.find((row) => row.bucket === "Zachcianki")?.sum || 0);
  const plan = data.savingsPlan;
  const monthControl = data.monthControl;
  const safeToSpend = selectSafeToSpend({ monthControl, recurring });
  const planWarnings = selectPlanFeasibilityWarnings({ plan });
  const monthControlFinancialFlow = selectMonthFinancialFlow(data, monthControl?.monthKey);
  const savingsScenarioCut = Number(recommendedCuts?.realisticCut || planSummary.potentialMonthly || 0);
  const plannedSpendAfterCuts = Math.max(0, Number(plan.currentMonthlySpend) - savingsScenarioCut);
  const plannedInvestmentAfterCuts = Math.max(0, Number(plan.currentMonthlyIncome) - plannedSpendAfterCuts);
  const savingsWaterfall = selectSavingsWaterfall({ plan, planSummary, plannedSpendAfterCuts, plannedInvestmentAfterCuts, recommendedCuts });
  const savingsRadar = selectSavingsRadar(recommendedCuts);
  const categoryStatus = (monthControl?.categoryStatus || []).slice(0, 10);
  const scopedStats = reportsAnalytics || emptyAnalytics();
  const transactionStats = transactionsAnalytics || emptyAnalytics();
  const yearStats = reportsYearAnalytics || (reportsTimeScope.scope === "year" ? scopedStats : emptyAnalytics());
  const oneoffs = (scopedStats.oneoffs?.length ? scopedStats.oneoffs : data.largeOneoffs).slice(0, 14);
  const recurringCalendar = [...(data.recurring || [])]
    .filter((row) => row.avgDay)
    .sort((a, b) => a.avgDay - b.avgDay || Number(b.monthlyAverage) - Number(a.monthlyAverage));
  const monthDashboard = selectMonthDashboard({
    bucketOverrides,
    calendarStats: controlCalendarStats,
    financialFlowTotal: monthControlFinancialFlow,
    isHistorical,
    monthControl,
    planWarnings,
    primaryPlanRows,
    safeToSpend,
  });
  const spendingPlanSections = selectSpendingPlanSections({
    financialFlowTotal: monthControlFinancialFlow,
    monthControl,
    parentStatus: monthDashboard.categoryStatus,
    safeToSpend,
  });
  const reportsSections = selectReportsSections({
    activeTimeLabel: reportsTimeScope.activeTimeLabel,
    year: data.year,
    budgetMix,
    financialFlows,
    fixedness: data.fixedness,
    kpis,
    monthly,
    needs,
    mixedNeeds,
    oneoffs,
    needsTarget,
    savingsTarget,
    scopedStats,
    yearStats,
    savingsFocus,
    wants,
    wantsTarget,
  });
  const reportsWorkspace = selectReportsWorkspace(reportsSections);
  const recurringSummary = selectRecurringSummary({ monthControl, recurring, recurringCalendar });
  const dataQualityChart = selectDataQualityChart({ kpis, scopedStats: transactionStats });
  const wealthDashboard = selectWealthDashboard({ financialFlows, monthly, reportsSections });
  const activeTimeLabel = activeView === "transactions"
    ? transactionsTimeScope.activeTimeLabel
    : activeView === "reports"
      ? reportsTimeScope.activeTimeLabel
      : controlTimeScope.activeTimeLabel;
  const moduleHeader = selectModuleHeader({
    activeTimeLabel,
    data,
    financialFlows,
    importHealth,
    fireSummary,
    monthDashboard,
    plan,
    planSummary,
    reportsSections,
    transactionPage,
    view: activeView,
    visibleSpend,
    wealthDashboard,
  });

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
    calendarStats: controlCalendarStats,
    controlTimeScope,
    reportsTimeScope,
    transactionsTimeScope,
    scopedTransactions: filteredTransactions,
    drillFilteredTransactions: filteredTransactions,
    scopedStats,
    filteredTransactions,
    visibleSpend,
    transactionPage,
    financialFlows,
    importHealth,
    fireSummary,
    monthControlFinancialFlow,
    monthDashboard,
    spendingPlanSections,
    reportsSections,
    reportsWorkspace,
    recurringSummary,
    wealthDashboard,
    kpis,
    monthly,
    budgetMix,
    recurring,
    latestYear,
    isHistorical,
    savingsTarget,
    wantsTarget,
    needsTarget,
    netIncome,
    needs,
    mixedNeeds,
    wants,
    plan,
    monthControl,
    safeToSpend,
    planWarnings,
    plannedSpendAfterCuts,
    plannedInvestmentAfterCuts,
    savingsWaterfall,
    savingsRadar,
    categoryStatus,
    oneoffs,
    recurringCalendar,
    dataQualityChart,
    activeTimeLabel,
    moduleHeader,
    views: buildSidebarNavigation(isHistorical),
  };
}
