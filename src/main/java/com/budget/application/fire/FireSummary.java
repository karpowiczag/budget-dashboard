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
        FireBudgetLink budgetLink,
        FireContributionPlan contributionPlan,
        FireWithdrawalPlan withdrawalPlan,
        FireDataQuality dataQuality,
        List<FireScenario> scenarios,
        List<FireAllocation> allocation,
        List<FireWrapper> wrappers,
        List<FirePortfolioBreakdown> portfolios,
        List<FireRebalanceAction> rebalancing,
        List<FireRisk> risks,
        List<FireActionItem> actionItems,
        List<FirePositionAnalysis> positionAnalyses,
        List<FireMilestone> milestones,
        List<FireLegalRule> legalRules,
        List<FireSource> sources
) {
    public FireSummary {
        if (budgetLink == null) budgetLink = FireBudgetLink.empty();
        if (contributionPlan == null) contributionPlan = FireContributionPlan.empty();
        if (withdrawalPlan == null) withdrawalPlan = FireWithdrawalPlan.empty();
        if (dataQuality == null) dataQuality = FireDataQuality.empty();
        scenarios = scenarios == null ? List.of() : List.copyOf(scenarios);
        allocation = allocation == null ? List.of() : List.copyOf(allocation);
        wrappers = wrappers == null ? List.of() : List.copyOf(wrappers);
        portfolios = portfolios == null ? List.of() : List.copyOf(portfolios);
        rebalancing = rebalancing == null ? List.of() : List.copyOf(rebalancing);
        risks = risks == null ? List.of() : List.copyOf(risks);
        actionItems = actionItems == null ? List.of() : List.copyOf(actionItems);
        positionAnalyses = positionAnalyses == null ? List.of() : List.copyOf(positionAnalyses);
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

    public record FirePortfolioBreakdown(
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

    public record FireRisk(
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

    public record FireBudgetLink(
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
        static FireBudgetLink empty() {
            return new FireBudgetLink(
                    false,
                    0,
                    0,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    false,
                    false,
                    "Brak odbudowanego budżetu domowego; FIRE używa tylko jawnych ustawień FIRE i realnych przepływów po imporcie."
            );
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

    public record FirePositionAnalysis(
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
        public FirePositionAnalysis {
            riskDrivers = riskDrivers == null ? List.of() : List.copyOf(riskDrivers);
            checklist = checklist == null ? List.of() : List.copyOf(checklist);
        }
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
