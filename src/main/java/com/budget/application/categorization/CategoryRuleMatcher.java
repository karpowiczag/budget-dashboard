package com.budget.application.categorization;

import java.util.Optional;

interface CategoryRuleMatcher {
    Optional<CategoryDecision> match(CategoryInput input);
}
