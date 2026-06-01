import { expect, test } from "@playwright/test";
import path from "node:path";

test("imports fake CSV and serves dashboard desktop workflow", async ({ page }) => {
  await page.goto("/");
  let importedFake = false;

  await expect(page.getByRole("heading", { name: /Ile możemy bezpiecznie wydać\?|Import danych/ })).toBeVisible();
  if (await page.getByRole("heading", { name: "Import danych" }).isVisible().catch(() => false)) {
    await uploadFakeCsv(page);
    importedFake = true;
  }

  await expect(page.getByRole("heading", { name: "Ile możemy bezpiecznie wydać?" })).toBeVisible();
  await expect(page.getByRole("navigation", { name: "Moduły budżetu" })).toBeVisible();
  await expect(page.getByRole("button", { name: /Kontrola/ })).toHaveClass(/active/);
  await expect(page.getByRole("heading", { name: "Kontrola bieżącego miesiąca" })).toBeVisible();
  await expect(page.getByRole("heading", { name: "Spending plan miesiąca" })).toBeVisible();
  await expect(page.getByRole("heading", { name: "Co ciąć teraz" })).toBeVisible();
  await expect(page.getByRole("heading", { name: "Tempo wydatków" })).toBeVisible();

  await page.getByRole("button", { name: "Raporty", exact: true }).click();
  await expect(page.getByRole("heading", { name: "Co się zmienia w czasie?" })).toBeVisible();
  await expect(page.getByRole("heading", { name: "Sankey przepływu pieniędzy" })).toBeVisible();
  await expect(page.getByRole("heading", { name: "Zakres raportu" })).toBeVisible();
  await page.getByRole("button", { name: "Trends", exact: true }).click();
  await expect(page.getByRole("heading", { name: "Cashflow miesięczny" })).toBeVisible();
  await page.getByRole("button", { name: "Wydatki", exact: true }).click();
  await expect(page.getByRole("heading", { name: "Udział kategorii w wydatkach" })).toBeVisible();
  await expect(page.getByRole("heading", { name: /Pareto kategorii/ })).toBeVisible();
  await page.getByRole("button", { name: "Trends", exact: true }).click();
  await expect(page.getByRole("heading", { name: "Trend kategorii miesiąc po miesiącu" })).toBeVisible();

  await page.getByRole("button", { name: "Majątek", exact: true }).click();
  await expect(page.getByRole("heading", { name: "Ile przesuwamy w oszczędności, inwestycje i dług?" })).toBeVisible();
  await expect(page.getByText("Transakcyjnie, nie saldo kont")).toBeVisible();
  await expect(page.getByRole("heading", { name: "Przepływy majątkowe" })).toBeVisible();

  await page.getByRole("button", { name: "Zobowiązania", exact: true }).click();
  await expect(page.getByRole("heading", { name: "Co wraca co miesiąc?" })).toBeVisible();
  await expect(page.getByRole("heading", { name: "Zobowiązania miesiąca" })).toBeVisible();
  await expect(page.getByRole("heading", { name: "Co przyjdzie w tym miesiącu" })).toBeVisible();

  await page.getByRole("button", { name: "Transakcje", exact: true }).click();
  await expect(page.getByRole("heading", { name: "Transakcje i kategoryzacja" })).toBeVisible();
  await expect(page.getByRole("table")).toBeVisible();
  await expect(page.getByRole("button", { name: "Pokaż wykresy" })).toBeVisible();
  await expect(page.getByRole("button", { name: /Filtry zaawansowane/ })).toBeVisible();
  await expect(page.getByRole("button", { name: /CSV/ }).first()).toBeVisible();

  await page.getByRole("button", { name: /Plan|Symulacja/ }).click();
  await expect(page.getByRole("heading", { name: "Jakie miesięczne limity ustawiamy?" })).toBeVisible();
  await expect(page.getByText(/Realistyczne cięcie|Realistycznie można było ciąć/)).toBeVisible();
  await expect(page.getByText("Główne limity")).toBeVisible();
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
  await expect(page.getByRole("heading", { name: "Ile możemy bezpiecznie wydać?" })).toBeVisible();
}
