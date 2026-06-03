package com.budget.domain.transaction;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

/**
 * The content key is the natural key shared by import de-duplication and the recategorize override.
 * It must canonicalize whitespace/case, strip the "transakcja nierozliczona" card marker, and scale
 * the amount — so the same underlying transaction always produces the same key regardless of cosmetic
 * differences between an unsettled-card row and its settled twin.
 */
class TransactionContentKeyTest {

    @Test
    void normalizesWhitespaceCaseMarkerAndAmountScale() {
        var unsettled = TransactionContentKey.of(
                LocalDate.of(2026, 1, 3), " konto ", new BigDecimal("-200"), "Biedronka   ZAKUP transakcja nierozliczona");
        var settled = TransactionContentKey.of(
                LocalDate.of(2026, 1, 3), "KONTO", new BigDecimal("-200.00"), "BIEDRONKA ZAKUP");

        assertThat(unsettled).isEqualTo(settled);
    }

    @Test
    void differentTransactionsProduceDifferentKeys() {
        var a = TransactionContentKey.of(LocalDate.of(2026, 1, 3), "konto", new BigDecimal("-200"), "BIEDRONKA");
        var b = TransactionContentKey.of(LocalDate.of(2026, 1, 4), "konto", new BigDecimal("-200"), "BIEDRONKA");
        var c = TransactionContentKey.of(LocalDate.of(2026, 1, 3), "konto", new BigDecimal("-201"), "BIEDRONKA");

        assertThat(a).isNotEqualTo(b);
        assertThat(a).isNotEqualTo(c);
    }
}
