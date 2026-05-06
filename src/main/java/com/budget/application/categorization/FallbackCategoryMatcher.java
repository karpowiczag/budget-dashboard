package com.budget.application.categorization;

import java.util.Optional;

final class FallbackCategoryMatcher implements CategoryRuleMatcher {
    @Override
    public Optional<CategoryDecision> match(CategoryInput input) {
        return Optional.of(new CategoryDecision("Do sprawdzenia", "", false));
    }
}
