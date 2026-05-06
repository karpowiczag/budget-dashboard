package com.budget.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
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
        LocalImport localImport
) {
    public BudgetProperties {
        if (security == null) security = new Security(false, "");
        if (database == null) database = new Database("");
        if (upload == null) upload = new Upload(12_582_912L);
        if (localImport == null) localImport = new LocalImport(".");
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

    public record LocalImport(String root) {
        public LocalImport {
            if (root == null || root.isBlank()) root = ".";
        }
    }
}
