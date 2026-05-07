import { expect, test } from "@playwright/test";
import path from "node:path";

test("imports fake CSV and serves dashboard desktop workflow", async ({ page }) => {
  await page.goto("/");
  let importedFake = false;

  await expect(page.getByRole("heading", { name: /Analiza 20\d{2}|Import danych/ })).toBeVisible();
  if (await page.getByRole("heading", { name: "Import danych" }).isVisible().catch(() => false)) {
    await uploadFakeCsv(page);
    importedFake = true;
  }

  await expect(page.getByRole("heading", { name: /Analiza 20\d{2}/ })).toBeVisible();
  await expect(page.getByRole("navigation", { name: "Widoki dashboardu" })).toBeVisible();
  await page.getByRole("button", { name: "Miesiąc", exact: true }).click();
  await expect(page.getByRole("button", { name: "Miesiąc", exact: true })).toHaveClass(/active/);
  await expect(page.getByRole("heading", { name: "Kontrola bieżącego miesiąca" })).toBeVisible();
  await expect(page.getByRole("heading", { name: "Mapa dziennych wydatków" })).toBeVisible();
  await expect(page.getByRole("heading", { name: "Największe przekroczenie limitu" })).toBeVisible();

  await page.getByRole("button", { name: "Raporty", exact: true }).click();
  await expect(page.getByRole("heading", { name: "Kontrolowalne wydatki" })).toBeVisible();
  await expect(page.getByRole("heading", { name: /Raporty:/ })).toBeVisible();
  await expect(page.getByRole("heading", { name: "Sankey przepływu pieniędzy" })).toBeVisible();
  await expect(page.getByRole("heading", { name: "Cashflow miesięczny" })).toBeVisible();
  await page.getByRole("button", { name: "Kategorie", exact: true }).click();
  await expect(page.getByRole("heading", { name: "Trend kategorii miesiąc po miesiącu" })).toBeVisible();
  await expect(page.getByRole("heading", { name: "Hierarchia: obszar → grupa → kategoria" })).toBeVisible();
  await expect(page.getByRole("heading", { name: /Pareto kategorii/ })).toBeVisible();
  await page.getByRole("button", { name: "Sprzedawcy", exact: true }).click();
  await expect(page.getByRole("heading", { name: "Koncentracja sprzedawców" })).toBeVisible();
  await expect(page.getByRole("heading", { name: "Trend sprzedawców miesiąc po miesiącu" })).toBeVisible();
  await expect(page.getByRole("heading", { name: /Sprzedawcy/ })).toBeVisible();
  await expect(page.getByRole("table").first()).toBeVisible();

  await page.getByRole("button", { name: "Transakcje", exact: true }).click();
  await expect(page.getByRole("table")).toBeVisible();
  await expect(page.getByRole("button", { name: /Filtry zaawansowane/ })).toBeVisible();
  await expect(page.getByRole("button", { name: /CSV/ }).first()).toBeVisible();

  await page.getByRole("button", { name: /Plan|Symulacja/ }).click();
  await expect(page.getByText(/Realistyczne cięcie|Realistycznie można było ciąć/)).toBeVisible();
  await expect(page.getByRole("table").first()).toBeVisible();
  if (importedFake) {
    await page.getByLabel("Komfortowy fundusz awaryjny w miesiącach").fill("8");
    await page.getByRole("button", { name: "Zapisz ustawienia" }).click();
    await expect(page.getByText(/Ustawienia zapisane/)).toBeVisible();
  }
});

async function uploadFakeCsv(page) {
  await expect(page.getByRole("heading", { name: "Import danych" })).toBeVisible();
  await page.locator('input[type="file"]').setInputFiles(
    path.resolve(process.cwd(), "../src/test/resources/fixtures/mbank/fake-2026.csv")
  );
  await expect(page.getByRole("heading", { name: /Analiza 20\d{2}/ })).toBeVisible();
}
