package com.budget.application.importing;

import java.util.List;

public record ImportSummary(
        String status,
        List<Integer> years,
        int transactions,
        double income,
        double spend,
        String message
) {
}
