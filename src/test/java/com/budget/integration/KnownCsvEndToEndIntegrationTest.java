package com.budget.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.budget.application.analysis.BudgetAnalysisService;
import com.budget.application.reporting.BudgetReportStore;
import com.budget.infrastructure.csv.BankCsvReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "app.database.url=jdbc:h2:mem:known_csv;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH;DB_CLOSE_DELAY=-1",
        "app.security.oauth-enabled=false"
})
class KnownCsvEndToEndIntegrationTest {
    private static final List<Integer> KNOWN_YEARS = List.of(2025, 2026);

    @Autowired
    private BankCsvReader csvReader;

    @Autowired
    private BudgetAnalysisService analysisService;

    @Autowired
    private BudgetReportStore store;

    @Test
    void importsAnalyzesAndStoresKnownLocalCsvExports() throws Exception {
        var csvFiles = KNOWN_YEARS.stream()
                .map(KnownCsvEndToEndIntegrationTest::latestCsvForYear)
                .flatMap(java.util.Optional::stream)
                .toList();
        assumeTrue(!csvFiles.isEmpty(), "Local private CSV folders 2025/ or 2026/ are not available");

        for (var csvFile : csvFiles) {
            var expectedYear = Integer.parseInt(csvFile.getParent().getFileName().toString());
            try (var input = Files.newInputStream(csvFile)) {
                var budgetInput = csvReader.read(input, csvFile.getFileName().toString(), expectedYear);
                var result = analysisService.analyze(budgetInput);

                store.save(result);

                assertThat(result.year()).isEqualTo(expectedYear);
                assertThat(result.transactionCount()).isEqualTo(budgetInput.transactions().size());
                assertThat(result.transactionCount()).isGreaterThan(100);
                assertThat(result.income()).isPositive();
                assertThat(result.income()).isLessThan(500_000);
                assertThat(result.spend()).isPositive();
                assertThat(result.transactions())
                        .filteredOn(tx -> "Spłata karty kredytowej".equals(tx.correctedCategory()))
                        .allSatisfy(tx -> {
                            assertThat(tx.analysisSpend()).isZero();
                            assertThat(tx.budgetBucket()).isEqualTo("Transfer techniczny");
                        });
                assertThat(result.transactions())
                        .filteredOn(tx -> "Zwroty i korekty".equals(tx.correctedCategory()))
                        .allSatisfy(tx -> assertThat(tx.income()).isZero());

                var storedPayload = store.findPayload(expectedYear);
                assertThat(storedPayload).containsKeys("kpis", "monthly", "categories", "transactions");
                assertThat(storedPayload.get("year")).isEqualTo(expectedYear);

                @SuppressWarnings("unchecked")
                var kpis = (Map<String, Object>) storedPayload.get("kpis");
                assertThat(((Number) kpis.get("transactions")).intValue()).isEqualTo(result.transactionCount());
                assertThat(((Number) kpis.get("income")).doubleValue()).isEqualTo(result.income());
                assertThat(((Number) kpis.get("spend")).doubleValue()).isEqualTo(result.spend());
            }
        }
    }

    private static java.util.Optional<Path> latestCsvForYear(int year) {
        var folder = Path.of(String.valueOf(year));
        if (!Files.isDirectory(folder)) {
            return java.util.Optional.empty();
        }
        try (var files = Files.list(folder)) {
            return files
                    .filter(path -> path.getFileName().toString().toLowerCase().endsWith(".csv"))
                    .max(java.util.Comparator.comparing(KnownCsvEndToEndIntegrationTest::lastModified));
        } catch (Exception e) {
            throw new IllegalStateException("Cannot scan local CSV folder " + folder, e);
        }
    }

    private static java.nio.file.attribute.FileTime lastModified(Path path) {
        try {
            return Files.getLastModifiedTime(path);
        } catch (Exception e) {
            throw new IllegalStateException("Cannot read modification time for " + path, e);
        }
    }
}
