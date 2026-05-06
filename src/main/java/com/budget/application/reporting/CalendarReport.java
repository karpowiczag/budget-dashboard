package com.budget.application.reporting;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record CalendarReport(
        int year,
        String month,
        List<CalendarDay> days
) {
    public record CalendarDay(
            LocalDate date,
            int day,
            BigDecimal spend,
            BigDecimal income,
            int transactions,
            TransactionRecord biggest
    ) {
    }
}
