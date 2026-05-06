package com.budget.domain.report;

import com.budget.domain.transaction.NormalizedTransaction;
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
