import { useEffect, useState } from "react";
import { DashboardFooter } from "./components/layout/DashboardFooter.jsx";
import { DashboardHeader } from "./components/layout/DashboardHeader.jsx";
import { DashboardTabs } from "./components/layout/DashboardTabs.jsx";
import { GlobalTimeFilter } from "./components/layout/GlobalTimeFilter.jsx";
import { KpiStrip } from "./components/layout/KpiStrip.jsx";
import { StateScreen } from "./components/ui/StateScreen.jsx";
import { useBudgetData } from "./hooks/useBudgetData.js";
import { useDashboardModel } from "./hooks/useDashboardModel.js";
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
  const { years, year, setYear, data, status, uploading, importStatus, handleUpload } = useBudgetData();
  const [query, setQuery] = useState("");
  const [bucket, setBucket] = useState("Wszystkie");
  const [customLimits, setCustomLimits] = useState({});
  const [view, setView] = useState("overview");
  const [selectedMonth, setSelectedMonth] = useState("");
  const [selectedDay, setSelectedDay] = useState("");
  const [timeScope, setTimeScope] = useState("all");
  const [drillFilter, setDrillFilter] = useState(null);

  useEffect(() => {
    if (!data?.monthly?.length) return;
    const months = data.monthly.filter((row) => row.transactions > 0);
    setSelectedMonth(months.at(-1)?.month || data.monthly[0].month);
  }, [data]);

  useEffect(() => {
    setSelectedDay("");
  }, [selectedMonth, year]);

  useEffect(() => {
    setCustomLimits({});
    setDrillFilter(null);
  }, [year]);

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
    setTimeScope(timeScope === "all" ? "month" : timeScope);
  }

  function handleLimitChange(category, limit) {
    setCustomLimits((current) => ({
      ...current,
      [category]: limit,
    }));
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
        onTimeScopeChange={setTimeScope}
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
          visibleSpend={model.visibleSpend}
          onBucketChange={setBucket}
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
