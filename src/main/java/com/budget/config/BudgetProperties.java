package com.budget.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record BudgetProperties(
        Security security,
        Database database,
        Upload upload,
        LocalImport localImport
) {
    public BudgetProperties {
        if (security == null) security = new Security(false, "");
        if (database == null) database = new Database("");
        if (upload == null) upload = new Upload(12_582_912L);
        if (localImport == null) localImport = new LocalImport(".");
    }

    public record Security(boolean oauthEnabled, String allowedGithubLogin) {
    }

    public record Database(String url) {
    }

    public record Upload(long maxBytes) {
    }

    public record LocalImport(String root) {
    }
}
