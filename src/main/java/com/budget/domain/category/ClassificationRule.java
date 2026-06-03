package com.budget.domain.category;

/**
 * A user-editable classification rule: a regex {@code pattern} over the transaction description that
 * maps a match to {@code categoryId}. Rules are evaluated in ascending {@code priority} (then id), so
 * a lower priority wins. {@code source} is {@code builtin} (seeded from the code taxonomy),
 * {@code personal} (seeded from private config), or {@code user} (created in the app). {@code matchType}
 * is {@code title} today (the regex/title stage); other match types are reserved.
 */
public record ClassificationRule(
        String ruleId,
        String matchType,
        String pattern,
        String categoryId,
        int priority,
        boolean enabled,
        String source
) {
}
