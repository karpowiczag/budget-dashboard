import { CHART_COLORS } from "./charts.js";
import { moneyDec, percent } from "./formatters.js";

// Resolve a design token at build time so charts follow the active light/dark
// theme. Falls back to the original literal when no document/var is available
// (e.g. SSR or the jsdom test environment), keeping behaviour identical there.
function cssVar(name, fallback) {
  if (typeof document === "undefined") return fallback;
  const value = getComputedStyle(document.documentElement).getPropertyValue(name);
  return value && value.trim() ? value.trim() : fallback;
}

const chartAxis = () => cssVar("--border-strong", "#cbd5e1");
const chartGrid = () => cssVar("--border-subtle", "#e5e7eb");
const chartText = () => cssVar("--text-2", "#475569");

const COLORS = {
  amber: "#b45309",
  blue: "#2563eb",
  gray: "#475569",
  green: "#0f766e",
  purple: "#9333ea",
  red: "#be123c",
  slate: "#17324d",
};

export function moneyAxis(value) {
  const amount = Number(value || 0);
  const abs = Math.abs(amount);
  if (abs >= 1000) {
    const thousands = amount / 1000;
    return `${thousands.toLocaleString("pl-PL", { maximumFractionDigits: abs < 10000 ? 1 : 0 })}k`;
  }
  return Math.round(amount).toLocaleString("pl-PL");
}

export function baseGrid(extra = {}) {
  return {
    bottom: 26,
    containLabel: true,
    left: 10,
    right: 18,
    top: 18,
    ...extra,
  };
}

export function categoryAxis(data, extra = {}) {
  return {
    axisLabel: { color: chartText(), fontSize: 12 },
    axisLine: { lineStyle: { color: chartAxis() } },
    axisTick: { show: false },
    data,
    type: "category",
    ...extra,
  };
}

export function valueAxis(extra = {}) {
  return {
    axisLabel: { color: chartText(), formatter: moneyAxis, fontSize: 12 },
    axisLine: { lineStyle: { color: chartAxis() } },
    splitLine: { lineStyle: { color: chartGrid() } },
    type: "value",
    ...extra,
  };
}

export function tooltip(formatter) {
  return {
    appendToBody: true,
    borderColor: "#d8e1ea",
    className: "chartTooltip",
    confine: true,
    formatter,
    trigger: "axis",
    valueFormatter: (value) => moneyDec(value),
  };
}

export function itemTooltip(formatter) {
  return {
    appendToBody: true,
    borderColor: "#d8e1ea",
    className: "chartTooltip",
    confine: true,
    formatter,
    trigger: "item",
  };
}

export function buildBudgetBurnDownOption(data = []) {
  return {
    grid: baseGrid({ bottom: 20, right: 22 }),
    tooltip: tooltip((params) => {
      const day = params?.[0]?.data?.day ?? params?.[0]?.axisValue ?? "";
      return [
        `<strong>Dzień ${escapeHtml(day)}</strong>`,
        ...params.map((item) => `${marker(item.color)}${escapeHtml(item.seriesName)}: ${moneyDec(item.value)}`),
      ].join("<br/>");
    }),
    xAxis: categoryAxis(data.map((row) => row.day)),
    yAxis: valueAxis(),
    series: [
      {
        areaStyle: { color: "rgba(37, 99, 235, 0.16)" },
        data: data.map((row) => ({ ...row, value: Number(row.cumulativeSpend || 0) })),
        emphasis: { focus: "series" },
        lineStyle: { color: COLORS.blue, width: 2 },
        name: "Wydane narastająco",
        showSymbol: false,
        type: "line",
      },
      {
        data: data.map((row) => ({ ...row, value: Number(row.targetPace || 0) })),
        emphasis: { focus: "series" },
        lineStyle: { color: COLORS.amber, type: "dashed", width: 2 },
        name: "Tempo targetu",
        showSymbol: false,
        type: "line",
      },
    ],
  };
}

