package com.budget.application.categorization;

import java.util.Set;

/**
 * Default {@link CategoryCatalog} backed by the static {@link BudgetTaxonomy} (Phase 3a).
 *
 * <p>Every lookup delegates to the existing taxonomy, so the catalog is byte-for-byte equivalent to
 * the prior direct-static reads. The daily-paced set is carried here (moved off
 * {@code BudgetAnalysisService}) so the behaviour now lives behind the seam; Phase 3b materializes
 * these as per-category flags on DB rows.
 */
public class BudgetTaxonomyCatalog implements CategoryCatalog {

    private static final Set<String> DAILY_PACED_LABELS = Set.of(
            "Żywność i chemia",
            "Jedzenie poza domem"
    );

    @Override
    public BudgetTaxonomy.CategoryDefinition definitionById(String categoryId) {
        return BudgetTaxonomy.category(categoryId);
    }

    @Override
    public BudgetTaxonomy.CategoryDefinition definitionByLabel(String label) {
        return BudgetTaxonomy.category(BudgetTaxonomy.categoryIdByLabel(label));
    }

    @Override
    public String categoryIdByLabel(String label) {
        return BudgetTaxonomy.categoryIdByLabel(label);
    }

    @Override
    public Set<String> wealthCategoryLabels() {
        return BudgetTaxonomy.wealthCategoryLabels();
    }

    @Override
    public boolean isDailyPaced(String categoryLabel) {
        return DAILY_PACED_LABELS.contains(categoryLabel);
    }
}
