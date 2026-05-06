package com.budget.application.settings;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class BudgetSettingsServiceTest {
    private final MemoryStore store = new MemoryStore();
    private final BudgetSettingsService service = new BudgetSettingsService(
            new BudgetSettingsDefaults(BigDecimal.valueOf(14_000), BigDecimal.valueOf(13_000), 3, 6),
            store
    );

    @Test
    void rejectsComfortEmergencyFundBelowMinimum() {
        var settings = new BudgetSettings(
                BigDecimal.valueOf(14_000),
                BigDecimal.valueOf(13_000),
                6,
                3,
                List.of()
        );

        assertThatThrownBy(() -> service.save(settings))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("emergencyFundComfortMonths");
    }

    @Test
    void rejectsDuplicateCategoryLimitsBeforePersistence() {
        var settings = new BudgetSettings(
                BigDecimal.valueOf(14_000),
                BigDecimal.valueOf(13_000),
                3,
                6,
                List.of(
                        new BudgetSettings.CategoryLimitSetting("Jedzenie poza domem", BigDecimal.valueOf(500), ""),
                        new BudgetSettings.CategoryLimitSetting("Jedzenie poza domem", BigDecimal.valueOf(450), "")
                )
        );

        assertThatThrownBy(() -> service.save(settings))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Duplicate category limit");
    }

    @Test
    void trimsAndRoundsCategoryLimitOverrides() {
        var saved = service.save(new BudgetSettings(
                BigDecimal.valueOf(14_000),
                BigDecimal.valueOf(13_000),
                3,
                6,
                List.of(new BudgetSettings.CategoryLimitSetting("  Jedzenie poza domem  ", BigDecimal.valueOf(499.999), "  test  "))
        ));

        assertThat(saved.categoryLimits())
                .singleElement()
                .satisfies(limit -> {
                    assertThat(limit.category()).isEqualTo("Jedzenie poza domem");
                    assertThat(limit.limit()).isEqualByComparingTo(BigDecimal.valueOf(500));
                    assertThat(limit.action()).isEqualTo("test");
                });
    }

    private static final class MemoryStore implements BudgetSettingsStore {
        private BudgetSettings settings;

        @Override
        public Optional<BudgetSettings> findDefaultSettings() {
            return Optional.ofNullable(settings);
        }

        @Override
        public BudgetSettings saveDefaultSettings(BudgetSettings settings) {
            this.settings = settings;
            return settings;
        }
    }
}
