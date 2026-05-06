package com.budget.application.analysis;

import static org.assertj.core.api.Assertions.assertThat;

import com.budget.application.categorization.CategoryClassifier;
import com.budget.domain.transaction.BankTransaction;
import com.budget.domain.report.BudgetInput;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class BudgetAnalysisServiceTest {
    private final CategoryClassifier classifier = new CategoryClassifier();
    private final BudgetAnalysisService service = new BudgetAnalysisService(new TransactionNormalizer(classifier), classifier);

    @Test
    void buildsDashboardCompatiblePayload() {
        var input = new BudgetInput(2026, "fixture.csv", List.of(
                new BankTransaction(LocalDate.of(2026, 1, 2), "konto", "PRZELEW EXPRESS ELIXIR PRZYCH. ALEKSANDER KARPOWICZ SOFTWARE WYNAGRODZENIE", "", 18000),
                new BankTransaction(LocalDate.of(2026, 1, 3), "konto", "BIEDRONKA ZAKUP", "Bez kategorii", -200),
                new BankTransaction(LocalDate.of(2026, 1, 4), "konto", "RĘCZNA SPŁATA KARTY KREDYT", "", -65000)
        ));

        var result = service.analyze(input);

        assertThat(result.payload()).containsKeys("kpis", "monthly", "budgetMix", "savingsPlan", "monthControl", "transactions");
        assertThat(result.income()).isEqualTo(18000);
        assertThat(result.spend()).isEqualTo(200);
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
                new BankTransaction(LocalDate.of(2026, 1, 1), "konto", "PRZELEW EXPRESS ELIXIR PRZYCH. ALEKSANDER KARPOWICZ SOFTWARE WYNAGRODZENIE", "", 10000)
        ));

        var result = service.analyze(input);
        var recurring = (List<?>) result.payload().get("recurring");

        assertThat(recurring).hasSize(1);
        assertThat(String.valueOf(recurring.getFirst())).contains("NETFLIX");
    }
}
