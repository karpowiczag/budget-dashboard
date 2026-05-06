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
  return ["Wszystkie", ...Array.from(buckets).filter(Boolean).sort()];
}

export function selectPlanRows(data, customLimits) {
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
}

export function selectPlanSummary(planRows) {
  const potentialMonthly = planRows.reduce((sum, row) => sum + row.potentialMonthly, 0);
  return {
    potentialMonthly,
    potentialYearly: potentialMonthly * 12,
  };
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
    cells,
    selected,
    selectedSpend: Number(selectedRow?.spend || 0),
    selectedIncome: Number(selectedRow?.income || 0),
    selectedTransactions: Number(selectedRow?.transactions || 0),
  };
}

export function selectVisibleSpend(transactions) {
  return transactions.reduce((sum, tx) => sum + Number(tx.spend || tx.analysisSpend || 0), 0);
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
    merchants: [],
    oneoffs: [],
  };
}
