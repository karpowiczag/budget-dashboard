const SAVINGS_TRANSACTION_BUCKETS = new Set(["Inwestycje", "Konto oszczędnościowe", "Nadpłata kredytu", "Oszczędzanie/inwestycje"]);
const INVESTMENT_MIX_BUCKET = "Inwestycje";
const SAVINGS_ACCOUNT_MIX_BUCKET = "Konto oszczędnościowe";
const LOAN_OVERPAYMENT_MIX_BUCKET = "Nadpłata kredytu";
const SURPLUS_BUCKET_PREFIX = "Nadwyżka";
const FINANCIAL_FLOW_CATEGORIES = new Set(["Oszczędności i inwestycje", "Konto oszczędnościowe", "Nadpłata kredytu"]);
const OBLIGATORY_BUCKETS = new Set(["Obowiązkowe stałe", "Obowiązkowe zmienne", "Potrzeby"]);
const CUT_NOW_BUCKETS = new Set(["Nieobowiązkowe", "Zachcianki", "Zachcianki do rozbicia"]);
const REVIEW_BUCKETS = new Set(["Do rozbicia", "Marketplace do rozbicia", "Potrzeby mieszane"]);
const REVIEW_CATEGORIES = new Set(["Zdrowie i uroda", "Do sprawdzenia", "Marketplace"]);
const PROTECTED_CATEGORIES = new Set([
  "Czynsz i wynajem",
  "Prąd",
  "TV, internet, telefon",
  "Ubezpieczenia",
  "Podatki",
  "Spłaty i raty",
  "Nadpłata kredytu",
  "Oszczędności i inwestycje",
  "Konto oszczędnościowe",
  "Pensja",
]);
const RECURRING_OBLIGATION_CATEGORIES = new Set([
  "Czynsz i wynajem",
  "Prąd",
  "TV, internet, telefon",
  "Ubezpieczenia",
  "Spłaty i raty",
  "Multimedia",
  "Opłaty bankowe",
]);
const RECURRING_FALSE_POSITIVE_CATEGORIES = new Set([
  "Żywność i chemia",
  "Jedzenie poza domem",
  "Odzież i obuwie",
  "Marketplace",
  "Zdrowie i uroda",
  "Transport i parking",
]);
const GENERIC_SUBCATEGORY_LABELS = new Set(["Ogólne"]);
const PRIMARY_LIMIT_EXCLUDED = new Set(["Przychody", "Transfer techniczny", "Do sprawdzenia"]);
const BUCKET_LIMIT_ALIASES = {
  "Obowiązkowe stałe": ["Obowiązkowe stałe"],
  "Obowiązkowe zmienne": ["Obowiązkowe zmienne", "Potrzeby"],
  "Do rozbicia": ["Do rozbicia", "Potrzeby mieszane", "Marketplace do rozbicia", "Zachcianki do rozbicia"],
  Nieobowiązkowe: ["Nieobowiązkowe", "Zachcianki"],
  Inwestycje: ["Inwestycje", "Oszczędzanie/inwestycje"],
  "Konto oszczędnościowe": ["Konto oszczędnościowe"],
  "Nadpłata kredytu": ["Nadpłata kredytu"],
};
export const BUDGET_BUCKET_OPTIONS = ["Obowiązkowe stałe", "Obowiązkowe zmienne", "Do rozbicia", "Nieobowiązkowe", "Inwestycje", "Konto oszczędnościowe", "Nadpłata kredytu"];

export function buildDashboardViews(isHistorical) {
  return [
    { id: "month", label: "Miesiąc" },
    { id: "plan", label: isHistorical ? "Symulacja" : "Plan" },
    { id: "reports", label: "Raporty" },
    { id: "recurring", label: "Cykliczne" },
    { id: "transactions", label: "Transakcje" },
    { id: "import", label: "Import" },
  ];
}

export function monthKeyFromLabel(monthLabel) {
  if (!monthLabel) return "";
  if (/^\d{4}-\d{2}$/.test(monthLabel)) return monthLabel;
  return `${monthLabel.slice(3, 7)}-${monthLabel.slice(0, 2)}`;
}

export function selectBuckets(data) {
  if (!data) return ["Wszystkie"];
  const buckets = new Set();
  (data.budgetMix || []).forEach((row) => buckets.add(row.bucket));
  (data.savingsPlan?.categoryLimits || []).forEach((row) => buckets.add(row.bucket));
  if ((data.budgetMix || []).some((row) => [INVESTMENT_MIX_BUCKET, SAVINGS_ACCOUNT_MIX_BUCKET, LOAN_OVERPAYMENT_MIX_BUCKET, "Oszczędzanie/inwestycje wykonane"].includes(row.bucket) && Number(row.sum || 0) !== 0)) {
    SAVINGS_TRANSACTION_BUCKETS.forEach((item) => buckets.add(item));
  }
  return ["Wszystkie", ...Array.from(buckets).filter(Boolean).sort()];
}

export function selectTransactionFilterOptions(data) {
  const hierarchy = data?.hierarchy || [];
  const categories = data?.categories || [];
  const fixedness = data?.fixedness || [];
  return {
    flows: [
      { value: "Wszystkie", label: "Wszystkie przepływy" },
      { value: "spend", label: "Wydatki analizowane" },
      { value: "income", label: "Dochód" },
      { value: "financial", label: "Oszczędności/nadpłaty" },
      { value: "excluded", label: "Wyłączone/techniczne" },
    ],
    areas: withAll(unique(hierarchy.map((row) => row.area))),
    groups: withAll(unique(hierarchy.map((row) => row.group))),
    categories: withAll(unique(categories.map((row) => row.category))),
    subcategories: [
      { value: "Wszystkie", label: "Wszystkie" },
      ...uniquePairs(hierarchy, (row) => `${row.category}|${row.subcategory}`)
        .map((row) => ({ value: row.subcategory, label: `${row.category} · ${row.subcategory}` })),
    ],
    fixedness: withAll(unique(fixedness.map((row) => row.type))),
    confidence: ["Wszystkie", "Wysoka", "Średnia", "Niska"],
    sort: [
      { value: "postedDate,desc", label: "Data: najnowsze" },
      { value: "postedDate,asc", label: "Data: najstarsze" },
      { value: "amount,desc", label: "Kwota: najwyższa" },
      { value: "amount,asc", label: "Kwota: najniższa" },
      { value: "spend,desc", label: "Wydatek: najwyższy" },
      { value: "spend,asc", label: "Wydatek: najniższy" },
      { value: "merchant,asc", label: "Sprzedawca A-Z" },
      { value: "merchant,desc", label: "Sprzedawca Z-A" },
      { value: "category,asc", label: "Kategoria A-Z" },
      { value: "category,desc", label: "Kategoria Z-A" },
      { value: "subcategory,asc", label: "Podkategoria A-Z" },
      { value: "subcategory,desc", label: "Podkategoria Z-A" },
      { value: "bucket,asc", label: "Koszyk A-Z" },
      { value: "bucket,desc", label: "Koszyk Z-A" },
      { value: "fixedness,asc", label: "Stałość A-Z" },
      { value: "fixedness,desc", label: "Stałość Z-A" },
      { value: "confidence,asc", label: "Pewność A-Z" },
      { value: "confidence,desc", label: "Pewność Z-A" },
    ],
    pageSizes: [25, 50, 100, 200],
  };
}

