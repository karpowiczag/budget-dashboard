package com.budget.application.fire;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record FireSummary(
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
        BigDecimal annualSpendTarget,
        BigDecimal monthlySpendTarget,
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
        FireContributionPlan contributionPlan,
        FireWithdrawalPlan withdrawalPlan,
        FireDataQuality dataQuality,
        List<FireScenario> scenarios,
        List<FireAllocation> allocation,
        List<FireWrapper> wrappers,
        List<FireRebalanceAction> rebalancing,
        List<FireActionItem> actionItems,
        List<FireMilestone> milestones,
        List<FireLegalRule> legalRules,
        List<FireSource> sources
) {
    public FireSummary {
        if (contributionPlan == null) contributionPlan = FireContributionPlan.empty();
        if (withdrawalPlan == null) withdrawalPlan = FireWithdrawalPlan.empty();
        if (dataQuality == null) dataQuality = FireDataQuality.empty();
        scenarios = scenarios == null ? List.of() : List.copyOf(scenarios);
        allocation = allocation == null ? List.of() : List.copyOf(allocation);
        wrappers = wrappers == null ? List.of() : List.copyOf(wrappers);
        rebalancing = rebalancing == null ? List.of() : List.copyOf(rebalancing);
        actionItems = actionItems == null ? List.of() : List.copyOf(actionItems);
        milestones = milestones == null ? List.of() : List.copyOf(milestones);
        legalRules = legalRules == null ? List.of() : List.copyOf(legalRules);
        sources = sources == null ? List.of() : List.copyOf(sources);
    }

    public record FireScenario(
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

    public record FireAllocation(
            String assetClass,
            BigDecimal value,
            BigDecimal share,
            BigDecimal targetShare,
            BigDecimal drift,
            String status
    ) {
    }

    public record FireWrapper(
            String wrapper,
            BigDecimal value,
            BigDecimal share,
            int positions,
            String liquidity
    ) {
    }

    public record FireRebalanceAction(
            String assetClass,
            BigDecimal currentShare,
            BigDecimal targetShare,
            BigDecimal drift,
            BigDecimal amountToTarget,
            String action,
            String priority
    ) {
    }

    public record FireContributionPlan(
            BigDecimal currentMonthly,
            BigDecimal requiredMonthlyBase,
            BigDecimal additionalMonthlyNeeded,
            BigDecimal annualIkeCapacityForHousehold,
            BigDecimal annualIkzeCapacityForHousehold,
            BigDecimal monthlyRetirementWrapperCapacity,
            String recommendation
    ) {
        static FireContributionPlan empty() {
            return new FireContributionPlan(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, "");
        }
    }

    public record FireWithdrawalPlan(
            BigDecimal monthlyTarget,
            BigDecimal annualTarget,
            BigDecimal liquidCapital,
            BigDecimal yearsCoveredByLiquidCapital,
            BigDecimal bridgeNeedToAge60,
            BigDecimal bridgeNeedToAge65,
            BigDecimal estimatedTaxReserve,
            String sequence
    ) {
        static FireWithdrawalPlan empty() {
            return new FireWithdrawalPlan(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, "");
        }
    }

    public record FireDataQuality(
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
        static FireDataQuality empty() {
            return new FireDataQuality(null, 0, 0, 0, 0, BigDecimal.ZERO, 0, BigDecimal.ZERO, "missing", "");
        }
    }

    public record FireActionItem(
            String priority,
            String type,
            String title,
            String detail,
            BigDecimal amount
    ) {
    }

    public record FireMilestone(
            int age,
            String label,
            String description,
            BigDecimal requiredCapital
    ) {
    }

    public record FireLegalRule(
            String id,
            String label,
            String value,
            String note,
            String sourceUrl
    ) {
    }

    public record FireSource(
            String fileName,
            String portfolio,
            LocalDate asOf,
            int positions,
            BigDecimal value
    ) {
    }
}
