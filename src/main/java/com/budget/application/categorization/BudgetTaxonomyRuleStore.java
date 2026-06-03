package com.budget.application.categorization;

import com.budget.domain.category.CategoryRule;
import java.util.List;
import java.util.stream.Stream;

/**
 * Default {@link ClassificationRuleStore} backed by the static {@link BudgetTaxonomy} rules plus the
 * supplied personal rules (Phase 3c). Reproduces the historical matcher order exactly — personal
 * rules first, then the built-in rules in declared order — so {@code new CategoryClassifier()} and
 * other non-Spring tests classify identically to the DB-backed store.
 */
public class BudgetTaxonomyRuleStore implements ClassificationRuleStore {
    private final List<CategoryRule> rules;

    public BudgetTaxonomyRuleStore() {
        this(PersonalCategoryRules.empty());
    }

    public BudgetTaxonomyRuleStore(PersonalCategoryRules personalRules) {
        this.rules = Stream.concat(personalRules.rules().stream(), BudgetTaxonomy.rules().stream()).toList();
    }

    @Override
    public List<CategoryRule> compiledRules() {
        return rules;
    }
}
