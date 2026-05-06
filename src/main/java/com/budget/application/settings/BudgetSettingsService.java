package com.budget.application.settings;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
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
        if (normalized.emergencyFundComfortMonths() < normalized.emergencyFundMinMonths()) {
            throw new IllegalArgumentException("emergencyFundComfortMonths cannot be lower than emergencyFundMinMonths");
        }
        var limitsByCategory = new LinkedHashMap<String, BudgetSettings.CategoryLimitSetting>();
        for (var row : normalized.categoryLimits()) {
            if (row.category() == null || row.category().isBlank()) {
                continue;
            }
            if (row.limit().signum() < 0) {
                throw new IllegalArgumentException("Category limits cannot be negative");
            }
            var category = row.category().trim();
            if (limitsByCategory.containsKey(category)) {
                throw new IllegalArgumentException("Duplicate category limit: " + category);
            }
            limitsByCategory.put(category, new BudgetSettings.CategoryLimitSetting(
                    category,
                    money(row.limit()),
                    row.action() == null ? "" : row.action().trim()
            ));
        }
        return new BudgetSettings(
                money(normalized.targetMonthlySpend()),
                money(normalized.aggressiveMonthlySpend()),
                normalized.emergencyFundMinMonths(),
                normalized.emergencyFundComfortMonths(),
                java.util.List.copyOf(limitsByCategory.values())
        );
    }

    private BigDecimal positiveOrDefault(BigDecimal value, BigDecimal fallback) {
        return value != null && value.signum() > 0 ? value : fallback;
    }

    private int positiveOrDefault(int value, int fallback) {
        return value > 0 ? value : fallback;
    }

    private BigDecimal money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }
}
