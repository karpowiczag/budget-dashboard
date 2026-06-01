package com.budget.application.categorization;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class BudgetTaxonomyTest {
    @Test
    void wealthCategoryLabelsAreTheCanonicalWealthBuildingSet() {
        // Persistence (FINANCIAL_FLOW_CATEGORIES), analysis (REAL_SAVING_CATEGORIES),
        // and FIRE linkage all derive from this set; a label rename must fail here.
        assertThat(BudgetTaxonomy.wealthCategoryLabels())
                .containsExactlyInAnyOrder("Inwestycje", "Konto oszczędnościowe", "Nadpłata kredytu");
    }
}
