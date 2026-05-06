import React, { useEffect, useMemo, useState } from "react";
import {
  AlertTriangle,
  ArrowDownRight,
  ArrowUpRight,
  Banknote,
  CalendarDays,
  ChartNoAxesCombined,
  CircleDollarSign,
  Download,
  Filter,
  Upload,
  Search,
  ShieldCheck,
  SlidersHorizontal,
  WalletCards,
} from "lucide-react";
import {
  Area,
  AreaChart,
  Bar,
  BarChart,
  CartesianGrid,
  Cell,
  Pie,
  PieChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";
import "../styles.css";

const PLN = new Intl.NumberFormat("pl-PL", {
  style: "currency",
  currency: "PLN",
  maximumFractionDigits: 0,
});

const PLN_DEC = new Intl.NumberFormat("pl-PL", {
  style: "currency",
  currency: "PLN",
  maximumFractionDigits: 2,
});

const PCT = new Intl.NumberFormat("pl-PL", {
  style: "percent",
  maximumFractionDigits: 1,
});

const COLORS = ["#0f766e", "#2563eb", "#b45309", "#9333ea", "#be123c", "#4d7c0f", "#475569"];

function money(value) {
  return PLN.format(Number(value || 0));
}

function moneyDec(value) {
  return PLN_DEC.format(Number(value || 0));
}

function percent(value) {
  return PCT.format(Number(value || 0));
}

function csrfHeaders() {
  const token = document.cookie
    .split("; ")
    .find((entry) => entry.startsWith("XSRF-TOKEN="))
    ?.split("=")[1];
  return token ? { "X-XSRF-TOKEN": decodeURIComponent(token) } : {};
}

function sortByAmount(rows, key = "spend") {
  return [...rows].sort((a, b) => Math.abs(b[key] || 0) - Math.abs(a[key] || 0));
}

function Kpi({ icon: Icon, label, value, detail, tone = "neutral" }) {
  return (
    <section className={`kpi ${tone}`}>
      <div className="kpiIcon" aria-hidden="true">
        <Icon size={18} />
      </div>
      <div>
        <p>{label}</p>
        <strong>{value}</strong>
        <span>{detail}</span>
      </div>
    </section>
  );
}

function Panel({ title, action, children, className = "" }) {
  return (
    <section className={`panel ${className}`}>
      <div className="panelHead">
        <h2>{title}</h2>
        {action}
      </div>
      {children}
    </section>
  );
}

function ImportView({ onUpload, uploading, importStatus }) {
  return (
    <Panel title="Import transakcji CSV">
      <div className="importBox">
        <div>
          <strong>Dodaj eksport bankowy</strong>
          <p>Plik CSV jest analizowany po stronie serwera i nie jest przechowywany po imporcie.</p>
        </div>
        <label className={`uploadButton ${uploading ? "disabled" : ""}`}>
          <Upload size={18} />
          <span>{uploading ? "Importuję..." : "Wybierz CSV"}</span>
          <input type="file" accept=".csv,text/csv" disabled={uploading} onChange={(event) => onUpload(event.target.files?.[0])} />
        </label>
      </div>
      {importStatus && <div className={`importStatus ${importStatus.type}`}>{importStatus.message}</div>}
    </Panel>
  );
}

export default function App() {
  const [years, setYears] = useState([]);
  const [year, setYear] = useState("");
  const [data, setData] = useState(null);
  const [status, setStatus] = useState("loading");
  const [uploading, setUploading] = useState(false);
  const [importStatus, setImportStatus] = useState(null);
  const [query, setQuery] = useState("");
  const [bucket, setBucket] = useState("Wszystkie");
  const [customLimits, setCustomLimits] = useState({});
  const [view, setView] = useState("overview");
  const [selectedMonth, setSelectedMonth] = useState("");
  const [selectedDay, setSelectedDay] = useState("");
  const [timeScope, setTimeScope] = useState("all");
  const [drillFilter, setDrillFilter] = useState(null);

  useEffect(() => {
    let cancelled = false;
    fetch("/api/years")
      .then((response) => {
        if (!response.ok) throw new Error("Nie mogę wczytać listy lat");
        return response.json();
      })
      .then((payload) => {
        if (cancelled) return;
        setYears(payload);
        const latest = payload.at(-1)?.year;
        if (latest) {
          setYear(String(latest));
        } else {
          setStatus("empty");
        }
      })
      .catch(() => {
        if (!cancelled) setStatus("error");
      });
    return () => {
      cancelled = true;
    };
  }, []);

  async function loadBudget(nextYear) {
    const response = await fetch(`/api/budget/${nextYear}`);
    if (!response.ok) throw new Error(`Nie mogę wczytać danych ${nextYear}`);
    const payload = await response.json();
    setData(payload);
    setStatus("ready");
  }

  async function handleUpload(file) {
    if (!file) return;
    setUploading(true);
    setImportStatus({ type: "info", message: "Importuję i kategoryzuję transakcje..." });
    try {
      const body = new FormData();
      body.append("file", file);
      const response = await fetch("/api/uploads", { method: "POST", body, headers: csrfHeaders() });
      const payload = await response.json();
      if (!response.ok) throw new Error(payload.error || "Import CSV nie powiódł się");
      const yearsResponse = await fetch("/api/years");
      const nextYears = await yearsResponse.json();
      setYears(nextYears);
      const importedYear = payload.years?.at(-1) || nextYears.at(-1)?.year;
      setImportStatus({ type: "success", message: `Zaimportowano ${payload.transactions} transakcji dla ${importedYear}.` });
      if (importedYear) {
        setYear(String(importedYear));
        await loadBudget(importedYear);
      }
    } catch (error) {
      setImportStatus({ type: "error", message: error.message });
      if (!data) setStatus("empty");
    } finally {
      setUploading(false);
    }
  }

  useEffect(() => {
    if (!year) return;
    let cancelled = false;
    setStatus("loading");
    fetch(`/api/budget/${year}`)
      .then((response) => {
        if (!response.ok) throw new Error(`Nie mogę wczytać danych ${year}`);
        return response.json();
      })
      .then((payload) => {
        if (!cancelled) {
          setData(payload);
          setStatus("ready");
        }
      })
      .catch(() => {
        if (!cancelled) setStatus("error");
      });
    return () => {
      cancelled = true;
    };
  }, [year]);

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

  const buckets = useMemo(() => {
    if (!data) return ["Wszystkie"];
    return ["Wszystkie", ...Array.from(new Set(data.transactions.map((tx) => tx["Koszyk budżetu"] || "Inne"))).sort()];
  }, [data]);

  const planRows = useMemo(() => {
    if (!data?.savingsPlan?.categoryLimits) return [];
    return data.savingsPlan.categoryLimits.slice(0, 14).map((row) => {
      const limit = Number(customLimits[row.category] ?? row.limit);
      const potentialMonthly = Math.max(0, Number(row.currentMonthly || 0) - limit);
      return {
        ...row,
        limit,
        potentialMonthly,
        potentialYearly: potentialMonthly * 12,
      };
    });
  }, [data, customLimits]);

  const planSummary = useMemo(() => {
    const potentialMonthly = planRows.reduce((sum, row) => sum + row.potentialMonthly, 0);
    return {
      potentialMonthly,
      potentialYearly: potentialMonthly * 12,
    };
  }, [planRows]);

  const monthStats = useMemo(() => {
    if (!data || !selectedMonth) return null;
    const monthKey = `${selectedMonth.slice(3, 7)}-${selectedMonth.slice(0, 2)}`;
    const transactions = data.transactions.filter((tx) => tx["Miesiąc"] === monthKey);
    const summary = data.monthly.find((row) => row.month === selectedMonth);
    const byCategory = new Map();
    const byBucket = new Map();
    const byMerchant = new Map();
    for (const tx of transactions) {
      const spend = Number(tx["Wydatek analizy"] || 0);
      if (!spend) continue;
      const category = tx["Kategoria skorygowana"] || "Inne";
      const bucketName = tx["Koszyk budżetu"] || "Inne";
      const merchant = tx.Sprzedawca || "Inne";
      byCategory.set(category, (byCategory.get(category) || 0) + spend);
      byBucket.set(bucketName, (byBucket.get(bucketName) || 0) + spend);
      byMerchant.set(merchant, (byMerchant.get(merchant) || 0) + spend);
    }
    const toRows = (map, keyName) =>
      [...map.entries()]
        .map(([name, spend]) => ({ [keyName]: name, spend }))
        .sort((a, b) => b.spend - a.spend);
    return {
      monthKey,
      summary,
      transactions,
      categories: toRows(byCategory, "category"),
      buckets: toRows(byBucket, "bucket"),
      merchants: toRows(byMerchant, "merchant"),
      oneoffs: transactions
        .filter((tx) => Number(tx["Wydatek analizy"] || 0) >= 500)
        .sort((a, b) => Number(b["Wydatek analizy"] || 0) - Number(a["Wydatek analizy"] || 0)),
    };
  }, [data, selectedMonth]);

  const calendarStats = useMemo(() => {
    if (!monthStats) return null;
    const [yearPart, monthPart] = monthStats.monthKey.split("-").map(Number);
    const daysInMonth = new Date(yearPart, monthPart, 0).getDate();
    const firstWeekday = new Date(yearPart, monthPart - 1, 1).getDay();
    const offset = (firstWeekday + 6) % 7;
    const byDay = new Map();
    for (const tx of monthStats.transactions) {
      const day = Number(tx.Data.slice(8, 10));
      if (!byDay.has(day)) byDay.set(day, []);
      byDay.get(day).push(tx);
    }
    const cells = [];
    for (let i = 0; i < offset; i += 1) cells.push({ empty: true, key: `empty-${i}` });
    for (let day = 1; day <= daysInMonth; day += 1) {
      const transactions = byDay.get(day) || [];
      const spend = transactions.reduce((sum, tx) => sum + Number(tx["Wydatek analizy"] || 0), 0);
      const income = transactions.reduce((sum, tx) => sum + Number(tx.Wpływ || 0), 0);
      const biggest = transactions
        .filter((tx) => Number(tx["Wydatek analizy"] || 0) > 0)
        .sort((a, b) => Number(b["Wydatek analizy"] || 0) - Number(a["Wydatek analizy"] || 0))[0];
      cells.push({ day, transactions, spend, income, biggest, key: `day-${day}` });
    }
    const selected = Number(selectedDay) || [...byDay.keys()].sort((a, b) => b - a)[0] || 1;
    const selectedTransactions = byDay.get(selected) || [];
    const byCategory = new Map();
    for (const tx of selectedTransactions) {
      const spend = Number(tx["Wydatek analizy"] || 0);
      if (!spend) continue;
      const cat = tx["Kategoria skorygowana"] || "Inne";
      byCategory.set(cat, (byCategory.get(cat) || 0) + spend);
    }
    return {
      monthKey: monthStats.monthKey,
      cells,
      selected,
      selectedTransactions,
      selectedSpend: selectedTransactions.reduce((sum, tx) => sum + Number(tx["Wydatek analizy"] || 0), 0),
      selectedIncome: selectedTransactions.reduce((sum, tx) => sum + Number(tx.Wpływ || 0), 0),
      selectedCategories: [...byCategory.entries()].map(([category, spend]) => ({ category, spend })).sort((a, b) => b.spend - a.spend),
    };
  }, [monthStats, selectedDay]);

  const scopedTransactions = useMemo(() => {
    if (!data) return [];
    if (timeScope === "all" || !selectedMonth) return data.transactions;
    const monthKey = `${selectedMonth.slice(3, 7)}-${selectedMonth.slice(0, 2)}`;
    if (timeScope === "month") {
      return data.transactions.filter((tx) => tx["Miesiąc"] === monthKey);
    }
    const day = String(calendarStats?.selected || selectedDay || "").padStart(2, "0");
    if (!day.trim()) return data.transactions.filter((tx) => tx["Miesiąc"] === monthKey);
    return data.transactions.filter((tx) => tx["Miesiąc"] === monthKey && tx.Data.endsWith(`-${day}`));
  }, [data, timeScope, selectedMonth, selectedDay, calendarStats]);

  const drillFilteredTransactions = useMemo(() => {
    if (!drillFilter) return scopedTransactions;
    return scopedTransactions.filter((tx) => {
      if (drillFilter.type === "area") return tx["Obszar budżetu"] === drillFilter.value;
      if (drillFilter.type === "group") return tx.Grupa === drillFilter.value;
      if (drillFilter.type === "category") return tx["Kategoria skorygowana"] === drillFilter.value;
      if (drillFilter.type === "subcategory") return `${tx["Kategoria skorygowana"]} · ${tx.Podkategoria}` === drillFilter.value;
      return true;
    });
  }, [scopedTransactions, drillFilter]);

  const scopedStats = useMemo(() => {
    const byArea = new Map();
    const byGroup = new Map();
    const byCategory = new Map();
    const bySubcategory = new Map();
    const byHierarchy = new Map();
    const byMerchant = new Map();
    let spend = 0;
    let income = 0;
    for (const tx of drillFilteredTransactions) {
      const txSpend = Number(tx["Wydatek analizy"] || 0);
      const txIncome = Number(tx.Wpływ || 0);
      spend += txSpend;
      income += txIncome;
      if (!txSpend) continue;
      const cat = tx["Kategoria skorygowana"] || "Inne";
      const area = tx["Obszar budżetu"] || "Inne";
      const group = tx.Grupa || "Inne";
      const subcategory = tx.Podkategoria || "Ogólne";
      const merchant = tx.Sprzedawca || "Inne";
      const hKey = `${area}|${group}|${cat}|${subcategory}`;
      byArea.set(area, (byArea.get(area) || 0) + txSpend);
      byGroup.set(group, (byGroup.get(group) || 0) + txSpend);
      byCategory.set(cat, (byCategory.get(cat) || 0) + txSpend);
      bySubcategory.set(`${cat} · ${subcategory}`, (bySubcategory.get(`${cat} · ${subcategory}`) || 0) + txSpend);
      byMerchant.set(merchant, (byMerchant.get(merchant) || 0) + txSpend);
      if (!byHierarchy.has(hKey)) {
        byHierarchy.set(hKey, {
          area,
          group,
          category: cat,
          subcategory,
          spend: 0,
          count: 0,
        });
      }
      const entry = byHierarchy.get(hKey);
      entry.spend += txSpend;
      entry.count += 1;
    }
    const rows = (map, key) => [...map.entries()].map(([name, spend]) => ({ [key]: name, spend })).sort((a, b) => b.spend - a.spend);
    const areaTop = rows(byArea, "area");
    const groupTop = rows(byGroup, "group");
    const categoryTop = rows(byCategory, "category");
    const subcategoryTop = rows(bySubcategory, "subcategory");
    const hierarchyTop = [...byHierarchy.values()].sort((a, b) => b.spend - a.spend);
    const merchants = [...byMerchant.entries()].map(([merchant, sum]) => ({ merchant, sum })).sort((a, b) => b.sum - a.sum);
    const oneoffs = scopedTransactions
      .filter((tx) => Number(tx["Wydatek analizy"] || 0) >= 500)
      .sort((a, b) => Number(b["Wydatek analizy"] || 0) - Number(a["Wydatek analizy"] || 0));
    return { spend, income, areaTop, groupTop, categoryTop, subcategoryTop, hierarchyTop, merchants, oneoffs };
  }, [drillFilteredTransactions]);

  const filteredTransactions = useMemo(() => {
    const needle = query.trim().toLowerCase();
    return drillFilteredTransactions
      .filter((tx) => bucket === "Wszystkie" || tx["Koszyk budżetu"] === bucket)
      .filter((tx) => {
        if (!needle) return true;
        return `${tx.Sprzedawca} ${tx.Opis} ${tx["Kategoria skorygowana"]} ${tx.Podkategoria}`
          .toLowerCase()
          .includes(needle);
      })
      .slice(0, 220);
  }, [drillFilteredTransactions, query, bucket]);

  const visibleSpend = useMemo(
    () => filteredTransactions.reduce((sum, tx) => sum + Number(tx["Wydatek analizy"] || 0), 0),
    [filteredTransactions],
  );

  if (status === "loading") {
    return <main className="state">Ładowanie danych budżetu...</main>;
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
    return <main className="state error">Nie udało się wczytać danych z API Spring Boot.</main>;
  }

  const k = data.kpis;
  const monthly = data.monthly.filter((row) => row.transactions > 0);
  const categoryTop = scopedStats.categoryTop.slice(0, 10);
  const hierarchyTop = scopedStats.hierarchyTop.filter((row) => row.spend > 0).slice(0, 16);
  const mix = data.budgetMix.filter((row) => Math.abs(row.sum) > 0);
  const recurring = data.recurring.slice(0, 12);
  const oneoffs = (timeScope === "all" ? data.largeOneoffs : scopedStats.oneoffs).slice(0, 14);
  const savingsTarget = k.income * 0.2;
  const wantsTarget = k.income * 0.3;
  const needs = mix.find((row) => row.bucket === "Potrzeby")?.sum || 0;
  const mixedNeeds = mix.find((row) => row.bucket === "Potrzeby mieszane")?.sum || 0;
  const wants = mix.find((row) => row.bucket === "Zachcianki")?.sum || 0;
  const plan = data.savingsPlan;
  const monthControl = data.monthControl;
  const latestYear = Math.max(...years.map((row) => Number(row.year)));
  const isHistorical = Number(data.year) < latestYear;
  const planTitle = isHistorical ? "Symulacja oszczędności historycznych" : "Plan oszczędzania";
  const plannedSpendAfterCuts = Math.max(0, plan.currentMonthlySpend - planSummary.potentialMonthly);
  const plannedInvestmentAfterCuts = Math.max(0, plan.currentMonthlyIncome - plannedSpendAfterCuts);
  const categoryStatus = (monthControl?.categoryStatus || []).slice(0, 10);
  const recurringCalendar = [...(data.recurring || [])]
    .filter((row) => row.avgDay)
    .sort((a, b) => a.avgDay - b.avgDay || b.monthlyAverage - a.monthlyAverage)
    .slice(0, 12);
  const views = [
    { id: "overview", label: "Podsumowanie" },
    { id: "plan", label: isHistorical ? "Symulacja" : "Plan" },
    { id: "month", label: "Miesiąc" },
    { id: "monthlyStats", label: "Statystyki" },
    { id: "categories", label: "Kategorie" },
    { id: "recurring", label: "Cykliczne" },
    { id: "transactions", label: "Transakcje" },
    { id: "import", label: "Import" },
  ];
  const activeTimeLabel =
    timeScope === "all"
      ? "Cały rok"
      : timeScope === "month"
        ? `Miesiąc ${selectedMonth}`
        : `Dzień ${String(calendarStats?.selected || "").padStart(2, "0")}.${selectedMonth}`;

  return (
    <main>
      <header className="topbar">
        <div>
          <p className="eyebrow">Budżet domowy</p>
          <h1>Analiza {data.year}</h1>
          <span>{data.period}</span>
        </div>
        <div className="controls">
          <div className="segmented" aria-label="Wybór roku">
            {years.map((option) => (
              <button key={option.year} className={year === String(option.year) ? "active" : ""} onClick={() => setYear(String(option.year))}>
                {option.year}
              </button>
            ))}
          </div>
          <a className="iconButton" href={`/api/budget/${year}`} title="Otwórz JSON API">
            <Download size={18} />
          </a>
        </div>
      </header>

      <section className="kpiGrid">
        <Kpi icon={Banknote} label="Dochód" value={money(k.income)} detail="tylko rozpoznane pensje" tone="good" />
        <Kpi icon={WalletCards} label="Wydatki analizowane" value={money(k.spend)} detail={`${money(k.spend / data.activeMonths)} / mies.`} />
        <Kpi icon={ArrowUpRight} label="Nadwyżka operacyjna" value={money(k.operatingSurplus)} detail={percent(k.savingsRate)} tone="good" />
        <Kpi icon={ShieldCheck} label="Inwestycje/nadpłaty" value={money(k.realSavingsOutgoing)} detail={`${money(k.realSavingsOutgoing / data.activeMonths)} / mies.`} tone="good" />
        <Kpi icon={AlertTriangle} label="Do sprawdzenia" value={money(k.toCheckAmount)} detail={`${k.toCheck} transakcji`} tone={k.toCheckAmount ? "warn" : "good"} />
        <Kpi icon={ArrowDownRight} label="Zachcianki" value={money(wants)} detail={`${money(wants / data.activeMonths)} / mies.`} tone="warn" />
      </section>

      <nav className="tabs" aria-label="Widoki dashboardu">
        {views.map((item) => (
          <button key={item.id} className={view === item.id ? "active" : ""} onClick={() => setView(item.id)}>
            {item.label}
          </button>
        ))}
      </nav>

      <section className="globalTime">
        <div className="timeHead">
          <div>
            <strong>Globalny filtr czasu</strong>
            <span>
              {activeTimeLabel}
            </span>
          </div>
          <div className="timeControls">
            <div className="segmented compact" aria-label="Zakres czasu">
              <button className={timeScope === "all" ? "active" : ""} onClick={() => setTimeScope("all")}>Cały rok</button>
              <button className={timeScope === "month" ? "active" : ""} onClick={() => setTimeScope("month")}>Wybrany miesiąc</button>
              <button className={timeScope === "day" ? "active" : ""} onClick={() => setTimeScope("day")}>Wybrany dzień</button>
            </div>
            <label className="select">
              <CalendarDays size={16} />
              <select value={selectedMonth} onChange={(event) => { setSelectedMonth(event.target.value); setTimeScope(timeScope === "all" ? "month" : timeScope); }}>
                {monthly.map((row) => (
                  <option key={row.month} value={row.month}>{row.month}</option>
                ))}
              </select>
            </label>
          </div>
        </div>
        {calendarStats && timeScope !== "all" && (
          <div className="globalCalendar">
            <div className="calendarWeekdays">
              {["Pon", "Wt", "Śr", "Czw", "Pt", "Sob", "Nd"].map((day) => (
                <span key={day}>{day}</span>
              ))}
            </div>
            <div className="calendarGrid compactCalendar">
              {calendarStats.cells.map((cell) =>
                cell.empty ? (
                  <div className="calendarCell empty" key={cell.key} />
                ) : (
                  <button
                    key={cell.key}
                    className={`calendarCell ${calendarStats.selected === cell.day ? "active" : ""} ${cell.spend >= 1000 ? "hot" : cell.spend >= 300 ? "warm" : ""}`}
                    onClick={() => { setSelectedDay(String(cell.day)); setTimeScope("day"); }}
                  >
                    <span>{cell.day}</span>
                    <strong>{cell.spend ? money(cell.spend) : ""}</strong>
                    <em>{cell.transactions.length ? `${cell.transactions.length} tx` : ""}</em>
                  </button>
                ),
              )}
            </div>
          </div>
        )}
        {drillFilter && (
          <div className="activeDrill">
            <span>Filtr: {drillFilter.label}</span>
            <button onClick={() => setDrillFilter(null)}>Wyczyść</button>
          </div>
        )}
      </section>

      {view === "overview" && (
        <section className="viewStack">
          <section className="gridTwo">
            <Panel title="Cashflow miesięczny">
              <div className="chart">
                <ResponsiveContainer width="100%" height="100%">
                  <AreaChart data={monthly} margin={{ top: 12, right: 18, left: 0, bottom: 0 }}>
                    <defs>
                      <linearGradient id="spend" x1="0" y1="0" x2="0" y2="1">
                        <stop offset="5%" stopColor="#2563eb" stopOpacity={0.32} />
                        <stop offset="95%" stopColor="#2563eb" stopOpacity={0.03} />
                      </linearGradient>
                      <linearGradient id="income" x1="0" y1="0" x2="0" y2="1">
                        <stop offset="5%" stopColor="#0f766e" stopOpacity={0.28} />
                        <stop offset="95%" stopColor="#0f766e" stopOpacity={0.02} />
                      </linearGradient>
                    </defs>
                    <CartesianGrid stroke="#e5e7eb" vertical={false} />
                    <XAxis dataKey="month" tick={{ fontSize: 12 }} />
                    <YAxis tickFormatter={(v) => `${Math.round(v / 1000)}k`} tick={{ fontSize: 12 }} />
                    <Tooltip formatter={(v) => moneyDec(v)} />
                    <Area type="monotone" dataKey="income" name="Wpływy" stroke="#0f766e" fill="url(#income)" strokeWidth={2} isAnimationActive={false} />
                    <Area type="monotone" dataKey="spend" name="Wydatki" stroke="#2563eb" fill="url(#spend)" strokeWidth={2} isAnimationActive={false} />
                  </AreaChart>
                </ResponsiveContainer>
              </div>
            </Panel>

            <Panel title="Model budżetu">
              <div className="mixList mixListFull">
                {mix.map((row, index) => (
                  <div className="mixRow" key={row.bucket}>
                    <i style={{ background: COLORS[index % COLORS.length] }} />
                    <span>{row.bucket}</span>
                    <strong>{money(row.sum)}</strong>
                    <em>{percent(row.incomeShare)}</em>
                  </div>
                ))}
              </div>
            </Panel>
          </section>

          <Panel title="Benchmark i presja na budżet">
            <div className="benchmark">
              <div>
                <span>Potrzeby + mieszane</span>
                <strong>{money(needs + mixedNeeds)}</strong>
                <p>limit 50%: {money(k.income * 0.5)}</p>
              </div>
              <div>
                <span>Zachcianki</span>
                <strong>{money(wants)}</strong>
                <p>punkt 30%: {money(wantsTarget)}</p>
              </div>
              <div>
                <span>Oszczędzanie operacyjne</span>
                <strong>{money(k.operatingSurplus)}</strong>
                <p>minimum 20%: {money(savingsTarget)}</p>
              </div>
              <div>
                <span>Nadwyżka po inwestycjach</span>
                <strong>{money(k.unassignedSurplus)}</strong>
                <p>do decyzji lub dalszego inwestowania</p>
              </div>
            </div>
          </Panel>
        </section>
      )}

      {view === "month" && (
        <Panel title={isHistorical ? "Kontrola ostatniego miesiąca" : "Do wydania i prognoza miesiąca"}>
        <div className="monthControl">
          <div className="monthCards">
            <div>
              <span>{monthControl.month}</span>
              <strong>{money(monthControl.spendToDate)}</strong>
              <p>wydane po {monthControl.elapsedDays} dniach</p>
            </div>
            <div>
              <span>{isHistorical ? "Wydatki miesiąca" : "Prognoza końca miesiąca"}</span>
              <strong>{money(monthControl.projectedSpend)}</strong>
              <p>{monthControl.projectedDelta >= 0 ? "pod targetem" : "ponad target"}: {money(Math.abs(monthControl.projectedDelta))}</p>
            </div>
            <div>
              <span>{isHistorical ? "Różnica do targetu" : "Do wydania"}</span>
              <strong>{money(monthControl.remainingBudget)}</strong>
              <p>{isHistorical ? "po faktycznych wydatkach" : `${money(monthControl.dailyAllowed)} dziennie`}</p>
            </div>
            <div>
              <span>Dochód miesiąca</span>
              <strong>{money(monthControl.incomeToDate)}</strong>
              <p>rozpoznane pensje</p>
            </div>
          </div>

          <div className="insightGrid">
            <div className="alerts">
              <h3>Alerty</h3>
              {(monthControl.alerts || []).length ? (
                monthControl.alerts.slice(0, 5).map((alert) => (
                  <div className={`alert ${alert.severity}`} key={`${alert.type}-${alert.message}`}>
                    <strong>{alert.type}</strong>
                    <span>{alert.message}</span>
                  </div>
                ))
              ) : (
                <div className="empty">Brak ostrych alertów dla wybranego okresu.</div>
              )}
            </div>

            <div className="limitProgress">
              <h3>Status limitów</h3>
              {categoryStatus.map((row) => {
                const pct = Math.min(1.35, Math.max(0, row.currentMonthProjection / row.limit || 0));
                return (
                  <div className="progressRow" key={row.category}>
                    <div>
                      <span>{row.category}</span>
                      <em>{money(row.currentMonthProjection)} / {money(row.limit)}</em>
                    </div>
                    <div className="barTrack">
                      <i className={pct > 1 ? "over" : pct > 0.8 ? "warn" : ""} style={{ width: `${Math.min(100, pct * 100)}%` }} />
                    </div>
                  </div>
                );
              })}
            </div>
          </div>
        </div>
        </Panel>
      )}

      {view === "monthlyStats" && (
        <section className="viewStack">
          <Panel title={`Statystyki: ${activeTimeLabel}`}>
            <div className="monthCards">
              <div>
                <span>Wydatki</span>
                <strong>{money(scopedStats.spend)}</strong>
                <p>{scopedTransactions.length} transakcji w zakresie</p>
              </div>
              <div>
                <span>Wpływy</span>
                <strong>{money(scopedStats.income)}</strong>
                <p>rozpoznane pensje</p>
              </div>
              <div>
                <span>Nadwyżka</span>
                <strong>{money(scopedStats.income - scopedStats.spend)}</strong>
                <p>{scopedStats.income ? percent((scopedStats.income - scopedStats.spend) / scopedStats.income) : "brak wpływów"}</p>
              </div>
              <div>
                <span>Duże wydatki</span>
                <strong>{scopedStats.oneoffs.length}</strong>
                <p>wydatki {"≥"} 500 zł</p>
              </div>
            </div>
          </Panel>

          <section className="gridTwo">
            <Panel title="Kategorie w zakresie">
              <div className="chart compact">
                <ResponsiveContainer width="100%" height="100%">
                  <BarChart data={scopedStats.categoryTop.slice(0, 10)} layout="vertical" margin={{ top: 6, right: 18, left: 94, bottom: 4 }}>
                    <CartesianGrid stroke="#e5e7eb" horizontal={false} />
                    <XAxis type="number" tickFormatter={(v) => `${Math.round(v / 1000)}k`} />
                    <YAxis dataKey="category" type="category" width={126} tick={{ fontSize: 12 }} />
                    <Tooltip formatter={(v) => moneyDec(v)} />
                    <Bar dataKey="spend" name="Wydatki" fill="#0f766e" radius={[0, 4, 4, 0]} isAnimationActive={false} />
                  </BarChart>
                </ResponsiveContainer>
              </div>
            </Panel>

            <Panel title="Top kategorie">
              <div className="tableWrap smallRows">
                <table>
                  <thead>
                    <tr>
                      <th>Kategoria</th>
                      <th>Wydatki</th>
                      <th>Udział</th>
                    </tr>
                  </thead>
                  <tbody>
                    {scopedStats.categoryTop.slice(0, 12).map((row) => (
                      <tr key={row.category}>
                        <td>{row.category}</td>
                        <td className="num">{money(row.spend)}</td>
                        <td className="num">{percent(row.spend / (scopedStats.spend || 1))}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </Panel>
          </section>

          <section className="gridTwo">
            <Panel title="Top sprzedawcy">
              <div className="tableWrap smallRows">
                <table>
                  <thead>
                    <tr>
                      <th>Sprzedawca</th>
                      <th>Wydatki</th>
                    </tr>
                  </thead>
                  <tbody>
                    {scopedStats.merchants.slice(0, 12).map((row) => (
                      <tr key={row.merchant}>
                        <td>{row.merchant}</td>
                        <td className="num">{money(row.sum)}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </Panel>

            <Panel title="Duże wydatki">
              <div className="tableWrap smallRows">
                <table>
                  <thead>
                    <tr>
                      <th>Data</th>
                      <th>Sprzedawca</th>
                      <th>Kategoria</th>
                      <th>Kwota</th>
                    </tr>
                  </thead>
                  <tbody>
                    {scopedStats.oneoffs.slice(0, 12).map((tx) => (
                      <tr key={tx.Lp}>
                        <td>{tx.Data}</td>
                        <td>{tx.Sprzedawca}</td>
                        <td>{tx["Kategoria skorygowana"]}</td>
                        <td className="num">{money(tx["Wydatek analizy"])}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </Panel>
          </section>

          <Panel title={`Transakcje: ${activeTimeLabel}`}>
            <div className="tableWrap transactions">
              <table>
                <thead>
                  <tr>
                    <th>Data</th>
                    <th>Sprzedawca</th>
                    <th>Kategoria</th>
                    <th>Podkategoria</th>
                    <th>Koszyk</th>
                    <th>Kwota</th>
                    <th>Pewność</th>
                  </tr>
                </thead>
                <tbody>
                  {scopedTransactions.slice(0, 160).map((tx) => (
                    <tr key={tx.Lp}>
                      <td>{tx.Data}</td>
                      <td>{tx.Sprzedawca}</td>
                      <td>{tx["Kategoria skorygowana"]}</td>
                      <td>{tx.Podkategoria}</td>
                      <td>{tx["Koszyk budżetu"]}</td>
                      <td className={`num ${Number(tx.Kwota) < 0 ? "neg" : "pos"}`}>{moneyDec(tx.Kwota)}</td>
                      <td>
                        <span className={`badge ${tx["Pewność kategorii"]}`}>{tx["Pewność kategorii"]}</span>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </Panel>
        </section>
      )}

      {view === "plan" && (
        <Panel title={planTitle}>
        <div className="savingPlan">
          {isHistorical && (
            <div className="historicalNotice">
              To jest rok zamknięty. Ten moduł nie jest planem działania na {data.year}, tylko pokazuje, ile dałyby limity i które nawyki przenieść do aktualnego budżetu.
            </div>
          )}
          <div className="planCards">
            <div>
              <span>{isHistorical ? "Hipotetyczny target" : "Target wydatków"}</span>
              <strong>{money(plan.targetMonthlySpend)}</strong>
              <p>ambitnie: {money(plan.aggressiveMonthlySpend)}</p>
            </div>
            <div>
              <span>{isHistorical ? "Możliwy przelew wtedy" : "Przelew inwestycyjny po pensji"}</span>
              <strong>{money(plan.targetInvestmentTransfer)}</strong>
              <p>agresywnie: {money(plan.aggressiveInvestmentTransfer)}</p>
            </div>
            <div>
              <span>{isHistorical ? "Utracony potencjał limitów" : "Potencjał z limitów"}</span>
              <strong>{money(planSummary.potentialMonthly)}</strong>
              <p>{money(planSummary.potentialYearly)} rocznie</p>
            </div>
            <div>
              <span>{isHistorical ? "Scenariusz po limitach" : "Po limitach"}</span>
              <strong>{money(plannedSpendAfterCuts)}</strong>
              <p>inwestycje: {money(plannedInvestmentAfterCuts)} / mies.</p>
            </div>
            <div>
              <span>Fundusz awaryjny 3 mies.</span>
              <strong>{money(plan.emergencyFundMin)}</strong>
              <p>komfort 6 mies.: {money(plan.emergencyFundComfort)}</p>
            </div>
          </div>

          <div className="planTable">
            <div className="tableWrap">
              <table>
                <thead>
                  <tr>
                    <th>Kategoria</th>
                    <th>Koszyk</th>
                    <th>{isHistorical ? "Było / mies." : "Teraz / mies."}</th>
                    <th>{isHistorical ? "Limit do symulacji" : "Limit"}</th>
                    <th>{isHistorical ? "Można było" : "Potencjał"}</th>
                    <th>Priorytet</th>
                    <th>{isHistorical ? "Wniosek" : "Co robić"}</th>
                  </tr>
                </thead>
                <tbody>
                  {planRows.map((row) => (
                    <tr key={row.category}>
                      <td>{row.category}</td>
                      <td>{row.bucket}</td>
                      <td className="num">{money(row.currentMonthly)}</td>
                      <td>
                        <input
                          className="limitInput"
                          type="number"
                          min="0"
                          step="50"
                          value={Math.round(row.limit)}
                          onChange={(event) =>
                            setCustomLimits((current) => ({
                              ...current,
                              [row.category]: Number(event.target.value || 0),
                            }))
                          }
                          aria-label={`Limit ${row.category}`}
                        />
                      </td>
                      <td className="num strong">{money(row.potentialMonthly)}</td>
                      <td>
                        <span className={`priority ${row.priority}`}>{row.priority}</span>
                      </td>
                      <td>{row.action}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        </div>
        </Panel>
      )}

      {view === "categories" && (
        <section className="viewStack">
          <section className="categorySummary">
            {scopedStats.areaTop.slice(0, 6).map((row, index) => (
              <button className="categoryTile clickable" key={row.area} onClick={() => setDrillFilter({ type: "area", value: row.area, label: `Obszar: ${row.area}` })}>
                <i style={{ background: COLORS[index % COLORS.length] }} />
                <span>{row.area}</span>
                <strong>{money(row.spend)}</strong>
                <em>{percent(row.spend / (scopedStats.spend || 1))}</em>
              </button>
            ))}
          </section>

          <section className="gridTwo">
            <Panel title="Obszary budżetu">
              <div className="chart compact">
                <ResponsiveContainer width="100%" height="100%">
                  <BarChart data={scopedStats.areaTop.slice(0, 8)} layout="vertical" margin={{ top: 6, right: 18, left: 100, bottom: 4 }}>
                    <CartesianGrid stroke="#e5e7eb" horizontal={false} />
                    <XAxis type="number" tickFormatter={(v) => `${Math.round(v / 1000)}k`} />
                    <YAxis dataKey="area" type="category" width={140} tick={{ fontSize: 12 }} />
                    <Tooltip formatter={(v) => moneyDec(v)} />
                    <Bar
                      dataKey="spend"
                      name="Wydatki"
                      fill="#0f766e"
                      radius={[0, 4, 4, 0]}
                      isAnimationActive={false}
                      onClick={(row) => setDrillFilter({ type: "area", value: row.area, label: `Obszar: ${row.area}` })}
                      cursor="pointer"
                    />
                  </BarChart>
                </ResponsiveContainer>
              </div>
            </Panel>

            <Panel title="Grupy kosztów">
              <div className="chart compact">
                <ResponsiveContainer width="100%" height="100%">
                  <BarChart data={scopedStats.groupTop.slice(0, 10)} layout="vertical" margin={{ top: 6, right: 18, left: 86, bottom: 4 }}>
                    <CartesianGrid stroke="#e5e7eb" horizontal={false} />
                    <XAxis type="number" tickFormatter={(v) => `${Math.round(v / 1000)}k`} />
                    <YAxis dataKey="group" type="category" width={126} tick={{ fontSize: 12 }} />
                    <Tooltip formatter={(v) => moneyDec(v)} />
                    <Bar
                      dataKey="spend"
                      name="Wydatki"
                      fill="#2563eb"
                      radius={[0, 4, 4, 0]}
                      isAnimationActive={false}
                      onClick={(row) => setDrillFilter({ type: "group", value: row.group, label: `Grupa: ${row.group}` })}
                      cursor="pointer"
                    />
                  </BarChart>
                </ResponsiveContainer>
              </div>
            </Panel>
          </section>

          <section className="gridTwo">
            <Panel title="Top kategorie">
              <div className="chart compact">
                <ResponsiveContainer width="100%" height="100%">
                  <BarChart data={categoryTop} layout="vertical" margin={{ top: 6, right: 18, left: 86, bottom: 4 }}>
                    <CartesianGrid stroke="#e5e7eb" horizontal={false} />
                    <XAxis type="number" tickFormatter={(v) => `${Math.round(v / 1000)}k`} />
                    <YAxis dataKey="category" type="category" width={120} tick={{ fontSize: 12 }} />
                    <Tooltip formatter={(v) => moneyDec(v)} />
                    <Bar
                      dataKey="spend"
                      name="Wydatki"
                      fill="#b45309"
                      radius={[0, 4, 4, 0]}
                      isAnimationActive={false}
                      onClick={(row) => setDrillFilter({ type: "category", value: row.category, label: `Kategoria: ${row.category}` })}
                      cursor="pointer"
                    />
                  </BarChart>
                </ResponsiveContainer>
              </div>
            </Panel>

            <Panel title="Top podkategorie">
              <div className="tableWrap smallRows">
                <table>
                  <thead>
                    <tr>
                      <th>Podkategoria</th>
                      <th>Kwota</th>
                      <th>Udział</th>
                    </tr>
                  </thead>
                  <tbody>
                    {scopedStats.subcategoryTop.slice(0, 16).map((row) => (
                      <tr
                        className="clickableRow"
                        key={row.subcategory}
                        onClick={() => setDrillFilter({ type: "subcategory", value: row.subcategory, label: `Podkategoria: ${row.subcategory}` })}
                      >
                        <td>{row.subcategory}</td>
                        <td className="num">{money(row.spend)}</td>
                        <td className="num">{percent(row.spend / (scopedStats.spend || 1))}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </Panel>
          </section>

          <Panel title="Hierarchia: obszar -> grupa -> kategoria -> podkategoria">
            <div className="tableWrap smallRows">
              <table>
                <thead>
                  <tr>
                    <th>Obszar</th>
                    <th>Grupa</th>
                    <th>Kategoria</th>
                    <th>Podkategoria</th>
                    <th>Kwota</th>
                    <th>Liczba</th>
                    <th>Udział</th>
                  </tr>
                </thead>
                <tbody>
                  {hierarchyTop.map((row) => (
                    <tr
                      className="clickableRow"
                      key={`${row.area}-${row.group}-${row.category}-${row.subcategory}`}
                      onClick={() => setDrillFilter({ type: "subcategory", value: `${row.category} · ${row.subcategory}`, label: `Podkategoria: ${row.category} · ${row.subcategory}` })}
                    >
                      <td>{row.area}</td>
                      <td>{row.group}</td>
                      <td>{row.category}</td>
                      <td>{row.subcategory}</td>
                      <td className="num">{money(row.spend)}</td>
                      <td className="num">{row.count}</td>
                      <td className="num">{percent(row.spend / (scopedStats.spend || 1))}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </Panel>
        </section>
      )}

      {view === "recurring" && (
        <section className="gridTwo">
          <Panel title="Cykliczne płatności">
            <div className="recurringCalendar">
              {recurringCalendar.map((row) => (
                <div className="recurringItem" key={`${row.merchant}-${row.category}`}>
                  <span>{row.avgDay}</span>
                  <div>
                    <strong>{row.merchant}</strong>
                    <p>{row.category} · {row.months} mies.</p>
                  </div>
                  <em>{money(row.monthlyAverage)}</em>
                </div>
              ))}
            </div>
          </Panel>

          <Panel title="Fundusze celowe">
            <div className="fundGrid">
              {(monthControl.sinkingFunds || []).map((fund) => (
                <div className="fundItem" key={fund.category}>
                  <span>{fund.name}</span>
                  <strong>{money(fund.monthlySetAside)}</strong>
                  <p>{money(fund.yearlyNeed)} rocznie</p>
                </div>
              ))}
            </div>
          </Panel>

          <Panel title="Cykliczne koszty">
          <div className="tableWrap smallRows">
            <table>
              <thead>
                <tr>
                  <th>Sprzedawca</th>
                  <th>Kategoria</th>
                  <th>Mies.</th>
                  <th>Suma</th>
                </tr>
              </thead>
              <tbody>
                {recurring.map((row) => (
                  <tr key={`${row.merchant}-${row.category}`}>
                    <td>{row.merchant}</td>
                    <td>{row.category}</td>
                    <td className="num">{row.months}</td>
                    <td className="num">{money(row.sum)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          </Panel>

          <Panel title="Duże jednorazowe">
          <div className="tableWrap smallRows">
            <table>
              <thead>
                <tr>
                  <th>Data</th>
                  <th>Sprzedawca</th>
                  <th>Kategoria</th>
                  <th>Kwota</th>
                </tr>
              </thead>
              <tbody>
                {oneoffs.map((row) => (
                  <tr key={`${row.date || row.Data}-${row.merchant || row.Sprzedawca}-${row.amount || row.Lp}`}>
                    <td>{row.date || row.Data}</td>
                    <td>{row.merchant || row.Sprzedawca}</td>
                    <td>{row.category || row["Kategoria skorygowana"]}</td>
                    <td className="num">{money(row.amount || row["Wydatek analizy"])}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          </Panel>
        </section>
      )}

      {view === "transactions" && (
      <Panel
        title="Transakcje"
        className="wide"
        action={
          <div className="panelActions">
            <label className="field">
              <Search size={16} />
              <input value={query} onChange={(event) => setQuery(event.target.value)} placeholder="Szukaj sprzedawcy, opisu, kategorii" />
            </label>
            <label className="select">
              <Filter size={16} />
              <select value={bucket} onChange={(event) => setBucket(event.target.value)}>
                {buckets.map((option) => (
                  <option key={option}>{option}</option>
                ))}
              </select>
            </label>
          </div>
        }
      >
        <div className="tableMeta">
          <span>
            <SlidersHorizontal size={15} /> Widoczne: {filteredTransactions.length}
          </span>
          <span>
            Zakres po filtrach: {drillFilteredTransactions.length}
          </span>
          <span>
            <CircleDollarSign size={15} /> Suma wydatków widocznych: {money(visibleSpend)}
          </span>
          <span>
            <CalendarDays size={15} /> {activeTimeLabel}
          </span>
        </div>
        <div className="tableWrap transactions">
          <table>
            <thead>
              <tr>
                <th>Data</th>
                <th>Sprzedawca</th>
                <th>Kategoria</th>
                <th>Podkategoria</th>
                <th>Koszyk</th>
                <th>Kwota</th>
                <th>Pewność</th>
              </tr>
            </thead>
            <tbody>
              {filteredTransactions.map((tx) => (
                <tr key={tx.Lp}>
                  <td>{tx.Data}</td>
                  <td>{tx.Sprzedawca}</td>
                  <td>{tx["Kategoria skorygowana"]}</td>
                  <td>{tx.Podkategoria}</td>
                  <td>{tx["Koszyk budżetu"]}</td>
                  <td className={`num ${Number(tx.Kwota) < 0 ? "neg" : "pos"}`}>{moneyDec(tx.Kwota)}</td>
                  <td>
                    <span className={`badge ${tx["Pewność kategorii"]}`}>{tx["Pewność kategorii"]}</span>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </Panel>
      )}

      {view === "import" && (
        <ImportView onUpload={handleUpload} uploading={uploading} importStatus={importStatus} />
      )}

      <footer>
        <ChartNoAxesCombined size={16} />
        Dane pochodzą z backendu Spring Boot i są przeliczane po każdym imporcie CSV.
      </footer>
    </main>
  );
}
