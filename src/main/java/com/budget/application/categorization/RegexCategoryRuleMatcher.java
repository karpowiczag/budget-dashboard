package com.budget.application.categorization;

import java.util.Optional;

final class RegexCategoryRuleMatcher implements CategoryRuleMatcher {
    @Override
    public Optional<CategoryDecision> match(CategoryInput input) {
        var source = input.normalizedDescription();
        for (var rule : BudgetCatalog.RULES) {
            if (rule.matches(source)) {
                return Optional.of(new CategoryDecision(rule.category(), rule.sourcePattern(), true));
            }
        }
        return Optional.empty();
    }
}
