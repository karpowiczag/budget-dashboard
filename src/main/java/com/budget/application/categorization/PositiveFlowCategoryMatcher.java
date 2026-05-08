package com.budget.application.categorization;

import java.util.Optional;

final class PositiveFlowCategoryMatcher implements CategoryRuleMatcher {
    @Override
    public Optional<CategoryDecision> match(CategoryInput input) {
        if (input.positiveAmount()) {
            return Optional.of(CategoryDecision.prelim("refundCorrection", "positive-flow-fallback", true));
        }
        return Optional.empty();
    }
}
