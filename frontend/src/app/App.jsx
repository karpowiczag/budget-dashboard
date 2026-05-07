import { useEffect, useMemo, useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { fetchAnalytics, fetchCalendar, fetchTransactions } from "./api/budgetApi.js";
import { budgetQueryKeys } from "./api/queryKeys.js";
import { DashboardFooter } from "./components/layout/DashboardFooter.jsx";
import { DashboardHeader } from "./components/layout/DashboardHeader.jsx";
import { DashboardTabs } from "./components/layout/DashboardTabs.jsx";
import { GlobalTimeFilter } from "./components/layout/GlobalTimeFilter.jsx";
import { KpiStrip } from "./components/layout/KpiStrip.jsx";
import { TransactionDrilldownModal } from "./components/tables/TransactionDrilldownModal.jsx";
import { StateScreen } from "./components/ui/StateScreen.jsx";
import { useBudgetData } from "./hooks/useBudgetData.js";
import { useDashboardModel } from "./hooks/useDashboardModel.js";
import { BUDGET_BUCKET_OPTIONS, limitKey, monthKeyFromLabel } from "./domain/budgetSelectors.js";
import { ImportView } from "./views/ImportView.jsx";
import { MonthControlView } from "./views/MonthControlView.jsx";
import { RecurringView } from "./views/RecurringView.jsx";
import { ReportsView } from "./views/ReportsView.jsx";
import { SavingsPlanView } from "./views/SavingsPlanView.jsx";
import { TransactionsView } from "./views/TransactionsView.jsx";
import "../styles.css";

export default function App() {
  const {
    years,
    year,
    setYear,
    data,
    importRuns,
    status,
    uploading,
    rebuilding,
    importStatus,
    budgetSettings,
    settingsStatus,
    handleUpload,
    handleRebuild,
    saveBudgetSettings,
  } = useBudgetData();
  const [query, setQuery] = useState("");
  const [bucket, setBucket] = useState("Wszystkie");
  const [transactionFilters, setTransactionFilters] = useState(defaultTransactionFilters);
  const [customLimits, setCustomLimits] = useState({});
  const [categoryBucketOverrides, setCategoryBucketOverrides] = useState({});
  const [settingsDraft, setSettingsDraft] = useState(null);
  const [view, setView] = useState("month");
  const [selectedMonth, setSelectedMonth] = useState("");
  const [selectedDay, setSelectedDay] = useState("");
  const [timeScope, setTimeScope] = useState("month");
  const [drillFilter, setDrillFilter] = useState(null);
  const [transactionPageIndex, setTransactionPageIndex] = useState(0);
  const [transactionInspector, setTransactionInspector] = useState(null);
  const [inspectorPageIndex, setInspectorPageIndex] = useState(0);

  useEffect(() => {
    if (!data?.monthly?.length) return;
    const months = data.monthly.filter((row) => row.transactions > 0);
    setSelectedMonth(months.at(-1)?.month || data.monthly[0].month);
  }, [data]);

  useEffect(() => {
    setSelectedDay("");
  }, [selectedMonth, year]);

  useEffect(() => {
    setDrillFilter(null);
    setTransactionPageIndex(0);
    setTransactionInspector(null);
    setInspectorPageIndex(0);
  }, [year]);

  useEffect(() => {
    if (!budgetSettings) return;
    setSettingsDraft(budgetSettings);
    setCustomLimits(limitsToMap(budgetSettings.categoryLimits));
    setCategoryBucketOverrides(bucketOverridesToMap(budgetSettings.categoryLimits));
  }, [budgetSettings]);

  useEffect(() => {
    setTransactionPageIndex(0);
  }, [year, selectedMonth, selectedDay, timeScope, drillFilter, query, bucket, transactionFilters]);

  const calendarMonth = useMemo(() => monthKeyFromLabel(selectedMonth), [selectedMonth]);
  const analyticsFilters = useMemo(
    () => apiFilters({ timeScope, selectedMonth, selectedDay, drillFilter }),
    [timeScope, selectedMonth, selectedDay, drillFilter],
  );
  const reportYearAnalyticsFilters = useMemo(() => ({ scope: "year" }), []);
  const transactionQueryFilters = useMemo(
    () => ({
      ...analyticsFilters,
      page: transactionPageIndex,
      size: transactionFilters.pageSize,
      sort: transactionFilters.sort,
      query,
      flow: transactionFilters.flow,
      bucket,
      area: transactionFilters.area,
      group: transactionFilters.group,
      category: transactionFilters.category,
      subcategory: transactionFilters.subcategory,
      fixedness: transactionFilters.fixedness,
      confidence: transactionFilters.confidence,
    }),
    [analyticsFilters, transactionPageIndex, transactionFilters, query, bucket],
  );
  const inspectorFilters = useMemo(() => {
    if (!transactionInspector) return null;
    const scopedFilters = transactionInspector.useTimeScope
      ? transactionFilterParams(apiFilters({ timeScope, selectedMonth, selectedDay, drillFilter: null }))
      : {};
    return {
      ...scopedFilters,
      ...(transactionInspector.filters || {}),
      page: inspectorPageIndex,
      size: 25,
      sort: transactionInspector.sort || "postedDate,desc",
    };
  }, [timeScope, selectedMonth, selectedDay, transactionInspector, inspectorPageIndex]);

  const calendarQuery = useQuery({
    queryKey: budgetQueryKeys.calendar(year, calendarMonth),
    queryFn: () => fetchCalendar(year, calendarMonth),
    enabled: !!data && !!year && !!calendarMonth,
  });
  const analyticsQuery = useQuery({
    queryKey: budgetQueryKeys.analytics(year, analyticsFilters),
    queryFn: () => fetchAnalytics(year, analyticsFilters),
    enabled: !!data && !!year,
  });
  const reportYearAnalyticsQuery = useQuery({
    queryKey: budgetQueryKeys.analytics(year, reportYearAnalyticsFilters),
    queryFn: () => fetchAnalytics(year, reportYearAnalyticsFilters),
    enabled: !!data && !!year && view === "reports" && timeScope !== "all",
  });
  const transactionsQuery = useQuery({
    queryKey: budgetQueryKeys.transactions(year, transactionQueryFilters),
    queryFn: () => fetchTransactions(year, transactionQueryFilters),
    enabled: !!data && !!year,
  });
  const inspectorQuery = useQuery({
    queryKey: budgetQueryKeys.transactions(year, inspectorFilters),
    queryFn: () => fetchTransactions(year, inspectorFilters),
    enabled: !!data && !!year && !!inspectorFilters,
  });

  const model = useDashboardModel({
    data,
    years,
    year,
    selectedMonth,
    selectedDay,
    timeScope,
    drillFilter,
    query,
    bucket,
    analytics: analyticsQuery.data || null,
    yearAnalytics: timeScope === "all" ? (analyticsQuery.data || null) : (reportYearAnalyticsQuery.data || null),
    calendar: calendarQuery.data || null,
    transactionPage: transactionsQuery.data || null,
    customLimits,
    bucketOverrides: categoryBucketOverrides,
    importRuns,
  });

  if (status === "loading") {
    return <StateScreen>Ładowanie danych budżetu...</StateScreen>;
  }

  if (status === "empty") {
    return (
      <main>
        <header className="topbar">
          <div>
            <p className="eyebrow">Budżet domowy</p>
            <h1>Import danych</h1>
            <span>Dodaj pierwszy eksport bankowy CSV, żeby zbudować dashboard.</span>
          </div>
        </header>
        <ImportView onUpload={handleUpload} uploading={uploading} importStatus={importStatus} />
      </main>
    );
  }

  if (status === "error" || !data) {
    return <StateScreen tone="error">Nie udało się wczytać danych z API Spring Boot.</StateScreen>;
  }

  const planTitle = model.isHistorical ? "Symulacja limitów" : "Plan i limity";
  const showGlobalTimeFilter = ["month", "reports", "transactions"].includes(view);
  const timeFilterVariant = view === "month" ? "full" : "compact";
  const timeFilterCopy = {
    month: {
      title: "Zakres kalendarza",
      description: "Steruje kalendarzem i drilldownem. Limity pokazują aktywny miesiąc planu.",
    },
    reports: {
      title: "Zakres analizy raportu",
      description: "Filtruje panele oznaczone jako Zakres. Trendy i model budżetu zostają roczne.",
    },
    transactions: {
      title: "Zakres transakcji",
      description: "Filtruje tabelę, audyt jakości i drilldowny transakcji.",
    },
  }[view];

  function handleMonthChange(nextMonth) {
    setSelectedMonth(nextMonth);
    setSelectedDay("");
    setTimeScope(timeScope === "all" || timeScope === "day" ? "month" : timeScope);
  }

  function handleTimeScopeChange(nextScope) {
    if (nextScope === "day") {
      const fallbackDay = selectedDay || model.calendarStats?.selected;
      if (!fallbackDay) {
        setTimeScope("month");
        return;
      }
      setSelectedDay(String(fallbackDay));
    }
    setTimeScope(nextScope);
  }

  function openTransactionInspector(config) {
    setTransactionInspector({
      title: config.title,
      subtitle: config.subtitle,
      filters: config.filters || {},
      sort: config.sort,
      useTimeScope: config.useTimeScope ?? true,
    });
    setInspectorPageIndex(0);
  }

  function handleTransactionFilterChange(field, value) {
    setTransactionFilters((current) => {
      const next = { ...current, [field]: value };
      if (field === "area") {
        next.group = "Wszystkie";
        next.category = "Wszystkie";
        next.subcategory = "Wszystkie";
      }
      if (field === "group") {
        next.category = "Wszystkie";
        next.subcategory = "Wszystkie";
      }
      if (field === "category") {
        next.subcategory = "Wszystkie";
      }
      return next;
    });
  }

  function resetTransactionFilters() {
    setQuery("");
    setBucket("Wszystkie");
    setTransactionFilters(defaultTransactionFilters);
  }

  function applyTransactionPreset(preset) {
    const filters = preset?.filters || {};
    const { bucket: presetBucket, ...transactionPreset } = filters;
    setQuery("");
    setBucket(presetBucket || "Wszystkie");
    setTransactionFilters({
      ...defaultTransactionFilters,
      ...transactionPreset,
      pageSize: transactionFilters.pageSize,
    });
  }

  function handleLimitChange(scope, name, limit) {
    const key = limitKey(scope, name);
    setCustomLimits((current) => ({
      ...current,
      [key]: limit,
    }));
    setSettingsDraft((current) => mergeLimit(current || budgetSettings, scope, name, limit));
  }

  function handleBucketOverrideChange(category, bucketOverride) {
    setCategoryBucketOverrides((current) => {
      const next = { ...current };
      if (bucketOverride) {
        next[category] = bucketOverride;
      } else {
        delete next[category];
      }
      return next;
    });
    setSettingsDraft((current) => mergeBucketOverride(current || budgetSettings, category, bucketOverride));
  }

  function handleSettingChange(field, value) {
    setSettingsDraft((current) => ({
      ...(current || budgetSettings || {}),
      [field]: value,
      categoryLimits: current?.categoryLimits || budgetSettings?.categoryLimits || [],
    }));
  }

  async function handleSaveSettings() {
    const base = settingsDraft || budgetSettings || {};
    const allLimitRows = [...(model.parentPlanRows || []), ...(model.planRows || [])];
    const limitRows = Object.entries(customLimits).map(([key, limit]) => {
      const [scope = "category", ...nameParts] = key.split(":");
      const name = nameParts.join(":");
      const row = allLimitRows.find((item) => limitKey(item) === key);
      return {
        scope,
        name,
        category: scope === "category" ? name : "",
        limit: Number(limit || 0),
        action: row?.action || "",
        bucketOverride: scope === "category" ? (categoryBucketOverrides[name] || "") : "",
      };
    });
    const categoryLimitsByKey = new Map(limitRows.map((row) => [limitKey(row), row]));
    Object.entries(categoryBucketOverrides).forEach(([category, bucketOverride]) => {
      const key = limitKey("category", category);
      const row = categoryLimitsByKey.get(key) || allLimitRows.find((item) => limitKey(item) === key);
      categoryLimitsByKey.set(key, {
        scope: "category",
        name: category,
        category,
        limit: Number(row?.limit || 0),
        action: row?.action || "",
        bucketOverride,
      });
    });
    const categoryLimits = Array.from(categoryLimitsByKey.values());
    const payload = {
      targetMonthlySpend: Number(base.targetMonthlySpend || data.savingsPlan.targetMonthlySpend),
      aggressiveMonthlySpend: Number(base.aggressiveMonthlySpend || data.savingsPlan.aggressiveMonthlySpend),
      emergencyFundMinMonths: Number(base.emergencyFundMinMonths || 3),
      emergencyFundComfortMonths: Number(base.emergencyFundComfortMonths || 6),
      categoryLimits,
    };
    try {
      setSettingsDraft(await saveBudgetSettings(payload));
    } catch {
      // Status is already surfaced by the data hook.
    }
  }

  return (
    <main>
      <DashboardHeader data={data} year={year} years={years} onYearChange={setYear} />

      <KpiStrip activeMonths={data.activeMonths} financialFlows={model.financialFlows} kpis={model.kpis} wants={model.wants} onInspect={openTransactionInspector} />

      <DashboardTabs views={model.views} activeView={view} onViewChange={setView} />

      {showGlobalTimeFilter && (
        <GlobalTimeFilter
          activeTimeLabel={model.activeTimeLabel}
          calendarStats={model.calendarStats}
          description={timeFilterCopy?.description}
          drillFilter={drillFilter}
          monthly={model.monthly}
          selectedMonth={selectedMonth}
          title={timeFilterCopy?.title}
          timeScope={timeScope}
          variant={timeFilterVariant}
          onClearDrill={() => setDrillFilter(null)}
          onMonthChange={handleMonthChange}
          onSelectDay={(day) => {
            setSelectedDay(day);
            setTimeScope("day");
          }}
          onTimeScopeChange={handleTimeScopeChange}
        />
      )}

      {view === "month" && (
        <MonthControlView
          monthDashboard={model.monthDashboard}
          savingsFocus={model.savingsFocus}
          onInspect={openTransactionInspector}
        />
      )}

      {view === "plan" && (
        <SavingsPlanView
          data={data}
          financialFlows={model.financialFlows}
          categoryExamples={model.categoryExamples}
          categorySubcategories={model.categorySubcategories}
          isHistorical={model.isHistorical}
          plan={model.plan}
          parentPlanRows={model.parentPlanRows}
          primaryPlanRows={model.primaryPlanRows}
          planRows={model.planRows}
          planSummary={model.planSummary}
          planTitle={planTitle}
          plannedInvestmentAfterCuts={model.plannedInvestmentAfterCuts}
          plannedSpendAfterCuts={model.plannedSpendAfterCuts}
          recommendedCuts={model.recommendedCuts}
          savingsRadar={model.savingsRadar}
          savingsWaterfall={model.savingsWaterfall}
          settings={settingsDraft || budgetSettings}
          settingsStatus={settingsStatus}
          bucketOptions={BUDGET_BUCKET_OPTIONS}
          onSaveSettings={handleSaveSettings}
          onSettingChange={handleSettingChange}
          onLimitChange={handleLimitChange}
          onBucketOverrideChange={handleBucketOverrideChange}
        />
      )}

      {view === "reports" && (
        <ReportsView
          reportsSections={model.reportsSections}
          onDrill={setDrillFilter}
          onInspect={openTransactionInspector}
        />
      )}

      {view === "recurring" && (
        <RecurringView
          recurringSummary={model.recurringSummary}
          onInspect={openTransactionInspector}
        />
      )}

      {view === "transactions" && (
        <TransactionsView
          activeTimeLabel={model.activeTimeLabel}
          bucket={bucket}
          buckets={model.buckets}
          drillFilteredTransactions={model.drillFilteredTransactions}
          filterOptions={model.transactionFilterOptions}
          filteredTransactions={model.filteredTransactions}
          query={query}
          presets={model.transactionPresets}
          dataQualityChart={model.dataQualityChart}
          transactionFilters={transactionFilters}
          transactionPage={model.transactionPage}
          visibleSpend={model.visibleSpend}
          yearTransactionTotal={data.kpis.transactions}
          onInspect={openTransactionInspector}
          onBucketChange={setBucket}
          onFilterChange={handleTransactionFilterChange}
          onPageChange={setTransactionPageIndex}
          onPreset={applyTransactionPreset}
          onQueryChange={setQuery}
          onResetFilters={resetTransactionFilters}
          onShowFullYear={() => {
            setSelectedDay("");
            setTimeScope("all");
            setDrillFilter(null);
            setTransactionPageIndex(0);
          }}
        />
      )}

      {view === "import" && (
        <ImportView
          activeYear={year}
          importHealth={model.importHealth}
          importRuns={importRuns}
          onRebuild={handleRebuild}
          onUpload={handleUpload}
          rebuilding={rebuilding}
          uploading={uploading}
          importStatus={importStatus}
        />
      )}

      {transactionInspector && (
        <TransactionDrilldownModal
          title={transactionInspector.title}
          subtitle={transactionInspector.subtitle || (transactionInspector.useTimeScope ? model.activeTimeLabel : `Cały ${year}`)}
          page={inspectorQuery.data}
          loading={inspectorQuery.isPending}
          error={inspectorQuery.error?.message || ""}
          onClose={() => setTransactionInspector(null)}
          onPageChange={setInspectorPageIndex}
        />
      )}

      <DashboardFooter />
    </main>
  );
}

function limitsToMap(categoryLimits = []) {
  return Object.fromEntries((categoryLimits || []).map((row) => [limitKey(row), Number(row.limit || 0)]));
}

function bucketOverridesToMap(categoryLimits = []) {
  return Object.fromEntries(
    (categoryLimits || [])
      .filter((row) => (row.scope || "category") === "category")
      .filter((row) => row.bucketOverride)
      .map((row) => [row.name || row.category, row.bucketOverride]),
  );
}

const defaultTransactionFilters = {
  flow: "Wszystkie",
  area: "Wszystkie",
  group: "Wszystkie",
  category: "Wszystkie",
  subcategory: "Wszystkie",
  fixedness: "Wszystkie",
  confidence: "Wszystkie",
  sort: "postedDate,desc",
  pageSize: 50,
};

function mergeLimit(settings, scope, name, limit) {
  const base = settings || {};
  const rows = [...(base.categoryLimits || [])];
  const key = limitKey(scope, name);
  const index = rows.findIndex((row) => limitKey(row) === key);
  const next = { scope, name, category: scope === "category" ? name : "", limit, action: rows[index]?.action || "", bucketOverride: rows[index]?.bucketOverride || "" };
  if (index >= 0) {
    rows[index] = next;
  } else {
    rows.push(next);
  }
  return { ...base, categoryLimits: rows };
}

function mergeBucketOverride(settings, category, bucketOverride) {
  const base = settings || {};
  const rows = [...(base.categoryLimits || [])];
  const key = limitKey("category", category);
  const index = rows.findIndex((row) => limitKey(row) === key);
  if (index >= 0) {
    rows[index] = { ...rows[index], scope: "category", name: category, category, bucketOverride };
  } else if (bucketOverride) {
    rows.push({ scope: "category", name: category, category, limit: 0, action: "", bucketOverride });
  }
  return { ...base, categoryLimits: rows };
}

export function apiFilters({ timeScope, selectedMonth, selectedDay, drillFilter }) {
  const month = monthKeyFromLabel(selectedMonth);
  let scope = timeScope === "all" ? "year" : timeScope;
  if ((scope === "month" || scope === "day") && !month) {
    scope = "year";
  }
  const day = String(selectedDay || "").padStart(2, "0");
  if (scope === "day" && (!day.trim() || day === "00")) {
    scope = "month";
  }
  const filters = {
    scope,
  };
  if (scope !== "year" && month) {
    filters.month = month;
  }
  if (scope === "day" && month) {
    filters.date = `${month}-${day}`;
  }
  if (drillFilter?.type === "area") filters.area = drillFilter.value;
  if (drillFilter?.type === "group") filters.group = drillFilter.value;
  if (drillFilter?.type === "category") filters.category = drillFilter.value;
  if (drillFilter?.type === "subcategory") filters.subcategory = drillFilter.value;
  return filters;
}

function transactionFilterParams(filters) {
  const { scope, ...params } = filters;
  return params;
}
