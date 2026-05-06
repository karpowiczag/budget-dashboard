package com.budget.infrastructure.persistence.jdbc;

import static org.assertj.core.api.Assertions.assertThat;

import com.budget.application.reporting.BudgetReportStore;
import com.budget.domain.report.BudgetAnalysisResult;
import com.budget.domain.transaction.NormalizedTransaction;
import java.time.LocalDate;
import java.util.LinkedHashMap;
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

    @Test
    void savesReportTransactionsAndImportAuditWithSpringDataJdbc() {
        var tx = new NormalizedTransaction(
                1,
                LocalDate.of(2026, 1, 2),
                "2026-01",
                "BIEDRONKA",
                "BIEDRONKA ZAKUP",
                "konto",
                "Bez kategorii",
                "Żywność i chemia",
                "Koszty codzienne",
                "Potrzeby podstawowe",
                "Market spożywczy",
                "Potrzeby",
                "Zmienne konieczne",
                "Wydatek",
                -123.45,
                0,
                123.45,
                0,
                0,
                0,
                0,
                0,
                "Wysoka",
                "",
                "BIEDRONKA"
        );
        var payload = new LinkedHashMap<String, Object>();
        payload.put("year", 2099);
        payload.put("transactions", List.of(tx.toPayloadMap()));

        store.save(new BudgetAnalysisResult(2099, "fixture.csv", payload, List.of(tx), 1, 0, 123.45));
        store.recordImportRun(2099, "fixture.csv", "ok", "test");

        assertThat(store.findPayload(2099)).containsEntry("year", 2099);
        assertThat(store.findYears()).anySatisfy(row -> {
            assertThat(row).containsEntry("year", 2099);
            assertThat(row).containsEntry("transactions", 1);
        });
        assertThat(store.findImportRuns()).anySatisfy(run -> {
            assertThat(run.year()).isEqualTo(2099);
            assertThat(run.status()).isEqualTo("ok");
        });
    }
}
