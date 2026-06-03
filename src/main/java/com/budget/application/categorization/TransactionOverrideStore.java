package com.budget.application.categorization;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

/**
 * Store for manual per-transaction recategorize overrides (Phase 3c-iii), keyed by the transaction
 * content fingerprint ({@code TransactionContentKey}) so an override survives re-import/rebuild —
 * the surrogate id and lp are regenerated each import, but the content key is stable. Applied at the
 * top of normalization with highest precedence (above the whole matcher chain).
 */
public interface TransactionOverrideStore {
    /** Content key → forced category id. */
    Map<String, String> overridesByKey();

    /** Create or replace the override for a content key (identity columns are auditable only). */
    void setOverride(String contentKey, String categoryId, LocalDate postedDate, String account, BigDecimal amount, String description);

    void clearOverride(String contentKey);
}
