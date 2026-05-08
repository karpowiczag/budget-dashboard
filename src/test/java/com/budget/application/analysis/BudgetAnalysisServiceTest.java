package com.budget.application.analysis;

import static org.assertj.core.api.Assertions.assertThat;

import com.budget.application.categorization.CategoryClassifier;
import com.budget.application.settings.BudgetSettings;
import com.budget.application.settings.BudgetSettingsDefaults;
import com.budget.application.settings.BudgetSettingsService;
import com.budget.application.settings.BudgetSettingsStore;
import com.budget.domain.report.BudgetInput;
import com.budget.domain.transaction.BankTransaction;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class BudgetAnalysisServiceTest {
    private final CategoryClassifier classifier = new CategoryClassifier();
    private final BudgetAnalysisService service = new BudgetAnalysisService(new TransactionNormalizer(classifier), classifier, settingsService());

    @Test
    void buildsTypedDashboardSnapshot() {
        var input = new BudgetInput(2026, "fixture.csv", List.of(
                new BankTransaction(LocalDate.of(2026, 1, 2), "konto", "PRZELEW EXPRESS ELIXIR PRZYCH. TEST EMPLOYER WYNAGRODZENIE", "", 18000),
                new BankTransaction(LocalDate.of(2026, 1, 3), "konto", "BIEDRONKA ZAKUP", "Bez kategorii", -200),
                new BankTransaction(LocalDate.of(2026, 1, 4), "konto", "RĘCZNA SPŁATA KARTY KREDYT", "", -65000)
        ));

        var result = service.analyze(input);

        assertThat(result.snapshot().kpis().income()).isEqualByComparingTo(BigDecimal.valueOf(18000));
        assertThat(result.snapshot().kpis().spend()).isEqualByComparingTo(BigDecimal.valueOf(200));
        assertThat(result.snapshot().monthly()).hasSize(12);
        assertThat(result.snapshot().budgetMix()).isNotEmpty();
        assertThat(result.snapshot().savingsPlan().categoryLimits()).isNotEmpty();
        assertThat(result.snapshot().monthControl().monthKey()).isEqualTo("2026-01");
        assertThat(result.income()).isEqualByComparingTo(BigDecimal.valueOf(18000));
        assertThat(result.spend()).isEqualByComparingTo(BigDecimal.valueOf(200));
        assertThat(result.transactions()).hasSize(3);
        assertThat(result.transactions().get(2).correctedCategory()).isEqualTo("Spłata karty kredytowej");
        assertThat(result.transactions().get(2).analysisSpend()).isZero();
    }

    @Test
    void detectsRecurringMonthlySpend() {
        var input = new BudgetInput(2026, "fixture.csv", List.of(
                new BankTransaction(LocalDate.of(2026, 1, 5), "konto", "NETFLIX", "", -60),
                new BankTransaction(LocalDate.of(2026, 2, 5), "konto", "NETFLIX", "", -60),
                new BankTransaction(LocalDate.of(2026, 3, 5), "konto", "NETFLIX", "", -60),
                new BankTransaction(LocalDate.of(2026, 1, 1), "konto", "PRZELEW EXPRESS ELIXIR PRZYCH. TEST EMPLOYER WYNAGRODZENIE", "", 10000)
        ));

        var result = service.analyze(input);

        assertThat(result.snapshot().recurring())
                .singleElement()
                .satisfies(row -> {
                    assertThat(row.merchant()).isEqualTo("NETFLIX");
                    assertThat(row.months()).isEqualTo(3);
                });
    }

    @Test
    void treatsBankWageCategoryAsRealIncome() {
        var input = new BudgetInput(2026, "fixture.csv", List.of(
                new BankTransaction(LocalDate.of(2026, 1, 2), "konto", "PRZELEW EXPRESS ELIXIR PRZYCH.", "Wynagrodzenie", 18_000),
                new BankTransaction(LocalDate.of(2026, 1, 3), "konto", "ZWROT LOSOWY", "", 500)
        ));

        var result = service.analyze(input);

        assertThat(result.income()).isEqualByComparingTo(BigDecimal.valueOf(18_000));
        assertThat(result.snapshot().kpis().income()).isEqualByComparingTo(BigDecimal.valueOf(18_000));
        assertThat(result.snapshot().kpis().excludedIncoming()).isEqualByComparingTo(BigDecimal.valueOf(500));
    }

    @Test
    void includesInvestmentsSavingsAccountAndMortgageOverpaymentsInMonthlySavingsFlow() {
        var input = new BudgetInput(2026, "fixture.csv", List.of(
                new BankTransaction(LocalDate.of(2026, 1, 1), "konto", "PRZELEW EXPRESS ELIXIR PRZYCH. TEST EMPLOYER WYNAGRODZENIE", "", 20_000),
                new BankTransaction(LocalDate.of(2026, 1, 2), "konto", "PRZELEW DO BM MBANKU IKZE", "", -3_000),
                new BankTransaction(LocalDate.of(2026, 1, 3), "konto", "PRZELEW WŁASNY NA KONTO OSZCZĘDNOŚCIOWE", "", -2_000),
                new BankTransaction(LocalDate.of(2026, 1, 4), "konto", "KREDYT - WCZEŚNIEJSZA SPŁATA", "", -5_000)
        ));

        var result = service.analyze(input);
        var january = result.snapshot().monthly().stream()
                .filter(row -> "2026-01".equals(row.monthKey()))
                .findFirst()
                .orElseThrow();

        assertThat(january.savingsInvestments()).isEqualByComparingTo(BigDecimal.valueOf(10_000));
        assertThat(result.snapshot().kpis().realSavingsOutgoing()).isEqualByComparingTo(BigDecimal.valueOf(10_000));
        assertThat(result.snapshot().kpis().savingsAccountNetChange()).isEqualByComparingTo(BigDecimal.valueOf(2_000));
        assertThat(result.snapshot().kpis().savingsAccountGrossDeposits()).isEqualByComparingTo(BigDecimal.valueOf(2_000));
        assertThat(result.snapshot().kpis().savingsAccountInflows()).isEqualByComparingTo(BigDecimal.valueOf(2_000));
        assertThat(result.snapshot().kpis().savingsAccountOutflows()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.snapshot().budgetMix())
                .filteredOn(row -> row.bucket().equals("Konto oszczędnościowe"))
                .singleElement()
                .satisfies(row -> assertThat(row.sum()).isEqualByComparingTo(BigDecimal.valueOf(2_000)));
    }

    @Test
    void calculatesSavingsAccountKpiAsNetTransactionMovementWithoutOpeningBalance() {
        var input = new BudgetInput(2026, "fixture.csv", List.of(
                new BankTransaction(LocalDate.of(2026, 1, 1), "konto", "PRZELEW EXPRESS ELIXIR PRZYCH. TEST EMPLOYER WYNAGRODZENIE", "", 20_000),
                new BankTransaction(LocalDate.of(2026, 1, 2), "konto", "PRZELEW WŁASNY NA KONTO OSZCZĘDNOŚCIOWE", "", -500),
                new BankTransaction(LocalDate.of(2026, 1, 3), "Konto oszczędnościowe", "PRZELEW WŁASNY", "", 500),
                new BankTransaction(LocalDate.of(2026, 1, 4), "Konto oszczędnościowe", "PRZELEW WŁASNY", "", -200),
                new BankTransaction(LocalDate.of(2026, 1, 5), "Konto oszczędnościowe", "ODSETKI", "", 10),
                new BankTransaction(LocalDate.of(2026, 1, 6), "Konto oszczędnościowe", "PODATEK OD ODSETEK KAPITAŁOWYCH", "", -2)
        ));

        var result = service.analyze(input);

        assertThat(result.snapshot().kpis().savingsAccountNetChange()).isEqualByComparingTo(BigDecimal.valueOf(308));
        assertThat(result.snapshot().kpis().savingsAccountGrossDeposits()).isEqualByComparingTo(BigDecimal.valueOf(500));
        assertThat(result.snapshot().kpis().savingsAccountInflows()).isEqualByComparingTo(BigDecimal.valueOf(510));
        assertThat(result.snapshot().kpis().savingsAccountOutflows()).isEqualByComparingTo(BigDecimal.valueOf(202));
    }

    @Test
    void doesNotDailyPaceFixedAndOneOffCategoriesInOpenMonthProjection() {
        var input = new BudgetInput(2026, "fixture.csv", List.of(
                new BankTransaction(LocalDate.of(2026, 5, 1), "konto", "PRZELEW EXPRESS ELIXIR PRZYCH. TEST EMPLOYER WYNAGRODZENIE", "", 18_000),
                new BankTransaction(LocalDate.of(2026, 5, 4), "konto", "OPLATY, ZALICZKA DOMOWSKIEGO 19F/16", "", -800),
                new BankTransaction(LocalDate.of(2026, 5, 5), "konto", "KREDYT - SPŁATA RATY", "", -1_500),
                new BankTransaction(LocalDate.of(2026, 5, 5), "konto", "BIEDRONKA ZAKUP", "", -500)
        ));

        var result = service.analyze(input);
        var statusByCategory = result.snapshot().monthControl().categoryStatus().stream()
                .collect(java.util.stream.Collectors.toMap(
                        row -> row.category(),
                        row -> row
                ));

        assertThat(statusByCategory.get("Czynsz i wynajem").currentMonthProjection())
                .isEqualByComparingTo(BigDecimal.valueOf(800));
        assertThat(statusByCategory.get("Rata kredytu").currentMonthProjection())
                .isEqualByComparingTo(BigDecimal.valueOf(1_500));
        assertThat(statusByCategory.get("Żywność i chemia").currentMonthProjection())
                .isEqualByComparingTo(BigDecimal.valueOf(3_100));
        assertThat(result.snapshot().monthControl().projectedSpend())
                .isEqualByComparingTo(BigDecimal.valueOf(5_400));
    }

    @Test
    void usesLargestObservedMonthAsDefaultLimitForRequiredUnevenBills() {
        var input = new BudgetInput(2026, "fixture.csv", List.of(
                new BankTransaction(LocalDate.of(2026, 1, 1), "konto", "PRZELEW EXPRESS ELIXIR PRZYCH. TEST EMPLOYER WYNAGRODZENIE", "", 18_000),
                new BankTransaction(LocalDate.of(2026, 1, 2), "konto", "TAURON PRAD", "", -50),
                new BankTransaction(LocalDate.of(2026, 5, 1), "konto", "PRZELEW EXPRESS ELIXIR PRZYCH. TEST EMPLOYER WYNAGRODZENIE", "", 18_000),
                new BankTransaction(LocalDate.of(2026, 5, 6), "konto", "TAURON PRAD", "", -250)
        ));

        var result = service.analyze(input);
        var electricityLimit = result.snapshot().savingsPlan().categoryLimits().stream()
                .filter(row -> row.category().equals("Prąd"))
                .findFirst()
                .orElseThrow();
        var electricityStatus = result.snapshot().monthControl().categoryStatus().stream()
                .filter(row -> row.category().equals("Prąd"))
                .findFirst()
                .orElseThrow();

        assertThat(electricityLimit.currentMonthly()).isEqualByComparingTo(BigDecimal.valueOf(150));
        assertThat(electricityLimit.limit()).isEqualByComparingTo(BigDecimal.valueOf(250));
        assertThat(electricityStatus.currentMonthProjection()).isEqualByComparingTo(BigDecimal.valueOf(250));
        assertThat(electricityStatus.projectedDelta()).isZero();
    }

    @Test
    void appliesManualCategoryBucketOverridesBeforeBuildingPlanAndTransactions() {
        var settings = new BudgetSettings(
                BigDecimal.valueOf(14_000),
                BigDecimal.valueOf(13_000),
                3,
                6,
                List.of(new BudgetSettings.CategoryLimitSetting("category", "Lekarz i apteka", "Lekarz i apteka", BigDecimal.valueOf(900), "", "Nieobowiązkowe"))
        );
        var serviceWithOverride = new BudgetAnalysisService(new TransactionNormalizer(classifier), classifier, settingsService(settings));
        var input = new BudgetInput(2026, "fixture.csv", List.of(
                new BankTransaction(LocalDate.of(2026, 1, 1), "konto", "PRZELEW EXPRESS ELIXIR PRZYCH. TEST EMPLOYER WYNAGRODZENIE", "", 18_000),
                new BankTransaction(LocalDate.of(2026, 1, 2), "konto", "APTEKA TEST", "", -400)
        ));

        var result = serviceWithOverride.analyze(input);

        assertThat(result.transactions())
                .filteredOn(tx -> tx.correctedCategory().equals("Lekarz i apteka"))
                .singleElement()
                .satisfies(tx -> {
                    assertThat(tx.budgetBucket()).isEqualTo("Nieobowiązkowe");
                    assertThat(tx.notes()).contains("Ręcznie zmieniony koszyk");
                });
        assertThat(result.snapshot().savingsPlan().categoryLimits())
                .filteredOn(row -> row.category().equals("Lekarz i apteka"))
                .singleElement()
                .satisfies(row -> assertThat(row.bucket()).isEqualTo("Nieobowiązkowe"));
        assertThat(result.snapshot().savingsPlan().parentLimits())
                .filteredOn(row -> row.scope().equals("bucket") && row.name().equals("Nieobowiązkowe"))
                .singleElement()
                .satisfies(row -> assertThat(row.currentMonthly()).isEqualByComparingTo(BigDecimal.valueOf(400)));
    }

    private BudgetSettingsService settingsService() {
        return settingsService(null);
    }

    private BudgetSettingsService settingsService(BudgetSettings settings) {
        return new BudgetSettingsService(
                new BudgetSettingsDefaults(BigDecimal.valueOf(14_000), BigDecimal.valueOf(13_000), 3, 6),
                new BudgetSettingsStore() {
                    @Override
                    public Optional<BudgetSettings> findDefaultSettings() {
                        return Optional.ofNullable(settings);
                    }

                    @Override
                    public BudgetSettings saveDefaultSettings(BudgetSettings settings) {
                        return settings;
                    }
                }
        );
    }
}
