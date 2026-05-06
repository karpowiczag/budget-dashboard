package com.budget.application.reporting;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record AnalyticsReport(
        int year,
        String scope,
        String month,
        LocalDate date,
        BigDecimal spend,
        BigDecimal income,
        int transactionCount,
        List<AreaSpend> areaTop,
        List<GroupSpend> groupTop,
        List<CategorySpend> categoryTop,
        List<SubcategorySpend> subcategoryTop,
        List<HierarchySpend> hierarchyTop,
        List<MerchantSpend> merchants,
        List<TransactionRecord> oneoffs
) {
    public record AreaSpend(String area, BigDecimal spend, int count) {
    }

    public record GroupSpend(String group, BigDecimal spend, int count) {
    }

    public record CategorySpend(String category, BigDecimal spend, int count) {
    }

    public record SubcategorySpend(String subcategory, String category, BigDecimal spend, int count) {
    }

    public record HierarchySpend(String area, String group, String category, String subcategory, BigDecimal spend, int count) {
    }

    public record MerchantSpend(String merchant, BigDecimal sum, int count) {
    }
}