export function buildDailyCalendarHeatmapOption(data = []) {
  const values = data.map((row) => Number(row.spend || 0));
  const max = Math.max(1, ...values);
  const dates = data.map((row) => row.date).filter(Boolean).sort();
  const range = dates.length ? [dates[0], dates.at(-1)] : undefined;
  return {
    calendar: {
      cellSize: ["auto", 34],
      dayLabel: {
        color: chartText(),
        firstDay: 1,
        nameMap: ["Nd", "Pn", "Wt", "Śr", "Cz", "Pt", "Sb"],
      },
      itemStyle: { borderColor: "#ffffff", borderWidth: 2 },
      left: 28,
      monthLabel: { color: chartText(), fontSize: 12 },
      orient: "horizontal",
      range,
      right: 28,
      top: 36,
      yearLabel: { show: false },
    },
    tooltip: itemTooltip((item) => {
      const row = item?.data || {};
      return [
        `<strong>${escapeHtml(row.date)}</strong>`,
        `Wydatki: ${moneyDec(row.spend)}`,
        `Wpływy: ${moneyDec(row.income)}`,
        `Transakcje: ${escapeHtml(row.transactions || 0)}`,
      ].join("<br/>");
    }),
    visualMap: {
      bottom: 0,
      calculable: true,
      inRange: { color: ["#e0f2fe", "#38bdf8", "#f59e0b", "#be123c"] },
      left: "center",
      max,
      min: 0,
      orient: "horizontal",
      textStyle: { color: chartText() },
    },
    series: [
      {
        coordinateSystem: "calendar",
        data: data.map((row) => ({ ...row, value: [row.date, Number(row.spend || 0)] })),
        name: "Wydatki dzienne",
        type: "heatmap",
      },
    ],
  };
}

export function buildCategoryLimitProjectionOption(data = []) {
  const rows = data.slice(0, 10);
  return {
    grid: baseGrid({ bottom: 14, left: 4, right: 20, top: 12 }),
    tooltip: tooltip((params) => {
      const row = params?.[0]?.data || {};
      return [
        `<strong>${escapeHtml(row.category)}</strong>`,
        ...params.map((item) => `${marker(item.color)}${escapeHtml(item.seriesName)}: ${moneyDec(item.value)}`),
      ].join("<br/>");
    }),
    xAxis: valueAxis(),
    yAxis: categoryAxis(rows.map((row) => row.category), {
      axisLabel: { color: chartText(), fontSize: 12, overflow: "truncate", width: 126 },
    }),
    series: [
      {
        barGap: "-60%",
        data: rows.map((row) => ({ ...row, value: Number(row.limit || 0) })),
        itemStyle: { borderRadius: [0, 4, 4, 0], color: "#cbd5e1" },
        name: "Limit",
        type: "bar",
      },
      {
        data: rows.map((row) => ({ ...row, value: Number(row.projected || 0) })),
        itemStyle: { borderRadius: [0, 4, 4, 0], color: COLORS.amber },
        name: "Prognoza",
        type: "bar",
      },
    ],
  };
}

export function buildLimitGaugeOption(data = []) {
  const worst = data.slice().sort((left, right) => Number(right.usage || 0) - Number(left.usage || 0))[0];
  const usage = Number(worst?.usage || 0);
  return {
    series: [
      {
        axisLabel: { color: chartText(), formatter: (value) => percent(Number(value) / 100) },
        axisLine: {
          lineStyle: {
            color: [
              [0.53, COLORS.green],
              [0.67, COLORS.amber],
              [1, COLORS.red],
            ],
            width: 14,
          },
        },
        detail: {
          color: chartText(),
          formatter: () => `${Math.round(usage * 100)}%`,
          fontSize: 26,
          fontWeight: 800,
        },
        max: 150,
        min: 0,
        pointer: { width: 4 },
        progress: { show: false },
        radius: "88%",
        splitLine: { distance: -14, length: 14 },
        title: { color: chartText(), fontSize: 12, offsetCenter: [0, "60%"] },
        type: "gauge",
        data: [{ ...worst, name: worst?.category || "Brak ryzyka", value: Math.min(150, Math.round(usage * 100)) }],
      },
    ],
    tooltip: itemTooltip((item) => {
      const row = item?.data || {};
      return `<strong>${escapeHtml(row.category || "Brak ryzyka")}</strong><br/>Prognoza: ${moneyDec(row.projected)}<br/>Limit: ${moneyDec(row.limit)}`;
    }),
  };
}

