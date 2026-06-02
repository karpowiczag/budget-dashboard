package com.budget.infrastructure.persistence.jdbc;

import static org.assertj.core.api.Assertions.assertThat;

import com.budget.application.analysis.BudgetAnalysisService;
import com.budget.application.networth.NetWorthService;
import com.budget.application.networth.NetWorthStore;
import com.budget.application.reporting.BudgetReportStore;
import com.budget.application.settings.BudgetSettings;
import com.budget.application.settings.BudgetSettingsService;
import com.budget.domain.networth.Account;
import com.budget.domain.report.BudgetInput;
import com.budget.domain.transaction.BankTransaction;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "app.database.url=jdbc:h2:mem:budget_networth;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH;DB_CLOSE_DELAY=-1",
        "app.security.oauth-enabled=false"
})
class JdbcNetWorthRepositoryTest {
    @Autowired
    private BudgetAnalysisService analysisService;

    @Autowired
    private BudgetReportStore reportStore;

    @Autowired
    private BudgetSettingsService settingsService;

    @Autowired
    private NetWorthStore store;

    @Autowired
    private NetWorthService service;

    @Test
    void derivesAccountBalanceFromAnchorPlusSignedFlows() {
        settingsService.save(new BudgetSettings(
                BigDecimal.valueOf(12_000), BigDecimal.valueOf(11_000), 4, 8, List.of()));
        var result = analysisService.analyze(new BudgetInput(2099, "fixture.csv", List.of(
                new BankTransaction(LocalDate.of(2099, 1, 1), "konto", "PRZELEW EXPRESS ELIXIR PRZYCH. TEST EMPLOYER WYNAGRODZENIE", "", 10_000),
                new BankTransaction(LocalDate.of(2099, 1, 2), "konto", "BIEDRONKA ZAKUP", "Bez kategorii", -123.45),
                new BankTransaction(LocalDate.of(2099, 1, 3), "konto", "NETFLIX", "", -60)
        )));
        reportStore.save(result);

        // Discovery picks up the account label from imported transactions.
        assertThat(store.discoverAccountKeys()).contains("konto");

        // SUM(amount) is the signed cash delta: +10000 - 123.45 - 60.
        assertThat(store.netFlowSince("konto", LocalDate.of(2098, 12, 31)))
                .isEqualByComparingTo("9816.55");
        // The date filter is strict (posted_date > anchor): excludes the day-1 income.
        assertThat(store.netFlowSince("konto", LocalDate.of(2099, 1, 1)))
                .isEqualByComparingTo("-183.45");

        store.saveAccount(new Account("konto", "Konto główne", "CHECKING", true, false,
                new BigDecimal("1000.00"), LocalDate.of(2098, 12, 31)));

        var overview = service.overview();
        var konto = overview.accounts().stream()
                .filter(a -> a.accountKey().equals("konto")).findFirst().orElseThrow();
        assertThat(konto.configured()).isTrue();
        assertThat(konto.netFlowSinceAnchor()).isEqualByComparingTo("9816.55");
        assertThat(konto.derivedBalance()).isEqualByComparingTo("10816.55");
        assertThat(overview.liquidTotal()).isEqualByComparingTo("10816.55");
        // Emergency-fund comfort = coreMonthlyCost (123.45) * 8 months.
        assertThat(overview.emergencyFundComfort()).isEqualByComparingTo("987.60");
        assertThat(overview.emergencyProgressComfort()).isGreaterThan(BigDecimal.ZERO);
    }
}
