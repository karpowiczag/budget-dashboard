package com.budget.application.categorization;

public record CategoryDecision(
        String category,
        String pattern,
        boolean matchedByTitle
) {
}
