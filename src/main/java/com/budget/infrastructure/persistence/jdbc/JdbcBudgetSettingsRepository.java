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
                ORDER BY category
                """, params(), this::mapLimit);
        var profile = profiles.getFirst();
        return Optional.of(new BudgetSettings(
                profile.targetMonthlySpend(),
                profile.aggressiveMonthlySpend(),
                profile.emergencyFundMinMonths(),
                profile.emergencyFundComfortMonths(),
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
                    emergency_fund_min_months, emergency_fund_comfort_months, updated_at
                ) VALUES (
                    :settingsKey, :targetMonthlySpend, :aggressiveMonthlySpend,
                    :emergencyFundMinMonths, :emergencyFundComfortMonths, :updatedAt
                )
                """, params()
                .addValue("targetMonthlySpend", settings.targetMonthlySpend())
                .addValue("aggressiveMonthlySpend", settings.aggressiveMonthlySpend())
                .addValue("emergencyFundMinMonths", settings.emergencyFundMinMonths())
                .addValue("emergencyFundComfortMonths", settings.emergencyFundComfortMonths())
                .addValue("updatedAt", OffsetDateTime.now()));

        for (var limit : settings.categoryLimits()) {
            jdbc.update("""
                    INSERT INTO budget_category_limit_settings (
                        settings_key, category, limit_amount, action
                    ) VALUES (
                        :settingsKey, :category, :limitAmount, :action
                    )
                    """, params()
                    .addValue("category", limit.category())
                    .addValue("limitAmount", limit.limit())
                    .addValue("action", limit.action()));
        }
        return findDefaultSettings().orElseThrow();
    }

    private SettingsRow mapSettingsWithoutLimits(ResultSet rs, int rowNum) throws SQLException {
        return new SettingsRow(
                rs.getBigDecimal("target_monthly_spend"),
                rs.getBigDecimal("aggressive_monthly_spend"),
                rs.getInt("emergency_fund_min_months"),
                rs.getInt("emergency_fund_comfort_months")
        );
    }

    private BudgetSettings.CategoryLimitSetting mapLimit(ResultSet rs, int rowNum) throws SQLException {
        return new BudgetSettings.CategoryLimitSetting(
                rs.getString("category"),
                rs.getBigDecimal("limit_amount"),
                rs.getString("action")
        );
    }

    private MapSqlParameterSource params() {
        return new MapSqlParameterSource("settingsKey", DEFAULT_KEY);
    }

    private record SettingsRow(
            java.math.BigDecimal targetMonthlySpend,
            java.math.BigDecimal aggressiveMonthlySpend,
            int emergencyFundMinMonths,
            int emergencyFundComfortMonths
    ) {
    }
}
