package com.budget.domain.report;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record BudgetSnapshot(
        int year,
        String period,
        LocalDate periodStart,
        LocalDate periodEnd,
        int activeMonths,
        Kpis kpis,
        List<MonthlySummary> monthly,
        List<CategorySummary> categories,
        List<HierarchySummary> hierarchy,
        List<BudgetMixItem> budgetMix,
        SavingsPlan savingsPlan,
        MonthControl monthControl,
        List<FixednessSummary> fixedness,
        List<MerchantSummary> topMerchants,
        List<RecurringItem> recurring,
        List<LargeOneOff> largeOneoffs
) {
    public BudgetSnapshot {
        monthly = copy(monthly);
        categories = copy(categories);
        hierarchy = copy(hierarchy);
        budgetMix = copy(budgetMix);
        fixedness = copy(fixedness);
        topMerchants = copy(topMerchants);
        recurring = copy(recurring);
        largeOneoffs = copy(largeOneoffs);
    }

    public record Kpis(
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

    public record MonthlySummary(
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

    public record CategorySummary(
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

    public record HierarchySummary(
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

    public record BudgetMixItem(
            String bucket,
            BigDecimal sum,
            BigDecimal monthlyAverage,
            BigDecimal incomeShare,
            String note
    ) {
    }

    public record SavingsPlan(
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
            List<CategoryLimit> categoryLimits
    ) {
        public SavingsPlan {
            categoryLimits = BudgetSnapshot.copy(categoryLimits);
        }
    }

    public record CategoryLimit(
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

    public record MonthControl(
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
            List<CategoryStatus> categoryStatus,
            List<Alert> alerts,
            List<SinkingFund> sinkingFunds
    ) {
        public MonthControl {
            categoryStatus = BudgetSnapshot.copy(categoryStatus);
            alerts = BudgetSnapshot.copy(alerts);
            sinkingFunds = BudgetSnapshot.copy(sinkingFunds);
        }
    }

    public record CategoryStatus(
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

    public record Alert(String type, String severity, String message) {
    }

    public record SinkingFund(
            String name,
            String category,
            BigDecimal monthlySetAside,
            BigDecimal yearlyNeed,
            String note
    ) {
    }

    public record FixednessSummary(
            String type,
            BigDecimal spend,
            BigDecimal excludedOutgoing,
            BigDecimal monthlyAverage,
            int count
    ) {
    }

    public record MerchantSummary(
            String merchant,
            String category,
            BigDecimal sum,
            int count,
            BigDecimal average
    ) {
    }

    public record RecurringItem(
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

    public record LargeOneOff(
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

    private static <T> List<T> copy(List<T> rows) {
        return rows == null ? List.of() : List.copyOf(rows);
    }
}
