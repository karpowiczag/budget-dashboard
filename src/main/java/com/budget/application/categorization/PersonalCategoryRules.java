package com.budget.application.categorization;

import com.budget.domain.category.CategoryRule;
import java.util.List;

public record PersonalCategoryRules(List<CategoryRule> rules) {
    public PersonalCategoryRules {
        rules = rules == null ? List.of() : List.copyOf(rules);
    }

    public static PersonalCategoryRules empty() {
        return new PersonalCategoryRules(List.of());
    }
}
