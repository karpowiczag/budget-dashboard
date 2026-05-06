package com.budget.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.budget.domain.BankTransaction;
import com.budget.domain.BudgetInput;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class BudgetAnalysisServiceTest {
    private final BudgetAnalysisService service = new BudgetAnalysisService(new CategoryClassifier());

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
}
