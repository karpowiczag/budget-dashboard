package com.budget.application.fire;

import java.math.BigDecimal;

/**
 * Pure compound-interest projection used by FIRE scenarios. Extracted from
 * {@link FireQueryService} so the growth math can be unit-tested in isolation,
 * without a Spring context or portfolio fixtures.
 *
 * <p>{@code futureValue} models a present principal compounded monthly plus an
 * ordinary annuity of equal end-of-month contributions.
 */
final class FireProjection {
    private FireProjection() {
    }

    static BigDecimal futureValue(BigDecimal principal, BigDecimal monthlyContribution, BigDecimal monthlyReturn, int months) {
        if (months <= 0) {
            return principal;
        }
        var rate = monthlyReturn.doubleValue();
        var factor = Math.pow(1 + rate, months);
        var contributionFactor = rate == 0 ? months : (factor - 1) / rate;
        return BigDecimal.valueOf(principal.doubleValue() * factor + monthlyContribution.doubleValue() * contributionFactor);
    }

    /**
     * Monte-Carlo success probability: the fraction of simulated paths whose terminal value
     * reaches {@code target}. Each month draws a normally-distributed real return from the
     * annual mean/volatility. Seeded for deterministic, testable results.
     */
    static double monteCarloSuccessRate(BigDecimal principal, BigDecimal monthlyContribution, BigDecimal expectedAnnualReturn,
            BigDecimal annualVolatility, int months, BigDecimal target, int paths, long seed) {
        if (paths <= 0) {
            return 0.0;
        }
        if (months <= 0) {
            return principal.doubleValue() >= target.doubleValue() ? 1.0 : 0.0;
        }
        var monthlyMean = expectedAnnualReturn.doubleValue() / 12.0;
        var monthlyVol = annualVolatility.doubleValue() / Math.sqrt(12.0);
        var start = principal.doubleValue();
        var contribution = monthlyContribution.doubleValue();
        var goal = target.doubleValue();
        var random = new java.util.Random(seed);
        var successes = 0;
        for (var path = 0; path < paths; path++) {
            var value = start;
            for (var month = 0; month < months; month++) {
                var monthlyReturn = monthlyMean + monthlyVol * random.nextGaussian();
                value = value * (1 + monthlyReturn) + contribution;
            }
            if (value >= goal) {
                successes++;
            }
        }
        return (double) successes / paths;
    }

    static BigDecimal requiredMonthlyContribution(BigDecimal principal, BigDecimal target, BigDecimal monthlyReturn, int months) {
        if (months <= 0) {
            return BigDecimal.ZERO;
        }
        var rate = monthlyReturn.doubleValue();
        var factor = Math.pow(1 + rate, months);
        var remaining = target.doubleValue() - principal.doubleValue() * factor;
        if (remaining <= 0) {
            return BigDecimal.ZERO;
        }
        var contributionFactor = rate == 0 ? months : (factor - 1) / rate;
        return BigDecimal.valueOf(remaining / contributionFactor);
    }
}
