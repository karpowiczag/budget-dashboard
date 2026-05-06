package com.budget.application.importing;

import java.math.BigDecimal;
import java.util.List;

public record ImportSummary(
        String status,
        List<Integer> years,
        int transactions,
        BigDecimal income,
        BigDecimal spend,
        String message
) {
}
