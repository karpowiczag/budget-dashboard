package com.budget.application.analysis;

import static org.assertj.core.api.Assertions.assertThat;

import com.budget.application.categorization.BudgetTaxonomy;
import com.budget.application.categorization.BudgetTaxonomyCatalog;
import com.budget.application.categorization.CategoryCatalog;
import com.budget.application.categorization.CategoryClassifier;
import com.budget.application.categorization.PersonalCategoryRules;
import com.budget.application.settings.BudgetSettings;
import com.budget.application.settings.BudgetSettingsDefaults;
import com.budget.application.settings.BudgetSettingsService;
import com.budget.application.settings.BudgetSettingsStore;
import com.budget.domain.report.BudgetInput;
import com.budget.domain.transaction.BankTransaction;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class BudgetAnalysisServiceTest {
    private final CategoryClassifier classifier = new CategoryClassifier();
    // Fixed at the start of the year so the control month equals the latest month with data
    // (the current calendar month is never ahead of the test data), keeping assertions stable.
    private final Clock clock = Clock.fixed(LocalDate.of(2026, 1, 1).atStartOfDay(ZoneOffset.UTC).toInstant(), ZoneOffset.UTC);
    private final BudgetAnalysisService service = new BudgetAnalysisService(new TransactionNormalizer(classifier), classifier, settingsService(), clock);

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
    void plannerTargetsCurrentCalendarMonthEvenBeforeItsTransactionsAreImported() {
        var marchClock = Clock.fixed(LocalDate.of(2026, 3, 10).atStartOfDay(ZoneOffset.UTC).toInstant(), ZoneOffset.UTC);
        var marchService = new BudgetAnalysisService(new TransactionNormalizer(classifier), classifier, settingsService(), marchClock);
        var input = new BudgetInput(2026, "fixture.csv", List.of(
                new BankTransaction(LocalDate.of(2026, 1, 1), "konto", "PRZELEW EXPRESS ELIXIR PRZYCH. TEST EMPLOYER WYNAGRODZENIE", "", 18_000),
                new BankTransaction(LocalDate.of(2026, 1, 4), "konto", "BIEDRONKA ZAKUP", "", -500)
        ));

        var control = marchService.analyze(input).snapshot().monthControl();

        assertThat(control.monthKey()).isEqualTo("2026-03");
        assertThat(control.elapsedDays()).isEqualTo(10);
        assertThat(control.remainingDays()).isEqualTo(21);
        assertThat(control.spendToDate()).isZero();
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
        var serviceWithOverride = new BudgetAnalysisService(new TransactionNormalizer(classifier), classifier, settingsService(settings), clock);
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

    @Test
    void renamingACategoryReflectsTheNewLabelWithoutSplittingItOrChangingTotals() {
        // Simulate a user rename of the groceries category in the catalog. Analysis groups by the
        // stable id and resolves the current label, so spend stays on one row under the new name.
        var renamedClassifier = new CategoryClassifier(PersonalCategoryRules.empty(), new RenamingCatalog());
        var renamedService = new BudgetAnalysisService(
                new TransactionNormalizer(renamedClassifier), renamedClassifier, settingsService(), clock);
        var input = new BudgetInput(2026, "fixture.csv", List.of(
                new BankTransaction(LocalDate.of(2026, 1, 1), "konto", "PRZELEW EXPRESS ELIXIR PRZYCH. TEST EMPLOYER WYNAGRODZENIE", "", 18_000),
                new BankTransaction(LocalDate.of(2026, 1, 3), "konto", "BIEDRONKA ZAKUP", "", -200),
                new BankTransaction(LocalDate.of(2026, 1, 9), "konto", "LIDL ZAKUP", "", -300)
        ));

        var result = renamedService.analyze(input);

        // Normalization resolves the current label by id (never crashes on the now-absent old label).
        assertThat(result.transactions())
                .filteredOn(tx -> "groceries".equals(tx.categoryId()))
                .allSatisfy(tx -> assertThat(tx.correctedCategory()).isEqualTo(RenamingCatalog.RENAMED_LABEL));
        // One merged grocery row under the new label, none under the old one, total preserved.
        var groceryRows = result.snapshot().categories().stream()
                .filter(row -> RenamingCatalog.RENAMED_LABEL.equals(row.category()))
                .toList();
        assertThat(groceryRows).hasSize(1);
        assertThat(groceryRows.get(0).spend()).isEqualByComparingTo(BigDecimal.valueOf(500));
        assertThat(result.snapshot().categories())
                .noneMatch(row -> "Żywność i chemia".equals(row.category()));
    }

    /** Catalog that renames the groceries category, otherwise delegating to the default taxonomy. */
    private static final class RenamingCatalog implements CategoryCatalog {
        private static final String RENAMED_LABEL = "Zakupy spożywcze";
        private static final String GROCERIES_ID = "groceries";
        private final CategoryCatalog delegate = new BudgetTaxonomyCatalog();

        @Override
        public BudgetTaxonomy.CategoryDefinition definitionById(String categoryId) {
            var definition = delegate.definitionById(categoryId);
            return GROCERIES_ID.equals(categoryId) ? rename(definition) : definition;
        }

        @Override
        public BudgetTaxonomy.CategoryDefinition definitionByLabel(String label) {
            if (RENAMED_LABEL.equals(label)) {
                return definitionById(GROCERIES_ID);
            }
            return delegate.definitionByLabel(label);
        }

        @Override
        public String categoryIdByLabel(String label) {
            return RENAMED_LABEL.equals(label) ? GROCERIES_ID : delegate.categoryIdByLabel(label);
        }

        @Override
        public java.util.Set<String> wealthCategoryLabels() {
            return delegate.wealthCategoryLabels();
        }

        @Override
        public java.util.Set<String> wealthCategoryIds() {
            return delegate.wealthCategoryIds();
        }

        @Override
        public boolean isDailyPaced(String categoryLabel) {
            return RENAMED_LABEL.equals(categoryLabel) || delegate.isDailyPaced(categoryLabel);
        }

        @Override
        public boolean isDailyPacedById(String categoryId) {
            return delegate.isDailyPacedById(categoryId);
        }

        private BudgetTaxonomy.CategoryDefinition rename(BudgetTaxonomy.CategoryDefinition d) {
            return new BudgetTaxonomy.CategoryDefinition(d.id(), RENAMED_LABEL, d.area(), d.analyticsGroup(),
                    d.budgetGroupId(), d.budgetBucketLabel(), d.fixedness(), d.flowType(),
                    d.discretionary(), d.excluded(), d.realIncome());
        }
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
