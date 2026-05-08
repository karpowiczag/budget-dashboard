package com.budget.application.categorization;

import java.math.BigDecimal;

record CategoryInput(String bankCategory, String description, BigDecimal amount) {
    String normalizedBankCategory() {
        return bankCategory == null ? "" : bankCategory.replaceAll("\\s+", " ").trim().toUpperCase();
    }

    String normalizedDescription() {
        return description == null ? "" : description.replaceAll("\\s+", " ").trim().toUpperCase();
    }

    boolean positiveAmount() {
        return amount != null && amount.signum() > 0;
    }

    boolean negativeAmount() {
        return amount != null && amount.signum() < 0;
    }
}