export function selectPlanRows(data, customLimits, bucketOverrides = {}) {
  if (!data?.savingsPlan?.categoryLimits) return [];
  return data.savingsPlan.categoryLimits.map((row) => {
    const key = limitKey(row);
    const limit = Number(customLimits[key] ?? customLimits[row.category] ?? row.limit);
    const originalBucket = canonicalBudgetBucket(row.bucket);
    const bucketOverride = canonicalBudgetBucket(bucketOverrides[row.category] || row.bucketOverride || "");
    const bucket = bucketOverride || originalBucket;
    const potentialMonthly = Math.max(0, Number(row.currentMonthly || 0) - limit);
    return {
      ...row,
      scope: row.scope || "category",
      name: row.name || row.category,
      originalBucket,
      bucket,
      bucketOverride,
      limit,
      potentialMonthly,
      potentialYearly: potentialMonthly * 12,
    };
  });
}

export function selectParentPlanRows(data, customLimits, planRows = []) {
  if (!data?.savingsPlan?.parentLimits) return [];
  return data.savingsPlan.parentLimits.map((row) => {
    const key = limitKey(row);
    const limit = Number(customLimits[key] ?? row.limit);
    const name = row.name || row.category;
    const currentMonthly = row.scope === "bucket" && planRows.length
      ? planRows
        .filter((child) => bucketAliasesForLimit(name).has(child.bucket))
        .reduce((sum, child) => sum + Number(child.currentMonthly || 0), 0)
      : Number(row.currentMonthly || 0);
    const potentialMonthly = Math.max(0, currentMonthly - limit);
    return {
      ...row,
      scope: row.scope || "group",
      name,
      currentMonthly,
      limit,
      potentialMonthly,
      potentialYearly: potentialMonthly * 12,
    };
  });
}

export function selectPrimaryPlanRows(parentPlanRows) {
  return (parentPlanRows || [])
    .filter(isPrimaryLimitRow)
    .sort(comparePrimaryPlanRows);
}

export function selectCategorySubcategories(data) {
  const grouped = new Map();
  (data?.hierarchy || []).forEach((row) => {
    if (!row.category || !row.subcategory) return;
    if (GENERIC_SUBCATEGORY_LABELS.has(row.subcategory)) return;
    if (!grouped.has(row.category)) grouped.set(row.category, new Set());
    grouped.get(row.category).add(row.subcategory);
  });
  return Object.fromEntries(
    Array.from(grouped.entries())
      .map(([category, subcategories]) => [
        category,
        Array.from(subcategories).sort((left, right) => left.localeCompare(right, "pl")),
      ])
      .sort(([left], [right]) => left.localeCompare(right, "pl")),
  );
}

export function selectCategoryExamples(data) {
  return Object.fromEntries(
    (data?.categories || [])
      .filter((row) => row.category)
      .map((row) => [
        row.category,
        (row.merchantExamples || []).filter(Boolean).slice(0, 5),
      ])
      .sort(([left], [right]) => left.localeCompare(right, "pl")),
  );
}

export function selectPlanSummary(planRows) {
  const potentialMonthly = planRows.reduce((sum, row) => sum + row.potentialMonthly, 0);
  return {
    potentialMonthly,
    potentialYearly: potentialMonthly * 12,
  };
}

export function selectSavingsFocus({ monthControl, planRows }) {
  const sourceRows = monthControl?.categoryStatus?.length ? monthControl.categoryStatus : planRows;
  const rows = (sourceRows || [])
    .map(toSavingsFocusRow)
    .filter((row) => row.current > 0 || row.limit > 0 || row.potentialMonthly > 0)
    .sort(compareSavingsFocus);
  const cutNow = rows.filter((row) => row.decision === "Do cięcia");
  const review = rows.filter((row) => row.decision === "Do rozbicia");
  const protectedRows = rows.filter((row) => row.decision === "Nie ciąć automatycznie");
  const topActions = [...cutNow, ...review].slice(0, 5);
  return {
    topActions,
    cutNow,
    review,
    protectedRows: protectedRows.slice(0, 5),
    protectedCount: protectedRows.length,
    cutNowPotential: sumBy(cutNow, "potentialMonthly"),
    reviewPotential: sumBy(review, "potentialMonthly"),
    protectedSpend: sumBy(protectedRows, "current"),
  };
}

export function selectRecommendedCuts({ plan, planRows, savingsFocus }) {
  const planFocus = selectSavingsFocus({ planRows });
  const focus = savingsFocus || planFocus;
  const cutNowPotential = Number(focus.cutNowPotential || 0);
  const reviewPotential = Number(focus.reviewPotential || 0);
  const realisticCut = roundToNearest(cutNowPotential * 0.7 + reviewPotential * 0.3, 50);
  const aggressiveCut = roundToNearest(cutNowPotential + reviewPotential, 50);
  const currentSpend = Number(plan?.currentMonthlySpend || 0);
  const currentIncome = Number(plan?.currentMonthlyIncome || 0);
  return {
    realisticCut,
    aggressiveCut,
    realisticSpend: Math.max(0, currentSpend - realisticCut),
    aggressiveSpend: Math.max(0, currentSpend - aggressiveCut),
    realisticInvestable: Math.max(0, currentIncome - Math.max(0, currentSpend - realisticCut)),
    aggressiveInvestable: Math.max(0, currentIncome - Math.max(0, currentSpend - aggressiveCut)),
    groups: [
      { label: "Uznaniowe", tone: "cut", rows: planFocus.cutNow },
      { label: "Do rozbicia", tone: "review", rows: planFocus.review },
      { label: "Obowiązkowe monitorowane", tone: "protected", rows: planFocus.protectedRows },
    ].filter((group) => group.rows.length),
  };
}

