package com.budget.application.reporting;

public class ReportNotFoundException extends RuntimeException {
    public ReportNotFoundException(int year) {
        super("Year not found: " + year);
    }
}
