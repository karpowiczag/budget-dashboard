package com.budget.application.reporting;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record YearSummary(
        int year,
        OffsetDateTime importedAt,
        int transactions,
        BigDecimal income,
        BigDecimal spend,
        String inputCsv
) {
}
