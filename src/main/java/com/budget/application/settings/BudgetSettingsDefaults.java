package com.budget.application.settings;

import java.math.BigDecimal;
import java.util.List;

public record BudgetSettingsDefaults(
        BigDecimal targetMonthlySpend,
        BigDecimal aggressiveMonthlySpend,
        int emergencyFundMinMonths,
        int emergencyFundComfortMonths
) {
    public BudgetSettings toSettings() {
        return new BudgetSettings(
                targetMonthlySpend,
                aggressiveMonthlySpend,
                emergencyFundMinMonths,
                emergencyFundComfortMonths,
                List.of()
        );
    }
}