export function selectBudgetBurnDown({ calendar, monthControl }) {
  if (!calendar?.days?.length || !monthControl) return [];
  const targetSpend = Number(monthControl.targetSpend || 0);
  const daysInMonth = Number(monthControl.daysInMonth || calendar.days.length || 1);
  let cumulativeSpend = 0;
  return calendar.days.map((day) => {
    cumulativeSpend += Number(day.spend || 0);
    return {
      day: Number(day.day),
      date: day.date,
      spend: Number(day.spend || 0),
      income: Number(day.income || 0),
      transactions: Number(day.transactions || 0),
      cumulativeSpend,
      targetPace: targetSpend ? (targetSpend / daysInMonth) * Number(day.day) : 0,
    };
  });
}

export function selectCategoryLimitChart(categoryStatus) {
  return selectLimitRiskRows(categoryStatus).map((row) => {
    const limit = Number(row.limit || 0);
    const projected = Number(row.currentMonthProjection || 0);
    const current = Number(row.currentMonthSpend || 0);
    const label = row.name || row.category;
    return {
      category: label,
      scope: row.scope || "category",
      name: row.name || label,
      bucket: row.bucket,
      current,
      projected,
      limit,
      remaining: Number(row.remainingThisMonth || 0),
      projectedDelta: Number(row.projectedDelta || 0),
      usage: limit > 0 ? projected / limit : 0,
      filter: row.filter || { category: row.category },
    };
  });
}

export function selectMonthlyParentLimitStatus(categoryStatus, parentPlanRows, bucketOverrides = {}) {
  const monthlyRows = categoryStatus || [];
  return (parentPlanRows || [])
    .filter(isPrimaryLimitRow)
    .map((parent) => {
      const name = parent.name || parent.category;
      const aliases = bucketAliasesForLimit(name);
      const children = monthlyRows
        .filter((row) => aliases.has(effectiveRowBucket(row, bucketOverrides)))
        .map((row) => toMonthlyParentLimitChild(row, effectiveRowBucket(row, bucketOverrides)))
        .filter((row) => Number(row.currentMonthProjection || 0) > 0 || Number(row.currentMonthSpend || 0) > 0 || Number(row.limit || 0) > 0)
        .sort(compareLimitRisk);
      const currentMonthSpend = children.reduce((sum, row) => sum + Number(row.currentMonthSpend || 0), 0);
      const currentMonthProjection = children.reduce((sum, row) => sum + Number(row.currentMonthProjection || 0), 0);
      const limit = Number(parent.limit || 0);
      const projectedDelta = limit - currentMonthProjection;
      const remainingThisMonth = limit - currentMonthSpend;
      return {
        ...parent,
        scope: "bucket",
        name,
        category: name,
        bucket: name,
        currentMonthSpend,
        currentMonthProjection,
        remainingThisMonth,
        projectedDelta,
        usage: limit > 0 ? currentMonthProjection / limit : 0,
        potentialMonthly: Math.max(0, currentMonthProjection - limit),
        potentialYearly: Math.max(0, currentMonthProjection - limit) * 12,
        filter: { bucket: name },
        children,
      };
    })
    .filter((row) => Number(row.currentMonthProjection || 0) > 0 || Number(row.currentMonthSpend || 0) > 0 || Number(row.currentMonthly || 0) > 0 || Number(row.limit || 0) > 0);
}

function toMonthlyParentLimitChild(row, bucket) {
  const currentMonthProjection = Number(row.currentMonthProjection || 0);
  const currentMonthSpend = Number(row.currentMonthSpend || 0);
  const limit = Number(row.limit || 0);
  return {
    ...row,
    scope: row.scope || "category",
    name: row.name || row.category,
    bucket,
    originalBucket: canonicalBudgetBucket(row.bucket),
    bucketOverride: bucket && bucket !== canonicalBudgetBucket(row.bucket) ? bucket : "",
    currentMonthSpend,
    currentMonthProjection,
    limit,
    remainingThisMonth: limit - currentMonthSpend,
    projectedDelta: limit - currentMonthProjection,
    usage: limit > 0 ? currentMonthProjection / limit : 0,
    potentialMonthly: Math.max(0, currentMonthProjection - limit),
    potentialYearly: Math.max(0, currentMonthProjection - limit) * 12,
    filter: { category: row.category },
  };
}

function effectiveRowBucket(row, bucketOverrides = {}) {
  return canonicalBudgetBucket(bucketOverrides[row.category] || row.bucket);
}

function selectLimitRiskRows(categoryStatus, size = 10) {
  return (categoryStatus || [])
    .filter((row) => Number(row.currentMonthProjection || 0) > 0 || Number(row.currentMonthSpend || 0) > 0 || Number(row.usage || 0) > 0)
    .slice()
    .sort(compareLimitRisk)
    .slice(0, size);
}

function compareLimitRisk(left, right) {
  const leftDelta = Number(left.projectedDelta || 0);
  const rightDelta = Number(right.projectedDelta || 0);
  const leftUsage = Number(left.usage || 0);
  const rightUsage = Number(right.usage || 0);
  const leftOver = leftDelta < 0 ? 1 : 0;
  const rightOver = rightDelta < 0 ? 1 : 0;

  return rightOver - leftOver || rightUsage - leftUsage || leftDelta - rightDelta;
}

export function selectSavingsWaterfall({ plan, planSummary, plannedSpendAfterCuts, plannedInvestmentAfterCuts, recommendedCuts }) {
  if (!plan) return [];
  const currentSpend = Number(plan.currentMonthlySpend || 0);
  const currentIncome = Number(plan.currentMonthlyIncome || 0);
  const cuts = Number(recommendedCuts?.realisticCut ?? planSummary?.potentialMonthly ?? 0);
  const targetSpend = Number(plannedSpendAfterCuts || Math.max(0, currentSpend - cuts));
  const investable = Number(plannedInvestmentAfterCuts || Math.max(0, currentIncome - targetSpend));
  return [
    { label: "Średni dochód", amount: currentIncome, kind: "total", tone: "income" },
    { label: "Koszt życia teraz", amount: -currentSpend, absAmount: currentSpend, kind: "delta", tone: "expense" },
    { label: "Cięcia limitów", amount: cuts, absAmount: cuts, kind: "delta", tone: "good" },
    { label: "Wolne środki po limitach", amount: investable, kind: "total", note: `Wydatki po limitach: ${targetSpend}`, tone: "saving" },
  ];
}

