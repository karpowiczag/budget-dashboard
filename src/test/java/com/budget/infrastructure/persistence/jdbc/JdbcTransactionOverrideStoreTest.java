package com.budget.infrastructure.persistence.jdbc;

import static org.assertj.core.api.Assertions.assertThat;

import com.budget.application.categorization.TransactionOverrideStore;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "app.database.url=jdbc:h2:mem:transaction_override;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH;DB_CLOSE_DELAY=-1",
        "app.security.oauth-enabled=false"
})
class JdbcTransactionOverrideStoreTest {
    @Autowired
    private TransactionOverrideStore store;

    @Test
    void setAndClearOverrideAreReflectedInTheSnapshot() {
        var key = "2026-01-03~|~KONTO~|~-200.00~|~BIEDRONKA ZAKUP";
        store.setOverride(key, "investments", LocalDate.of(2026, 1, 3), "konto", new BigDecimal("-200.00"), "BIEDRONKA ZAKUP");
        assertThat(store.overridesByKey()).containsEntry(key, "investments");

        // Upsert replaces the category for the same key.
        store.setOverride(key, "savingsAccount", LocalDate.of(2026, 1, 3), "konto", new BigDecimal("-200.00"), "BIEDRONKA ZAKUP");
        assertThat(store.overridesByKey()).containsEntry(key, "savingsAccount");

        store.clearOverride(key);
        assertThat(store.overridesByKey()).doesNotContainKey(key);
    }
}
