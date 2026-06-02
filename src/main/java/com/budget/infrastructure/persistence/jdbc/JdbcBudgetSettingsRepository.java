package com.budget.infrastructure.persistence.jdbc;

import com.budget.application.settings.BudgetSettings;
import com.budget.application.settings.BudgetSettingsStore;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.Optional;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class JdbcBudgetSettingsRepository implements BudgetSettingsStore {
    private static final String DEFAULT_KEY = "default";

    private final NamedParameterJdbcTemplate jdbc;

    public JdbcBudgetSettingsRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<BudgetSettings> findDefaultSettings() {
        var profiles = jdbc.query("""
                SELECT * FROM budget_settings_profiles WHERE settings_key = :settingsKey
                """, params(), this::mapSettingsWithoutLimits);
        if (profiles.isEmpty()) {
            return Optional.empty();
        }
        var limits = jdbc.query("""
                SELECT * FROM budget_category_limit_settings
                WHERE settings_key = :settingsKey
                ORDER BY limit_scope, limit_name, category
                """, params(), this::mapLimit);
        var profile = profiles.getFirst();
        return Optional.of(new BudgetSettings(
                profile.targetMonthlySpend(),
                profile.aggressiveMonthlySpend(),
                profile.emergencyFundMinMonths(),
                profile.emergencyFundComfortMonths(),
                profile.netIncomeRatio(),
                limits
        ));
    }

    @Override
    @Transactional
    public BudgetSettings saveDefaultSettings(BudgetSettings settings) {
        jdbc.update("DELETE FROM budget_settings_profiles WHERE settings_key = :settingsKey", params());
        jdbc.update("""
                INSERT INTO budget_settings_profiles (
                    settings_key, target_monthly_spend, aggressive_monthly_spend,
                    emergency_fund_min_months, emergency_fund_comfort_months, net_income_ratio, updated_at
                ) VALUES (
                    :settingsKey, :targetMonthlySpend, :aggressiveMonthlySpend,
                    :emergencyFundMinMonths, :emergencyFundComfortMonths, :netIncomeRatio, :updatedAt
                )
                """, params()
                .addValue("targetMonthlySpend", settings.targetMonthlySpend())
                .addValue("aggressiveMonthlySpend", settings.aggressiveMonthlySpend())
                .addValue("emergencyFundMinMonths", settings.emergencyFundMinMonths())
                .addValue("emergencyFundComfortMonths", settings.emergencyFundComfortMonths())
                .addValue("netIncomeRatio", settings.netIncomeRatio())
                .addValue("updatedAt", OffsetDateTime.now()));

        for (var limit : settings.categoryLimits()) {
            jdbc.update("""
                    INSERT INTO budget_category_limit_settings (
                        settings_key, category, limit_scope, limit_name, limit_amount, action, budget_bucket_override
                    ) VALUES (
                        :settingsKey, :category, :scope, :name, :limitAmount, :action, :bucketOverride
                    )
                    """, params()
                    .addValue("category", persistenceCategory(limit))
                    .addValue("scope", limit.scope())
                    .addValue("name", limit.displayName())
                    .addValue("limitAmount", limit.limit())
                    .addValue("action", limit.action())
                    .addValue("bucketOverride", limit.bucketOverride()));
        }
        return findDefaultSettings().orElseThrow();
    }

    private SettingsRow mapSettingsWithoutLimits(ResultSet rs, int rowNum) throws SQLException {
        return new SettingsRow(
                rs.getBigDecimal("target_monthly_spend"),
                rs.getBigDecimal("aggressive_monthly_spend"),
                rs.getInt("emergency_fund_min_months"),
                rs.getInt("emergency_fund_comfort_months"),
                rs.getBigDecimal("net_income_ratio")
        );
    }

    private BudgetSettings.CategoryLimitSetting mapLimit(ResultSet rs, int rowNum) throws SQLException {
        return new BudgetSettings.CategoryLimitSetting(
                rs.getString("limit_scope"),
                rs.getString("limit_name"),
                rs.getString("category"),
                rs.getBigDecimal("limit_amount"),
                rs.getString("action"),
                rs.getString("budget_bucket_override")
        );
    }

    private String persistenceCategory(BudgetSettings.CategoryLimitSetting limit) {
        return "category".equals(limit.scope()) ? limit.displayName() : limit.scope() + ":" + limit.displayName();
    }

    private MapSqlParameterSource params() {
        return new MapSqlParameterSource("settingsKey", DEFAULT_KEY);
    }

    private record SettingsRow(
            java.math.BigDecimal targetMonthlySpend,
            java.math.BigDecimal aggressiveMonthlySpend,
            int emergencyFundMinMonths,
            int emergencyFundComfortMonths,
            java.math.BigDecimal netIncomeRatio
    ) {
    }
}
