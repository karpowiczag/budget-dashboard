package com.budget.application.categorization;

record CategoryInput(String bankCategory, String description, double amount) {
    String normalizedDescription() {
        return description == null ? "" : description.replaceAll("\\s+", " ").trim().toUpperCase();
    }
}
