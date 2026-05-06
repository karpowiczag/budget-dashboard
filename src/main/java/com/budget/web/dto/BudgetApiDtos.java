package com.budget.web.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public final class BudgetApiDtos {
    private BudgetApiDtos() {
    }

    public record SessionResponse(boolean authenticated, String name) {
    }

    public record YearSummaryResponse(
            int year,
            OffsetDateTime importedAt,
            int transactions,
            BigDecimal income,
            BigDecimal spend,
            String inputCsv
    ) {
    }

    public record DashboardResponse(
            int year,
            String period,
            LocalDate periodStart,
            LocalDate periodEnd,
            int activeMonths,
            KpisResponse kpis,
            List<MonthlySummaryResponse> monthly,
            List<CategorySummaryResponse> categories,
            List<HierarchySummaryResponse> hierarchy,
            List<BudgetMixItemResponse> budgetMix,
            SavingsPlanResponse savingsPlan,
            MonthControlResponse monthControl,
            List<FixednessSummaryResponse> fixedness,
            List<MerchantSummaryResponse> topMerchants,
            List<RecurringItemResponse> recurring,
            List<LargeOneOffResponse> largeOneoffs
    ) {
    }

    public record KpisResponse(
            BigDecimal income,
            BigDecimal spend,
            BigDecimal discretionary,
            BigDecimal operatingSurplus,
            BigDecimal savingsRate,
            BigDecimal excludedGross,
            BigDecimal excludedOutgoing,
            BigDecimal excludedIncoming,
            BigDecimal excludedNet,
            BigDecimal realSavingsOutgoing,
            BigDecimal unassignedSurplus,
            int transactions,
            int corrections,
            int lowConfidence,
            int toCheck,
            BigDecimal toCheckAmount
    ) {
    }

    public record MonthlySummaryResponse(
            String month,
            String monthKey,
            BigDecimal income,
            BigDecimal spend,
            BigDecimal discretionary,
            BigDecimal excluded,
            BigDecimal savingsInvestments,
            BigDecimal netFlow,
            BigDecimal savingsRate,
            int transactions,
            BigDecimal spendPerDay
    ) {
    }

    public record CategorySummaryResponse(
            String category,
            String group,
            BigDecimal spend,
            BigDecimal income,
            BigDecimal excluded,
            BigDecimal monthlyAverage,
            String maxMonth,
            BigDecimal maxAmount,
            boolean discretionary,
            int count
    ) {
    }

    public record HierarchySummaryResponse(
            String area,
            String group,
            String category,
            String subcategory,
            BigDecimal spend,
            BigDecimal income,
            BigDecimal excluded,
            BigDecimal monthlyAverage,
            int count,
            boolean discretionary
    ) {
    }

    public record BudgetMixItemResponse(
            String bucket,
            BigDecimal sum,
            BigDecimal monthlyAverage,
            BigDecimal incomeShare,
            String note
    ) {
    }

    public record SavingsPlanResponse(
            BigDecimal currentMonthlySpend,
            BigDecimal currentMonthlyIncome,
            BigDecimal coreMonthlyCost,
            BigDecimal targetMonthlySpend,
            BigDecimal aggressiveMonthlySpend,
            BigDecimal targetInvestmentTransfer,
            BigDecimal aggressiveInvestmentTransfer,
            BigDecimal monthlyCutNeeded,
            BigDecimal emergencyFundMin,
            BigDecimal emergencyFundComfort,
            List<CategoryLimitResponse> categoryLimits
    ) {
    }

    public record CategoryLimitResponse(
            String category,
            String bucket,
            BigDecimal currentMonthly,
            BigDecimal limit,
            BigDecimal potentialMonthly,
            BigDecimal potentialYearly,
            String priority,
            String action
    ) {
    }

    public record MonthControlResponse(
            String month,
            String monthKey,
            int elapsedDays,
            int remainingDays,
            int daysInMonth,
            BigDecimal incomeToDate,
            BigDecimal spendToDate,
            BigDecimal projectedSpend,
            BigDecimal targetSpend,
            BigDecimal remainingBudget,
            BigDecimal dailyAllowed,
            BigDecimal projectedDelta,
            List<CategoryStatusResponse> categoryStatus,
            List<AlertResponse> alerts,
            List<SinkingFundResponse> sinkingFunds
    ) {
    }

    public record CategoryStatusResponse(
            String category,
            String bucket,
            BigDecimal currentMonthly,
            BigDecimal limit,
            BigDecimal potentialMonthly,
            BigDecimal potentialYearly,
            String priority,
            String action,
            BigDecimal currentMonthSpend,
            BigDecimal currentMonthProjection,
            BigDecimal remainingThisMonth,
            BigDecimal projectedDelta,
            BigDecimal usage
    ) {
    }

    public record AlertResponse(String type, String severity, String message) {
    }

    public record SinkingFundResponse(
            String name,
            String category,
            BigDecimal monthlySetAside,
            BigDecimal yearlyNeed,
            String note
    ) {
    }

    public record FixednessSummaryResponse(
            String type,
            BigDecimal spend,
            BigDecimal excludedOutgoing,
            BigDecimal monthlyAverage,
            int count
    ) {
    }

    public record MerchantSummaryResponse(
            String merchant,
            String category,
            BigDecimal sum,
            int count,
            BigDecimal average
    ) {
    }

    public record RecurringItemResponse(
            String merchant,
            String category,
            String bucket,
            BigDecimal sum,
            int months,
            int count,
            BigDecimal monthlyAverage,
            int avgDay,
            LocalDate lastDate
    ) {
    }

    public record LargeOneOffResponse(
            LocalDate date,
            String merchant,
            String category,
            String bucket,
            BigDecimal amount,
            String month,
            String confidence,
            String description
    ) {
    }

    public record CalendarResponse(int year, String month, List<CalendarDayResponse> days) {
    }

    public record CalendarDayResponse(
            LocalDate date,
            int day,
            BigDecimal spend,
            BigDecimal income,
            int transactions,
            TransactionResponse biggest
    ) {
    }

    public record AnalyticsResponse(
            int year,
            String scope,
            String month,
            LocalDate date,
            BigDecimal spend,
            BigDecimal income,
            int transactionCount,
            List<AreaSpendResponse> areaTop,
            List<GroupSpendResponse> groupTop,
            List<CategorySpendResponse> categoryTop,
            List<SubcategorySpendResponse> subcategoryTop,
            List<HierarchySpendResponse> hierarchyTop,
            List<AnalyticsMerchantSpendResponse> merchants,
            List<TransactionResponse> oneoffs
    ) {
    }

    public record AreaSpendResponse(String area, BigDecimal spend, int count) {
    }

    public record GroupSpendResponse(String group, BigDecimal spend, int count) {
    }

    public record CategorySpendResponse(String category, BigDecimal spend, int count) {
    }

    public record SubcategorySpendResponse(String subcategory, String category, BigDecimal spend, int count) {
    }

    public record HierarchySpendResponse(String area, String group, String category, String subcategory, BigDecimal spend, int count) {
    }

    public record AnalyticsMerchantSpendResponse(String merchant, BigDecimal sum, int count) {
    }

    public record TransactionPageResponse(
            List<TransactionResponse> items,
            int page,
            int size,
            long totalItems,
            int totalPages,
            String sort
    ) {
    }

    public record TransactionResponse(
            long id,
            int lp,
            LocalDate postedDate,
            String month,
            String merchant,
            String description,
            String account,
            String bankCategory,
            String correctedCategory,
            String area,
            String group,
            String subcategory,
            String bucket,
            String fixedness,
            String type,
            BigDecimal amount,
            BigDecimal income,
            BigDecimal spend,
            BigDecimal discretionary,
            BigDecimal excluded,
            BigDecimal excludedOutgoing,
            BigDecimal excludedIncoming,
            BigDecimal excludedNet,
            String confidence,
            String notes,
            String matchedRule
    ) {
    }

    public record ImportSummaryResponse(
            String status,
            List<Integer> years,
            int transactions,
            BigDecimal income,
            BigDecimal spend,
            String message
    ) {
    }

    public record ImportRunResponse(
            long id,
            Integer year,
            String inputCsv,
            String status,
            String message,
            OffsetDateTime createdAt
    ) {
    }

    public record BudgetSettingsDto(
            BigDecimal targetMonthlySpend,
            BigDecimal aggressiveMonthlySpend,
            int emergencyFundMinMonths,
            int emergencyFundComfortMonths,
            List<CategoryLimitSettingDto> categoryLimits
    ) {
    }

    public record CategoryLimitSettingDto(String category, BigDecimal limit, String action) {
    }
}
