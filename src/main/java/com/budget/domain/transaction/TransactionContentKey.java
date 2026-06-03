package com.budget.domain.transaction;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Stable content fingerprint of a bank transaction — date + account + amount + normalized
 * description — used as the natural key that survives re-import/rebuild (the surrogate id and lp are
 * regenerated every import). The canonicalization is shared by import de-duplication and the manual
 * recategorize override so the two can never disagree about what "the same transaction" is.
 */
public final class TransactionContentKey {
    private static final Pattern UNSETTLED_CARD_MARKER = Pattern.compile(
            "\\btransakcja\\s+nierozliczona\\b",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
    );
    // Normalized fields are uppercased with collapsed whitespace, so this token cannot occur in them.
    private static final String SEPARATOR = "~|~";

    private TransactionContentKey() {
    }

    /** The canonical content key for a transaction's identity fields. */
    public static String of(LocalDate date, String account, BigDecimal amount, String description) {
        return (date == null ? "" : date.toString())
                + SEPARATOR + account(account)
                + SEPARATOR + amount(amount).toPlainString()
                + SEPARATOR + description(description);
    }

    public static String account(String value) {
        return normalize(value);
    }

    /** Description with the "transakcja nierozliczona" card marker stripped, then normalized. */
    public static String description(String value) {
        return normalize(UNSETTLED_CARD_MARKER.matcher(value == null ? "" : value).replaceAll(""));
    }

    public static BigDecimal amount(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP);
    }

    private static String normalize(String value) {
        return (value == null ? "" : value)
                .replaceAll("\\s+", " ")
                .trim()
                .toUpperCase(Locale.ROOT);
    }
}
