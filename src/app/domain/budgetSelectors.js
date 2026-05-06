export function monthKeyFromLabel(monthLabel) {
  if (!monthLabel) return "";
  return `${monthLabel.slice(3, 7)}-${monthLabel.slice(0, 2)}`;
}

export function selectBuckets(data) {
  if (!data) return ["Wszystkie"];
  return ["Wszystkie", ...Array.from(new Set(data.transactions.map((tx) => tx["Koszyk budżetu"] || "Inne"))).sort()];
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

  return {
    monthKey,
    summary,
    transactions,
    categories: mapToSpendRows(byCategory, "category"),
    buckets: mapToSpendRows(byBucket, "bucket"),
    merchants: mapToSpendRows(byMerchant, "merchant"),
    oneoffs: transactions
      .filter((tx) => Number(tx["Wydatek analizy"] || 0) >= 500)
      .sort((a, b) => Number(b["Wydatek analizy"] || 0) - Number(a["Wydatek analizy"] || 0)),
  };
}

export function selectCalendarStats(monthStats, selectedDay) {
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
    selectedCategories: mapToSpendRows(byCategory, "category"),
  };
}

export function selectScopedTransactions(data, timeScope, selectedMonth, selectedDay, calendarStats) {
  if (!data) return [];
  if (timeScope === "all" || !selectedMonth) return data.transactions;
  const monthKey = monthKeyFromLabel(selectedMonth);
  if (timeScope === "month") {
    return data.transactions.filter((tx) => tx["Miesiąc"] === monthKey);
  }
  const day = String(calendarStats?.selected || selectedDay || "").padStart(2, "0");
  if (!day.trim()) return data.transactions.filter((tx) => tx["Miesiąc"] === monthKey);
  return data.transactions.filter((tx) => tx["Miesiąc"] === monthKey && tx.Data.endsWith(`-${day}`));
}

export function selectDrillFilteredTransactions(scopedTransactions, drillFilter) {
  if (!drillFilter) return scopedTransactions;
  return scopedTransactions.filter((tx) => {
    if (drillFilter.type === "area") return tx["Obszar budżetu"] === drillFilter.value;
    if (drillFilter.type === "group") return tx.Grupa === drillFilter.value;
    if (drillFilter.type === "category") return tx["Kategoria skorygowana"] === drillFilter.value;
    if (drillFilter.type === "subcategory") return `${tx["Kategoria skorygowana"]} · ${tx.Podkategoria}` === drillFilter.value;
    return true;
  });
}

export function selectScopedStats(scopedTransactions, drillFilteredTransactions) {
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
    const subcategoryKey = `${cat} · ${subcategory}`;
    const hierarchyKey = `${area}|${group}|${cat}|${subcategory}`;

    byArea.set(area, (byArea.get(area) || 0) + txSpend);
    byGroup.set(group, (byGroup.get(group) || 0) + txSpend);
    byCategory.set(cat, (byCategory.get(cat) || 0) + txSpend);
    bySubcategory.set(subcategoryKey, (bySubcategory.get(subcategoryKey) || 0) + txSpend);
    byMerchant.set(merchant, (byMerchant.get(merchant) || 0) + txSpend);

    if (!byHierarchy.has(hierarchyKey)) {
      byHierarchy.set(hierarchyKey, {
        area,
        group,
        category: cat,
        subcategory,
        spend: 0,
        count: 0,
      });
    }
    const entry = byHierarchy.get(hierarchyKey);
    entry.spend += txSpend;
    entry.count += 1;
  }

  const hierarchyTop = [...byHierarchy.values()].sort((a, b) => b.spend - a.spend);
  const merchants = [...byMerchant.entries()].map(([merchant, sum]) => ({ merchant, sum })).sort((a, b) => b.sum - a.sum);
  const oneoffs = scopedTransactions
    .filter((tx) => Number(tx["Wydatek analizy"] || 0) >= 500)
    .sort((a, b) => Number(b["Wydatek analizy"] || 0) - Number(a["Wydatek analizy"] || 0));

  return {
    spend,
    income,
    areaTop: mapToSpendRows(byArea, "area"),
    groupTop: mapToSpendRows(byGroup, "group"),
    categoryTop: mapToSpendRows(byCategory, "category"),
    subcategoryTop: mapToSpendRows(bySubcategory, "subcategory"),
    hierarchyTop,
    merchants,
    oneoffs,
  };
}

export function selectFilteredTransactions(drillFilteredTransactions, query, bucket) {
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
}

export function selectVisibleSpend(transactions) {
  return transactions.reduce((sum, tx) => sum + Number(tx["Wydatek analizy"] || 0), 0);
}

function mapToSpendRows(map, keyName) {
  return [...map.entries()]
    .map(([name, spend]) => ({ [keyName]: name, spend }))
    .sort((a, b) => b.spend - a.spend);
}
