package com.budget.domain.goal;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * A named savings goal with an amount and an optional target date. Progress and the
 * required monthly contribution are derived on the client from {@code targetAmount},
 * {@code currentAmount} and {@code targetDate}.
 */
public record Goal(
        String goalId,
        String name,
        BigDecimal targetAmount,
        BigDecimal currentAmount,
        LocalDate targetDate,
        String note
) {
}
