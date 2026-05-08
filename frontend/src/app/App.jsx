import { useEffect, useMemo, useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { fetchAnalytics, fetchCalendar, fetchTransactions } from "./api/budgetApi.js";
import { budgetQueryKeys } from "./api/queryKeys.js";
import { AppShell } from "./components/layout/AppShell.jsx";
import { DashboardFooter } from "./components/layout/DashboardFooter.jsx";
import { ModuleHeader } from "./components/layout/ModuleHeader.jsx";
import { SidebarNav } from "./components/layout/SidebarNav.jsx";
import { TimeScopeControl } from "./components/layout/TimeScopeControl.jsx";
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
import { WealthView } from "./views/WealthView.jsx";
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
  const [view, setView] = useState("control");
  const [localTimes, setLocalTimes] = useState({
    control: { scope: "month", month: "", day: "", drillFilter: null },
    reports: { scope: "year", month: "", day: "", drillFilter: null },
    transactions: { scope: "month", month: "", day: "", drillFilter: null },
  });
  const [transactionPageIndex, setTransactionPageIndex] = useState(0);
  const [transactionInspector, setTransactionInspector] = useState(null);
  const [inspectorPageIndex, setInspectorPageIndex] = useState(0);

  useEffect(() => {
    if (!data?.monthly?.length) return;
    const months = data.monthly.filter((row) => row.transactions > 0);
    const fallbackMonth = months.at(-1)?.month || data.monthly[0].month;
    setLocalTimes((current) => ({
      control: { ...current.control, month: current.control.month || fallbackMonth },
      reports: { ...current.reports, month: current.reports.month || fallbackMonth },
      transactions: { ...current.transactions, month: current.transactions.month || fallbackMonth },
    }));
  }, [data]);

  useEffect(() => {
    setLocalTimes((current) => ({
      control: { ...current.control, month: "", day: "", drillFilter: null },
      reports: { ...current.reports, month: "", day: "", drillFilter: null },
      transactions: { ...current.transactions, month: "", day: "", drillFilter: null },
    }));
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
  }, [year, localTimes.transactions, query, bucket, transactionFilters]);

  const controlMonth = useMemo(() => monthKeyFromLabel(localTimes.control.month), [localTimes.control.month]);
  const reportsFilters = useMemo(() => apiFilters(localTimes.reports), [localTimes.reports]);
  const transactionsTimeFilters = useMemo(() => apiFilters(localTimes.transactions), [localTimes.transactions]);
  const reportYearAnalyticsFilters = useMemo(() => ({ scope: "year" }), []);
  const transactionQueryFilters = useMemo(
    () => ({
      ...transactionsTimeFilters,
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
      reviewStatus: transactionFilters.reviewStatus,
    }),
    [transactionsTimeFilters, transactionPageIndex, transactionFilters, query, bucket],
  );
  const inspectorFilters = useMemo(() => {
    if (!transactionInspector) return null;
    const sourceTime = localTimes[transactionInspector.timeKey] || localTimes.transactions;
    const scopedFilters = transactionInspector.useTimeScope
      ? transactionFilterParams(apiFilters(sourceTime))
      : {};
    return {
      ...scopedFilters,
      ...(transactionInspector.filters || {}),
      page: inspectorPageIndex,
      size: 25,
      sort: transactionInspector.sort || "postedDate,desc",
    };
  }, [localTimes, transactionInspector, inspectorPageIndex]);

  const calendarQuery = useQuery({
    queryKey: budgetQueryKeys.calendar(year, controlMonth),
    queryFn: () => fetchCalendar(year, controlMonth),
    enabled: !!data && !!year && !!controlMonth,
  });
  const reportsAnalyticsQuery = useQuery({
    queryKey: budgetQueryKeys.analytics(year, reportsFilters),
    queryFn: () => fetchAnalytics(year, reportsFilters),
    enabled: !!data && !!year && view === "reports",
  });
  const reportYearAnalyticsQuery = useQuery({
    queryKey: budgetQueryKeys.analytics(year, reportYearAnalyticsFilters),
    queryFn: () => fetchAnalytics(year, reportYearAnalyticsFilters),
    enabled: !!data && !!year && view === "reports" && reportsFilters.scope !== "year",
  });
  const transactionsAnalyticsQuery = useQuery({
    queryKey: budgetQueryKeys.analytics(year, transactionsTimeFilters),
    queryFn: () => fetchAnalytics(year, transactionsTimeFilters),
    enabled: !!data && !!year && view === "transactions",
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
    activeView: view,
    controlTime: localTimes.control,
    data,
    years,
    reportsTime: localTimes.reports,
    transactionsTime: localTimes.transactions,
    reportsAnalytics: reportsAnalyticsQuery.data || null,
    reportsYearAnalytics: reportsFilters.scope === "year" ? (reportsAnalyticsQuery.data || null) : (reportYearAnalyticsQuery.data || null),
    transactionsAnalytics: transactionsAnalyticsQuery.data || null,
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

  function updateLocalTime(key, nextTime) {
    setLocalTimes((current) => ({
      ...current,
      [key]: normalizeTime(nextTime),
    }));
  }

  function clearLocalDrill(key) {
    setLocalTimes((current) => ({
      ...current,
      [key]: { ...current[key], drillFilter: null },
    }));
  }

  function openTransactionInspector(config) {
    setTransactionInspector({
      title: config.title,
      subtitle: config.subtitle,
      filters: config.filters || {},
      sort: config.sort,
      timeKey: config.timeKey || timeKeyForView(view),
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
    <AppShell
      sidebar={
        <SidebarNav
          activeView={view}
          data={data}
          year={year}
          years={years}
          views={model.views}
          onViewChange={setView}
          onYearChange={setYear}
        />
      }
    >
      <ModuleHeader header={model.moduleHeader} action={moduleTimeControl(view, model, localTimes, updateLocalTime, clearLocalDrill)} />

      {view === "control" && (
        <MonthControlView
          monthDashboard={model.monthDashboard}
          savingsFocus={model.savingsFocus}
          spendingPlanSections={model.spendingPlanSections}
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
          dataQualityChart={model.dataQualityChart}
          reportsSections={model.reportsSections}
          reportsWorkspace={model.reportsWorkspace}
          onInspect={openTransactionInspector}
        />
      )}

      {view === "wealth" && (
        <WealthView
          wealthDashboard={model.wealthDashboard}
          onInspect={openTransactionInspector}
        />
      )}

      {view === "obligations" && (
        <RecurringView
          recurringSummary={model.recurringSummary}
          onInspect={openTransactionInspector}
        />
      )}

      {view === "transactions" && (
        <TransactionsView
          activeTimeLabel={model.transactionsTimeScope.activeTimeLabel}
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
            updateLocalTime("transactions", { ...localTimes.transactions, scope: "year", day: "", drillFilter: null });
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
          subtitle={transactionInspector.subtitle || inspectorSubtitle(transactionInspector, model, year)}
          page={inspectorQuery.data}
          loading={inspectorQuery.isPending}
          error={inspectorQuery.error?.message || ""}
          onClose={() => setTransactionInspector(null)}
          onPageChange={setInspectorPageIndex}
        />
      )}

      <DashboardFooter />
    </AppShell>
  );
}

function moduleTimeControl(view, model, localTimes, updateLocalTime, clearLocalDrill) {
  const months = model.monthly || [];
  if (view === "control") {
    return (
      <TimeScopeControl
        calendarStats={model.calendarStats}
        chips={model.controlTimeScope.chips}
        months={months}
        time={localTimes.control}
        variant="full"
        onChange={(next) => updateLocalTime("control", next)}
        onClearDrill={() => clearLocalDrill("control")}
      />
    );
  }
  if (view === "reports") {
    return (
      <TimeScopeControl
        chips={model.reportsTimeScope.chips}
        months={months}
        time={localTimes.reports}
        onChange={(next) => updateLocalTime("reports", next)}
        onClearDrill={() => clearLocalDrill("reports")}
      />
    );
  }
  if (view === "transactions") {
    return (
      <TimeScopeControl
        chips={model.transactionsTimeScope.chips}
        months={months}
        time={localTimes.transactions}
        onChange={(next) => updateLocalTime("transactions", next)}
        onClearDrill={() => clearLocalDrill("transactions")}
      />
    );
  }
  return null;
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
  reviewStatus: "Wszystkie",
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

export function apiFilters(input) {
  const timeScope = input.timeScope || input.scope || "month";
  const selectedMonth = input.selectedMonth ?? input.month ?? "";
  const selectedDay = input.selectedDay ?? input.day ?? "";
  const drillFilter = input.drillFilter || null;
  const month = monthKeyFromLabel(selectedMonth);
  let scope = timeScope === "all" ? "year" : timeScope;
  if ((scope === "month" || scope === "day") && !month) {
    scope = "year";
  }
  const day = String(selectedDay || "").padStart(2, "0");
  if (scope === "day" && (!day.trim() || day === "00")) {
    scope = "month";
  }
  const filters = { scope };
  if (scope !== "year" && month) filters.month = month;
  if (scope === "day" && month) filters.date = `${month}-${day}`;
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

function normalizeTime(time) {
  return {
    scope: time?.scope === "all" ? "year" : (time?.scope || "month"),
    month: time?.month || "",
    day: time?.scope === "day" ? String(time?.day || "") : "",
    drillFilter: time?.drillFilter || null,
  };
}

function timeKeyForView(view) {
  if (view === "reports") return "reports";
  if (view === "transactions") return "transactions";
  return "control";
}

function inspectorSubtitle(inspector, model, year) {
  if (!inspector.useTimeScope) return `Cały ${year}`;
  if (inspector.timeKey === "reports") return model.reportsTimeScope.activeTimeLabel;
  if (inspector.timeKey === "transactions") return model.transactionsTimeScope.activeTimeLabel;
  return model.controlTimeScope.activeTimeLabel;
}
