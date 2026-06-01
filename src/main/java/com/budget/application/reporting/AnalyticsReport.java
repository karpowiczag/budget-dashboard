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
        List<FinancialFlow> financialFlows,
        List<MerchantSpend> merchants,
        List<TransactionRecord> oneoffs,
        List<MonthlyCategoryTrend> monthlyCategoryTrends,
        List<MonthlyHierarchyTrend> monthlyHierarchyTrends,
        List<MonthlyBucketTrend> monthlyBucketTrends,
        List<MonthlyMerchantTrend> monthlyMerchantTrends,
        List<FixednessBreakdown> fixednessBreakdown,
        List<ConfidenceBreakdown> confidenceBreakdown,
        List<AmountBand> amountBands
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

    public record FinancialFlow(String category, BigDecimal outgoing, int count) {
    }

    public record MerchantSpend(String merchant, BigDecimal sum, int count) {
    }

    public record MonthlyCategoryTrend(String month, String monthKey, String category, BigDecimal spend, int count) {
    }

    public record MonthlyHierarchyTrend(String month, String monthKey, String area, String group, String category, String subcategory, BigDecimal spend, int count) {
    }

    public record MonthlyBucketTrend(String month, String monthKey, String bucket, BigDecimal spend, int count) {
    }

    public record MonthlyMerchantTrend(String month, String monthKey, String merchant, BigDecimal spend, int count) {
    }

    public record FixednessBreakdown(String fixedness, BigDecimal spend, int count) {
    }

    public record ConfidenceBreakdown(String confidence, int count, BigDecimal spend, BigDecimal income, BigDecimal excluded) {
    }

    public record AmountBand(String label, BigDecimal minAmount, BigDecimal maxAmount, int count, BigDecimal spend) {
    }
}
