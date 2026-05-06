package com.budget.application.settings;

import java.math.BigDecimal;
import java.util.List;

public record BudgetSettings(
        BigDecimal targetMonthlySpend,
        BigDecimal aggressiveMonthlySpend,
        int emergencyFundMinMonths,
        int emergencyFundComfortMonths,
        List<CategoryLimitSetting> categoryLimits
) {
    public BudgetSettings {
        categoryLimits = categoryLimits == null ? List.of() : List.copyOf(categoryLimits);
    }

    public record CategoryLimitSetting(String category, BigDecimal limit, String action) {
        public CategoryLimitSetting {
            if (category == null) category = "";
            if (limit == null) limit = BigDecimal.ZERO;
            if (action == null) action = "";
        }
    }
}
