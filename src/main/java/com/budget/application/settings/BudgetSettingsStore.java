package com.budget.application.settings;

import java.util.Optional;

public interface BudgetSettingsStore {
    Optional<BudgetSettings> findDefaultSettings();

    BudgetSettings saveDefaultSettings(BudgetSettings settings);
}
