package com.budget.infrastructure.persistence.jdbc;

import com.budget.application.categorization.ClassificationRuleAdmin;
import com.budget.application.categorization.ClassificationRuleStore;
import com.budget.domain.category.CategoryRule;
import com.budget.domain.category.ClassificationRule;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.regex.Pattern;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * DB-backed {@link ClassificationRuleStore} (Phase 3c). Rules are seeded from {@link
 * com.budget.application.categorization.BudgetTaxonomy} + private config on first boot, then become
 * user-editable. The enabled rules are compiled once into an immutable in-memory snapshot loaded
 * lazily and invalidated on every write, so the classification hot path never recompiles patterns.
 * Patterns use {@code CASE_INSENSITIVE | UNICODE_CASE} (identical to the static taxonomy) so Polish
 * diacritics match exactly.
 */
@Repository
public class JdbcClassificationRuleStore implements ClassificationRuleStore, ClassificationRuleAdmin {
    private static final int PATTERN_FLAGS = Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE;

    private final NamedParameterJdbcTemplate jdbc;
    private volatile List<CategoryRule> snapshot;

    public JdbcClassificationRuleStore(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<CategoryRule> compiledRules() {
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

    /**
     * First-boot seed: insert the supplied default rules when the table is empty. No-op once any rule
     * exists, so user edits are never overwritten. The caller supplies priority/source already set
     * (personal rules in a lower priority band than built-in to preserve precedence).
     */
    @Transactional
    public void seedIfEmpty(List<ClassificationRule> defaults) {
        var count = jdbc.queryForObject("SELECT COUNT(*) FROM classification_rule", new MapSqlParameterSource(), Integer.class);
        if (count != null && count > 0) {
            return;
        }
        var now = OffsetDateTime.now();
        for (var rule : defaults) {
            jdbc.update("""
                    INSERT INTO classification_rule (match_type, pattern, category_id, priority, enabled, source, updated_at)
                    VALUES (:matchType, :pattern, :categoryId, :priority, :enabled, :source, :updatedAt)
                    """, new MapSqlParameterSource()
                    .addValue("matchType", rule.matchType())
                    .addValue("pattern", rule.pattern())
                    .addValue("categoryId", rule.categoryId())
                    .addValue("priority", rule.priority())
                    .addValue("enabled", rule.enabled())
                    .addValue("source", rule.source())
                    .addValue("updatedAt", now));
        }
        invalidate();
    }

    @Override
    public List<ClassificationRule> rawRules() {
        return jdbc.query("SELECT * FROM classification_rule ORDER BY priority, id", new MapSqlParameterSource(),
                (rs, rowNum) -> new ClassificationRule(
                        String.valueOf(rs.getLong("id")),
                        rs.getString("match_type"),
                        rs.getString("pattern"),
                        rs.getString("category_id"),
                        rs.getInt("priority"),
                        rs.getBoolean("enabled"),
                        rs.getString("source")));
    }

    @Override
    @Transactional
    public void saveRule(ClassificationRule rule) {
        var params = new MapSqlParameterSource()
                .addValue("matchType", rule.matchType() == null || rule.matchType().isBlank() ? "title" : rule.matchType())
                .addValue("pattern", rule.pattern())
                .addValue("categoryId", rule.categoryId())
                .addValue("priority", rule.priority())
                .addValue("enabled", rule.enabled())
                .addValue("source", rule.source() == null || rule.source().isBlank() ? "user" : rule.source())
                .addValue("updatedAt", OffsetDateTime.now());
        if (rule.ruleId() == null || !rule.ruleId().matches("\\d+")) {
            // Blank or non-numeric id (e.g. "new") means create; the IDENTITY column assigns the id.
            jdbc.update("""
                    INSERT INTO classification_rule (match_type, pattern, category_id, priority, enabled, source, updated_at)
                    VALUES (:matchType, :pattern, :categoryId, :priority, :enabled, :source, :updatedAt)
                    """, params);
        } else {
            jdbc.update("""
                    UPDATE classification_rule SET
                        match_type = :matchType, pattern = :pattern, category_id = :categoryId,
                        priority = :priority, enabled = :enabled, source = :source, updated_at = :updatedAt
                    WHERE id = :id
                    """, params.addValue("id", Long.parseLong(rule.ruleId())));
        }
        invalidate();
    }

    @Override
    @Transactional
    public void deleteRule(String ruleId) {
        jdbc.update("DELETE FROM classification_rule WHERE id = :id",
                new MapSqlParameterSource("id", Long.parseLong(ruleId)));
        invalidate();
    }

    /** Drop the cached snapshot; the next read rebuilds (and recompiles) it from the DB. */
    void invalidate() {
        snapshot = null;
    }

    private List<CategoryRule> load() {
        return jdbc.query("SELECT * FROM classification_rule WHERE enabled = TRUE ORDER BY priority, id",
                new MapSqlParameterSource(), (rs, rowNum) -> {
                    var pattern = rs.getString("pattern");
                    var sourcePattern = "personal".equals(rs.getString("source")) ? "personal:" + pattern : pattern;
                    return new CategoryRule(Pattern.compile(pattern, PATTERN_FLAGS), rs.getString("category_id"), sourcePattern);
                });
    }
}
