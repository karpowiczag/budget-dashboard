package com.budget.application.reporting;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TransactionRecord(
        long id,
        int lp,
        LocalDate postedDate,
        String month,
        String merchant,
        String description,
        String account,
        String bankCategory,
        String correctedCategory,
        String area,
        String group,
        String subcategory,
        String bucket,
        String fixedness,
        String type,
        BigDecimal amount,
        BigDecimal income,
        BigDecimal spend,
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