export function selectSavingsRadar(recommendedCuts) {
  return (recommendedCuts?.groups || [])
    .map((group) => ({
      label: group.label,
      value: sumBy(group.rows || [], "potentialMonthly"),
      count: (group.rows || []).length,
    }))
    .filter((row) => row.value > 0);
}

export function selectDailyCalendarHeatmap(calendar) {
  return (calendar?.days || [])
    .map((day) => ({
      date: day.date,
      day: Number(day.day || 0),
      spend: Number(day.spend || 0),
      income: Number(day.income || 0),
      transactions: Number(day.transactions || 0),
      filter: { date: day.date },
    }))
    .filter((row) => row.date);
}

export function selectMonthDashboard({ bucketOverrides = {}, calendarStats, financialFlowTotal = 0, isHistorical = false, monthControl, primaryPlanRows = [] }) {
  if (!monthControl) {
    return {
      title: "Miesiąc",
      cards: [],
      alerts: [],
      categoryStatus: [],
      selectedDay: null,
    };
  }
  const parentStatus = selectMonthlyParentLimitStatus(monthControl.categoryStatus || [], primaryPlanRows, bucketOverrides);
  const limitStatus = parentStatus.length ? parentStatus : (monthControl.categoryStatus || []);

  return {
    title: isHistorical ? "Kontrola zamkniętego miesiąca" : "Kontrola bieżącego miesiąca",
    cards: [
      {
        label: monthControl.month,
        value: monthControl.spendToDate,
        detail: `wydane po ${monthControl.elapsedDays} dniach`,
        filter: { flow: "spend" },
      },
      {
        label: isHistorical ? "Wydatki miesiąca" : "Prognoza końca miesiąca",
        value: monthControl.projectedSpend,
        detail: `${monthControl.projectedDelta >= 0 ? "pod targetem" : "ponad target"}: ${Math.abs(monthControl.projectedDelta)} PLN`,
        filter: { flow: "spend" },
      },
      {
        label: isHistorical ? "Różnica do targetu" : "Do wydania",
        value: monthControl.remainingBudget,
        detail: isHistorical ? "po faktycznych wydatkach" : `${monthControl.dailyAllowed} dziennie`,
        tone: Number(monthControl.remainingBudget || 0) < 0 ? "warn" : "neutral",
        filter: { flow: "spend" },
      },
      {
        label: "Dochód miesiąca",
        value: monthControl.incomeToDate,
        detail: "rozpoznane pensje",
        filter: { flow: "income" },
      },
      {
        label: "Oszczędności/nadpłaty",
        value: financialFlowTotal,
        detail: "poza kosztem życia",
        filter: { flow: "financial" },
      },
    ],
    alerts: monthControl.alerts || [],
    categoryStatus: selectLimitRiskRows(limitStatus),
    savingsFocus: selectSavingsFocus({ planRows: limitStatus }),
    burnDown: selectBudgetBurnDown({ calendar: calendarStats?.rawCalendar, monthControl }),
    dailyHeatmap: selectDailyCalendarHeatmap(calendarStats?.rawCalendar),
    limitChart: selectCategoryLimitChart(limitStatus),
    selectedDay: calendarStats ? {
      day: calendarStats.selected,
      date: calendarStats.selectedDate,
      spend: calendarStats.selectedSpend,
      income: calendarStats.selectedIncome,
      transactions: calendarStats.selectedTransactions,
    } : null,
  };
}

export function selectReportsSections({
  activeTimeLabel,
  year,
  budgetMix,
  financialFlows,
  fixedness,
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
}) {
  const scoped = scopedStats || emptyAnalytics();
  const trendStats = yearStats || scoped;
  const yearLabel = year ? `Cały ${year}` : "Cały rok";
  const scopeLabel = activeTimeLabel || yearLabel;
  const financialFlowRows = scoped?.financialFlows?.length ? scoped.financialFlows : (financialFlows?.rows || []);
  const financialFlowTotal = financialFlowRows.reduce((sum, row) => sum + Number(row.outgoing || 0), 0);
  const financialFlowCount = financialFlowRows.reduce((sum, row) => sum + Number(row.count || 0), 0);
  const surplus = Number(scoped?.income || 0) - Number(scoped?.spend || 0);
  const fixednessSource = scoped?.fixednessBreakdown?.length ? scoped.fixednessBreakdown : (fixedness || []);
  const outlierSource = scoped?.oneoffs?.length ? scoped.oneoffs : (oneoffs || []);

  return {
    activeTimeLabel: scopeLabel,
    yearLabel,
    budgetMix: budgetMix || [],
    monthly: monthly || [],
    scopedStats: scoped,
    yearStats: trendStats,
    financialFlows: financialFlowRows,
    financialFlowTotal,
    financialFlowCount,
    scopeCards: [
      { label: "Wydatki", value: scoped?.spend || 0, detail: `${scoped?.transactionCount || 0} transakcji w zakresie`, filter: { flow: "spend" } },
      { label: "Wpływy", value: scoped?.income || 0, detail: "rozpoznane dochody", filter: { flow: "income" } },
      { label: "Nadwyżka operacyjna", value: surplus, detail: scoped?.income ? surplus / Number(scoped.income) : null },
      { label: "Oszczędności/nadpłaty", value: financialFlowTotal, detail: `${financialFlowCount} transakcji poza kosztem życia`, filter: { flow: "financial" } },
    ],
    benchmarkCards: [
      { label: "Obowiązkowe", value: Number(needs || 0) + Number(mixedNeeds || 0), detail: `punkt odniesienia 50%: ${Number(kpis?.income || 0) * 0.5}` },
      { label: "Nieobowiązkowe", value: wants || 0, detail: `punkt odniesienia 30%: ${wantsTarget || 0}` },
      { label: "Oszczędzanie operacyjne", value: kpis?.operatingSurplus || 0, detail: `minimum 20%: ${savingsTarget || 0}` },
      { label: "Nadwyżka po inwestycjach", value: kpis?.unassignedSurplus || 0, detail: "do decyzji lub dalszego inwestowania" },
    ],
    cashflowSeries: selectMonthlyCashflowSeries(monthly || []),
    cashflowSankey: selectCashflowSankey({ financialFlows: { rows: financialFlowRows }, scopedStats: scoped }),
    budgetMixChart: selectBudgetMixChart(budgetMix || []),
    categoryShare: selectCategoryShare(scoped?.categoryTop || [], scoped?.spend || 0),
    categoryPareto: selectCategoryPareto(scoped?.categoryTop || []),
    categoryTrends: selectMonthlyDimensionTrends(trendStats?.monthlyCategoryTrends || [], "category", { type: "category" }),
    fixednessChart: selectFixednessChart(fixednessSource),
    hierarchySunburst: selectHierarchySunburst(scoped?.hierarchyTop || []),
    merchantFunnel: selectMerchantFunnel(scoped?.merchants || []),
    merchantShare: selectMerchantShare(scoped?.merchants || [], scoped?.spend || 0),
    merchantTrends: selectMonthlyDimensionTrends(trendStats?.monthlyMerchantTrends || [], "merchant", { type: "merchant" }),
    outlierTimeline: selectOutlierTimeline(outlierSource),
    controllable: {
      topActions: savingsFocus?.topActions || [],
      cutNowPotential: Number(savingsFocus?.cutNowPotential || 0),
      reviewPotential: Number(savingsFocus?.reviewPotential || 0),
    },
  };
}