export function buildMonthlyCashflowComboOption(data = []) {
  return {
    grid: baseGrid({ right: 44 }),
    legend: { bottom: 0, textStyle: { color: chartText() } },
    tooltip: tooltip((params) => {
      const label = params?.[0]?.data?.month ?? params?.[0]?.axisValue ?? "";
      return [
        `<strong>${escapeHtml(label)}</strong>`,
        ...params.map((item) => {
          const value = item.seriesName === "Stopa oszczędzania" ? percent(item.value) : moneyDec(item.value);
          return `${marker(item.color)}${escapeHtml(item.seriesName)}: ${value}`;
        }),
      ].join("<br/>");
    }),
    xAxis: categoryAxis(data.map((row) => row.month)),
    yAxis: [
      valueAxis(),
      {
        axisLabel: { color: chartText(), formatter: (value) => percent(value), fontSize: 12 },
        axisLine: { lineStyle: { color: chartAxis() } },
        splitLine: { show: false },
        type: "value",
      },
    ],
    series: [
      barSeries("Wpływy", "income", data, COLORS.green),
      barSeries("Wydatki", "spend", data, COLORS.blue),
      barSeries("Inwestycje/nadpłaty", "savingsInvestments", data, COLORS.purple),
      {
        data: data.map((row) => ({ ...row, value: Number(row.savingsRate || 0) })),
        lineStyle: { color: COLORS.amber, width: 2 },
        name: "Stopa oszczędzania",
        showSymbol: false,
        type: "line",
        yAxisIndex: 1,
      },
    ],
  };
}

export function buildFireProjectionOption(scenarios = [], target = 0, currentAge = 36, targetAge = 50, currentValue = 0) {
  const ages = Array.from({ length: Math.max(1, targetAge - currentAge + 1) }, (_, index) => currentAge + index);
  return {
    grid: baseGrid({ right: 28 }),
    legend: { bottom: 0, textStyle: { color: chartText() } },
    tooltip: tooltip((params) => {
      const age = params?.[0]?.axisValue ?? "";
      return [
        `<strong>Wiek ${escapeHtml(age)}</strong>`,
        ...params.map((item) => `${marker(item.color)}${escapeHtml(item.seriesName)}: ${moneyDec(item.value)}`),
      ].join("<br/>");
    }),
    xAxis: categoryAxis(ages),
    yAxis: valueAxis(),
    series: [
      ...scenarios.map((scenario, index) => ({
        data: ages.map((age) => ({
          scenario: scenario.id,
          age,
          value: interpolate(Number(currentValue || 0), Number(scenario.projectedAtFire || 0), ages, age),
        })),
        lineStyle: { color: [COLORS.amber, COLORS.blue, COLORS.green][index] || COLORS.slate, width: 2 },
        name: scenario.label,
        showSymbol: false,
        type: "line",
      })),
      {
        data: ages.map((age) => ({ age, value: Number(target || 0) })),
        lineStyle: { color: COLORS.red, type: "dashed", width: 2 },
        name: "Cel FIRE",
        showSymbol: false,
        type: "line",
      },
    ],
  };
}

export function buildFireAllocationOption(rows = []) {
  return {
    legend: { bottom: 0, textStyle: { color: chartText() } },
    series: [
      {
        data: rows.map((row) => ({
          name: row.assetClass,
          value: Number(row.value || 0),
          ...row,
        })),
        emphasis: { itemStyle: { shadowBlur: 8, shadowColor: "rgba(15, 23, 42, 0.18)" } },
        label: { formatter: "{b}: {d}%" },
        radius: ["42%", "72%"],
        type: "pie",
      },
    ],
    tooltip: itemTooltip((item) => {
      const row = item?.data || {};
      return [
        `<strong>${escapeHtml(row.assetClass || row.name)}</strong>`,
        `Wartość: ${moneyDec(row.value)}`,
        `Udział: ${percent(row.share)}`,
        `Cel: ${percent(row.targetShare)}`,
      ].join("<br/>");
    }),
  };
}

function interpolate(currentValue, projectedAtFire, ages, age) {
  const start = Math.max(0, currentValue);
  const index = Math.max(0, ages.indexOf(age));
  const denominator = Math.max(1, ages.length - 1);
  return start + (projectedAtFire - start) * (index / denominator);
}

