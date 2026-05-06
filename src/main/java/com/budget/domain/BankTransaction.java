package com.budget.domain;

import java.time.LocalDate;

public record BankTransaction(
        LocalDate date,
        String account,
        String description,
        String bankCategory,
        double amount
) {
}
