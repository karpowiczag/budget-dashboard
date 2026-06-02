package com.budget.application.settings;

import java.math.BigDecimal;
import java.util.List;

public record BudgetSettings(
        BigDecimal targetMonthlySpend,
        BigDecimal aggressiveMonthlySpend,
        int emergencyFundMinMonths,
        int emergencyFundComfortMonths,
        BigDecimal netIncomeRatio,
        List<CategoryLimitSetting> categoryLimits
) {
    /** Backwards-compatible constructor; net-income ratio falls back to the service default. */
    public BudgetSettings(BigDecimal targetMonthlySpend, BigDecimal aggressiveMonthlySpend, int emergencyFundMinMonths, int emergencyFundComfortMonths, List<CategoryLimitSetting> categoryLimits) {
        this(targetMonthlySpend, aggressiveMonthlySpend, emergencyFundMinMonths, emergencyFundComfortMonths, null, categoryLimits);
    }

    public BudgetSettings {
        categoryLimits = categoryLimits == null ? List.of() : List.copyOf(categoryLimits);
    }

    public record CategoryLimitSetting(String scope, String name, String category, BigDecimal limit, String action, String bucketOverride) {
        public CategoryLimitSetting(String category, BigDecimal limit, String action) {
            this("category", category, category, limit, action, "");
        }

        public CategoryLimitSetting(String scope, String name, String category, BigDecimal limit, String action) {
            this(scope, name, category, limit, action, "");
        }

        public CategoryLimitSetting {
            if (scope == null || scope.isBlank()) scope = "category";
            scope = scope.trim();
            if (name == null || name.isBlank()) name = category;
            if (name == null) name = "";
            name = name.trim();
            if (category == null) category = "";
            category = category.trim();
            if (category.isBlank() && "category".equals(scope)) category = name;
            if (limit == null) limit = BigDecimal.ZERO;
            if (action == null) action = "";
            if (bucketOverride == null) bucketOverride = "";
            bucketOverride = bucketOverride.trim();
        }

        public String displayName() {
            return name.isBlank() ? category : name;
        }
    }
}
