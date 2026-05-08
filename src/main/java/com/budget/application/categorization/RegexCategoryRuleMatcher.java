package com.budget.application.categorization;

import com.budget.domain.category.CategoryRule;
import java.util.List;
import java.util.Optional;

final class RegexCategoryRuleMatcher implements CategoryRuleMatcher {
    private final List<CategoryRule> rules;

    RegexCategoryRuleMatcher() {
        this(PersonalCategoryRules.empty());
    }

    RegexCategoryRuleMatcher(PersonalCategoryRules personalRules) {
        this.rules = java.util.stream.Stream.concat(
                        personalRules.rules().stream(),
                        BudgetTaxonomy.RULES.stream()
                )
                .toList();
    }

    @Override
    public Optional<CategoryDecision> match(CategoryInput input) {
        var source = input.normalizedDescription();
        for (var rule : rules) {
            if (rule.matches(source)) {
                return Optional.of(CategoryDecision.prelim(rule.categoryId(), rule.sourcePattern(), true));
            }
        }
        return Optional.empty();
    }
}
