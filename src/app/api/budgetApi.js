function csrfHeaders() {
  const token = document.cookie
    .split("; ")
    .find((entry) => entry.startsWith("XSRF-TOKEN="))
    ?.split("=")[1];
  return token ? { "X-XSRF-TOKEN": decodeURIComponent(token) } : {};
}

async function readJson(response, fallbackMessage) {
  const payload = await response.json();
  if (!response.ok) {
    throw new Error(payload.error || fallbackMessage);
  }
  return payload;
}

export async function fetchYears() {
  const response = await fetch("/api/years");
  if (!response.ok) throw new Error("Nie mogę wczytać listy lat");
  return response.json();
}

export async function fetchBudget(year) {
  const response = await fetch(`/api/budget/${year}`);
  if (!response.ok) throw new Error(`Nie mogę wczytać danych ${year}`);
  return response.json();
}

export async function uploadTransactions(file) {
  const body = new FormData();
  body.append("file", file);
  const response = await fetch("/api/uploads", {
    method: "POST",
    body,
    headers: csrfHeaders(),
  });
  return readJson(response, "Import CSV nie powiódł się");
}
