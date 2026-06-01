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