export function buildCashflowSankeyOption(model = { links: [], nodes: [] }) {
  const links = model.links || [];
  const nodes = model.nodes || [];
  return {
    series: [
      {
        bottom: 26,
        data: nodes.map((node) => ({ ...node, shortName: sankeyLabel(node.name) })),
        draggable: false,
        emphasis: { focus: "adjacency" },
        label: {
          color: chartText(),
          fontSize: 12,
          fontWeight: 700,
          formatter: ({ data }) => data.shortName || data.name,
          lineHeight: 15,
          overflow: "truncate",
          width: 156,
        },
        layoutIterations: 64,
        left: 22,
        lineStyle: { color: "source", curveness: 0.42, opacity: 0.46 },
        links,
        nodeAlign: "justify",
        nodeGap: 18,
        nodeWidth: 16,
        right: 188,
        top: 26,
        type: "sankey",
      },
    ],
    tooltip: itemTooltip((item) => {
      const row = item?.data || {};
      if (row.source && row.target) {
        return `${escapeHtml(row.source)} → ${escapeHtml(row.target)}<br/>${moneyDec(row.value)}`;
      }
      return escapeHtml(row.name || "");
    }),
  };
}

function sankeyLabel(value) {
  const labels = {
    "Dostępne środki": "Dostępne",
    "Środki z salda": "Saldo",
    "Wolne środki po przepływach": "Wolne środki",
    "Nadwyżka operacyjna po kosztach": "Nadwyżka operacyjna",
    "Nadwyżka do decyzji": "Nadwyżka do decyzji",
    "Nadwyżka operacyjna": "Nadwyżka operacyjna",
    "Oszczędności/nadpłaty": "Oszczędności",
    Inwestycje: "Inwestycje",
    "Konto oszczędnościowe": "Konto oszcz.",
    "Obowiązkowe stałe": "Stałe",
    "Obowiązkowe zmienne": "Zmienne",
    "Do rozbicia": "Do rozbicia",
    Nieobowiązkowe: "Nieobowiązkowe",
    "Marketplace do rozbicia": "Marketplace",
    "Potrzeby mieszane": "Mieszane potrzeby",
  };
  return Object.hasOwn(labels, value) ? labels[value] : value;
}

export function buildCategoryParetoOption(data = []) {
  return {
    grid: baseGrid({ bottom: 70, right: 46 }),
    tooltip: tooltip((params) => {
      const row = params?.[0]?.data || {};
      return [
        `<strong>${escapeHtml(row.category)}</strong>`,
        ...params.map((item) => {
          const value = item.seriesName === "Udział skumulowany" ? percent(item.value) : moneyDec(item.value);
          return `${marker(item.color)}${escapeHtml(item.seriesName)}: ${value}`;
        }),
      ].join("<br/>");
    }),
    xAxis: categoryAxis(data.map((row) => row.category), {
      axisLabel: { interval: 0, rotate: 30, width: 90, overflow: "truncate" },
    }),
    yAxis: [
      valueAxis(),
      {
        axisLabel: { color: chartText(), formatter: (value) => percent(value), fontSize: 12 },
        axisLine: { lineStyle: { color: chartAxis() } },
        max: 1,
        min: 0,
        splitLine: { show: false },
        type: "value",
      },
    ],
    series: [
      {
        data: data.map((row) => ({ ...row, value: Number(row.spend || 0) })),
        itemStyle: { borderRadius: [4, 4, 0, 0], color: COLORS.green },
        name: "Wydatki",
        type: "bar",
      },
      {
        data: data.map((row) => ({ ...row, value: Number(row.cumulativeShare || 0) })),
        lineStyle: { color: COLORS.amber, width: 2 },
        name: "Udział skumulowany",
        showSymbol: false,
        type: "line",
        yAxisIndex: 1,
      },
    ],
  };
}

export function buildCategoryTrendOption(data = []) {
  return buildMonthlyDimensionTrendOption({
    data,
    dimensionKey: "category",
    name: "Trend kategorii",
  });
}

export function buildMerchantTrendOption(data = []) {
  return buildMonthlyDimensionTrendOption({
    data,
    dimensionKey: "merchant",
    name: "Trend sprzedawców",
  });
}

export function buildHierarchySunburstOption(data = []) {
  return {
    series: [
      {
        data,
        emphasis: { focus: "ancestor" },
        highlightPolicy: "ancestor",
        itemStyle: { borderColor: "#ffffff", borderWidth: 2 },
        label: { minAngle: 8, rotate: "radial" },
        radius: [0, "92%"],
        sort: (left, right) => Number(right.value || 0) - Number(left.value || 0),
        type: "sunburst",
      },
    ],
    tooltip: itemTooltip((item) => {
      const row = item?.data || {};
      return `<strong>${escapeHtml(row.name)}</strong><br/>${moneyDec(row.value)}${row.count ? `<br/>${escapeHtml(row.count)} transakcji` : ""}`;
    }),
  };
}