export function selectRecurringSummary({ monthControl, recurring, recurringCalendar }) {
  const obligations = selectRecurringObligations(recurring || recurringCalendar || []);
  const filteredCalendar = recurringCalendar?.length ? selectRecurringObligations(recurringCalendar) : [];
  const calendarSource = filteredCalendar.length ? filteredCalendar : obligations;
  const sortedCalendar = calendarSource
    .slice()
    .sort((left, right) => Number(left.avgDay || 99) - Number(right.avgDay || 99) || Number(right.monthlyAverage || 0) - Number(left.monthlyAverage || 0));
  const recurringMonthly = obligations.reduce((sum, row) => sum + Number(row.monthlyAverage || 0), 0);
  const sinkingFunds = monthControl?.sinkingFunds || [];
  const sinkingMonthly = sinkingFunds.reduce((sum, fund) => sum + Number(fund.monthlySetAside || 0), 0);
  return {
    cards: [
      { label: "Cykliczne koszty", value: recurringMonthly, detail: `${obligations.length} realnych zobowiązań` },
      { label: "Fundusze celowe", value: sinkingMonthly, detail: `${sinkingFunds.length} rezerw miesięcznych` },
      { label: "Najbliższy miesiąc", value: monthControl?.month || "", detail: "bazuje na cyklicznych wzorcach i limitach", textValue: true },
    ],
    recurringCalendar: sortedCalendar.slice(0, 16),
    recurringTimeline: sortedCalendar.slice(0, 24).map((row) => ({
      merchant: row.merchant,
      category: row.category,
      bucket: row.bucket,
      day: Number(row.avgDay || 1),
      amount: Number(row.monthlyAverage || 0),
      months: Number(row.months || 0),
      count: Number(row.count || 0),
      filter: { query: row.merchant },
    })),
    sinkingFunds,
  };
}

export function selectRecurringObligations(recurring = []) {
  return (recurring || []).filter((row) => {
    const category = row.category || "";
    const months = Number(row.months || 0);
    const amount = Number(row.monthlyAverage || 0);
    if (RECURRING_OBLIGATION_CATEGORIES.has(category)) return months >= 2 && amount > 0;
    if (RECURRING_FALSE_POSITIVE_CATEGORIES.has(category)) return false;
    return months >= 4 && amount >= 100;
  });
}

export function selectMonthlyCashflowSeries(monthly) {
  return (monthly || [])
    .filter((row) => Number(row.transactions || 0) > 0)
    .map((row) => ({
      month: row.month,
      monthKey: row.monthKey,
      income: Number(row.income || 0),
      spend: Number(row.spend || 0),
      savingsInvestments: Number(row.savingsInvestments || 0),
      netFlow: Number(row.netFlow || 0),
      savingsRate: Number(row.savingsRate || 0),
      transactions: Number(row.transactions || 0),
    }));
}

export function selectBudgetMixChart(budgetMix) {
  return (budgetMix || [])
    .filter((row) => Math.abs(Number(row.sum || 0)) > 0)
    .map((row) => ({
      name: row.bucket,
      bucket: row.bucket,
      value: Math.abs(Number(row.sum || 0)),
      sum: Number(row.sum || 0),
      monthlyAverage: Number(row.monthlyAverage || 0),
      incomeShare: Number(row.incomeShare || 0),
      filter: budgetMixFilter(row.bucket),
    }));
}

export function selectCashflowSankey({ financialFlows, scopedStats }) {
  const links = [];
  const scopedFinancialFlowTotal = (financialFlows?.rows || []).reduce((sum, row) => sum + Number(row.outgoing || 0), 0);
  const scopedSpend = Number(scopedStats?.spend || 0);
  const scopedIncome = Number(scopedStats?.income || 0);
  const scopedOutflow = scopedSpend + scopedFinancialFlowTotal;
  const fundingFromBalance = Math.max(0, scopedOutflow - scopedIncome);
  const scopedSurplus = Math.max(0, scopedIncome - scopedOutflow);

  addSankeyLink(links, "Wpływy", "Dostępne środki", scopedIncome, { flow: "income" });
  addSankeyLink(links, "Środki z salda", "Dostępne środki", fundingFromBalance, {});
  addSankeyLink(links, "Dostępne środki", "Koszt życia", scopedSpend, { flow: "spend" });

  (financialFlows?.rows || []).forEach((row) => {
    addSankeyLink(links, "Dostępne środki", row.category, Number(row.outgoing || 0), { flow: "financial", category: row.category });
  });

  addSankeyLink(links, "Dostępne środki", "Wolne środki po przepływach", scopedSurplus, {});

  const nodes = Array.from(new Set(links.flatMap((link) => [link.source, link.target])))
    .map((name) => ({ name }));
  return { links, nodes };
}

export function selectCategoryPareto(categoryTop) {
  const rows = (categoryTop || [])
    .filter((row) => Number(row.spend || 0) > 0)
    .slice(0, 12);
  const total = rows.reduce((sum, row) => sum + Number(row.spend || 0), 0);
  let cumulative = 0;
  return rows.map((row) => {
    cumulative += Number(row.spend || 0);
    return {
      category: row.category,
      spend: Number(row.spend || 0),
      count: Number(row.count || 0),
      cumulativeShare: total ? cumulative / total : 0,
      filter: { category: row.category },
    };
  });
}

