package com.budget.application;

public record CategoryDecision(
        String category,
        String pattern,
        boolean matchedByTitle
) {
}