export function buildBudgetMixTreemapOption(data = []) {
  return {
    tooltip: {
      appendToBody: true,
      borderColor: "#d8e1ea",
      className: "chartTooltip",
      formatter: (item) => {
        const row = item.data || {};
        return `<strong>${escapeHtml(row.name)}</strong><br/>${moneyDec(row.sum ?? row.value)}<br/>${percent(row.incomeShare || 0)} wpływów`;
      },
    },
    series: [
      {
        breadcrumb: { show: false },
        data: data.map((row, index) => ({
          ...row,
          itemStyle: { color: CHART_COLORS[index % CHART_COLORS.length] },
          label: { color: "#ffffff", formatter: "{b}", fontSize: 12, fontWeight: 800 },
          value: Number(row.value || row.sum || 0),
        })),
        leafDepth: 1,
        nodeClick: false,
        roam: false,
        type: "treemap",
        upperLabel: { show: false },
      },
    ],
  };
}

export function buildBudgetMixDonutOption(data = []) {
  return buildShareDonutOption({
    data,
    labelKey: "name",
    name: "Udział koszyków",
    tooltipFormatter: (row) => [
      `<strong>${escapeHtml(row.name)}</strong>`,
      `Kwota: ${moneyDec(row.sum ?? row.value)}`,
      `Udział wpływów: ${percent(row.incomeShare || row.share || 0)}`,
    ].join("<br/>"),
    valueKey: "value",
  });
}

export function buildCategoryShareDonutOption(data = []) {
  return buildShareDonutOption({
    data,
    labelKey: "category",
    name: "Udział kategorii",
    tooltipFormatter: (row) => [
      `<strong>${escapeHtml(row.category || row.name)}</strong>`,
      `Wydatki: ${moneyDec(row.spend ?? row.value)}`,
      `Udział: ${percent(row.share || 0)}`,
      row.count ? `Transakcje: ${escapeHtml(row.count)}` : "",
    ].filter(Boolean).join("<br/>"),
    valueKey: "spend",
  });
}

export function buildMerchantShareDonutOption(data = []) {
  return buildShareDonutOption({
    data,
    labelKey: "merchant",
    name: "Udział sprzedawców",
    tooltipFormatter: (row) => [
      `<strong>${escapeHtml(row.merchant || row.name)}</strong>`,
      `Wydatki: ${moneyDec(row.sum ?? row.value)}`,
      `Udział: ${percent(row.share || 0)}`,
      row.count ? `Transakcje: ${escapeHtml(row.count)}` : "",
    ].filter(Boolean).join("<br/>"),
    valueKey: "sum",
  });
}

export function buildFixednessBreakdownOption(data = []) {
  return {
    legend: { bottom: 0, textStyle: { color: chartText() } },
    series: [
      {
        avoidLabelOverlap: true,
        data: data.map((row, index) => ({
          ...row,
          itemStyle: { color: CHART_COLORS[index % CHART_COLORS.length] },
          name: row.fixedness,
          value: Number(row.total || row.spend || 0),
        })),
        emphasis: { label: { show: true, fontSize: 14, fontWeight: 800 } },
        label: { formatter: "{b}\n{d}%", fontSize: 12 },
        name: "Stałość kosztów",
        radius: ["48%", "74%"],
        type: "pie",
      },
    ],
    tooltip: itemTooltip((item) => {
      const row = item?.data || {};
      return `<strong>${escapeHtml(row.fixedness || row.name)}</strong><br/>${moneyDec(row.value)}<br/>${escapeHtml(row.count || 0)} transakcji`;
    }),
  };
}