export function selectCategoryShare(categoryTop, totalSpend = 0) {
  const rows = (categoryTop || [])
    .filter((row) => Number(row.spend || 0) > 0)
    .slice(0, 10);
  const topTotal = rows.reduce((sum, row) => sum + Number(row.spend || 0), 0);
  const denominator = Math.max(Number(totalSpend || 0), topTotal, 1);
  const result = rows.map((row) => ({
    category: row.category,
    spend: Number(row.spend || 0),
    count: Number(row.count || 0),
    share: Number(row.spend || 0) / denominator,
    filter: { category: row.category },
  }));
  const remaining = denominator - topTotal;
  if (remaining > 1) {
    result.push({
      category: "Pozostałe",
      spend: remaining,
      count: 0,
      share: remaining / denominator,
      filter: null,
    });
  }
  return result;
}

export function selectHierarchySunburst(hierarchyTop) {
  const roots = new Map();
  (hierarchyTop || []).forEach((row) => {
    const area = row.area || "Inne";
    const group = row.group || "Inne";
    const category = row.category || "Inne";
    const subcategory = row.subcategory || "Ogólne";
    const spend = Number(row.spend || 0);
    if (spend <= 0) return;
    const areaNode = ensureSunburstNode(roots, area, { area });
    const groupNode = ensureSunburstNode(areaNode.childrenMap, group, { group });
    const categoryNode = ensureSunburstNode(groupNode.childrenMap, category, { category });
    const subNode = ensureSunburstNode(categoryNode.childrenMap, subcategory, { subcategory });
    subNode.value += spend;
    subNode.count += Number(row.count || 0);
  });
  return Array.from(roots.values()).map(finalizeSunburstNode);
}

export function selectMonthlyDimensionTrends(rows, dimensionKey, filterDescriptor) {
  const totals = new Map();
  (rows || []).forEach((row) => {
    const dimension = row[dimensionKey];
    if (!dimension) return;
    totals.set(dimension, (totals.get(dimension) || 0) + Number(row.spend || 0));
  });
  const topDimensions = Array.from(totals.entries())
    .sort((left, right) => right[1] - left[1])
    .slice(0, 8)
    .map(([dimension]) => dimension);
  return (rows || [])
    .filter((row) => topDimensions.includes(row[dimensionKey]))
    .map((row) => ({
      ...row,
      spend: Number(row.spend || 0),
      count: Number(row.count || 0),
      filter: filterDescriptor?.type === "merchant"
        ? { query: row[dimensionKey] }
        : { [filterDescriptor?.type || dimensionKey]: row[dimensionKey] },
    }));
}

export function selectMerchantFunnel(merchants) {
  return (merchants || [])
    .slice(0, 10)
    .map((row) => ({
      merchant: row.merchant,
      sum: Number(row.sum || 0),
      count: Number(row.count || 0),
      filter: { query: row.merchant },
    }))
    .filter((row) => row.merchant && row.sum > 0);
}

export function selectMerchantShare(merchants, totalSpend = 0) {
  const rows = (merchants || [])
    .filter((row) => Number(row.sum || 0) > 0)
    .slice(0, 10);
  const topTotal = rows.reduce((sum, row) => sum + Number(row.sum || 0), 0);
  const denominator = Math.max(Number(totalSpend || 0), topTotal, 1);
  const result = rows.map((row) => ({
    merchant: row.merchant,
    sum: Number(row.sum || 0),
    count: Number(row.count || 0),
    share: Number(row.sum || 0) / denominator,
    filter: { query: row.merchant },
  }));
  const remaining = denominator - topTotal;
  if (remaining > 1) {
    result.push({
      merchant: "Pozostali",
      sum: remaining,
      count: 0,
      share: remaining / denominator,
      filter: null,
    });
  }
  return result;
}

export function selectFixednessChart(fixedness) {
  return (fixedness || [])
    .map((row) => ({
      fixedness: row.type || row.fixedness,
      spend: Number(row.spend || 0),
      excludedOutgoing: Number(row.excludedOutgoing || 0),
      total: Number(row.spend || 0) + Number(row.excludedOutgoing || 0),
      count: Number(row.count || 0),
      filter: row.type || row.fixedness ? { fixedness: row.type || row.fixedness } : {},
    }))
    .filter((row) => row.total > 0)
    .sort((left, right) => right.total - left.total);
}

export function selectOutlierTimeline(oneoffs) {
  return (oneoffs || [])
    .map((row) => ({
      date: row.postedDate || row.date,
      merchant: row.merchant,
      category: row.correctedCategory || row.category,
      amount: Number(row.spend || row.amount || 0),
      confidence: row.confidence,
      filter: { query: row.merchant },
    }))
    .filter((row) => row.date && row.amount > 0)
    .sort((left, right) => left.date.localeCompare(right.date))
    .slice(0, 40);
}

export function selectDataQualityChart({ kpis, scopedStats }) {
  const confidence = (scopedStats?.confidenceBreakdown || []).map((row) => ({
    confidence: row.confidence,
    count: Number(row.count || 0),
    spend: Number(row.spend || 0),
    income: Number(row.income || 0),
    excluded: Number(row.excluded || 0),
    filter: { confidence: row.confidence },
  }));
  const amountBands = (scopedStats?.amountBands || []).map((row) => ({
    label: row.label,
    minAmount: Number(row.minAmount || 0),
    maxAmount: row.maxAmount == null ? null : Number(row.maxAmount),
    count: Number(row.count || 0),
    spend: Number(row.spend || 0),
  }));
  const toCheck = (scopedStats?.categoryTop || []).find((row) => row.category === "Do sprawdzenia");
  const lowConfidence = confidence.find((row) => row.confidence === "Niska");
  return {
    cards: [
      { label: "Do sprawdzenia", value: Number(toCheck?.count || 0), amount: Number(toCheck?.spend || 0), filter: { category: "Do sprawdzenia" } },
      { label: "Niska pewność", value: Number(lowConfidence?.count || 0), filter: { confidence: "Niska" } },
      { label: "Korekty roku", value: Number(kpis?.corrections || 0), filter: null, useTimeScope: false },
    ],
    confidence,
    amountBands,
  };
}

