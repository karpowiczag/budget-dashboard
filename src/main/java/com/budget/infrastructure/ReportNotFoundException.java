package com.budget.infrastructure;

public class ReportNotFoundException extends RuntimeException {
    public ReportNotFoundException(int year) {
        super("Year not found: " + year);
    }
}
