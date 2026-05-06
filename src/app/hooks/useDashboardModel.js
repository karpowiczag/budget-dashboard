import { useMemo } from "react";
import {
  selectBuckets,
  selectCalendarStats,
  selectDrillFilteredTransactions,
  selectFilteredTransactions,
  selectMonthStats,
  selectPlanRows,
  selectPlanSummary,
  selectScopedStats,
  selectScopedTransactions,
  selectVisibleSpend,
} from "../domain/budgetSelectors.js";

export function useDashboardModel({
  data,
  years,
  year,
  selectedMonth,
  selectedDay,
  timeScope,
  drillFilter,
  query,
  bucket,
  customLimits,
}) {
  const buckets = useMemo(() => selectBuckets(data), [data]);
  const planRows = useMemo(() => selectPlanRows(data, customLimits), [data, customLimits]);
  const planSummary = useMemo(() => selectPlanSummary(planRows), [planRows]);
  const monthStats = useMemo(() => selectMonthStats(data, selectedMonth), [data, selectedMonth]);
  const calendarStats = useMemo(() => selectCalendarStats(monthStats, selectedDay), [monthStats, selectedDay]);
  const scopedTransactions = useMemo(
    () => selectScopedTransactions(data, timeScope, selectedMonth, selectedDay, calendarStats),
    [data, timeScope, selectedMonth, selectedDay, calendarStats],
  );
  const drillFilteredTransactions = useMemo(
    () => selectDrillFilteredTransactions(scopedTransactions, drillFilter),
    [scopedTransactions, drillFilter],
  );
  const scopedStats = useMemo(
    () => selectScopedStats(scopedTransactions, drillFilteredTransactions),
    [scopedTransactions, drillFilteredTransactions],
  );
  const filteredTransactions = useMemo(
    () => selectFilteredTransactions(drillFilteredTransactions, query, bucket),
    [drillFilteredTransactions, query, bucket],
  );
  const visibleSpend = useMemo(() => selectVisibleSpend(filteredTransactions), [filteredTransactions]);

  if (!data) {
    return {
      buckets,
      planRows,
      planSummary,
      monthStats,
      calendarStats,
      scopedTransactions,
      drillFilteredTransactions,
      scopedStats,
      filteredTransactions,
      visibleSpend,
    };
  }

  const kpis = data.kpis;
  const monthly = data.monthly.filter((row) => row.transactions > 0);
  const budgetMix = data.budgetMix.filter((row) => Math.abs(row.sum) > 0);
  const recurring = data.recurring.slice(0, 12);
  const latestYear = Math.max(...years.map((row) => Number(row.year)));
  const isHistorical = Number(data.year) < latestYear;
  const savingsTarget = kpis.income * 0.2;
  const wantsTarget = kpis.income * 0.3;
  const needs = budgetMix.find((row) => row.bucket === "Potrzeby")?.sum || 0;
  const mixedNeeds = budgetMix.find((row) => row.bucket === "Potrzeby mieszane")?.sum || 0;
  const wants = budgetMix.find((row) => row.bucket === "Zachcianki")?.sum || 0;
  const plan = data.savingsPlan;
  const monthControl = data.monthControl;
  const plannedSpendAfterCuts = Math.max(0, plan.currentMonthlySpend - planSummary.potentialMonthly);
  const plannedInvestmentAfterCuts = Math.max(0, plan.currentMonthlyIncome - plannedSpendAfterCuts);
  const categoryStatus = (monthControl?.categoryStatus || []).slice(0, 10);
  const oneoffs = (timeScope === "all" ? data.largeOneoffs : scopedStats.oneoffs).slice(0, 14);
  const recurringCalendar = [...(data.recurring || [])]
    .filter((row) => row.avgDay)
    .sort((a, b) => a.avgDay - b.avgDay || b.monthlyAverage - a.monthlyAverage)
    .slice(0, 12);
  const activeTimeLabel = buildActiveTimeLabel(timeScope, selectedMonth, calendarStats);

  return {
    buckets,
    planRows,
    planSummary,
    monthStats,
    calendarStats,
    scopedTransactions,
    drillFilteredTransactions,
    scopedStats,
    filteredTransactions,
    visibleSpend,
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
