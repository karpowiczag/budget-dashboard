package com.budget.domain;

import java.util.List;
import java.util.Map;

public record BudgetAnalysisResult(
        int year,
        String fileName,
        Map<String, Object> payload,
        List<NormalizedTransaction> transactions,
        int transactionCount,
        double income,
        double spend
) {
}
