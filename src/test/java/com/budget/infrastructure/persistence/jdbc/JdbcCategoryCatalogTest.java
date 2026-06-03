package com.budget.infrastructure.persistence.jdbc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.budget.application.categorization.BudgetTaxonomy;
import com.budget.application.categorization.BudgetTaxonomyCatalog;
import com.budget.application.categorization.CategoryCatalog;
import com.budget.application.settings.BudgetSettingsService;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

/**
 * Phase 3b-i is behaviour-preserving: the DB-backed catalog, after first-boot seeding, must be
 * byte-for-byte equivalent to the static {@link BudgetTaxonomy}. These tests pin that equivalence
 * and the seed shape so any later DB drift is caught.
 */
@SpringBootTest(properties = {
        "app.database.url=jdbc:h2:mem:budget_categories;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH;DB_CLOSE_DELAY=-1",
        "app.security.oauth-enabled=false"
})
class JdbcCategoryCatalogTest {
    @Autowired
    private CategoryCatalog catalog;
    @Autowired
    private JdbcCategoryCatalog jdbcCatalog;
    @Autowired
    private NamedParameterJdbcTemplate jdbc;

    @Test
    void definitionLookupsMatchTaxonomyForEveryCategory() {
        for (var definition : BudgetTaxonomy.categories()) {
            assertThat(catalog.definitionById(definition.id()))
                    .as("definitionById(%s)", definition.id())
                    .isEqualTo(definition);
            assertThat(catalog.definitionByLabel(definition.label()))
                    .as("definitionByLabel(%s)", definition.label())
                    .isEqualTo(definition);
            assertThat(catalog.categoryIdByLabel(definition.label()))
                    .as("categoryIdByLabel(%s)", definition.label())
                    .isEqualTo(definition.id());
        }
    }

    @Test
    void categoryIdByLabelReproducesTaxonomyEdgeCases() {
        // Legacy aliases resolve to the current id.
        BudgetTaxonomy.legacyCategoryLabelAliases().forEach((alias, id) ->
                assertThat(catalog.categoryIdByLabel(alias)).as("alias %s", alias).isEqualTo(id));
        // Blank -> "" ; unknown label -> returned unchanged (matches BudgetTaxonomy).
        assertThat(catalog.categoryIdByLabel("")).isEqualTo("");
        assertThat(catalog.categoryIdByLabel("   ")).isEqualTo("");
        assertThat(catalog.categoryIdByLabel("Zupełnie nieznana")).isEqualTo("Zupełnie nieznana");
        assertThatThrownBy(() -> catalog.definitionById("nieznaneId"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void wealthAndDailyPacedMatchTaxonomy() {
        assertThat(catalog.wealthCategoryLabels())
                .isEqualTo(BudgetTaxonomy.wealthCategoryLabels())
                .containsExactlyInAnyOrder("Inwestycje", "Konto oszczędnościowe", "Nadpłata kredytu");
        for (var definition : BudgetTaxonomy.categories()) {
            assertThat(catalog.isDailyPaced(definition.label()))
                    .as("isDailyPaced(%s)", definition.label())
                    .isEqualTo(BudgetTaxonomyCatalog.dailyPacedLabels().contains(definition.label()));
        }
        assertThat(catalog.isDailyPaced("Żywność i chemia")).isTrue();
        assertThat(catalog.isDailyPaced("Jedzenie poza domem")).isTrue();
        assertThat(catalog.isDailyPaced("Pensja")).isFalse();
        assertThat(catalog.isDailyPaced("Zupełnie nieznana")).isFalse();
    }

    @Test
    void seedsEveryCategoryAndGroupInTaxonomyOrder() {
        var categoryCount = jdbc.queryForObject("SELECT COUNT(*) FROM category", new MapSqlParameterSource(), Integer.class);
        var groupCount = jdbc.queryForObject("SELECT COUNT(*) FROM category_group", new MapSqlParameterSource(), Integer.class);
        assertThat(categoryCount).isEqualTo(BudgetTaxonomy.categories().size());
        assertThat(groupCount).isEqualTo(BudgetTaxonomy.budgetGroups().size());

        var seededIds = jdbc.query("SELECT category_id FROM category ORDER BY sort_order",
                new MapSqlParameterSource(), (rs, n) -> rs.getString(1));
        var taxonomyIds = BudgetTaxonomy.categories().stream().map(BudgetTaxonomy.CategoryDefinition::id).toList();
        assertThat(seededIds).isEqualTo(taxonomyIds);
    }

    @Test
    void seedIsIdempotent() {
        var before = jdbc.queryForObject("SELECT COUNT(*) FROM category", new MapSqlParameterSource(), Integer.class);
        jdbcCatalog.seedIfEmpty(
                BudgetTaxonomy.budgetGroups(),
                BudgetTaxonomy.categories(),
                BudgetTaxonomyCatalog.dailyPacedLabels(),
                Set.copyOf(BudgetSettingsService.defaultSinkingFundCategories()),
                Set.of());
        var after = jdbc.queryForObject("SELECT COUNT(*) FROM category", new MapSqlParameterSource(), Integer.class);
        assertThat(after).isEqualTo(before);
    }
}
