package com.budget.application.categorization;

import java.util.Optional;

final class RegexCategoryRuleMatcher implements CategoryRuleMatcher {
    private final ClassificationRuleStore ruleStore;

    RegexCategoryRuleMatcher() {
        this(new BudgetTaxonomyRuleStore());
    }

    RegexCategoryRuleMatcher(PersonalCategoryRules personalRules) {
        this(new BudgetTaxonomyRuleStore(personalRules));
    }

    RegexCategoryRuleMatcher(ClassificationRuleStore ruleStore) {
        this.ruleStore = ruleStore;
    }

    @Override
    public Optional<CategoryDecision> match(CategoryInput input) {
        var source = input.normalizedDescription();
        for (var rule : ruleStore.compiledRules()) {
            if (rule.matches(source)) {
                return Optional.of(CategoryDecision.prelim(rule.categoryId(), rule.sourcePattern(), true));
            }
        }
        return Optional.empty();
    }
}
