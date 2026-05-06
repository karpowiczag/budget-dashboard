package com.budget.application.categorization;

import java.util.Optional;

final class PositiveFlowCategoryMatcher implements CategoryRuleMatcher {
    @Override
    public Optional<CategoryDecision> match(CategoryInput input) {
        if (input.amount() > 0) {
            return Optional.of(new CategoryDecision("Zwroty i korekty", "positive-flow-fallback", true));
        }
        return Optional.empty();
    }
}
