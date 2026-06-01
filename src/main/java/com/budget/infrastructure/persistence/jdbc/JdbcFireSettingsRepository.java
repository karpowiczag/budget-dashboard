package com.budget.infrastructure.persistence.jdbc;

import com.budget.application.fire.FireSettings;
import com.budget.application.fire.FireSettingsStore;
import java.nio.file.Path;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.Optional;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class JdbcFireSettingsRepository implements FireSettingsStore {
    private static final String DEFAULT_KEY = "default";

    private final NamedParameterJdbcTemplate jdbc;

    public JdbcFireSettingsRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<FireSettings> findDefaultSettings() {
        var rows = jdbc.query("""
                SELECT * FROM fire_settings_profiles WHERE settings_key = :settingsKey
                """, params(), this::map);
        return rows.stream().findFirst();
    }

    @Override
    @Transactional
    public FireSettings saveDefaultSettings(FireSettings settings) {
        jdbc.update("DELETE FROM fire_settings_profiles WHERE settings_key = :settingsKey", params());
        jdbc.update("""
                INSERT INTO fire_settings_profiles (
                    settings_key, reports_path, current_age, target_age,
                    monthly_spend_override, monthly_contribution_override,
                    safe_withdrawal_rate, pessimistic_real_return, expected_real_return, optimistic_real_return,
                    target_equity_share, target_bond_share, target_cash_share, target_alternative_share,
                    rebalance_band, updated_at
                ) VALUES (
                    :settingsKey, :reportsPath, :currentAge, :targetAge,
                    :monthlySpendOverride, :monthlyContributionOverride,
                    :safeWithdrawalRate, :pessimisticRealReturn, :expectedRealReturn, :optimisticRealReturn,
                    :targetEquityShare, :targetBondShare, :targetCashShare, :targetAlternativeShare,
                    :rebalanceBand, :updatedAt
                )
                """, params()
                .addValue("reportsPath", settings.reportsPath().toString())
                .addValue("currentAge", settings.currentAge())
                .addValue("targetAge", settings.targetAge())
                .addValue("monthlySpendOverride", settings.monthlySpendOverride())
                .addValue("monthlyContributionOverride", settings.monthlyContributionOverride())
                .addValue("safeWithdrawalRate", settings.safeWithdrawalRate())
                .addValue("pessimisticRealReturn", settings.pessimisticRealReturn())
                .addValue("expectedRealReturn", settings.expectedRealReturn())
                .addValue("optimisticRealReturn", settings.optimisticRealReturn())
                .addValue("targetEquityShare", settings.targetEquityShare())
                .addValue("targetBondShare", settings.targetBondShare())
                .addValue("targetCashShare", settings.targetCashShare())
                .addValue("targetAlternativeShare", settings.targetAlternativeShare())
                .addValue("rebalanceBand", settings.rebalanceBand())
                .addValue("updatedAt", OffsetDateTime.now()));
        return findDefaultSettings().orElseThrow();
    }

    private FireSettings map(ResultSet rs, int rowNum) throws SQLException {
        return new FireSettings(
                Path.of(rs.getString("reports_path")),
                rs.getInt("current_age"),
                rs.getInt("target_age"),
                rs.getBigDecimal("monthly_spend_override"),
                rs.getBigDecimal("monthly_contribution_override"),
                rs.getBigDecimal("safe_withdrawal_rate"),
                rs.getBigDecimal("pessimistic_real_return"),
                rs.getBigDecimal("expected_real_return"),
                rs.getBigDecimal("optimistic_real_return"),
                rs.getBigDecimal("target_equity_share"),
                rs.getBigDecimal("target_bond_share"),
                rs.getBigDecimal("target_cash_share"),
                rs.getBigDecimal("target_alternative_share"),
                rs.getBigDecimal("rebalance_band")
        );
    }

    private MapSqlParameterSource params() {
        return new MapSqlParameterSource("settingsKey", DEFAULT_KEY);
    }
}