export function selectImportHealth({ data, importRuns, years }) {
  const runs = importRuns || [];
  const latestRun = runs[0] || null;
  const yearRows = years || [];
  const totalTransactions = yearRows.reduce((sum, row) => sum + Number(row.transactions || 0), 0);
  const duplicatesRemoved = runs.reduce((sum, row) => sum + Number(row.duplicatesRemoved || 0), 0);
  return {
    latestRun,
    cards: [
      { label: "Lata w bazie", value: yearRows.length, detail: yearRows.map((row) => row.year).join(", ") || "brak" },
      { label: "Transakcje w bazie", value: totalTransactions, detail: data?.year ? `aktywny rok: ${data.year}` : "po importach" },
      { label: "Do sprawdzenia", value: Number(data?.kpis?.toCheck || 0), amount: Number(data?.kpis?.toCheckAmount || 0), detail: "aktywny rok" },
      { label: "Usunięte duplikaty", value: duplicatesRemoved, detail: latestRun ? "ostatnie 50 importów" : "brak historii" },
    ],
  };
}

export function selectTransactionPresets() {
  return [
    { label: "Do sprawdzenia", filters: { category: "Do sprawdzenia" } },
    { label: "Niska pewność", filters: { confidence: "Niska" } },
    { label: "Transfery techniczne", filters: { flow: "excluded" } },
    { label: "Inwestycje", filters: { flow: "financial", bucket: "Inwestycje" } },
    { label: "Konto oszczędnościowe", filters: { flow: "financial", bucket: "Konto oszczędnościowe" } },
    { label: "Nadpłaty kredytu", filters: { flow: "financial", bucket: "Nadpłata kredytu" } },
    { label: "Duże kwoty", filters: { flow: "spend", sort: "amount,asc" } },
  ];
}

export function selectMonthStats(data, selectedMonth) {
  if (!data || !selectedMonth) return null;
  const monthKey = monthKeyFromLabel(selectedMonth);
  return {
    monthKey,
    summary: data.monthly.find((row) => row.month === selectedMonth || row.monthKey === monthKey),
  };
}

export function selectCalendarStats(calendarReport, selectedDay) {
  if (!calendarReport?.days?.length) return null;
  const [yearPart, monthPart] = calendarReport.month.split("-").map(Number);
  const firstWeekday = new Date(yearPart, monthPart - 1, 1).getDay();
  const offset = (firstWeekday + 6) % 7;
  const cells = [];
  for (let i = 0; i < offset; i += 1) cells.push({ empty: true, key: `empty-${i}` });

  calendarReport.days.forEach((day) => {
    cells.push({
      key: `day-${day.day}`,
      day: day.day,
      spend: Number(day.spend || 0),
      income: Number(day.income || 0),
      transactions: Number(day.transactions || 0),
      biggest: day.biggest,
    });
  });

  const selected = Number(selectedDay) || calendarReport.days.filter((day) => day.transactions > 0).at(-1)?.day || 1;
  const selectedRow = calendarReport.days.find((day) => day.day === selected);

  return {
    monthKey: calendarReport.month,
    rawCalendar: calendarReport,
    cells,
    selected,
    selectedDate: selectedRow?.date,
    selectedSpend: Number(selectedRow?.spend || 0),
    selectedIncome: Number(selectedRow?.income || 0),
    selectedTransactions: Number(selectedRow?.transactions || 0),
  };
}

export function selectVisibleSpend(transactions) {
  return transactions.reduce((sum, tx) => sum + Number(tx.spend || tx.analysisSpend || 0), 0);
}

export function selectFinancialFlows(data) {
  const rows = (data?.categories || [])
    .filter((row) => FINANCIAL_FLOW_CATEGORIES.has(row.category) && Number(row.excluded || 0) > 0)
    .map((row) => ({
      category: row.category,
      outgoing: Number(row.excluded || 0),
      count: Number(row.count || 0),
      kind: financialFlowKind(row.category),
    }))
    .sort((left, right) => right.outgoing - left.outgoing || left.category.localeCompare(right.category));
  const investments = rows.filter((row) => row.kind === "investment");
  const savingsAccounts = rows.filter((row) => row.kind === "savingsAccount");
  const loanOverpayments = rows.filter((row) => row.kind === "loanOverpayment");
  const investmentTotal = investments.reduce((sum, row) => sum + row.outgoing, 0);
  const savingsAccountGrossTotal = savingsAccounts.reduce((sum, row) => sum + row.outgoing, 0);
  const savingsAccountTotal = Number(data?.kpis?.savingsAccountNetChange ?? savingsAccountGrossTotal);
  const savingsAccountInflows = Number(data?.kpis?.savingsAccountInflows ?? savingsAccountGrossTotal);
  const savingsAccountOutflows = Number(data?.kpis?.savingsAccountOutflows ?? Math.max(0, savingsAccountInflows - savingsAccountTotal));
  const loanOverpaymentTotal = loanOverpayments.reduce((sum, row) => sum + row.outgoing, 0);
  return {
    rows,
    investments,
    savingsAccounts,
    loanOverpayments,
    investmentTotal,
    investmentCount: investments.reduce((sum, row) => sum + row.count, 0),
    savingsAccountTotal,
    savingsAccountGrossTotal,
    savingsAccountInflows,
    savingsAccountOutflows,
    savingsAccountCount: savingsAccounts.reduce((sum, row) => sum + row.count, 0),
    loanOverpaymentTotal,
    loanOverpaymentCount: loanOverpayments.reduce((sum, row) => sum + row.count, 0),
    total: investmentTotal + savingsAccountTotal + loanOverpaymentTotal,
    count: rows.reduce((sum, row) => sum + row.count, 0),
  };
}

function financialFlowKind(category) {
  if (category === "Nadpłata kredytu") return "loanOverpayment";
  if (category === "Konto oszczędnościowe") return "savingsAccount";
  return "investment";
}

export function selectMonthFinancialFlow(data, monthKey) {
  if (!data || !monthKey) return 0;
  const row = (data.monthly || []).find((item) => item.monthKey === monthKey || item.month === monthKey);
  return Number(row?.savingsInvestments || 0);
}

export function emptyAnalytics() {
  return {
    spend: 0,
    income: 0,
    transactionCount: 0,
    areaTop: [],
    groupTop: [],
    categoryTop: [],
    subcategoryTop: [],
    hierarchyTop: [],
    financialFlows: [],
    merchants: [],
    oneoffs: [],
    monthlyCategoryTrends: [],
    monthlyBucketTrends: [],
    monthlyMerchantTrends: [],
    fixednessBreakdown: [],
    confidenceBreakdown: [],
    amountBands: [],
  };
}

function unique(values) {
  return Array.from(new Set(values.filter(Boolean))).sort((left, right) => left.localeCompare(right, "pl"));
}

function withAll(values) {
  return ["Wszystkie", ...values];
}

