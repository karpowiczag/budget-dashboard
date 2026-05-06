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
        Categorization categorization
) {
    public BudgetProperties {
        if (security == null) security = new Security(false, "");
        if (database == null) database = new Database("");
        if (upload == null) upload = new Upload(12_582_912L);
        if (localImport == null) localImport = new LocalImport(".", true);
        if (budgetSettings == null) budgetSettings = new BudgetSettings(BigDecimal.valueOf(14_000), BigDecimal.valueOf(13_000), 3, 6);
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
