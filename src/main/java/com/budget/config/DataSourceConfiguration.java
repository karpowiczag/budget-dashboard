package com.budget.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import javax.sql.DataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

@Configuration(proxyBeanMethods = false)
public class DataSourceConfiguration {
    @Bean
    DataSource dataSource(BudgetProperties properties, Environment environment) {
        var databaseUrl = properties.database().url();
        var config = new HikariConfig();
        config.setMaximumPoolSize(4);
        config.setMinimumIdle(0);
        config.setPoolName("budget-db");

        if (isProd(environment)) {
            requireProductionPostgresUrl(databaseUrl);
        }

        if (StringUtils.hasText(databaseUrl) && databaseUrl.startsWith("jdbc:")) {
            configureJdbc(databaseUrl, config);
        } else if (StringUtils.hasText(databaseUrl)) {
            configurePostgres(databaseUrl, config);
        } else if (properties.database().allowLocalH2()) {
            config.setJdbcUrl("jdbc:h2:file:./data/budget;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH");
            config.setUsername("sa");
            config.setPassword("");
            config.setDriverClassName("org.h2.Driver");
        } else {
            throw new IllegalStateException(
                    "DATABASE_URL must be configured. Use PostgreSQL locally via compose, or set APP_ALLOW_LOCAL_H2=true only for disposable development runs."
            );
        }
        return new HikariDataSource(config);
    }

    private boolean isProd(Environment environment) {
        return java.util.Arrays.asList(environment.getActiveProfiles()).contains("prod");
    }

    private void requireProductionPostgresUrl(String databaseUrl) {
        if (!StringUtils.hasText(databaseUrl)) {
            throw new IllegalStateException("DATABASE_URL must be configured when the prod profile is active");
        }
        if (!isProductionPostgresUrl(databaseUrl)) {
            throw new IllegalStateException("DATABASE_URL must point to PostgreSQL when the prod profile is active");
        }
    }

    static boolean isProductionPostgresUrl(String databaseUrl) {
        return StringUtils.hasText(databaseUrl)
                && (databaseUrl.startsWith("jdbc:postgresql:")
                || databaseUrl.startsWith("postgres://")
                || databaseUrl.startsWith("postgresql://"));
    }

    private void configureJdbc(String jdbcUrl, HikariConfig config) {
        config.setJdbcUrl(jdbcUrl);
        if (jdbcUrl.startsWith("jdbc:h2:")) {
            config.setUsername("sa");
            config.setPassword("");
            config.setDriverClassName("org.h2.Driver");
        } else if (jdbcUrl.startsWith("jdbc:postgresql:")) {
            config.setDriverClassName("org.postgresql.Driver");
        } else {
            throw new IllegalArgumentException("Unsupported JDBC URL; use jdbc:postgresql or local jdbc:h2");
        }
    }

    private void configurePostgres(String databaseUrl, HikariConfig config) {
        var uri = URI.create(databaseUrl);
        var userInfo = uri.getRawUserInfo();
        var username = "";
        var password = "";
        if (StringUtils.hasText(userInfo)) {
            var parts = userInfo.split(":", 2);
            username = decode(parts[0]);
            password = parts.length > 1 ? decode(parts[1]) : "";
        }
        var path = uri.getPath() == null ? "" : uri.getPath();
        var query = StringUtils.hasText(uri.getRawQuery()) ? "?" + uri.getRawQuery() : "?sslmode=require";
        if (!query.contains("sslmode=")) {
            query = query + "&sslmode=require";
        }
        config.setJdbcUrl("jdbc:postgresql://" + uri.getHost() + ":" + effectivePort(uri) + path + query);
        config.setUsername(username);
        config.setPassword(password);
        config.setDriverClassName("org.postgresql.Driver");
    }

    private int effectivePort(URI uri) {
        return uri.getPort() > 0 ? uri.getPort() : 5432;
    }

    private String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }
}