function uniquePairs(values, keyFn) {
  const seen = new Set();
  return values
    .filter((row) => row?.subcategory && row?.category)
    .filter((row) => {
      const key = keyFn(row);
      if (seen.has(key)) return false;
      seen.add(key);
      return true;
    })
    .sort((left, right) => `${left.category} ${left.subcategory}`.localeCompare(`${right.category} ${right.subcategory}`, "pl"));
}

function addSankeyLink(links, source, target, value, filter) {
  const amount = Number(value || 0);
  if (!source || !target || amount <= 0) return;
  links.push({ source, target, value: amount, filter });
}

function ensureSunburstNode(nodes, name, filter) {
  if (!nodes.has(name)) {
    nodes.set(name, {
      childrenMap: new Map(),
      count: 0,
      filter,
      name,
      value: 0,
    });
  }
  return nodes.get(name);
}

function finalizeSunburstNode(node) {
  const children = Array.from(node.childrenMap.values()).map(finalizeSunburstNode);
  const childValue = children.reduce((sum, child) => sum + Number(child.value || 0), 0);
  return {
    children,
    count: node.count || children.reduce((sum, child) => sum + Number(child.count || 0), 0),
    filter: node.filter,
    name: node.name,
    value: node.value || childValue,
  };
}

function budgetMixFilter(bucket) {
  return switchValue(bucket, {
    "Obowiązkowe stałe": { bucket: "Obowiązkowe stałe" },
    "Obowiązkowe zmienne": { bucket: "Obowiązkowe zmienne" },
    "Do rozbicia": { bucket: "Do rozbicia" },
    Nieobowiązkowe: { bucket: "Nieobowiązkowe" },
    Inwestycje: { flow: "financial", bucket: "Inwestycje" },
    "Konto oszczędnościowe": { flow: "financial", bucket: "Konto oszczędnościowe" },
    "Nadpłata kredytu": { flow: "financial", bucket: "Nadpłata kredytu" },
    Potrzeby: { bucket: "Potrzeby" },
    "Potrzeby mieszane": { bucket: "Potrzeby mieszane" },
    Zachcianki: { bucket: "Zachcianki" },
    "Marketplace do rozbicia": { bucket: "Zachcianki do rozbicia" },
    "Oszczędzanie/inwestycje wykonane": { flow: "financial" },
  });
}

function switchValue(value, cases) {
  return Object.hasOwn(cases, value) ? cases[value] : null;
}

export function bucketAliasesForLimit(name) {
  return new Set(BUCKET_LIMIT_ALIASES[name] || [name]);
}

export function canonicalBudgetBucket(bucket) {
  if (!bucket) return "";
  for (const option of BUDGET_BUCKET_OPTIONS) {
    if (bucketAliasesForLimit(option).has(bucket)) {
      return option;
    }
  }
  return bucket;
}

function isPrimaryLimitRow(row) {
  return row?.scope === "bucket" && !PRIMARY_LIMIT_EXCLUDED.has(row.name || row.category);
}

export function limitKey(rowOrScope, name) {
  if (typeof rowOrScope === "string") {
    return `${rowOrScope || "category"}:${name || ""}`;
  }
  const row = rowOrScope || {};
  return `${row.scope || "category"}:${row.name || row.category || ""}`;
}

function toSavingsFocusRow(row) {
  const limit = Number(row.limit || 0);
  const current = Number(row.currentMonthProjection || row.currentMonthSpend || row.currentMonthly || 0);
  const potentialMonthly = Math.max(0, Number(row.potentialMonthly || 0), limit > 0 ? current - limit : 0);
  const decision = savingsDecision(row);
  const scope = row.scope || "category";
  const name = row.name || row.category;
  const filter = scope === "bucket"
    ? { bucket: name }
    : scope === "area"
      ? { area: name }
      : scope === "group"
        ? { group: name }
        : { category: row.category };
  return {
    ...row,
    current,
    limit,
    potentialMonthly,
    overLimit: limit > 0 && current > limit,
    decision,
    tone: decision === "Do cięcia" ? "cut" : decision === "Do rozbicia" ? "review" : "protected",
    note: savingsDecisionNote(row, decision),
    filter,
  };
}

function savingsDecision(row) {
  const bucket = row.name && row.scope === "bucket" ? row.name : row.bucket;
  if (PROTECTED_CATEGORIES.has(row.category) || OBLIGATORY_BUCKETS.has(bucket)) return "Nie ciąć automatycznie";
  if (REVIEW_CATEGORIES.has(row.category) || REVIEW_BUCKETS.has(bucket)) return "Do rozbicia";
  if (CUT_NOW_BUCKETS.has(bucket)) return "Do cięcia";
  return Number(row.potentialMonthly || 0) > 0 ? "Do rozbicia" : "Nie ciąć automatycznie";
}

function savingsDecisionNote(row, decision) {
  if (decision === "Do cięcia") return "kontrolowalny wydatek";
  if (decision === "Do rozbicia") return row.category === "Zdrowie i uroda" ? "oddziel leczenie od urody" : "sprawdź transakcje przed cięciem";
  return "potrzeba lub zobowiązanie";
}

function compareSavingsFocus(left, right) {
  const rank = { "Do cięcia": 0, "Do rozbicia": 1, "Nie ciąć automatycznie": 2 };
  return rank[left.decision] - rank[right.decision]
    || Number(right.potentialMonthly || 0) - Number(left.potentialMonthly || 0)
    || Number(right.current || 0) - Number(left.current || 0)
    || String(left.category).localeCompare(String(right.category), "pl");
}

function comparePrimaryPlanRows(left, right) {
  const rank = {
    "Obowiązkowe stałe": 0,
    "Obowiązkowe zmienne": 1,
    "Do rozbicia": 2,
    Nieobowiązkowe: 3,
    Inwestycje: 4,
    "Konto oszczędnościowe": 5,
    "Nadpłata kredytu": 6,
    Potrzeby: 0,
    "Potrzeby mieszane": 2,
    Zachcianki: 3,
    "Zachcianki do rozbicia": 2,
  };
  const leftName = left.name || left.category || "";
  const rightName = right.name || right.category || "";
  return (rank[leftName] ?? 20) - (rank[rightName] ?? 20)
    || Number(right.currentMonthly || 0) - Number(left.currentMonthly || 0)
    || leftName.localeCompare(rightName, "pl");
}

function sumBy(rows, key) {
  return (rows || []).reduce((sum, row) => sum + Number(row[key] || 0), 0);
}

function roundToNearest(value, step) {
  if (!value || !step) return 0;
  return Math.round(value / step) * step;
}
