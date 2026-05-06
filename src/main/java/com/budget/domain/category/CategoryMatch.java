package com.budget.domain.category;

public record CategoryMatch(String category, String pattern) {
    public static CategoryMatch none() {
        return new CategoryMatch("", "");
    }

    public boolean matched() {
        return category != null && !category.isBlank();
    }
}
