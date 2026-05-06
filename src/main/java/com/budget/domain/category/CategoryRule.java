package com.budget.domain.category;

import java.util.regex.Pattern;

public record CategoryRule(Pattern pattern, String category, String sourcePattern) {
    public boolean matches(String text) {
        return pattern.matcher(text).find();
    }
}
