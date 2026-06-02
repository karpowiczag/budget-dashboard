package com.budget.infrastructure.persistence.jdbc;

import com.budget.application.networth.NetWorthStore;
import com.budget.domain.networth.Account;
import com.budget.domain.networth.Liability;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class JdbcNetWorthRepository implements NetWorthStore {
    private final NamedParameterJdbcTemplate jdbc;

    public JdbcNetWorthRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<String> discoverAccountKeys() {
        return jdbc.query("""
                SELECT DISTINCT account FROM budget_transactions
                WHERE account IS NOT NULL AND account <> ''
                ORDER BY account
                """, new MapSqlParameterSource(), (rs, n) -> rs.getString("account"));
    }

    @Override
    public List<Account> accounts() {
        return jdbc.query("SELECT * FROM networth_accounts ORDER BY name", new MapSqlParameterSource(), this::map);
    }

    @Override
    @Transactional
    public Account saveAccount(Account account) {
        jdbc.update("DELETE FROM networth_accounts WHERE account_key = :accountKey",
                new MapSqlParameterSource("accountKey", account.accountKey()));
        jdbc.update("""
                INSERT INTO networth_accounts (
                    account_key, name, kind, liquid, exclude_from_net_worth,
                    anchor_balance, anchor_date, updated_at
                ) VALUES (
                    :accountKey, :name, :kind, :liquid, :excludeFromNetWorth,
                    :anchorBalance, :anchorDate, :updatedAt
                )
                """, new MapSqlParameterSource()
                .addValue("accountKey", account.accountKey())
                .addValue("name", account.name())
                .addValue("kind", account.kind())
                .addValue("liquid", account.liquid())
                .addValue("excludeFromNetWorth", account.excludeFromNetWorth())
                .addValue("anchorBalance", account.anchorBalance())
                .addValue("anchorDate", account.anchorDate())
                .addValue("updatedAt", OffsetDateTime.now()));
        return account;
    }

    @Override
    public BigDecimal netFlowSince(String accountKey, LocalDate sinceExclusive) {
        var sum = jdbc.queryForObject("""
                SELECT COALESCE(SUM(amount), 0) FROM budget_transactions
                WHERE account = :accountKey AND posted_date > :since
                """, new MapSqlParameterSource()
                .addValue("accountKey", accountKey)
                .addValue("since", sinceExclusive), BigDecimal.class);
        return sum == null ? BigDecimal.ZERO : sum;
    }

    @Override
    public List<Liability> liabilities() {
        return jdbc.query("SELECT * FROM networth_liabilities ORDER BY name", new MapSqlParameterSource(), this::mapLiability);
    }

    @Override
    @Transactional
    public Liability saveLiability(Liability liability) {
        jdbc.update("DELETE FROM networth_liabilities WHERE liability_key = :liabilityKey",
                new MapSqlParameterSource("liabilityKey", liability.liabilityKey()));
        jdbc.update("""
                INSERT INTO networth_liabilities (
                    liability_key, name, kind, current_principal,
                    annual_interest_rate, monthly_payment, as_of, updated_at
                ) VALUES (
                    :liabilityKey, :name, :kind, :currentPrincipal,
                    :annualInterestRate, :monthlyPayment, :asOf, :updatedAt
                )
                """, new MapSqlParameterSource()
                .addValue("liabilityKey", liability.liabilityKey())
                .addValue("name", liability.name())
                .addValue("kind", liability.kind())
                .addValue("currentPrincipal", liability.currentPrincipal())
                .addValue("annualInterestRate", liability.annualInterestRate())
                .addValue("monthlyPayment", liability.monthlyPayment())
                .addValue("asOf", liability.asOf())
                .addValue("updatedAt", OffsetDateTime.now()));
        return liability;
    }

    @Override
    @Transactional
    public void deleteLiability(String liabilityKey) {
        jdbc.update("DELETE FROM networth_liabilities WHERE liability_key = :liabilityKey",
                new MapSqlParameterSource("liabilityKey", liabilityKey));
    }

    private Account map(ResultSet rs, int rowNum) throws SQLException {
        return new Account(
                rs.getString("account_key"),
                rs.getString("name"),
                rs.getString("kind"),
                rs.getBoolean("liquid"),
                rs.getBoolean("exclude_from_net_worth"),
                rs.getBigDecimal("anchor_balance"),
                rs.getObject("anchor_date", LocalDate.class)
        );
    }

    private Liability mapLiability(ResultSet rs, int rowNum) throws SQLException {
        return new Liability(
                rs.getString("liability_key"),
                rs.getString("name"),
                rs.getString("kind"),
                rs.getBigDecimal("current_principal"),
                rs.getBigDecimal("annual_interest_rate"),
                rs.getBigDecimal("monthly_payment"),
                rs.getObject("as_of", LocalDate.class)
        );
    }
}
