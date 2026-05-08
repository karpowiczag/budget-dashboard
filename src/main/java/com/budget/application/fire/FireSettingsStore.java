package com.budget.application.fire;

import java.util.Optional;

public interface FireSettingsStore {
    Optional<FireSettings> findDefaultSettings();

    FireSettings saveDefaultSettings(FireSettings settings);
}
