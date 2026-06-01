package com.budget.application.fire;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class FireSettingsServiceTest {
    private final FireSettings defaults = settings(
            36,
            50,
            null,
            null,
            "0.035",
            "0.020",
            "0.040",
            "0.055",
            "0.800",
            "0.100",
            "0.050",
            "0.050",
            "0.050"
    );
    private final MemoryStore store = new MemoryStore();
    private final FireSettingsService service = new FireSettingsService(defaults, store);

    @Test
    void returnsDefaultsWhenNoPersistedSettingsExist() {
        var current = service.current();

        assertThat(current.currentAge()).isEqualTo(36);
        assertThat(current.targetAge()).isEqualTo(50);
        assertThat(current.safeWithdrawalRate()).isEqualByComparingTo("0.035000");
    }

    @Test
    void savesValidatedOverrides() {
        var saved = service.save(settings(
                37,
                51,
                "15000",
                "8000",
                "0.033",
                "0.010",
                "0.035",
                "0.055",
                "0.700",
                "0.200",
                "0.050",
                "0.050",
                "0.050"
        ));

        assertThat(saved.currentAge()).isEqualTo(37);
        assertThat(saved.monthlySpendOverride()).isEqualByComparingTo("15000.00");
        assertThat(saved.targetEquityShare()).isEqualByComparingTo("0.700000");
        assertThat(service.current().monthlyContributionOverride()).isEqualByComparingTo("8000.00");
    }

    @Test
    void keepsReportsPathEnvironmentControlled() {
        var incoming = settings(36, 50, null, null, "0.035", "0.020", "0.040", "0.055", "0.800", "0.100", "0.050", "0.050", "0.050");
        var saved = service.save(new FireSettings(
                Path.of("C:/private/other"),
                incoming.currentAge(),
                incoming.targetAge(),
                incoming.monthlySpendOverride(),
                incoming.monthlyContributionOverride(),
                incoming.safeWithdrawalRate(),
                incoming.pessimisticRealReturn(),
                incoming.expectedRealReturn(),
                incoming.optimisticRealReturn(),
                incoming.targetEquityShare(),
                incoming.targetBondShare(),
                incoming.targetCashShare(),
                incoming.targetAlternativeShare(),
                incoming.rebalanceBand()
        ));

        assertThat(saved.reportsPath()).isEqualTo(defaults.reportsPath());
    }

    @Test
    void rejectsAllocationThatDoesNotSumToOneHundredPercent() {
        var invalid = settings(36, 50, null, null, "0.035", "0.020", "0.040", "0.055", "0.900", "0.100", "0.050", "0.050", "0.050");

        assertThatThrownBy(() -> service.save(invalid))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("sum to 100%");
    }

    @Test
    void rejectsTargetAgeBeforeCurrentAge() {
        var invalid = settings(50, 50, null, null, "0.035", "0.020", "0.040", "0.055", "0.800", "0.100", "0.050", "0.050", "0.050");

        assertThatThrownBy(() -> service.save(invalid))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("targetAge");
    }

    private FireSettings settings(
            int currentAge,
            int targetAge,
            String monthlySpend,
            String monthlyContribution,
            String swr,
            String low,
            String base,
            String high,
            String equity,
            String bonds,
            String cash,
            String alternatives,
            String band
    ) {
        return new FireSettings(
                Path.of("fire/investments_reports"),
                currentAge,
                targetAge,
                decimalOrNull(monthlySpend),
                decimalOrNull(monthlyContribution),
                new BigDecimal(swr),
                new BigDecimal(low),
                new BigDecimal(base),
                new BigDecimal(high),
                new BigDecimal(equity),
                new BigDecimal(bonds),
                new BigDecimal(cash),
                new BigDecimal(alternatives),
                new BigDecimal(band)
        );
    }

    private BigDecimal decimalOrNull(String value) {
        return value == null ? null : new BigDecimal(value);
    }

    private static final class MemoryStore implements FireSettingsStore {
        private FireSettings settings;

        @Override
        public Optional<FireSettings> findDefaultSettings() {
            return Optional.ofNullable(settings);
        }

        @Override
        public FireSettings saveDefaultSettings(FireSettings settings) {
            this.settings = settings;
            return settings;
        }
    }
}
