import { useEffect, useState } from "react";
import { fetchAnalytics, fetchCalendar, fetchTransactions } from "./api/budgetApi.js";
import { DashboardFooter } from "./components/layout/DashboardFooter.jsx";
import { DashboardHeader } from "./components/layout/DashboardHeader.jsx";
import { DashboardTabs } from "./components/layout/DashboardTabs.jsx";
import { GlobalTimeFilter } from "./components/layout/GlobalTimeFilter.jsx";
import { KpiStrip } from "./components/layout/KpiStrip.jsx";
import { StateScreen } from "./components/ui/StateScreen.jsx";
import { useBudgetData } from "./hooks/useBudgetData.js";
import { useDashboardModel } from "./hooks/useDashboardModel.js";
import { monthKeyFromLabel } from "./domain/budgetSelectors.js";
import { CategoriesView } from "./views/CategoriesView.jsx";
import { ImportView } from "./views/ImportView.jsx";
import { MonthControlView } from "./views/MonthControlView.jsx";
import { MonthlyStatsView } from "./views/MonthlyStatsView.jsx";
import { OverviewView } from "./views/OverviewView.jsx";
import { RecurringView } from "./views/RecurringView.jsx";
import { SavingsPlanView } from "./views/SavingsPlanView.jsx";
import { TransactionsView } from "./views/TransactionsView.jsx";
import "../styles.css";

export default function App() {
  const {
    years,
    year,
    setYear,
    data,
    status,
    uploading,
    importStatus,
    budgetSettings,
    settingsStatus,
    handleUpload,
    saveBudgetSettings,
  } = useBudgetData();
  const [query, setQuery] = useState("");
  const [bucket, setBucket] = useState("Wszystkie");
  const [customLimits, setCustomLimits] = useState({});
  const [settingsDraft, setSettingsDraft] = useState(null);
  const [view, setView] = useState("overview");
  const [selectedMonth, setSelectedMonth] = useState("");
  const [selectedDay, setSelectedDay] = useState("");
  const [timeScope, setTimeScope] = useState("all");
  const [drillFilter, setDrillFilter] = useState(null);
  const [analytics, setAnalytics] = useState(null);
  const [calendar, setCalendar] = useState(null);
  const [transactionPage, setTransactionPage] = useState(null);
  const [transactionPageIndex, setTransactionPageIndex] = useState(0);

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
    setAnalytics(null);
    setCalendar(null);
    setTransactionPage(null);
    setTransactionPageIndex(0);
  }, [year]);

  useEffect(() => {
    if (!budgetSettings) return;
    setSettingsDraft(budgetSettings);
    setCustomLimits(limitsToMap(budgetSettings.categoryLimits));
  }, [budgetSettings]);

  useEffect(() => {
    setTransactionPageIndex(0);
  }, [year, selectedMonth, selectedDay, timeScope, drillFilter, query, bucket]);

  useEffect(() => {
    if (!data || !year || !selectedMonth) return undefined;
    let cancelled = false;
    const month = monthKeyFromLabel(selectedMonth);
    fetchCalendar(year, month)
      .then((payload) => {
        if (!cancelled) setCalendar(payload);
      })
      .catch(() => {
        if (!cancelled) setCalendar(null);
      });
    return () => {
      cancelled = true;
    };
  }, [data, year, selectedMonth]);

  useEffect(() => {
    if (!data || !year) return undefined;
    let cancelled = false;
    const filters = apiFilters({ timeScope, selectedMonth, selectedDay, drillFilter });
    fetchAnalytics(year, filters)
      .then((payload) => {
        if (!cancelled) setAnalytics(payload);
      })
      .catch(() => {
        if (!cancelled) setAnalytics(null);
      });
    return () => {
      cancelled = true;
    };
  }, [data, year, timeScope, selectedMonth, selectedDay, drillFilter]);

  useEffect(() => {
    if (!data || !year) return undefined;
    let cancelled = false;
    const filters = {
      ...apiFilters({ timeScope, selectedMonth, selectedDay, drillFilter }),
      page: transactionPageIndex,
      size: 50,
      sort: "postedDate,desc",
      query,
      bucket,
    };
    fetchTransactions(year, filters)
      .then((payload) => {
        if (!cancelled) setTransactionPage(payload);
      })
      .catch(() => {
        if (!cancelled) setTransactionPage(null);
      });
    return () => {
      cancelled = true;
    };
  }, [data, year, timeScope, selectedMonth, selectedDay, drillFilter, query, bucket, transactionPageIndex]);

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
    analytics,
    calendar,
    transactionPage,
    customLimits,
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

  const categoryTop = model.scopedStats.categoryTop.slice(0, 10);
  const hierarchyTop = model.scopedStats.hierarchyTop.filter((row) => row.spend > 0).slice(0, 16);
  const planTitle = model.isHistorical ? "Symulacja oszczędności historycznych" : "Plan oszczędzania";

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

  function handleLimitChange(category, limit) {
    setCustomLimits((current) => ({
      ...current,
      [category]: limit,
    }));
    setSettingsDraft((current) => mergeLimit(current || budgetSettings, category, limit));
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
    const categoryLimits = Object.entries(customLimits).map(([category, limit]) => {
      const row = model.planRows.find((item) => item.category === category);
      return {
        category,
        limit: Number(limit || 0),
        action: row?.action || "",
      };
    });
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

      <KpiStrip activeMonths={data.activeMonths} kpis={model.kpis} wants={model.wants} />

      <DashboardTabs views={model.views} activeView={view} onViewChange={setView} />

      <GlobalTimeFilter
        activeTimeLabel={model.activeTimeLabel}
        calendarStats={model.calendarStats}
        drillFilter={drillFilter}
        monthly={model.monthly}
        selectedMonth={selectedMonth}
        timeScope={timeScope}
        onClearDrill={() => setDrillFilter(null)}
        onMonthChange={handleMonthChange}
        onSelectDay={(day) => {
          setSelectedDay(day);
          setTimeScope("day");
        }}
        onTimeScopeChange={handleTimeScopeChange}
      />

      {view === "overview" && (
        <OverviewView
          budgetMix={model.budgetMix}
          kpis={model.kpis}
          monthly={model.monthly}
          needs={model.needs}
          mixedNeeds={model.mixedNeeds}
          savingsTarget={model.savingsTarget}
          wants={model.wants}
          wantsTarget={model.wantsTarget}
        />
      )}

      {view === "month" && (
        <MonthControlView
          categoryStatus={model.categoryStatus}
          isHistorical={model.isHistorical}
          monthControl={model.monthControl}
        />
      )}

      {view === "monthlyStats" && (
        <MonthlyStatsView
          activeTimeLabel={model.activeTimeLabel}
          scopedStats={model.scopedStats}
          scopedTransactions={model.scopedTransactions}
        />
      )}

      {view === "plan" && (
        <SavingsPlanView
          data={data}
          isHistorical={model.isHistorical}
          plan={model.plan}
          planRows={model.planRows}
          planSummary={model.planSummary}
          planTitle={planTitle}
          plannedInvestmentAfterCuts={model.plannedInvestmentAfterCuts}
          plannedSpendAfterCuts={model.plannedSpendAfterCuts}
          settings={settingsDraft || budgetSettings}
          settingsStatus={settingsStatus}
          onSaveSettings={handleSaveSettings}
          onSettingChange={handleSettingChange}
          onLimitChange={handleLimitChange}
        />
      )}

      {view === "categories" && (
        <CategoriesView
          categoryTop={categoryTop}
          hierarchyTop={hierarchyTop}
          scopedStats={model.scopedStats}
          onDrill={setDrillFilter}
        />
      )}

      {view === "recurring" && (
        <RecurringView
          monthControl={model.monthControl}
          oneoffs={model.oneoffs}
          recurring={model.recurring}
          recurringCalendar={model.recurringCalendar}
        />
      )}

      {view === "transactions" && (
        <TransactionsView
          activeTimeLabel={model.activeTimeLabel}
          bucket={bucket}
          buckets={model.buckets}
          drillFilteredTransactions={model.drillFilteredTransactions}
          filteredTransactions={model.filteredTransactions}
          query={query}
          transactionPage={model.transactionPage}
          visibleSpend={model.visibleSpend}
          onBucketChange={setBucket}
          onPageChange={setTransactionPageIndex}
          onQueryChange={setQuery}
        />
      )}

      {view === "import" && (
        <ImportView onUpload={handleUpload} uploading={uploading} importStatus={importStatus} />
      )}

      <DashboardFooter />
    </main>
  );
}

