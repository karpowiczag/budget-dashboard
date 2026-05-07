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
        String flow,
        String bucket,
        String area,
        String group,
        String category,
        String subcategory,
        String fixedness,
        String confidence
) {
    private static final int DEFAULT_SIZE = 50;
    private static final int MAX_SIZE = 200;
    private static final int MAX_PAGE = 100_000;
    private static final int MAX_FILTER_LENGTH = 160;

    public TransactionQuery {
        page = Math.max(0, page);
        if (page > MAX_PAGE) {
            throw new IllegalArgumentException("page cannot be greater than " + MAX_PAGE);
        }
        size = size <= 0 ? DEFAULT_SIZE : Math.min(size, MAX_SIZE);
        sort = sanitizeSort(sort);
        month = trimToNull(month, "month");
        query = trimToNull(query, "query");
        flow = sanitizeFlow(flow);
        bucket = trimToNull(bucket, "bucket");
        area = trimToNull(area, "area");
        group = trimToNull(group, "group");
        category = trimToNull(category, "category");
        subcategory = trimToNull(subcategory, "subcategory");
        fixedness = trimToNull(fixedness, "fixedness");
        confidence = trimToNull(confidence, "confidence");
    }

    public TransactionQuery(
            int year,
            int page,
            int size,
            String sort,
            String month,
            LocalDate date,
            String query,
            String flow,
            String bucket,
            String area,
            String group,
            String category,
            String subcategory
    ) {
        this(year, page, size, sort, month, date, query, flow, bucket, area, group, category, subcategory, null, null);
    }

    public long offset() {
        return Math.multiplyExact((long) page, (long) size);
    }

    public String orderByClause() {
        var parts = sort.split(",", 2);
        var direction = parts.length == 2 && "asc".equalsIgnoreCase(parts[1]) ? "ASC" : "DESC";
        var column = switch (parts[0]) {
            case "amount" -> "amount";
            case "spend" -> "analysis_spend";
            case "merchant" -> "merchant";
            case "category" -> "corrected_category";
            case "subcategory" -> "subcategory";
            case "bucket" -> "bucket";
            case "fixedness" -> "fixedness";
            case "confidence" -> "confidence";
            default -> "posted_date";
        };
        return column + " " + direction + ", id " + direction;
    }

    public TransactionQuery withPageSize(int nextPage, int nextSize) {
        return new TransactionQuery(year, nextPage, nextSize, sort, month, date, query, flow, bucket, area, group, category, subcategory, fixedness, confidence);
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
            case "subcategory" -> "subcategory";
            case "bucket" -> "bucket";
            case "fixedness" -> "fixedness";
            case "confidence" -> "confidence";
            default -> "postedDate";
        };
        var direction = parts.length == 2 && "asc".equalsIgnoreCase(parts[1]) ? "asc" : "desc";
        return field + "," + direction;
    }

    private static String sanitizeFlow(String value) {
        var flow = trimToNull(value, "flow");
        if (flow == null) {
            return null;
        }
        return switch (flow.toLowerCase(java.util.Locale.ROOT)) {
            case "income", "spend", "excluded", "financial" -> flow.toLowerCase(java.util.Locale.ROOT);
            default -> throw new IllegalArgumentException("Unsupported transaction flow: " + value);
        };
    }

    private static String trimToNull(String value, String parameterName) {
        if (value == null || value.isBlank()) {
            return null;
        }
        var trimmed = value.trim();
        if (trimmed.length() > MAX_FILTER_LENGTH) {
            throw new IllegalArgumentException(parameterName + " cannot be longer than " + MAX_FILTER_LENGTH + " characters");
        }
        return trimmed;
    }
}
