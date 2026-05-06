package com.budget.domain;

import java.util.List;

public record BudgetInput(int year, String fileName, List<BankTransaction> transactions) {
}
