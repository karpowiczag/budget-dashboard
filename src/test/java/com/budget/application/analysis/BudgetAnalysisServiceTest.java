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

    private BudgetSettingsService settingsService() {
        return new BudgetSettingsService(
                new BudgetSettingsDefaults(BigDecimal.valueOf(14_000), BigDecimal.valueOf(13_000), 3, 6),
                new BudgetSettingsStore() {
                    @Override
                    public Optional<BudgetSettings> findDefaultSettings() {
                        return Optional.empty();
                    }

                    @Override
                    public BudgetSettings saveDefaultSettings(BudgetSettings settings) {
                        return settings;
                    }
                }
        );
    }
}