export function buildSavingsWaterfallOption(data = []) {
  const colors = {
    expense: COLORS.red,
    good: COLORS.green,
    income: COLORS.blue,
    neutral: COLORS.blue,
    saving: COLORS.purple,
    target: COLORS.amber,
  };
  const bases = waterfallBases(data);
  return {
    grid: baseGrid({ bottom: 48 }),
    tooltip: tooltip((params) => {
      const item = (Array.isArray(params) ? params.find((entry) => entry.seriesName === "Kwota") : params) || {};
      const row = item?.data || {};
      return [
        `<strong>${escapeHtml(row.label)}</strong>`,
        `${row.amount < 0 ? "Zmniejszenie" : "Kwota"}: ${moneyDec(row.absAmount ?? row.amount ?? item.value)}`,
        row.note ? escapeHtml(row.note).replace(/(-?\d+(?:\.\d+)?)/g, (value) => moneyDec(value)) : "",
      ].filter(Boolean).join("<br/>");
    }),
    xAxis: categoryAxis(data.map((row) => row.label), {
      axisLabel: { interval: 0, width: 104, overflow: "break" },
    }),
    yAxis: valueAxis(),
    series: [
      {
        data: bases.map((base) => ({ value: base })),
        itemStyle: { borderColor: "transparent", color: "transparent" },
        silent: true,
        stack: "waterfall",
        type: "bar",
      },
      {
        data: data.map((row) => ({
          ...row,
          itemStyle: { color: colors[row.tone] || COLORS.blue },
          value: Math.abs(Number(row.absAmount ?? row.amount ?? 0)),
        })),
        itemStyle: { borderRadius: [4, 4, 0, 0] },
        name: "Kwota",
        stack: "waterfall",
        type: "bar",
      },
    ],
  };
}

export function buildSavingsRadarOption(data = []) {
  const max = Math.max(1, ...data.map((row) => Number(row.value || 0)));
  return {
    radar: {
      indicator: data.map((row) => ({ name: row.label, max: Math.ceil(max * 1.15) })),
      radius: "66%",
      splitArea: { areaStyle: { color: ["#f8fafc", "#ffffff"] } },
      splitLine: { lineStyle: { color: chartGrid() } },
    },
    series: [
      {
        areaStyle: { color: "rgba(15, 118, 110, 0.18)" },
        data: [{ name: "Potencjał", value: data.map((row) => Number(row.value || 0)), rows: data }],
        lineStyle: { color: COLORS.green, width: 2 },
        symbolSize: 6,
        type: "radar",
      },
    ],
    tooltip: itemTooltip((item) => {
      const rows = item?.data?.rows || data;
      return rows.map((row) => `${escapeHtml(row.label)}: ${moneyDec(row.value)}`).join("<br/>");
    }),
  };
}

export function buildOutlierTimelineOption(data = []) {
  return buildScatterTimelineOption({
    color: COLORS.red,
    data,
    emptyDomain: [],
    name: "Duże wydatki",
    xKey: "date",
  });
}

export function buildMerchantFunnelOption(data = []) {
  return {
    series: [
      {
        data: data.map((row) => ({
          ...row,
          name: row.merchant,
          value: Number(row.sum || row.spend || 0),
        })),
        gap: 3,
        label: {
          color: chartText(),
          formatter: ({ data }) => `${truncate(data.merchant, 22)}\n${moneyAxis(data.value)}`,
          fontSize: 12,
        },
        left: "6%",
        maxSize: "90%",
        minSize: "16%",
        sort: "descending",
        top: 18,
        type: "funnel",
      },
    ],
    tooltip: itemTooltip((item) => {
      const row = item?.data || {};
      return `<strong>${escapeHtml(row.merchant)}</strong><br/>${moneyDec(row.value)}<br/>${escapeHtml(row.count || 0)} transakcji`;
    }),
  };
}

export function buildRecurringTimelineOption(data = []) {
  return {
    grid: baseGrid({ bottom: 48, left: 18, right: 28, top: 24 }),
    tooltip: tooltip((params) => {
      const item = Array.isArray(params) ? params[0] : params;
      const row = item?.data || {};
      return [
        `<strong>${escapeHtml(row.merchant)}</strong>`,
        `Dzień: ${escapeHtml(row.day)}`,
        `Kwota: ${moneyDec(row.amount)}`,
      ].join("<br/>");
    }),
    xAxis: {
      ...valueAxis({ max: 31, min: 1, interval: 5 }),
      axisLabel: { color: chartText(), fontSize: 12, showMaxLabel: false },
      name: "Dzień miesiąca",
      nameGap: 28,
      nameLocation: "middle",
      splitLine: { lineStyle: { color: chartGrid() } },
    },
    yAxis: valueAxis({ name: "Kwota", nameGap: 34, nameLocation: "middle" }),
    series: [
      {
        data: data.map((row) => ({ ...row, value: [Number(row.day || 0), Number(row.amount || 0)] })),
        itemStyle: { color: COLORS.green },
        name: "Cykliczne",
        symbolSize: (value) => symbolSize(value?.[1]),
        type: "scatter",
      },
    ],
  };
}

