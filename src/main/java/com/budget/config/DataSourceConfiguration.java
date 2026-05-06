package com.budget.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import javax.sql.DataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

@Configuration
public class DataSourceConfiguration {
    @Bean
    DataSource dataSource(BudgetProperties properties) {
        String databaseUrl = properties.database().url();
        HikariConfig config = new HikariConfig();
        config.setMaximumPoolSize(4);
        config.setMinimumIdle(0);
        config.setPoolName("budget-db");

        if (StringUtils.hasText(databaseUrl)) {
            configurePostgres(databaseUrl, config);
        } else {
            config.setJdbcUrl("jdbc:h2:file:./data/budget;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH");
            config.setUsername("sa");
            config.setPassword("");
            config.setDriverClassName("org.h2.Driver");
        }
        return new HikariDataSource(config);
    }

    private void configurePostgres(String databaseUrl, HikariConfig config) {
        URI uri = URI.create(databaseUrl);
        String userInfo = uri.getRawUserInfo();
        String username = "";
        String password = "";
        if (StringUtils.hasText(userInfo)) {
            String[] parts = userInfo.split(":", 2);
            username = decode(parts[0]);
            password = parts.length > 1 ? decode(parts[1]) : "";
        }
        String path = uri.getPath() == null ? "" : uri.getPath();
        String query = StringUtils.hasText(uri.getRawQuery()) ? "?" + uri.getRawQuery() : "?sslmode=require";
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
