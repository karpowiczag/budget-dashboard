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

  const nav = page.getByRole("navigation", { name: "Sekcje budżetu" });

  // Default landing: Przegląd (Overview) home.
  await expect(nav).toBeVisible();
  await expect(nav.getByRole("button", { name: /Przegląd/ })).toHaveClass(/active/);
  await expect(page.getByRole("heading", { name: "Ile możemy bezpiecznie wydać?" })).toBeVisible();
  await expect(page.getByRole("heading", { name: "Skrót" })).toBeVisible();

  // Budżet section: Ten miesiąc (control), then Limity (plan).
  await nav.getByRole("button", { name: /Budżet/ }).click();
  await expect(page.getByRole("heading", { name: "Kontrola bieżącego miesiąca" })).toBeVisible();
  await expect(page.getByRole("heading", { name: "Co ciąć teraz" })).toBeVisible();
  await expect(page.getByRole("heading", { name: "Tempo wydatków" })).toBeVisible();
  await page.getByRole("button", { name: "Limity", exact: true }).click();
  await expect(page.getByRole("heading", { name: "Jakie miesięczne limity ustawiamy?" })).toBeVisible();
  await expect(page.getByText("Główne limity")).toBeVisible();
  if (importedFake) {
    await page.getByLabel("Komfortowy fundusz awaryjny w miesiącach").fill("8");
    await page.getByRole("button", { name: "Zapisz ustawienia" }).click();
    await expect(page.getByText(/Ustawienia zapisane/)).toBeVisible();
  }

  // Analiza section: Raporty (default), then Cykliczne.
  await nav.getByRole("button", { name: /Analiza/ }).click();
  await expect(page.getByRole("heading", { name: "Co się zmienia w czasie?" })).toBeVisible();
  await expect(page.getByRole("heading", { name: "Sankey przepływu pieniędzy" })).toBeVisible();
  await page.getByRole("button", { name: "Trends", exact: true }).click();
  await expect(page.getByRole("heading", { name: "Cashflow miesięczny" })).toBeVisible();
  await page.getByRole("button", { name: "Wydatki", exact: true }).click();
  await expect(page.getByRole("heading", { name: "Udział kategorii w wydatkach" })).toBeVisible();
  await page.getByRole("button", { name: "Cykliczne", exact: true }).click();
  await expect(page.getByRole("heading", { name: "Co wraca co miesiąc?" })).toBeVisible();
  await expect(page.getByRole("heading", { name: "Zobowiązania miesiąca" })).toBeVisible();

  // Majątek section: Przepływy (wealth).
  await nav.getByRole("button", { name: /Majątek/ }).click();
  await expect(page.getByRole("heading", { name: "Ile przesuwamy w oszczędności, inwestycje i dług?" })).toBeVisible();
  await expect(page.getByRole("heading", { name: "Przepływy majątkowe" })).toBeVisible();

  // Transakcje section.
  await nav.getByRole("button", { name: /Transakcje/ }).click();
  await expect(page.getByRole("heading", { name: "Transakcje i kategoryzacja" })).toBeVisible();
  await expect(page.getByRole("table")).toBeVisible();
  await expect(page.getByRole("button", { name: /CSV/ }).first()).toBeVisible();
});

async function uploadFakeCsv(page) {
  await expect(page.getByRole("heading", { name: "Import danych" })).toBeVisible();
  await page.locator('input[type="file"]').setInputFiles(
    path.resolve(process.cwd(), "../src/test/resources/fixtures/mbank/fake-2026.csv"),
  );
  await expect(page.getByRole("heading", { name: "Ile możemy bezpiecznie wydać?" })).toBeVisible();
}
