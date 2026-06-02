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
            BigDecimal toCheckAmount,
            BigDecimal savingsAccountNetChange,
            BigDecimal savingsAccountGrossDeposits,
            BigDecimal savingsAccountInflows,
            BigDecimal savingsAccountOutflows
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
            int count,
            List<String> merchantExamples
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
            List<CategoryLimitResponse> parentLimits,
            List<CategoryLimitResponse> categoryLimits
    ) {
    }

    public record CategoryLimitResponse(
            String scope,
            String name,
            boolean parent,
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
            List<FinancialFlowResponse> financialFlows,
            List<AnalyticsMerchantSpendResponse> merchants,
            List<TransactionResponse> oneoffs,
            List<MonthlyCategoryTrendResponse> monthlyCategoryTrends,
            List<MonthlyHierarchyTrendResponse> monthlyHierarchyTrends,
            List<MonthlyBucketTrendResponse> monthlyBucketTrends,
            List<MonthlyMerchantTrendResponse> monthlyMerchantTrends,
            List<FixednessBreakdownResponse> fixednessBreakdown,
            List<ConfidenceBreakdownResponse> confidenceBreakdown,
            List<AmountBandResponse> amountBands
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

    public record FinancialFlowResponse(String category, BigDecimal outgoing, int count) {
    }

    public record AnalyticsMerchantSpendResponse(String merchant, BigDecimal sum, int count) {
    }

    public record MonthlyCategoryTrendResponse(String month, String monthKey, String category, BigDecimal spend, int count) {
    }

    public record MonthlyHierarchyTrendResponse(String month, String monthKey, String area, String group, String category, String subcategory, BigDecimal spend, int count) {
    }

    public record MonthlyBucketTrendResponse(String month, String monthKey, String bucket, BigDecimal spend, int count) {
    }

    public record MonthlyMerchantTrendResponse(String month, String monthKey, String merchant, BigDecimal spend, int count) {
    }

    public record FixednessBreakdownResponse(String fixedness, BigDecimal spend, int count) {
    }

    public record ConfidenceBreakdownResponse(String confidence, int count, BigDecimal spend, BigDecimal income, BigDecimal excluded) {
    }

    public record AmountBandResponse(String label, BigDecimal minAmount, BigDecimal maxAmount, int count, BigDecimal spend) {
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
            String categoryId,
            String correctedCategory,
            String subcategoryId,
            String area,
            String group,
            String subcategory,
            String flowType,
            String budgetGroupId,
            String budgetGroup,
            String reviewStatus,
            String reviewReason,
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
            int duplicatesRemoved,
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
            int duplicatesRemoved,
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

    public record CategoryLimitSettingDto(String scope, String name, String category, BigDecimal limit, String action, String bucketOverride) {
    }

    public record NetWorthResponse(
            List<NetWorthAccountResponse> accounts,
            List<LiabilityResponse> liabilities,
            BigDecimal liquidTotal,
            BigDecimal investedAssets,
            BigDecimal totalAssets,
            BigDecimal totalLiabilities,
            BigDecimal netWorth,
            BigDecimal emergencyFundMin,
            BigDecimal emergencyFundComfort,
            BigDecimal emergencyProgressComfort
    ) {
    }

    public record NetWorthAccountResponse(
            String accountKey,
            String name,
            String kind,
            boolean liquid,
            boolean excludeFromNetWorth,
            boolean configured,
            BigDecimal anchorBalance,
            LocalDate anchorDate,
            BigDecimal netFlowSinceAnchor,
            BigDecimal derivedBalance
    ) {
    }

    public record AccountUpsertRequest(
            String name,
            String kind,
            boolean liquid,
            boolean excludeFromNetWorth,
            BigDecimal anchorBalance,
            LocalDate anchorDate
    ) {
    }

    public record LiabilityResponse(
            String liabilityKey,
            String name,
            String kind,
            BigDecimal currentPrincipal,
            BigDecimal annualInterestRate,
            BigDecimal monthlyPayment,
            LocalDate asOf
    ) {
    }

    public record LiabilityUpsertRequest(
            String name,
            String kind,
            BigDecimal currentPrincipal,
            BigDecimal annualInterestRate,
            BigDecimal monthlyPayment,
            LocalDate asOf
    ) {
    }

    public record FireSummaryResponse(
            LocalDate asOf,
            boolean reportsLoaded,
            String reportsPath,
            int sourceCount,
            int positionCount,
            int currentAge,
            int targetAge,
            int yearsToFire,
            BigDecimal currentPortfolioValue,
            BigDecimal costBasis,
            BigDecimal unrealizedGain,
            BigDecimal emergencyFundValue,
            BigDecimal retirementLockedValue,
            BigDecimal liquidFireCapital,
            BigDecimal bridgeableLiquidCapital,
            BigDecimal emergencyReserveTarget,
            BigDecimal annualSpendTarget,
            BigDecimal monthlySpendTarget,
            boolean spendTargetConfigured,
            BigDecimal safeWithdrawalRate,
            BigDecimal fireNumber,
            BigDecimal gapToFireNumber,
            BigDecimal bridgeCapitalToAge60,
            BigDecimal bridgeCapitalToAge65,
            BigDecimal liquidBridgeGapToAge60,
            BigDecimal liquidBridgeGapToAge65,
            BigDecimal taxableCapitalValue,
            BigDecimal taxableUnrealizedGain,
            BigDecimal estimatedCapitalGainsTax,
            BigDecimal currentMonthlyWealthContribution,
            FireBudgetLinkResponse budgetLink,
            FireContributionPlanResponse contributionPlan,
            FireWithdrawalPlanResponse withdrawalPlan,
            FireDataQualityResponse dataQuality,
            List<FireScenarioResponse> scenarios,
            List<FireAllocationResponse> allocation,
            List<FireWrapperResponse> wrappers,
            List<FirePortfolioBreakdownResponse> portfolios,
            List<FireRebalanceActionResponse> rebalancing,
            List<FireRiskResponse> risks,
            List<FireActionItemResponse> actionItems,
            List<FirePositionAnalysisResponse> positionAnalyses,
            List<FireMilestoneResponse> milestones,
            List<FireLegalRuleResponse> legalRules,
            List<FireSourceResponse> sources
    ) {
    }

    public record FireScenarioResponse(
            String id,
            String label,
            BigDecimal realReturn,
            BigDecimal projectedAtFire,
            BigDecimal gapAtFire,
            BigDecimal requiredMonthlyContribution,
            BigDecimal currentPlanMonthlyContribution,
            boolean onTrack
    ) {
    }

    public record FireAllocationResponse(
            String assetClass,
            BigDecimal value,
            BigDecimal share,
            BigDecimal targetShare,
            BigDecimal drift,
            String status
    ) {
    }

    public record FireWrapperResponse(
            String wrapper,
            BigDecimal value,
            BigDecimal share,
            int positions,
            String liquidity
    ) {
    }

    public record FirePortfolioBreakdownResponse(
            String portfolio,
            BigDecimal value,
            BigDecimal share,
            BigDecimal investmentValue,
            BigDecimal emergencyValue,
            BigDecimal retirementLockedValue,
            BigDecimal taxableValue,
            int positions,
            String role,
            String note
    ) {
    }

    public record FireRebalanceActionResponse(
            String assetClass,
            BigDecimal currentShare,
            BigDecimal targetShare,
            BigDecimal drift,
            BigDecimal amountToTarget,
            String action,
            String priority
    ) {
    }

    public record FireRiskResponse(
            String id,
            String level,
            String area,
            String title,
            String metric,
            String value,
            String threshold,
            String detail,
            String recommendation
    ) {
    }

    public record FireContributionPlanResponse(
            BigDecimal currentMonthly,
            BigDecimal requiredMonthlyBase,
            BigDecimal additionalMonthlyNeeded,
            BigDecimal annualIkeCapacityForHousehold,
            BigDecimal annualIkzeCapacityForHousehold,
            BigDecimal monthlyRetirementWrapperCapacity,
            String recommendation
    ) {
    }

    public record FireBudgetLinkResponse(
            boolean linked,
            int budgetYear,
            int activeMonths,
            BigDecimal monthlyIncome,
            BigDecimal currentMonthlyLivingSpend,
            BigDecimal targetMonthlySpend,
            BigDecimal actualMonthlyInvestments,
            BigDecimal savingsAccountMonthlyNet,
            BigDecimal savingsAccountMonthlyGrossDeposits,
            BigDecimal loanOverpaymentMonthly,
            BigDecimal firePortfolioMonthlyContribution,
            BigDecimal targetInvestableSurplus,
            BigDecimal unassignedSurplusMonthly,
            BigDecimal emergencyReserveTarget,
            boolean spendOverrideUsed,
            boolean contributionOverrideUsed,
            String note
    ) {
    }

    public record FireWithdrawalPlanResponse(
            BigDecimal monthlyTarget,
            BigDecimal annualTarget,
            BigDecimal liquidCapital,
            BigDecimal yearsCoveredByLiquidCapital,
            BigDecimal bridgeNeedToAge60,
            BigDecimal bridgeNeedToAge65,
            BigDecimal estimatedTaxReserve,
            String sequence
    ) {
    }

    public record FireDataQualityResponse(
            LocalDate newestReportDate,
            int sourceCount,
            int positionCount,
            int staleSourceCount,
            int unknownAssetClassCount,
            BigDecimal unknownAssetClassValue,
            int unknownWrapperCount,
            BigDecimal unknownWrapperValue,
            String status,
            String note
    ) {
    }

    public record FireActionItemResponse(
            String priority,
            String type,
            String title,
            String detail,
            BigDecimal amount
    ) {
    }

    public record FirePositionAnalysisResponse(
            String instrument,
            String isin,
            String portfolio,
            String assetClass,
            String instrumentType,
            String fireRole,
            String wrapper,
            String account,
            String currency,
            LocalDate priceDate,
            BigDecimal value,
            BigDecimal costBasis,
            BigDecimal unrealizedGain,
            BigDecimal returnPct,
            BigDecimal shareOfPortfolio,
            BigDecimal shareOfInvestments,
            String riskLevel,
            String reviewFocus,
            String decision,
            String decisionReason,
            String action,
            String perspective,
            List<String> riskDrivers,
            List<String> checklist
    ) {
    }

    public record FireMilestoneResponse(
            int age,
            String label,
            String description,
            BigDecimal requiredCapital
    ) {
    }

    public record FireLegalRuleResponse(
            String id,
            String label,
            String value,
            String note,
            String sourceUrl
    ) {
    }

    public record FireSourceResponse(
            String fileName,
            String portfolio,
            LocalDate asOf,
            int positions,
            BigDecimal value
    ) {
    }

    public record FireSettingsDto(
            String reportsPath,
            int currentAge,
            int targetAge,
            BigDecimal monthlySpendOverride,
            BigDecimal monthlyContributionOverride,
            BigDecimal safeWithdrawalRate,
            BigDecimal pessimisticRealReturn,
            BigDecimal expectedRealReturn,
            BigDecimal optimisticRealReturn,
            BigDecimal targetEquityShare,
            BigDecimal targetBondShare,
            BigDecimal targetCashShare,
            BigDecimal targetAlternativeShare,
            BigDecimal rebalanceBand
    ) {
    }
}
