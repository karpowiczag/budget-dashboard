package com.budget.application.settings;

import java.math.BigDecimal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BudgetSettingsService {
    private final BudgetSettingsDefaults defaults;
    private final BudgetSettingsStore store;

    public BudgetSettingsService(BudgetSettingsDefaults defaults, BudgetSettingsStore store) {
        this.defaults = defaults;
        this.store = store;
    }

    public BudgetSettings current() {
        return mergeWithDefaults(store.findDefaultSettings().orElse(defaults.toSettings()));
    }

    @Transactional
    public BudgetSettings save(BudgetSettings settings) {
        return store.saveDefaultSettings(validate(settings));
    }

    private BudgetSettings mergeWithDefaults(BudgetSettings settings) {
        return new BudgetSettings(
                positiveOrDefault(settings.targetMonthlySpend(), defaults.targetMonthlySpend()),
                positiveOrDefault(settings.aggressiveMonthlySpend(), defaults.aggressiveMonthlySpend()),
                positiveOrDefault(settings.emergencyFundMinMonths(), defaults.emergencyFundMinMonths()),
                positiveOrDefault(settings.emergencyFundComfortMonths(), defaults.emergencyFundComfortMonths()),
                settings.categoryLimits()
        );
    }

    private BudgetSettings validate(BudgetSettings settings) {
        if (settings == null) {
            throw new IllegalArgumentException("Budget settings body is required");
        }
        var normalized = mergeWithDefaults(settings);
        if (normalized.aggressiveMonthlySpend().compareTo(normalized.targetMonthlySpend()) > 0) {
            throw new IllegalArgumentException("aggressiveMonthlySpend cannot be greater than targetMonthlySpend");
        }
        var limits = normalized.categoryLimits().stream()
                .filter(row -> row.category() != null && !row.category().isBlank())
                .map(row -> {
                    if (row.limit().signum() < 0) {
                        throw new IllegalArgumentException("Category limits cannot be negative");
                    }
                    return new BudgetSettings.CategoryLimitSetting(row.category().trim(), row.limit(), row.action());
                })
                .toList();
        return new BudgetSettings(
                normalized.targetMonthlySpend(),
                normalized.aggressiveMonthlySpend(),
                normalized.emergencyFundMinMonths(),
                normalized.emergencyFundComfortMonths(),
                limits
        );
    }

    private BigDecimal positiveOrDefault(BigDecimal value, BigDecimal fallback) {
        return value != null && value.signum() > 0 ? value : fallback;
    }

    private int positiveOrDefault(int value, int fallback) {
        return value > 0 ? value : fallback;
    }
}
