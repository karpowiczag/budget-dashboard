package com.budget.application.categorization;

import java.util.Optional;

final class BankCategoryRuleMatcher implements CategoryRuleMatcher {
    @Override
    public Optional<CategoryDecision> match(CategoryInput input) {
        if (input.positiveAmount() && "WYNAGRODZENIE".equals(input.normalizedBankCategory())) {
            return Optional.of(CategoryDecision.prelim("salary", "bank-category:Wynagrodzenie", true));
        }
        if (input.negativeAmount() && "LOKATY I KONTO OSZCZ.".equals(input.normalizedBankCategory())) {
            return Optional.of(CategoryDecision.prelim(BudgetTaxonomy.CATEGORY_SAVINGS_ACCOUNT, "bank-category:Lokaty i konto oszcz.", true));
        }
        return Optional.empty();
    }
}
