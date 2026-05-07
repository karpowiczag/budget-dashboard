function csrfHeaders() {
  const token = document.cookie
    .split("; ")
    .find((entry) => entry.startsWith("XSRF-TOKEN="))
    ?.split("=")[1];
  return token ? { "X-XSRF-TOKEN": decodeURIComponent(token) } : {};
}

async function readJson(response, fallbackMessage) {
  const payload = await response.json().catch(() => ({}));
  if (!response.ok) {
    throw new Error(payload.detail || payload.error || fallbackMessage);
  }
  return payload;
}

function queryString(params) {
  const query = new URLSearchParams();
  Object.entries(params).forEach(([key, value]) => {
    if (value === undefined || value === null || value === "" || value === "Wszystkie") return;
    query.set(key, value);
  });
  const value = query.toString();
  return value ? `?${value}` : "";
}

export async function fetchYears() {
  const response = await fetch("/api/v1/years");
  return readJson(response, "Nie mogę wczytać listy lat");
}

export async function fetchDashboard(year) {
  const response = await fetch(`/api/v1/reports/${year}/dashboard`);
  return readJson(response, `Nie mogę wczytać danych ${year}`);
}

export async function fetchCalendar(year, month) {
  const response = await fetch(`/api/v1/reports/${year}/calendar${queryString({ month })}`);
  return readJson(response, "Nie mogę wczytać kalendarza");
}

export async function fetchAnalytics(year, params = {}) {
  const response = await fetch(`/api/v1/reports/${year}/analytics${queryString(params)}`);
  return readJson(response, "Nie mogę wczytać analityki");
}

export async function fetchTransactions(year, params = {}) {
  const response = await fetch(`/api/v1/reports/${year}/transactions${queryString(params)}`);
  return readJson(response, "Nie mogę wczytać transakcji");
}

export async function fetchImportRuns() {
  const response = await fetch("/api/v1/imports/runs");
  return readJson(response, "Nie mogę wczytać historii importów");
}

export async function fetchBudgetSettings() {
  const response = await fetch("/api/v1/settings/budget");
  return readJson(response, "Nie mogę wczytać ustawień budżetu");
}

export async function updateBudgetSettings(settings) {
  const response = await fetch("/api/v1/settings/budget", {
    method: "PUT",
    headers: {
      "Content-Type": "application/json",
      ...csrfHeaders(),
    },
    body: JSON.stringify(settings),
  });
  return readJson(response, "Nie mogę zapisać ustawień budżetu");
}

export async function uploadTransactions(file) {
  const body = new FormData();
  body.append("file", file);
  const response = await fetch("/api/v1/imports/uploads", {
    method: "POST",
    body,
    headers: csrfHeaders(),
  });
  return readJson(response, "Import CSV nie powiódł się");
}

export async function rebuildTransactions(year) {
  const response = await fetch(`/api/v1/imports/rebuild${queryString({ year })}`, {
    method: "POST",
    headers: csrfHeaders(),
  });
  return readJson(response, "Przebudowa danych nie powiodła się");
}
