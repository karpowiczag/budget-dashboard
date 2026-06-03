package com.budget.infrastructure.persistence.jdbc;

import com.budget.application.categorization.BudgetTaxonomy;
import com.budget.application.categorization.CategoryCatalog;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * DB-backed {@link CategoryCatalog} (Phase 3b). The category rows are seeded from
 * {@link BudgetTaxonomy} on first boot ({@code CategoryCatalogSeeder}), after which they are
 * user-owned. The classifier reads this on the hot path, so the catalog is held in an immutable
 * in-memory snapshot loaded lazily on first access and invalidated on every write.
 *
 * <p>Resolution semantics mirror {@link BudgetTaxonomy} exactly so behaviour is byte-for-byte
 * identical until a user edits the catalog (pinned by {@code JdbcCategoryCatalogTest}).
 */
@Repository
public class JdbcCategoryCatalog implements CategoryCatalog {
    private final NamedParameterJdbcTemplate jdbc;
    private volatile Snapshot snapshot;

    public JdbcCategoryCatalog(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public BudgetTaxonomy.CategoryDefinition definitionById(String categoryId) {
        var definition = snapshot().byId().get(categoryId);
        if (definition == null) {
            throw new IllegalArgumentException("Unknown budget category id: " + categoryId);
        }
        return definition;
    }

    @Override
    public BudgetTaxonomy.CategoryDefinition definitionByLabel(String label) {
        return definitionById(categoryIdByLabel(label));
    }

    @Override
    public String categoryIdByLabel(String label) {
        if (label == null || label.isBlank()) {
            return "";
        }
        var snapshot = snapshot();
        var alias = snapshot.aliasToId().get(label);
        if (alias != null) {
            return alias;
        }
        return snapshot.labelToId().getOrDefault(label, label);
    }

    @Override
    public Set<String> wealthCategoryLabels() {
        return snapshot().wealthLabels();
    }

    @Override
    public boolean isDailyPaced(String categoryLabel) {
        return snapshot().dailyPacedLabels().contains(categoryLabel);
    }

    @Override
    public Set<String> wealthCategoryIds() {
        return snapshot().wealthIds();
    }

    @Override
    public boolean isDailyPacedById(String categoryId) {
        return snapshot().dailyPacedIds().contains(categoryId);
    }

    /**
     * First-boot seed: populate the empty category tables from the supplied defaults. No-op once
     * any category exists, so user edits are never clobbered. Groups are inserted first to satisfy
     * the foreign key; sort order follows the supplied iteration order.
     */
    @Transactional
    public void seedIfEmpty(
            Collection<BudgetTaxonomy.BudgetGroup> groups,
            Collection<BudgetTaxonomy.CategoryDefinition> categories,
            Set<String> dailyPacedLabels,
            Set<String> sinkingFundLabels,
            Set<String> protectedLabels) {
        var count = jdbc.queryForObject("SELECT COUNT(*) FROM category", new MapSqlParameterSource(), Integer.class);
        if (count != null && count > 0) {
            return;
        }
        var now = OffsetDateTime.now();
        var groupSort = 0;
        for (var group : groups) {
            jdbc.update("INSERT INTO category_group (group_id, label, sort_order) VALUES (:groupId, :label, :sortOrder)",
                    new MapSqlParameterSource()
                            .addValue("groupId", group.id())
                            .addValue("label", group.label())
                            .addValue("sortOrder", groupSort++));
        }
        var categorySort = 0;
        for (var definition : categories) {
            jdbc.update("""
                    INSERT INTO category (
                        category_id, label, area, analytics_group, group_id, budget_bucket_label,
                        fixedness, flow_type, discretionary, excluded, real_income,
                        daily_paced, protected_flag, sinking_fund_eligible, sort_order, archived, updated_at
                    ) VALUES (
                        :categoryId, :label, :area, :analyticsGroup, :groupId, :budgetBucketLabel,
                        :fixedness, :flowType, :discretionary, :excluded, :realIncome,
                        :dailyPaced, :protectedFlag, :sinkingFundEligible, :sortOrder, :archived, :updatedAt
                    )
                    """, new MapSqlParameterSource()
                    .addValue("categoryId", definition.id())
                    .addValue("label", definition.label())
                    .addValue("area", definition.area())
                    .addValue("analyticsGroup", definition.analyticsGroup())
                    .addValue("groupId", definition.budgetGroupId())
                    .addValue("budgetBucketLabel", definition.budgetBucketLabel())
                    .addValue("fixedness", definition.fixedness())
                    .addValue("flowType", definition.flowType())
                    .addValue("discretionary", definition.discretionary())
                    .addValue("excluded", definition.excluded())
                    .addValue("realIncome", definition.realIncome())
                    .addValue("dailyPaced", dailyPacedLabels.contains(definition.label()))
                    .addValue("protectedFlag", protectedLabels.contains(definition.label()))
                    .addValue("sinkingFundEligible", sinkingFundLabels.contains(definition.label()))
                    .addValue("sortOrder", categorySort++)
                    .addValue("archived", false)
                    .addValue("updatedAt", now));
        }
        invalidate();
    }

    /** Drop the cached snapshot; the next read rebuilds it from the DB. */
    void invalidate() {
        snapshot = null;
    }

    private Snapshot snapshot() {
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

    private Snapshot load() {
        var byId = new LinkedHashMap<String, BudgetTaxonomy.CategoryDefinition>();
        var labelToId = new LinkedHashMap<String, String>();
        var wealthLabels = new LinkedHashSet<String>();
        var wealthIds = new LinkedHashSet<String>();
        var dailyPacedLabels = new LinkedHashSet<String>();
        var dailyPacedIds = new LinkedHashSet<String>();
        jdbc.query("SELECT * FROM category ORDER BY sort_order, label", new MapSqlParameterSource(), (ResultSet rs) -> {
            var definition = map(rs);
            byId.put(definition.id(), definition);
            labelToId.put(definition.label(), definition.id());
            if (BudgetTaxonomy.FLOW_WEALTH_TRANSFER.equals(definition.flowType())) {
                wealthLabels.add(definition.label());
                wealthIds.add(definition.id());
            }
            if (rs.getBoolean("daily_paced")) {
                dailyPacedLabels.add(definition.label());
                dailyPacedIds.add(definition.id());
            }
        });
        return new Snapshot(
                Map.copyOf(byId),
                Map.copyOf(labelToId),
                Map.copyOf(BudgetTaxonomy.legacyCategoryLabelAliases()),
                Set.copyOf(wealthLabels),
                Set.copyOf(wealthIds),
                Set.copyOf(dailyPacedLabels),
                Set.copyOf(dailyPacedIds));
    }

    private BudgetTaxonomy.CategoryDefinition map(ResultSet rs) throws SQLException {
        return new BudgetTaxonomy.CategoryDefinition(
                rs.getString("category_id"),
                rs.getString("label"),
                rs.getString("area"),
                rs.getString("analytics_group"),
                rs.getString("group_id"),
                rs.getString("budget_bucket_label"),
                rs.getString("fixedness"),
                rs.getString("flow_type"),
                rs.getBoolean("discretionary"),
                rs.getBoolean("excluded"),
                rs.getBoolean("real_income"));
    }

    private record Snapshot(
            Map<String, BudgetTaxonomy.CategoryDefinition> byId,
            Map<String, String> labelToId,
            Map<String, String> aliasToId,
            Set<String> wealthLabels,
            Set<String> wealthIds,
            Set<String> dailyPacedLabels,
            Set<String> dailyPacedIds) {
    }
}
