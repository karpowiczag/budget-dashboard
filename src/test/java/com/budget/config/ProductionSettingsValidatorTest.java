package com.budget.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class ProductionSettingsValidatorTest {
    @Test
    void rejectsBlankRequiredProductionSettings() {
        var environment = new MockEnvironment();
        environment.setActiveProfiles("prod");

        assertThatThrownBy(() -> new ProductionSettingsValidator(environment).afterSingletonsInstantiated())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("DATABASE_URL");
    }

    @Test
    void dataSourceRejectsBlankDatabaseUrlInProduction() {
        var environment = new MockEnvironment();
        environment.setActiveProfiles("prod");
        var properties = new BudgetProperties(null, new BudgetProperties.Database(""), null, null, null, null);

        assertThatThrownBy(() -> new DataSourceConfiguration().dataSource(properties, environment))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("DATABASE_URL");
    }

    @Test
    void dataSourceRejectsH2DatabaseUrlInProduction() {
        var environment = new MockEnvironment();
        environment.setActiveProfiles("prod");
        var properties = new BudgetProperties(
                null,
                new BudgetProperties.Database("jdbc:h2:mem:prod_is_not_allowed"),
                null,
                null,
                null,
                null
        );

        assertThatThrownBy(() -> new DataSourceConfiguration().dataSource(properties, environment))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("PostgreSQL");
    }

    @Test
    void dataSourceAcceptsPostgresDatabaseUrlShapesInProduction() {
        assertThat(DataSourceConfiguration.isProductionPostgresUrl("jdbc:postgresql://localhost:5432/budget")).isTrue();
        assertThat(DataSourceConfiguration.isProductionPostgresUrl("postgres://user:pass@example.test:5432/budget")).isTrue();
        assertThat(DataSourceConfiguration.isProductionPostgresUrl("postgresql://user:pass@example.test:5432/budget")).isTrue();
    }

    @Test
    void acceptsCompleteProductionSettings() {
        var environment = new MockEnvironment()
                .withProperty("app.database.url", "postgres://user:pass@example.test:5432/budget")
                .withProperty("app.security.allowed-google-email", "owner@example.test")
                .withProperty("spring.security.oauth2.client.registration.google.client-id", "client-id")
                .withProperty("spring.security.oauth2.client.registration.google.client-secret", "client-secret");
        environment.setActiveProfiles("prod");

        assertThatCode(() -> new ProductionSettingsValidator(environment).afterSingletonsInstantiated())
                .doesNotThrowAnyException();
    }

    @Test
    void ignoresLocalProfile() {
        var environment = new MockEnvironment();

        assertThatCode(() -> new ProductionSettingsValidator(environment).afterSingletonsInstantiated())
                .doesNotThrowAnyException();
    }
}
