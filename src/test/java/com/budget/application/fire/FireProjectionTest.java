package com.budget.application.fire;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class FireProjectionTest {
    private static final BigDecimal ONE_PERCENT = BigDecimal.valueOf(0.01);

    @Test
    void zeroReturnFutureValueIsPrincipalPlusContributions() {
        var value = FireProjection.futureValue(new BigDecimal("1000"), new BigDecimal("100"), BigDecimal.ZERO, 12);
        assertThat(value).isEqualByComparingTo("2200");
    }

    @Test
    void positiveReturnGrowsPrincipalAndAnnuityAsOrdinaryAnnuity() {
        // FV of 100/month for 12 months at 1% monthly = 100 * ((1.01^12 - 1) / 0.01) = 1268.25
        var annuity = FireProjection.futureValue(BigDecimal.ZERO, new BigDecimal("100"), ONE_PERCENT, 12);
        assertThat(annuity).isCloseTo(new BigDecimal("1268.25"), within(new BigDecimal("0.05")));

        // FV of 1000 principal compounded monthly at 1% for 12 months = 1000 * 1.01^12 = 1126.83
        var compounded = FireProjection.futureValue(new BigDecimal("1000"), BigDecimal.ZERO, ONE_PERCENT, 12);
        assertThat(compounded).isCloseTo(new BigDecimal("1126.83"), within(new BigDecimal("0.05")));
    }

    @Test
    void requiredContributionInvertsTheAnnuityFormula() {
        var required = FireProjection.requiredMonthlyContribution(BigDecimal.ZERO, new BigDecimal("1268.25"), ONE_PERCENT, 12);
        assertThat(required).isCloseTo(new BigDecimal("100"), within(new BigDecimal("0.05")));

        var zeroRate = FireProjection.requiredMonthlyContribution(BigDecimal.ZERO, new BigDecimal("1200"), BigDecimal.ZERO, 12);
        assertThat(zeroRate).isEqualByComparingTo("100");
    }

    @Test
    void requiredContributionIsZeroWhenPrincipalAlreadyReachesTarget() {
        var required = FireProjection.requiredMonthlyContribution(new BigDecimal("2000"), new BigDecimal("1000"), ONE_PERCENT, 12);
        assertThat(required).isEqualByComparingTo("0");
    }

    @Test
    void nonPositiveHorizonShortCircuits() {
        assertThat(FireProjection.futureValue(new BigDecimal("5000"), new BigDecimal("100"), ONE_PERCENT, 0))
                .isEqualByComparingTo("5000");
        assertThat(FireProjection.requiredMonthlyContribution(new BigDecimal("100"), new BigDecimal("9999"), ONE_PERCENT, 0))
                .isEqualByComparingTo("0");
    }
}
