package com.budget.infrastructure.persistence.jdbc;

import com.budget.application.categorization.TransactionOverrideStore;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * DB-backed {@link TransactionOverrideStore} (Phase 3c-iii). The content-key → category-id map is
 * cached in an immutable snapshot loaded lazily and invalidated on every write, since the
 * normalization hot path reads it for every transaction.
 */
@Repository
public class JdbcTransactionOverrideStore implements TransactionOverrideStore {
    private final NamedParameterJdbcTemplate jdbc;
    private volatile Map<String, String> snapshot;

    public JdbcTransactionOverrideStore(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Map<String, String> overridesByKey() {
        var current = snapshot;
        if (current == null) {
            synchronized (this) {
                current = snapshot;
                if (current == null) {
                    current = load();
                    snapshot = current;
                }
            }
        }
        return current;
    }

    @Override
    @Transactional
    public void setOverride(String contentKey, String categoryId, LocalDate postedDate, String account, BigDecimal amount, String description) {
        jdbc.update("DELETE FROM transaction_override WHERE content_key = :contentKey",
                new MapSqlParameterSource("contentKey", contentKey));
        jdbc.update("""
                INSERT INTO transaction_override (content_key, category_id, posted_date, account, amount, description, updated_at)
                VALUES (:contentKey, :categoryId, :postedDate, :account, :amount, :description, :updatedAt)
                """, new MapSqlParameterSource()
                .addValue("contentKey", contentKey)
                .addValue("categoryId", categoryId)
                .addValue("postedDate", postedDate)
                .addValue("account", account)
                .addValue("amount", amount)
                .addValue("description", description)
                .addValue("updatedAt", OffsetDateTime.now()));
        invalidate();
    }

    @Override
    @Transactional
    public void clearOverride(String contentKey) {
        jdbc.update("DELETE FROM transaction_override WHERE content_key = :contentKey",
                new MapSqlParameterSource("contentKey", contentKey));
        invalidate();
    }

    public void invalidate() {
        snapshot = null;
    }

    private Map<String, String> load() {
        var map = new LinkedHashMap<String, String>();
        jdbc.query("SELECT content_key, category_id FROM transaction_override", new MapSqlParameterSource(),
                rs -> { map.put(rs.getString("content_key"), rs.getString("category_id")); });
        return Map.copyOf(map);
    }
}
