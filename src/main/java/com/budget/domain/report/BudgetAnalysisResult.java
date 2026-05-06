package com.budget.domain.report;

import com.budget.domain.transaction.NormalizedTransaction;
import java.math.BigDecimal;
import java.util.List;

public record BudgetAnalysisResult(
        int year,
        String fileName,
        BudgetSnapshot snapshot,
        List<NormalizedTransaction> transactions,
        int transactionCount,
        BigDecimal income,
        BigDecimal spend
) {
}
