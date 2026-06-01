package com.budget.application.categorization;

import java.util.Optional;
import java.util.regex.Pattern;

final class PositiveEmployerIncomeMatcher implements CategoryRuleMatcher {
    private static final Pattern GOOGLE_EMPLOYER = Pattern.compile("\\bGOOGLE\\b");
    private static final Pattern GOOGLE_CONSUMER_FLOW = Pattern.compile("\\bGOOGLE PLAY\\b|\\bGOOGLE PAY\\b|\\bGPAY\\b");

    @Override
    public Optional<CategoryDecision> match(CategoryInput input) {
        var description = input.normalizedDescription();
        if (input.positiveAmount()
                && GOOGLE_EMPLOYER.matcher(description).find()
                && !GOOGLE_CONSUMER_FLOW.matcher(description).find()) {
            return Optional.of(CategoryDecision.prelim("salary", "employer-income:GOOGLE", true));
        }
        return Optional.empty();
    }
}
