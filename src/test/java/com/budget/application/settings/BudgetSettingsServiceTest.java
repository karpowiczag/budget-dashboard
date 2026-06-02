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
    void rejectsDuplicateBudgetLimitsBeforePersistence() {
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
                .hasMessageContaining("Duplicate budget limit");
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
                    assertThat(limit.scope()).isEqualTo("category");
                    assertThat(limit.name()).isEqualTo("Jedzenie poza domem");
                    assertThat(limit.limit()).isEqualByComparingTo(BigDecimal.valueOf(500));
                    assertThat(limit.action()).isEqualTo("test");
                    assertThat(limit.bucketOverride()).isBlank();
                });
    }

    @Test
    void acceptsManualBucketOverrideForCategoryLimit() {
        var saved = service.save(new BudgetSettings(
                BigDecimal.valueOf(14_000),
                BigDecimal.valueOf(13_000),
                3,
                6,
                List.of(new BudgetSettings.CategoryLimitSetting("category", "Lekarz i apteka", "Lekarz i apteka", BigDecimal.valueOf(900), "review", "Obowiązkowe zmienne"))
        ));

        assertThat(saved.categoryLimits())
                .singleElement()
                .satisfies(limit -> {
                    assertThat(limit.category()).isEqualTo("Lekarz i apteka");
                    assertThat(limit.bucketOverride()).isEqualTo("Obowiązkowe zmienne");
                });
    }

    @Test
    void rejectsUnsupportedBucketOverride() {
        var settings = new BudgetSettings(
                BigDecimal.valueOf(14_000),
                BigDecimal.valueOf(13_000),
                3,
                6,
                List.of(new BudgetSettings.CategoryLimitSetting("category", "Lekarz i apteka", "Lekarz i apteka", BigDecimal.valueOf(900), "review", "Random"))
        );

        assertThatThrownBy(() -> service.save(settings))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported budget bucket override");
    }

    @Test
    void acceptsParentLimitOverridesByScopeAndName() {
        var saved = service.save(new BudgetSettings(
                BigDecimal.valueOf(14_000),
                BigDecimal.valueOf(13_000),
                3,
                6,
                List.of(new BudgetSettings.CategoryLimitSetting("group", "Styl życia", "", BigDecimal.valueOf(2_000), "limit group"))
        ));

        assertThat(saved.categoryLimits())
                .singleElement()
                .satisfies(limit -> {
                    assertThat(limit.scope()).isEqualTo("group");
                    assertThat(limit.name()).isEqualTo("Styl życia");
                    assertThat(limit.category()).isBlank();
                    assertThat(limit.limit()).isEqualByComparingTo(BigDecimal.valueOf(2_000));
                });
    }

    @Test
    void defaultsNetIncomeRatioWhenUnsetOrOutOfRange() {
        var unset = service.save(new BudgetSettings(BigDecimal.valueOf(14_000), BigDecimal.valueOf(13_000), 3, 6, List.of()));
        assertThat(unset.netIncomeRatio()).isEqualByComparingTo("0.75");
        assertThat(service.current().netIncomeRatio()).isEqualByComparingTo("0.75");

        var tooHigh = service.save(new BudgetSettings(BigDecimal.valueOf(14_000), BigDecimal.valueOf(13_000), 3, 6, new BigDecimal("1.5"), List.of()));
        assertThat(tooHigh.netIncomeRatio()).isEqualByComparingTo("0.75");
    }

    @Test
    void preservesValidNetIncomeRatio() {
        var saved = service.save(new BudgetSettings(BigDecimal.valueOf(14_000), BigDecimal.valueOf(13_000), 3, 6, new BigDecimal("0.82"), List.of()));
        assertThat(saved.netIncomeRatio()).isEqualByComparingTo("0.82");
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
