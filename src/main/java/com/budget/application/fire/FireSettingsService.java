package com.budget.application.fire;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FireSettingsService {
    private static final BigDecimal ZERO = BigDecimal.ZERO;
    private static final BigDecimal ONE = BigDecimal.ONE;
    private static final BigDecimal SHARE_TOLERANCE = new BigDecimal("0.010000");

    private final FireSettings defaults;
    private final FireSettingsStore store;

    public FireSettingsService(FireSettings defaults, FireSettingsStore store) {
        this.defaults = defaults;
        this.store = store;
    }

    public FireSettings current() {
        return mergeWithDefaults(store.findDefaultSettings().orElse(defaults));
    }

    @Transactional
    public FireSettings save(FireSettings settings) {
        return store.saveDefaultSettings(validate(settings));
    }

    private FireSettings mergeWithDefaults(FireSettings settings) {
        return new FireSettings(
                // Intentionally environment-controlled: the server owns the MyFund
                // reports path. Honouring a client-supplied path would let the API
                // redirect server-side file reads (see keepsReportsPathEnvironmentControlled).
                defaults.reportsPath(),
                positiveOrDefault(settings.currentAge(), defaults.currentAge()),
                positiveOrDefault(settings.targetAge(), defaults.targetAge()),
                moneyOrNull(settings.monthlySpendOverride()),
                moneyOrNull(settings.monthlyContributionOverride()),
                fractionOrDefault(settings.safeWithdrawalRate(), defaults.safeWithdrawalRate()),
                settings.pessimisticRealReturn() == null ? defaults.pessimisticRealReturn() : rate(settings.pessimisticRealReturn()),
                settings.expectedRealReturn() == null ? defaults.expectedRealReturn() : rate(settings.expectedRealReturn()),
                settings.optimisticRealReturn() == null ? defaults.optimisticRealReturn() : rate(settings.optimisticRealReturn()),
                fractionOrDefault(settings.targetEquityShare(), defaults.targetEquityShare()),
                fractionOrDefault(settings.targetBondShare(), defaults.targetBondShare()),
                fractionOrDefault(settings.targetCashShare(), defaults.targetCashShare()),
                fractionOrDefault(settings.targetAlternativeShare(), defaults.targetAlternativeShare()),
                fractionOrDefault(settings.rebalanceBand(), defaults.rebalanceBand())
        );
    }

    private FireSettings validate(FireSettings settings) {
        if (settings == null) {
            throw new IllegalArgumentException("FIRE settings body is required");
        }
        var normalized = mergeWithDefaults(settings);
        if (normalized.currentAge() < 18 || normalized.currentAge() > 90) {
            throw new IllegalArgumentException("currentAge must be between 18 and 90");
        }
        if (normalized.targetAge() <= normalized.currentAge() || normalized.targetAge() > 90) {
            throw new IllegalArgumentException("targetAge must be greater than currentAge and no more than 90");
        }
        if (normalized.safeWithdrawalRate().compareTo(new BigDecimal("0.015")) < 0 || normalized.safeWithdrawalRate().compareTo(new BigDecimal("0.070")) > 0) {
            throw new IllegalArgumentException("safeWithdrawalRate must be between 1.5% and 7.0%");
        }
        if (normalized.rebalanceBand().compareTo(new BigDecimal("0.010")) < 0 || normalized.rebalanceBand().compareTo(new BigDecimal("0.200")) > 0) {
            throw new IllegalArgumentException("rebalanceBand must be between 1% and 20%");
        }
        var shareSum = normalized.targetEquityShare()
                .add(normalized.targetBondShare())
                .add(normalized.targetCashShare())
                .add(normalized.targetAlternativeShare());
        if (shareSum.subtract(ONE).abs().compareTo(SHARE_TOLERANCE) > 0) {
            throw new IllegalArgumentException("Target allocation shares must sum to 100%");
        }
        if (normalized.pessimisticRealReturn().compareTo(normalized.expectedRealReturn()) > 0
                || normalized.expectedRealReturn().compareTo(normalized.optimisticRealReturn()) > 0) {
            throw new IllegalArgumentException("Real return scenarios must be ordered pessimistic <= expected <= optimistic");
        }
        return normalized;
    }

    private int positiveOrDefault(int value, int fallback) {
        return value > 0 ? value : fallback;
    }

    private BigDecimal fractionOrDefault(BigDecimal value, BigDecimal fallback) {
        var normalized = value == null ? fallback : value;
        if (normalized == null) {
            return ZERO.setScale(6, RoundingMode.HALF_UP);
        }
        if (normalized.signum() < 0 || normalized.compareTo(ONE) > 0) {
            throw new IllegalArgumentException("Fraction settings must be between 0 and 1");
        }
        return normalized.setScale(6, RoundingMode.HALF_UP);
    }

    private BigDecimal rate(BigDecimal value) {
        if (value.compareTo(new BigDecimal("-0.500")) < 0 || value.compareTo(new BigDecimal("0.500")) > 0) {
            throw new IllegalArgumentException("Real return scenarios must be between -50% and 50%");
        }
        return value.setScale(6, RoundingMode.HALF_UP);
    }

    private BigDecimal moneyOrNull(BigDecimal value) {
        if (value == null || value.signum() <= 0) {
            return null;
        }
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    public FireSettings defaults() {
        return Objects.requireNonNull(defaults);
    }
}
