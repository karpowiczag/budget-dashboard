import { expect, test } from "@playwright/test";
import path from "node:path";

test("imports fake CSV and serves dashboard desktop workflow", async ({ page }) => {
  await page.goto("/");

  if (await page.getByRole("heading", { name: "Import danych" }).isVisible().catch(() => false)) {
    await uploadFakeCsv(page);
  } else {
    await expect(page.getByRole("heading", { name: "Analiza 2026" })).toBeVisible();
    await page.getByRole("button", { name: "Import" }).click();
    await uploadFakeCsv(page);
  }

  await expect(page.getByRole("heading", { name: "Analiza 2026" })).toBeVisible();
  await expect(page.getByRole("navigation", { name: "Widoki dashboardu" })).toBeVisible();
  await page.getByRole("button", { name: "Podsumowanie" }).click();
  await expect(page.getByRole("button", { name: "Podsumowanie" })).toHaveClass(/active/);

  await page.getByRole("button", { name: "Kategorie" }).click();
  await expect(page.getByRole("cell", { name: "Żywność i chemia", exact: true })).toBeVisible();

  await page.getByRole("button", { name: "Transakcje" }).click();
  await expect(page.getByRole("table")).toBeVisible();
  await expect(page.getByRole("cell", { name: "BIEDRONKA" }).first()).toBeVisible();

  await page.getByRole("button", { name: "Plan" }).click();
  await page.getByLabel("Komfortowy fundusz awaryjny w miesiącach").fill("8");
  await page.getByRole("button", { name: "Zapisz ustawienia" }).click();
  await expect(page.getByText(/Ustawienia zapisane/)).toBeVisible();
});

async function uploadFakeCsv(page) {
  await expect(page.getByRole("heading", { name: /Import/ })).toBeVisible();
  await page.locator('input[type="file"]').setInputFiles(
    path.resolve(process.cwd(), "../src/test/resources/fixtures/mbank/fake-2026.csv")
  );
  await expect(page.getByText(/Zaimportowano/)).toBeVisible();
}
