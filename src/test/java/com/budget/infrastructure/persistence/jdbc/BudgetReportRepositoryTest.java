package com.budget.infrastructure.persistence.jdbc;

import static org.assertj.core.api.Assertions.assertThat;

import com.budget.application.analysis.BudgetAnalysisService;
import com.budget.application.reporting.BudgetReportStore;
import com.budget.application.reporting.TransactionQuery;
import com.budget.application.settings.BudgetSettings;
import com.budget.application.settings.BudgetSettingsService;
import com.budget.domain.report.BudgetInput;
import com.budget.domain.transaction.BankTransaction;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "app.database.url=jdbc:h2:mem:budget_repo;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH;DB_CLOSE_DELAY=-1",
        "app.security.oauth-enabled=false"
})
class BudgetReportRepositoryTest {
    @Autowired
    private BudgetReportStore store;

    @Autowired
    private BudgetAnalysisService analysisService;

    @Autowired
    private BudgetSettingsService settingsService;

    @Test
    void savesStructuredSnapshotTransactionsAndImportAuditWithSpringDataJdbc() {
        settingsService.save(new BudgetSettings(
                BigDecimal.valueOf(12_000),
                BigDecimal.valueOf(11_000),
                4,
                8,
                List.of(new BudgetSettings.CategoryLimitSetting("Żywność i chemia", BigDecimal.valueOf(100), "test limit"))
        ));

        var result = analysisService.analyze(new BudgetInput(2099, "fixture.csv", List.of(
                new BankTransaction(LocalDate.of(2099, 1, 1), "konto", "PRZELEW EXPRESS ELIXIR PRZYCH. TEST EMPLOYER WYNAGRODZENIE", "", 10_000),
                new BankTransaction(LocalDate.of(2099, 1, 2), "konto", "BIEDRONKA ZAKUP", "Bez kategorii", -123.45),
                new BankTransaction(LocalDate.of(2099, 1, 3), "konto", "NETFLIX", "", -60)
        )));

        store.save(result);
        store.recordImportRun(2099, "fixture.csv", "ok", "test", 2);

        var dashboard = store.findDashboard(2099);
        assertThat(dashboard.year()).isEqualTo(2099);
        assertThat(dashboard.kpis().transactions()).isEqualTo(3);
        assertThat(dashboard.kpis().spend()).isEqualByComparingTo(BigDecimal.valueOf(183.45));
        assertThat(dashboard.savingsPlan().targetMonthlySpend()).isEqualByComparingTo(BigDecimal.valueOf(12_000));
        assertThat(dashboard.savingsPlan().emergencyFundMin()).isEqualByComparingTo(BigDecimal.valueOf(493.8));
        assertThat(dashboard.savingsPlan().categoryLimits())
                .filteredOn(row -> "Żywność i chemia".equals(row.category()))
                .singleElement()
                .satisfies(row -> {
                    assertThat(row.limit()).isEqualByComparingTo(BigDecimal.valueOf(100));
                    assertThat(row.action()).isEqualTo("test limit");
                });
        assertThat(dashboard.categories()).extracting(row -> row.category()).contains("Żywność i chemia");

        assertThat(store.findYears()).anySatisfy(row -> {
            assertThat(row.year()).isEqualTo(2099);
            assertThat(row.transactions()).isEqualTo(3);
        });

        var page = store.findTransactions(new TransactionQuery(2099, 0, 2, "postedDate,desc", "2099-01", null, "biedronka", null, null, null, null, null, null));
        assertThat(page.totalItems()).isEqualTo(1);
        assertThat(page.items()).singleElement().satisfies(row -> {
            assertThat(row.merchant()).isEqualTo("BIEDRONKA");
            assertThat(row.spend()).isEqualByComparingTo(BigDecimal.valueOf(123.45));
        });

        var spendPage = store.findTransactions(new TransactionQuery(2099, 0, 50, "spend,desc", null, null, null, "spend", null, null, null, null, null));
        assertThat(spendPage.totalItems()).isEqualTo(2);
        assertThat(spendPage.items()).extracting(row -> row.merchant()).contains("BIEDRONKA", "NETFLIX");

        var incomePage = store.findTransactions(new TransactionQuery(2099, 0, 50, "postedDate,desc", null, null, null, "income", null, null, null, null, null));
        assertThat(incomePage.totalItems()).isEqualTo(1);
        assertThat(incomePage.items()).singleElement().satisfies(row -> assertThat(row.income()).isEqualByComparingTo(BigDecimal.valueOf(10_000)));

        var analytics = store.findAnalytics(new TransactionQuery(2099, 0, 50, "spend,desc", null, null, null, null, null, null, null, null, null));
        assertThat(analytics.monthlyCategoryTrends())
                .filteredOn(row -> "2099-01".equals(row.monthKey()))
                .extracting(row -> row.category())
                .contains("Żywność i chemia");
        assertThat(analytics.monthlyBucketTrends())
                .filteredOn(row -> "2099-01".equals(row.monthKey()))
                .extracting(row -> row.bucket())
                .isNotEmpty();
        assertThat(analytics.monthlyMerchantTrends())
                .filteredOn(row -> "2099-01".equals(row.monthKey()))
                .extracting(row -> row.merchant())
                .contains("BIEDRONKA", "NETFLIX");
        assertThat(analytics.fixednessBreakdown())
                .extracting(row -> row.fixedness())
                .contains("Zmienne konieczne");
        assertThat(analytics.confidenceBreakdown())
                .filteredOn(row -> "Wysoka".equals(row.confidence()))
                .singleElement()
                .satisfies(row -> {
                    assertThat(row.count()).isEqualTo(3);
                    assertThat(row.spend()).isEqualByComparingTo(BigDecimal.valueOf(183.45));
                    assertThat(row.income()).isEqualByComparingTo(BigDecimal.valueOf(10_000));
                });
        assertThat(analytics.amountBands())
                .filteredOn(row -> "100-250".equals(row.label()))
                .singleElement()
                .satisfies(row -> {
                    assertThat(row.count()).isEqualTo(1);
                    assertThat(row.spend()).isEqualByComparingTo(BigDecimal.valueOf(123.45));
                });

        var classifiedPage = store.findTransactions(new TransactionQuery(
                2099, 0, 50, "postedDate,desc",
                null, null, "biedronka", null, null, null, null, null, null,
                "Zmienne konieczne", "Wysoka"
        ));
        assertThat(classifiedPage.totalItems()).isEqualTo(1);
        assertThat(classifiedPage.items()).singleElement().satisfies(row -> {
            assertThat(row.fixedness()).isEqualTo("Zmienne konieczne");
            assertThat(row.confidence()).isEqualTo("Wysoka");
        });

        assertThat(store.findImportRuns()).anySatisfy(run -> {
            assertThat(run.year()).isEqualTo(2099);
            assertThat(run.status()).isEqualTo("ok");
            assertThat(run.duplicatesRemoved()).isEqualTo(2);
        });

        var replacement = analysisService.analyze(new BudgetInput(2099, "replacement.csv", List.of(
                new BankTransaction(LocalDate.of(2099, 2, 1), "konto", "PRZELEW EXPRESS ELIXIR PRZYCH. TEST EMPLOYER WYNAGRODZENIE", "", 11_000),
                new BankTransaction(LocalDate.of(2099, 2, 2), "konto", "LIDL ZAKUP", "Bez kategorii", -50)
        )));
        store.save(replacement);

        var replacedDashboard = store.findDashboard(2099);
        assertThat(replacedDashboard.kpis().transactions()).isEqualTo(2);
        assertThat(replacedDashboard.kpis().spend()).isEqualByComparingTo(BigDecimal.valueOf(50));
        var replacedPage = store.findTransactions(new TransactionQuery(2099, 0, 50, "postedDate,desc", null, null, null, null, null, null, null, null, null));
        assertThat(replacedPage.totalItems()).isEqualTo(2);
        assertThat(replacedPage.items()).extracting(row -> row.description()).containsExactly(
                "LIDL ZAKUP",
                "PRZELEW EXPRESS ELIXIR PRZYCH. TEST EMPLOYER WYNAGRODZENIE"
        );
    }
}
