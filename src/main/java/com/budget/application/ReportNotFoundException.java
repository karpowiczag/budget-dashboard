package com.budget.application;

public class ReportNotFoundException extends RuntimeException {
    public ReportNotFoundException(int year) {
        super("Year not found: " + year);
    }
}
