package com.budget.config;

import java.util.Arrays;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
final class ProductionSettingsValidator implements SmartInitializingSingleton {
    private final Environment environment;

    ProductionSettingsValidator(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void afterSingletonsInstantiated() {
        if (!Arrays.asList(environment.getActiveProfiles()).contains("prod")) {
            return;
        }
        require(environment, "app.database.url", "DATABASE_URL");
        require(environment, "app.security.allowed-google-email", "APP_ALLOWED_GOOGLE_EMAIL");
        require(environment, "spring.security.oauth2.client.registration.google.client-id", "GOOGLE_CLIENT_ID");
        require(environment, "spring.security.oauth2.client.registration.google.client-secret", "GOOGLE_CLIENT_SECRET");
    }

    private void require(Environment environment, String property, String envName) {
        var value = environment.getProperty(property);
        if (!StringUtils.hasText(value) || value.startsWith("${")) {
            throw new IllegalStateException(envName + " must be configured when the prod profile is active");
        }
    }
}
