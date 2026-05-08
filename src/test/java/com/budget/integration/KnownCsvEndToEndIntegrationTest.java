package com.budget.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.budget.application.analysis.BudgetAnalysisService;
import com.budget.application.reporting.BudgetReportStore;
import com.budget.application.reporting.TransactionQuery;
import com.budget.domain.report.BudgetInput;
import com.budget.domain.transaction.BankTransaction;
import com.budget.infrastructure.csv.BankCsvReader;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
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
        var csvFilesByYear = KNOWN_YEARS.stream()
                .map(year -> new YearCsvFiles(year, csvFilesForYear(year)))
                .filter(row -> !row.files().isEmpty())
                .toList();
        assumeTrue(!csvFilesByYear.isEmpty(), "Local private CSV folders 2025/ or 2026/ are not available");

        for (var yearFiles : csvFilesByYear) {
            var sources = new ArrayList<List<BankTransaction>>();
            var largestSingleFile = 0;
            for (var csvFile : yearFiles.files()) {
                try (var input = Files.newInputStream(csvFile)) {
                    var csvInput = csvReader.read(input, csvFile.getFileName().toString(), yearFiles.year());
                    sources.add(csvInput.transactions());
                    largestSingleFile = Math.max(largestSingleFile, csvInput.transactions().size());
                }
            }
            var budgetInput = new BudgetInput(
                    yearFiles.year(),
                    "known-local-" + yearFiles.year() + "-" + yearFiles.files().size() + "-files.csv",
                    mergeTransactionSources(sources)
            );
            var result = analysisService.analyze(budgetInput);

            store.save(result);

            assertThat(result.year()).isEqualTo(yearFiles.year());
            assertThat(result.transactionCount()).isEqualTo(budgetInput.transactions().size());
            assertThat(result.transactionCount()).isPositive();
            assertThat(result.transactionCount()).isGreaterThanOrEqualTo(largestSingleFile);
            assertThat(result.income()).isPositive();
            assertThat(result.income()).isLessThan(BigDecimal.valueOf(500_000));
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

            var dashboard = store.findDashboard(yearFiles.year());
            assertThat(dashboard.kpis().transactions()).isEqualTo(result.transactionCount());
            assertThat(dashboard.kpis().income()).isEqualByComparingTo(result.income());
            assertThat(dashboard.kpis().spend()).isEqualByComparingTo(result.spend());
            assertThat(dashboard.monthly()).hasSize(12);
            assertThat(dashboard.categories()).isNotEmpty();

            var firstPage = store.findTransactions(new TransactionQuery(yearFiles.year(), 0, 50, "postedDate,desc", null, null, null, null, null, null, null, null, null));
            assertThat(firstPage.totalItems()).isEqualTo(result.transactionCount());
            assertThat(firstPage.items()).hasSizeLessThanOrEqualTo(50);
        }
    }

    private static List<Path> csvFilesForYear(int year) {
        var folder = Path.of(String.valueOf(year));
        if (!Files.isDirectory(folder)) {
            return List.of();
        }
        try (var files = Files.list(folder)) {
            return files
                    .filter(path -> path.getFileName().toString().toLowerCase().endsWith(".csv"))
                    .sorted(Comparator.comparing(Path::toString))
                    .toList();
        } catch (Exception e) {
            throw new IllegalStateException("Cannot scan local CSV folder " + folder, e);
        }
    }

    private static List<BankTransaction> mergeTransactionSources(List<List<BankTransaction>> sources) {
        var maxCounts = new LinkedHashMap<BankTransaction, Integer>();
        for (var source : sources) {
            var sourceCounts = new LinkedHashMap<BankTransaction, Integer>();
            for (var transaction : source) {
                sourceCounts.merge(transaction, 1, Integer::sum);
            }
            sourceCounts.forEach((transaction, count) -> maxCounts.merge(transaction, count, Math::max));
        }
        var merged = new ArrayList<BankTransaction>();
        maxCounts.forEach((transaction, count) -> {
            for (var index = 0; index < count; index++) {
                merged.add(transaction);
            }
        });
        merged.sort(Comparator.comparing(BankTransaction::date)
                .thenComparing(BankTransaction::account)
                .thenComparing(BankTransaction::description)
                .thenComparing(BankTransaction::amount));
        return merged;
    }

    private record YearCsvFiles(int year, List<Path> files) {
    }
}
