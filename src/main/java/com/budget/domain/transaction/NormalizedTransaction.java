package com.budget.domain.transaction;

import java.math.BigDecimal;
import java.time.LocalDate;

public record NormalizedTransaction(
        int lp,
        LocalDate date,
        String month,
        String merchant,
        String description,
        String account,
        String bankCategory,
        String correctedCategory,
        String budgetArea,
        String group,
        String subcategory,
        String budgetBucket,
        String fixedness,
        String type,
        BigDecimal amount,
        BigDecimal income,
        BigDecimal analysisSpend,
        BigDecimal discretionary,
        BigDecimal excluded,
        BigDecimal excludedOutgoing,
        BigDecimal excludedIncoming,
        BigDecimal excludedNet,
        String confidence,
        String notes,
        String matchedRule
) {
}
