package com.budget.application.categorization;

import java.util.Optional;

final class FallbackCategoryMatcher implements CategoryRuleMatcher {
    @Override
    public Optional<CategoryDecision> match(CategoryInput input) {
        return Optional.of(CategoryDecision.prelim(BudgetTaxonomy.CATEGORY_UNKNOWN, "", false));
    }
}