function limitsToMap(categoryLimits = []) {
  return Object.fromEntries((categoryLimits || []).map((row) => [row.category, Number(row.limit || 0)]));
}

function mergeLimit(settings, category, limit) {
  const base = settings || {};
  const rows = [...(base.categoryLimits || [])];
  const index = rows.findIndex((row) => row.category === category);
  const next = { category, limit, action: rows[index]?.action || "" };
  if (index >= 0) {
    rows[index] = next;
  } else {
    rows.push(next);
  }
  return { ...base, categoryLimits: rows };
}

function apiFilters({ timeScope, selectedMonth, selectedDay, drillFilter }) {
  const month = monthKeyFromLabel(selectedMonth);
  const filters = {
    scope: timeScope === "all" ? "year" : timeScope,
  };
  if (timeScope !== "all" && month) {
    filters.month = month;
  }
  if (timeScope === "day" && month) {
    const day = String(selectedDay || "").padStart(2, "0");
    if (day.trim() && day !== "00") {
      filters.date = `${month}-${day}`;
    }
  }
  if (drillFilter?.type === "area") filters.area = drillFilter.value;
  if (drillFilter?.type === "group") filters.group = drillFilter.value;
  if (drillFilter?.type === "category") filters.category = drillFilter.value;
  if (drillFilter?.type === "subcategory") filters.subcategory = drillFilter.value;
  return filters;
}
