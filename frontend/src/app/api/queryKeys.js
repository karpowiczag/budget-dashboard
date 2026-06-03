export const budgetQueryKeys = {
  analytics: (year, filters) => ["budget", "analytics", String(year || ""), filters || {}],
  budgetSettings: ["budget", "settings"],
  calendar: (year, month) => ["budget", "calendar", String(year || ""), month || ""],
  dashboard: (year) => ["budget", "dashboard", String(year || "")],
  fire: ["budget", "fire"],
  fireSettings: ["budget", "fire", "settings"],
  importRuns: ["budget", "imports", "runs"],
  netWorth: ["budget", "networth"],
  goals: ["budget", "goals"],
  categories: ["budget", "categories"],
  transactions: (year, filters) => ["budget", "transactions", String(year || ""), filters || {}],
  years: ["budget", "years"],
};
