export const budgetQueryKeys = {
  analytics: (year, filters) => ["budget", "analytics", String(year || ""), filters || {}],
  budgetSettings: ["budget", "settings"],
  calendar: (year, month) => ["budget", "calendar", String(year || ""), month || ""],
  dashboard: (year) => ["budget", "dashboard", String(year || "")],
  importRuns: ["budget", "imports", "runs"],
  transactions: (year, filters) => ["budget", "transactions", String(year || ""), filters || {}],
  years: ["budget", "years"],
};
