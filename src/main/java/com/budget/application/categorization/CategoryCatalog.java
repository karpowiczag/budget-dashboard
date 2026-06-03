package com.budget.application.categorization;

import java.util.Set;

/**
 * Read-side seam over the category taxonomy.
 *
 * <p>Phase 3a backs this with the static {@link BudgetTaxonomy} (behaviour-identical). It exists so
 * that classification and analysis read category metadata and behaviour flags through one interface
 * instead of reaching for the static taxonomy directly. Phase 3b swaps in a DB-backed implementation
 * so categories become user-owned (add/rename/hide) without touching the callers.
 *
 * <p>The bucket / area / fixedness / flow vocabularies stay fixed (they encode planning semantics);
 * only the per-category catalog moves behind this port.
 */
public interface CategoryCatalog {

    /** Category metadata by stable id. */
    BudgetTaxonomy.CategoryDefinition definitionById(String categoryId);

    /** Category metadata by user-facing label, resolving legacy aliases. */
    BudgetTaxonomy.CategoryDefinition definitionByLabel(String label);

    /** Resolve a label (or legacy alias) to its stable category id. */
    String categoryIdByLabel(String label);

    /** Labels whose outflow is wealth-building (savings/investments/overpayment) and excluded from spend. */
    Set<String> wealthCategoryLabels();

    /** Stable ids of the wealth-building categories — rename-safe equivalent of {@link #wealthCategoryLabels()}. */
    Set<String> wealthCategoryIds();

    /**
     * True when month-end projection should linearly pace the category by elapsed days
     * (high-frequency, evenly-spread spending such as groceries and dining out).
     */
    boolean isDailyPaced(String categoryLabel);

    /** Rename-safe equivalent of {@link #isDailyPaced(String)}, keyed by stable category id. */
    boolean isDailyPacedById(String categoryId);
}
