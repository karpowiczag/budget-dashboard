package com.budget.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.budget.application.importing.BudgetImportService;
import com.budget.application.importing.ImportSummary;
import com.budget.application.importing.TransactionImportFile;
import com.budget.application.reporting.BudgetReportStore;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "app.database.url=jdbc:h2:mem:fake_csv_import;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH;DB_CLOSE_DELAY=-1",
        "app.security.oauth-enabled=false"
})
class FakeCsvImportIntegrationTest {
    @Autowired
    private BudgetImportService importService;

    @Autowired
    private BudgetReportStore store;

    @Test
    void importsAnalyzesAndPersistsFakeMbankExports() throws Exception {
        var summary2026 = importFixture("/fixtures/mbank/fake-2026.csv", "lista_operacji_260101_260331_fake.csv");
        var summary2025 = importFixture("/fixtures/mbank/fake-2025.csv", "lista_operacji_250101_251231_fake.csv");

        assertThat(summary2026).isEqualTo(new ImportSummary("ok", List.of(2026), 17, 70_000, 5_080, "CSV imported; raw file was not retained"));
        assertThat(summary2025).isEqualTo(new ImportSummary("ok", List.of(2025), 6, 29_000, 1_550, "CSV imported; raw file was not retained"));

        assertThat(store.findYears()).anySatisfy(row -> {
            assertThat(row).containsEntry("year", 2025);
            assertThat(number(row, "transactions")).isEqualTo(6);
            assertThat(number(row, "income")).isEqualTo(29_000);
            assertThat(number(row, "spend")).isEqualTo(1_550);
        }).anySatisfy(row -> {
            assertThat(row).containsEntry("year", 2026);
            assertThat(number(row, "transactions")).isEqualTo(17);
            assertThat(number(row, "income")).isEqualTo(70_000);
            assertThat(number(row, "spend")).isEqualTo(5_080);
        });

        var payload2026 = store.findPayload(2026);
        assertThat(payload2026).containsEntry("year", 2026);
        assertThat(number(payload2026, "activeMonths")).isEqualTo(3);

        var kpis = map(payload2026, "kpis");
        assertThat(number(kpis, "income")).isEqualTo(70_000);
        assertThat(number(kpis, "spend")).isEqualTo(5_080);
        assertThat(number(kpis, "excludedOutgoing")).isEqualTo(70_000);
        assertThat(number(kpis, "excludedIncoming")).isEqualTo(120);
        assertThat(number(kpis, "toCheck")).isEqualTo(1);
        assertThat(number(kpis, "toCheckAmount")).isEqualTo(750);

        assertMonthly(payload2026, "01.2026", 34_000, 3_460, 10);
        assertMonthly(payload2026, "02.2026", 18_000, 810, 4);
        assertMonthly(payload2026, "03.2026", 18_000, 810, 3);

        var transactions = rows(payload2026, "transactions");
        assertTransaction(transactions, "RĘCZNA SPŁATA KARTY KREDYT", "Spłata karty kredytowej", 0, 65_000, 0, "Transfer techniczny");
        assertTransaction(transactions, "ZWROT ZAKUPU SKLEP TESTOWY", "Zwroty i korekty", 0, 0, 120, "Transfer techniczny");
        assertTransaction(transactions, "PRZELEW DO BM MBANKU IKZE", "Oszczędności i inwestycje", 0, 3_000, 0, "Oszczędzanie/inwestycje");
        assertTransaction(transactions, "SKLEP TAJEMNICZY", "Do sprawdzenia", 750, 0, 0, "Do sprawdzenia");
        assertThat(findTransaction(transactions, "BIEDRONKA ZAKUP; KASA 1")).containsEntry("Podkategoria", "Market spożywczy");

        assertThat(rows(payload2026, "recurring"))
                .anySatisfy(row -> {
                    assertThat(row).containsEntry("merchant", "NETFLIX");
                    assertThat(row).containsEntry("category", "Multimedia, książki i prasa");
                    assertThat(number(row, "months")).isEqualTo(3);
                    assertThat(number(row, "monthlyAverage")).isEqualTo(60);
                });

        assertThat(store.findPayload(2025)).containsEntry("year", 2025);
        assertThat(store.findImportRuns())
                .filteredOn(run -> "ok".equals(run.status()))
                .extracting(run -> run.year())
                .contains(2025, 2026);
    }

    private ImportSummary importFixture(String resourcePath, String fileName) throws IOException {
        var bytes = resourceBytes(resourcePath);
        return importService.importUpload(new TransactionImportFile(fileName, bytes.length, () -> new ByteArrayInputStream(bytes)));
    }

    private byte[] resourceBytes(String resourcePath) throws IOException {
        try (var input = getClass().getResourceAsStream(resourcePath)) {
            if (input == null) {
                throw new IllegalArgumentException("Missing test resource " + resourcePath);
            }
            return input.readAllBytes();
        }
    }

    private void assertMonthly(Map<String, Object> payload, String month, double income, double spend, int transactions) {
        var row = rows(payload, "monthly").stream()
                .filter(item -> month.equals(item.get("month")))
                .findFirst()
                .orElseThrow();
        assertThat(number(row, "income")).isEqualTo(income);
        assertThat(number(row, "spend")).isEqualTo(spend);
        assertThat(number(row, "transactions")).isEqualTo(transactions);
    }

    private void assertTransaction(
            List<Map<String, Object>> transactions,
            String description,
            String category,
            double analysisSpend,
            double excludedOutgoing,
            double excludedIncoming,
            String bucket
    ) {
        var row = findTransaction(transactions, description);
        assertThat(row).containsEntry("Kategoria skorygowana", category);
        assertThat(row).containsEntry("Koszyk budżetu", bucket);
        assertThat(number(row, "Wydatek analizy")).isEqualTo(analysisSpend);
        assertThat(number(row, "Wyłączone wychodzące")).isEqualTo(excludedOutgoing);
        assertThat(number(row, "Wyłączone przychodzące")).isEqualTo(excludedIncoming);
    }

    private Map<String, Object> findTransaction(List<Map<String, Object>> transactions, String description) {
        return transactions.stream()
                .filter(row -> description.equals(row.get("Opis")))
                .findFirst()
                .orElseThrow();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> map(Map<String, Object> source, String key) {
        return (Map<String, Object>) source.get(key);
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> rows(Map<String, Object> source, String key) {
        return (List<Map<String, Object>>) source.get(key);
    }

    private double number(Map<String, Object> source, String key) {
        return ((Number) source.get(key)).doubleValue();
    }
}
