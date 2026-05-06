package com.budget.application.reporting;

import java.util.List;

public record TransactionPage(
        List<TransactionRecord> items,
        int page,
        int size,
        long totalItems,
        int totalPages,
        String sort
) {
    public static TransactionPage of(List<TransactionRecord> items, TransactionQuery query, long totalItems) {
        var totalPages = totalItems == 0 ? 0 : (int) Math.ceil(totalItems / (double) query.size());
        return new TransactionPage(items, query.page(), query.size(), totalItems, totalPages, query.sort());
    }
}