export function buildSpendBarOption({ color = COLORS.green, data = [], dataKey }) {
  return buildHorizontalBarOption({
    color,
    data,
    labelKey: dataKey,
    name: "Wydatki",
    valueKey: "spend",
  });
}

export function buildConfidenceOption(data = []) {
  return {
    legend: { bottom: 0, textStyle: { color: chartText() } },
    series: [
      {
        data: data.map((row, index) => ({
          ...row,
          itemStyle: { color: CHART_COLORS[index % CHART_COLORS.length] },
          name: row.confidence,
          value: Number(row.count || 0),
        })),
        label: { formatter: "{b}: {c}", fontSize: 12 },
        radius: ["46%", "72%"],
        type: "pie",
      },
    ],
    tooltip: itemTooltip((item) => {
      const row = item?.data || {};
      return `<strong>${escapeHtml(row.confidence || row.name)}</strong><br/>Transakcje: ${escapeHtml(row.value || 0)}<br/>Wydatki: ${moneyDec(row.spend)}`;
    }),
  };
}

export function buildAmountBandsOption(data = []) {
  return buildSimpleBarOption({
    color: COLORS.green,
    data,
    labelKey: "label",
    name: "Transakcje wg kwoty",
    valueKey: "count",
    yAxisFormatter: (value) => String(Math.round(Number(value || 0))),
  });
}

function buildShareDonutOption({ data, labelKey, name, tooltipFormatter, valueKey }) {
  return {
    legend: {
      bottom: 0,
      textStyle: { color: chartText() },
      type: "scroll",
    },
    series: [
      {
        avoidLabelOverlap: true,
        data: data.map((row, index) => ({
          ...row,
          itemStyle: { color: CHART_COLORS[index % CHART_COLORS.length] },
          name: row[labelKey] || row.name,
          value: Number(row[valueKey] ?? row.value ?? 0),
        })),
        emphasis: { label: { show: true, fontSize: 14, fontWeight: 800 } },
        label: {
          formatter: ({ data: row, percent: itemPercent }) => {
            const share = row.share == null ? itemPercent / 100 : Number(row.share || 0);
            return `${truncate(row.name, 18)}\n${percent(share)}`;
          },
          fontSize: 12,
        },
        minAngle: 4,
        minShowLabelAngle: 8,
        name,
        radius: ["42%", "72%"],
        type: "pie",
      },
    ],
    tooltip: itemTooltip((item) => tooltipFormatter(item?.data || {})),
  };
}

function buildHorizontalBarOption({ color, data, labelKey, name, valueKey }) {
  return {
    grid: baseGrid({ bottom: 16, left: 4, right: 20, top: 12 }),
    tooltip: tooltip((params) => {
      const item = Array.isArray(params) ? params[0] : params;
      const row = item?.data || {};
      return `${escapeHtml(row[labelKey])}: ${moneyDec(row[valueKey] ?? item.value)}`;
    }),
    xAxis: valueAxis(),
    yAxis: categoryAxis(data.map((row) => row[labelKey]), {
      axisLabel: { color: chartText(), fontSize: 12, overflow: "truncate", width: 136 },
    }),
    series: [
      {
        data: data.map((row) => ({ ...row, value: Number(row[valueKey] || 0) })),
        itemStyle: { borderRadius: [0, 4, 4, 0], color },
        name,
        type: "bar",
      },
    ],
  };
}

