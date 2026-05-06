package com.budget.application;

import java.util.Optional;

interface CategoryRuleMatcher {
    Optional<CategoryDecision> match(CategoryInput input);
}
