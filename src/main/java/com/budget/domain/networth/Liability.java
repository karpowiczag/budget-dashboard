package com.budget.domain.networth;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * A household debt tracked on the liabilities side of net worth. {@code currentPrincipal}
 * is the outstanding balance; {@code annualInterestRate} and {@code monthlyPayment} are
 * optional and feed the later debt-payoff module (Phase 2d).
 */
public record Liability(
        String liabilityKey,
        String name,
        String kind,
        BigDecimal currentPrincipal,
        BigDecimal annualInterestRate,
        BigDecimal monthlyPayment,
        LocalDate asOf
) {
}