function buildMonthlyDimensionTrendOption({ data, dimensionKey, name }) {
  const months = uniqueOrdered(data.map((row) => row.monthKey || row.month));
  const labelsByMonth = new Map(data.map((row) => [row.monthKey || row.month, row.month || row.monthKey]));
  const dimensions = uniqueOrdered(data.map((row) => row[dimensionKey])).slice(0, 8);
  return {
    dataZoom: months.length > 8 ? [{ bottom: 6, height: 18, type: "slider" }] : [],
    grid: baseGrid({ bottom: months.length > 8 ? 54 : 30, right: 26, top: 32 }),
    legend: {
      formatter: (name) => truncate(name, 28),
      top: 0,
      textStyle: { color: chartText() },
      type: "scroll",
    },
    tooltip: tooltip((params) => {
      const month = labelsByMonth.get(params?.[0]?.axisValue) || params?.[0]?.axisValue || "";
      return [
        `<strong>${escapeHtml(month)}</strong>`,
        ...params
          .filter((item) => Number(item.value || 0) > 0)
          .map((item) => `${marker(item.color)}${escapeHtml(item.seriesName)}: ${moneyDec(item.value)}`),
      ].join("<br/>");
    }),
    xAxis: categoryAxis(months, {
      axisLabel: { color: chartText(), formatter: (value) => labelsByMonth.get(value) || value, fontSize: 12 },
    }),
    yAxis: valueAxis(),
    series: dimensions.map((dimension, index) => ({
      areaStyle: { opacity: 0.12 },
      data: months.map((monthKey) => {
        const row = data.find((candidate) => (candidate.monthKey || candidate.month) === monthKey && candidate[dimensionKey] === dimension);
        return row ? { ...row, value: Number(row.spend || 0) } : { [dimensionKey]: dimension, monthKey, value: 0 };
      }),
      emphasis: { focus: "series" },
      lineStyle: { color: CHART_COLORS[index % CHART_COLORS.length], width: 2 },
      name: dimension,
      showSymbol: false,
      stack: name,
      type: "line",
    })),
  };
}

function buildSimpleBarOption({ color, data, labelKey, name, valueKey, yAxisFormatter = moneyAxis }) {
  return {
    grid: baseGrid({ bottom: 26, right: 20, top: 12 }),
    tooltip: tooltip((params) => {
      const item = Array.isArray(params) ? params[0] : params;
      const row = item?.data || {};
      return `${escapeHtml(row[labelKey])}: ${escapeHtml(row[valueKey] ?? item.value)}`;
    }),
    xAxis: categoryAxis(data.map((row) => row[labelKey])),
    yAxis: valueAxis({ axisLabel: { color: chartText(), formatter: yAxisFormatter, fontSize: 12 } }),
    series: [
      {
        data: data.map((row) => ({ ...row, value: Number(row[valueKey] || 0) })),
        itemStyle: { borderRadius: [4, 4, 0, 0], color },
        name,
        type: "bar",
      },
    ],
  };
}

function buildScatterTimelineOption({ color, data, name, xKey }) {
  return {
    grid: baseGrid({ bottom: 52, right: 20 }),
    tooltip: tooltip((params) => {
      const item = Array.isArray(params) ? params[0] : params;
      const row = item?.data || {};
      return [
        `<strong>${escapeHtml(row.merchant || row[xKey])}</strong>`,
        `Data: ${escapeHtml(row[xKey])}`,
        `Kwota: ${moneyDec(row.amount)}`,
      ].join("<br/>");
    }),
    xAxis: categoryAxis(data.map((row) => row[xKey]), {
      axisLabel: { interval: "auto", rotate: 25 },
    }),
    yAxis: valueAxis(),
    series: [
      {
        data: data.map((row) => ({ ...row, value: [row[xKey], Number(row.amount || 0), Number(row.amount || 0)] })),
        itemStyle: { color },
        name,
        symbolSize: (value) => symbolSize(value?.[1]),
        type: "scatter",
      },
    ],
  };
}

function barSeries(name, key, data, color) {
  return {
    data: data.map((row) => ({ ...row, value: Number(row[key] || 0) })),
    itemStyle: { borderRadius: [4, 4, 0, 0], color },
    name,
    type: "bar",
  };
}

function waterfallBases(data) {
  let running = 0;
  return data.map((row) => {
    const amount = Number(row.amount || 0);
    if (row.kind === "total") {
      running = amount;
      return 0;
    }
    const next = running + amount;
    const base = amount < 0 ? next : running;
    running = next;
    return Math.max(0, base);
  });
}

function uniqueOrdered(values) {
  return Array.from(new Set(values.filter(Boolean)));
}

function truncate(value, length) {
  const text = String(value ?? "");
  return text.length > length ? `${text.slice(0, length - 1)}…` : text;
}

function marker(color) {
  return `<span style="display:inline-block;width:9px;height:9px;border-radius:999px;background:${color};margin-right:6px"></span>`;
}

function symbolSize(value) {
  const amount = Math.abs(Number(value || 0));
  if (!amount) return 8;
  return Math.max(8, Math.min(28, Math.sqrt(amount) / 3.4));
}

function escapeHtml(value) {
  return String(value ?? "")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#039;");
}
