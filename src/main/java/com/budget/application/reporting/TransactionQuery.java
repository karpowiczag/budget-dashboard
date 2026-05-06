package com.budget.application.reporting;

import java.time.LocalDate;

public record TransactionQuery(
        int year,
        int page,
        int size,
        String sort,
        String month,
        LocalDate date,
        String query,
        String bucket,
        String area,
        String group,
        String category,
        String subcategory
) {
    public TransactionQuery {
        page = Math.max(0, page);
        size = size <= 0 ? 50 : Math.min(size, 200);
        sort = sanitizeSort(sort);
        month = blankToNull(month);
        query = blankToNull(query);
        bucket = blankToNull(bucket);
        area = blankToNull(area);
        group = blankToNull(group);
        category = blankToNull(category);
        subcategory = blankToNull(subcategory);
    }

    public int offset() {
        return page * size;
    }

    public String orderByClause() {
        var parts = sort.split(",", 2);
        var direction = parts.length == 2 && "asc".equalsIgnoreCase(parts[1]) ? "ASC" : "DESC";
        var column = switch (parts[0]) {
            case "amount" -> "amount";
            case "spend" -> "analysis_spend";
            case "merchant" -> "merchant";
            case "category" -> "corrected_category";
            default -> "posted_date";
        };
        return column + " " + direction + ", id " + direction;
    }

    public TransactionQuery withPageSize(int nextPage, int nextSize) {
        return new TransactionQuery(year, nextPage, nextSize, sort, month, date, query, bucket, area, group, category, subcategory);
    }

    private static String sanitizeSort(String value) {
        if (value == null || value.isBlank()) {
            return "postedDate,desc";
        }
        var parts = value.split(",", 2);
        var field = switch (parts[0]) {
            case "amount" -> "amount";
            case "spend", "analysisSpend" -> "spend";
            case "merchant" -> "merchant";
            case "category", "correctedCategory" -> "category";
            default -> "postedDate";
        };
        var direction = parts.length == 2 && "asc".equalsIgnoreCase(parts[1]) ? "asc" : "desc";
        return field + "," + direction;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
