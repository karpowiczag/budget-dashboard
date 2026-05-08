package com.budget.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app")
public record BudgetProperties(
        @Valid
        Security security,
        @Valid
        Database database,
        @Valid
        Upload upload,
        @Valid
        LocalImport localImport,
        @Valid
        BudgetSettings budgetSettings,
        @Valid
        Fire fire,
        @Valid
        Categorization categorization
) {
    public BudgetProperties {
        if (security == null) security = new Security(false, "");
        if (database == null) database = new Database("");
        if (upload == null) upload = new Upload(12_582_912L);
        if (localImport == null) localImport = new LocalImport(".", true);
        if (budgetSettings == null) budgetSettings = new BudgetSettings(BigDecimal.valueOf(14_000), BigDecimal.valueOf(13_000), 3, 6);
        if (fire == null) fire = new Fire(
                "fire/investments_reports",
                36,
                50,
                null,
                null,
                BigDecimal.valueOf(0.035),
                BigDecimal.valueOf(0.02),
                BigDecimal.valueOf(0.04),
                BigDecimal.valueOf(0.055),
                BigDecimal.valueOf(0.80),
                BigDecimal.valueOf(0.10),
                BigDecimal.valueOf(0.05),
                BigDecimal.valueOf(0.05),
                BigDecimal.valueOf(0.05)
        );
        if (categorization == null) categorization = new Categorization(List.of());
    }

    public record Security(boolean oauthEnabled, String allowedGoogleEmail) {
        public Security {
            if (allowedGoogleEmail == null) allowedGoogleEmail = "";
        }
    }

    public record Database(String url) {
        public Database {
            if (url == null) url = "";
        }
    }

    public record Upload(@Positive long maxBytes) {
    }

    public record LocalImport(String root, boolean rebuildEnabled) {
        public LocalImport {
            if (root == null || root.isBlank()) root = ".";
        }
    }

    public record BudgetSettings(
            @Positive BigDecimal targetMonthlySpend,
            @Positive BigDecimal aggressiveMonthlySpend,
            @Positive int emergencyFundMinMonths,
            @Positive int emergencyFundComfortMonths
    ) {
        public BudgetSettings {
            if (targetMonthlySpend == null) targetMonthlySpend = BigDecimal.valueOf(14_000);
            if (aggressiveMonthlySpend == null) aggressiveMonthlySpend = BigDecimal.valueOf(13_000);
        }
    }

    public record Fire(
            String reportsPath,
            @Positive int currentAge,
            @Positive int targetAge,
            BigDecimal monthlySpendOverride,
            BigDecimal monthlyContributionOverride,
            @Positive BigDecimal safeWithdrawalRate,
            BigDecimal pessimisticRealReturn,
            BigDecimal expectedRealReturn,
            BigDecimal optimisticRealReturn,
            BigDecimal targetEquityShare,
            BigDecimal targetBondShare,
            BigDecimal targetCashShare,
            BigDecimal targetAlternativeShare,
            @Positive BigDecimal rebalanceBand
    ) {
        public Fire {
            if (reportsPath == null || reportsPath.isBlank()) reportsPath = "fire/investments_reports";
            if (targetAge <= currentAge) targetAge = currentAge + 1;
            if (monthlySpendOverride != null && monthlySpendOverride.signum() <= 0) monthlySpendOverride = null;
            if (monthlyContributionOverride != null && monthlyContributionOverride.signum() < 0) monthlyContributionOverride = null;
            if (safeWithdrawalRate == null) safeWithdrawalRate = BigDecimal.valueOf(0.035);
            if (pessimisticRealReturn == null) pessimisticRealReturn = BigDecimal.valueOf(0.02);
            if (expectedRealReturn == null) expectedRealReturn = BigDecimal.valueOf(0.04);
            if (optimisticRealReturn == null) optimisticRealReturn = BigDecimal.valueOf(0.055);
            if (targetEquityShare == null) targetEquityShare = BigDecimal.valueOf(0.80);
            if (targetBondShare == null) targetBondShare = BigDecimal.valueOf(0.10);
            if (targetCashShare == null) targetCashShare = BigDecimal.valueOf(0.05);
            if (targetAlternativeShare == null) targetAlternativeShare = BigDecimal.valueOf(0.05);
            if (rebalanceBand == null) rebalanceBand = BigDecimal.valueOf(0.05);
        }
    }

    public record Categorization(List<Rule> personalRules) {
        public Categorization {
            personalRules = personalRules == null ? List.of() : List.copyOf(personalRules);
        }
    }

    public record Rule(String pattern, String category) {
        public Rule {
            if (pattern == null) pattern = "";
            if (category == null) category = "";
        }
    }
}
