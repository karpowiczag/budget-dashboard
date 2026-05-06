package com.budget.domain.transaction;

import java.math.BigDecimal;
import java.time.LocalDate;

public record BankTransaction(
        LocalDate date,
        String account,
        String description,
        String bankCategory,
        BigDecimal amount
) {
    public BankTransaction(LocalDate date, String account, String description, String bankCategory, double amount) {
        this(date, account, description, bankCategory, BigDecimal.valueOf(amount));
    }
}
