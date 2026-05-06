package com.budget.domain.transaction;

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
        double amount,
        double income,
        double analysisSpend,
        double discretionary,
        double excluded,
        double excludedOutgoing,
        double excludedIncoming,
        double excludedNet,
        String confidence,
        String notes,
        String matchedRule
) {
}
