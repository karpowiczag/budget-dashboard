import { useMemo } from "react";
import {
  emptyAnalytics,
  selectBuckets,
  selectCalendarStats,
  selectMonthStats,
  selectPlanRows,
  selectPlanSummary,
  selectVisibleSpend,
} from "../domain/budgetSelectors.js";

export function useDashboardModel({
  data,
  years,
  selectedMonth,
  selectedDay,
  timeScope,
  analytics,
  calendar,
  transactionPage,
  customLimits,
}) {
  const buckets = useMemo(() => selectBuckets(data), [data]);
  const planRows = useMemo(() => selectPlanRows(data, customLimits), [data, customLimits]);
  const planSummary = useMemo(() => selectPlanSummary(planRows), [planRows]);
  const monthStats = useMemo(() => selectMonthStats(data, selectedMonth), [data, selectedMonth]);
  const calendarStats = useMemo(() => selectCalendarStats(calendar, selectedDay), [calendar, selectedDay]);
  const scopedStats = useMemo(() => analytics || emptyAnalytics(), [analytics]);
  const filteredTransactions = useMemo(() => transactionPage?.items || [], [transactionPage]);
  const visibleSpend = useMemo(() => selectVisibleSpend(filteredTransactions), [filteredTransactions]);

  if (!data) {
    return {
      buckets,
      planRows,
      planSummary,
      monthStats,
      calendarStats,
      scopedStats,
      filteredTransactions,
      visibleSpend,
      transactionPage,
    };
  }

  const kpis = data.kpis;
  const monthly = data.monthly.filter((row) => row.transactions > 0);
  const budgetMix = data.budgetMix.filter((row) => Math.abs(Number(row.sum)) > 0);
  const recurring = data.recurring.slice(0, 12);
  const latestYear = Math.max(...years.map((row) => Number(row.year)));
  const isHistorical = Number(data.year) < latestYear;
  const savingsTarget = Number(kpis.income) * 0.2;
  const wantsTarget = Number(kpis.income) * 0.3;
  const needs = Number(budgetMix.find((row) => row.bucket === "Potrzeby")?.sum || 0);
  const mixedNeeds = Number(budgetMix.find((row) => row.bucket === "Potrzeby mieszane")?.sum || 0);
  const wants = Number(budgetMix.find((row) => row.bucket === "Zachcianki")?.sum || 0);
  const plan = data.savingsPlan;
  const monthControl = data.monthControl;
  const plannedSpendAfterCuts = Math.max(0, Number(plan.currentMonthlySpend) - planSummary.potentialMonthly);
  const plannedInvestmentAfterCuts = Math.max(0, Number(plan.currentMonthlyIncome) - plannedSpendAfterCuts);
  const categoryStatus = (monthControl?.categoryStatus || []).slice(0, 10);
  const oneoffs = (scopedStats.oneoffs?.length ? scopedStats.oneoffs : data.largeOneoffs).slice(0, 14);
  const recurringCalendar = [...(data.recurring || [])]
    .filter((row) => row.avgDay)
    .sort((a, b) => a.avgDay - b.avgDay || Number(b.monthlyAverage) - Number(a.monthlyAverage))
    .slice(0, 12);
  const activeTimeLabel = buildActiveTimeLabel(timeScope, selectedMonth, calendarStats);

  return {
    buckets,
    planRows,
    planSummary,
    monthStats,
    calendarStats,
    scopedTransactions: filteredTransactions,
    drillFilteredTransactions: filteredTransactions,
    scopedStats,
    filteredTransactions,
    visibleSpend,
    transactionPage,
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
    categoryStatus,
    oneoffs,
    recurringCalendar,
    activeTimeLabel,
    views: buildViews(isHistorical),
  };
}

function buildViews(isHistorical) {
  return [
    { id: "overview", label: "Podsumowanie" },
    { id: "plan", label: isHistorical ? "Symulacja" : "Plan" },
    { id: "month", label: "Miesiąc" },
    { id: "monthlyStats", label: "Statystyki" },
    { id: "categories", label: "Kategorie" },
    { id: "recurring", label: "Cykliczne" },
    { id: "transactions", label: "Transakcje" },
    { id: "import", label: "Import" },
  ];
}

function buildActiveTimeLabel(timeScope, selectedMonth, calendarStats) {
  if (timeScope === "all") return "Cały rok";
  if (timeScope === "month") return `Miesiąc ${selectedMonth}`;
  return `Dzień ${String(calendarStats?.selected || "").padStart(2, "0")}.${selectedMonth}`;
}
