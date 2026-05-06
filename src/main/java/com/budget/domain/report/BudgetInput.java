package com.budget.domain.report;

import com.budget.domain.transaction.BankTransaction;
import java.util.List;

public record BudgetInput(int year, String fileName, List<BankTransaction> transactions) {
    public BudgetInput {
        transactions = transactions == null ? List.of() : List.copyOf(transactions);
    }
}
