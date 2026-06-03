package com.budget.infrastructure.persistence.jdbc;

import static org.assertj.core.api.Assertions.assertThat;

import com.budget.application.categorization.BudgetTaxonomyRuleStore;
import com.budget.application.categorization.ClassificationRuleStore;
import com.budget.application.categorization.PersonalCategoryRules;
import com.budget.domain.category.CategoryRule;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

/**
 * Phase 3c-i is behaviour-preserving: after first-boot seeding, the DB-backed rule store must produce
 * exactly the same compiled rules, in the same precedence order, as the historical in-memory matcher
 * ({@link BudgetTaxonomyRuleStore} = personal rules first, then the built-in taxonomy rules). Compared
 * against that reference (built from the same {@link PersonalCategoryRules} bean the seeder used) the
 * test is independent of whether a private config defines personal rules, and never hard-codes one.
 */
@SpringBootTest(properties = {
        "app.database.url=jdbc:h2:mem:classification_rules;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH;DB_CLOSE_DELAY=-1",
        "app.security.oauth-enabled=false"
})
class JdbcClassificationRuleStoreTest {
    @Autowired
    private ClassificationRuleStore store;
    @Autowired
    private JdbcClassificationRuleStore jdbcStore;
    @Autowired
    private PersonalCategoryRules personalRules;
    @Autowired
    private NamedParameterJdbcTemplate jdbc;

    @Test
    void compiledRulesMatchTheHistoricalMatcherOrderAndContent() {
        var compiled = store.compiledRules();
        var reference = new BudgetTaxonomyRuleStore(personalRules).compiledRules();
        assertThat(compiled).hasSameSizeAs(reference);
        assertThat(compiled.stream().map(CategoryRule::categoryId).toList())
                .isEqualTo(reference.stream().map(CategoryRule::categoryId).toList());
        assertThat(compiled.stream().map(CategoryRule::sourcePattern).toList())
                .isEqualTo(reference.stream().map(CategoryRule::sourcePattern).toList());
    }

    @Test
    void seedsEveryRuleFromTheHistoricalMatcher() {
        var count = jdbc.queryForObject("SELECT COUNT(*) FROM classification_rule", new MapSqlParameterSource(), Integer.class);
        assertThat(count).isEqualTo(new BudgetTaxonomyRuleStore(personalRules).compiledRules().size());
    }

    @Test
    void seedIsIdempotent() {
        var before = jdbc.queryForObject("SELECT COUNT(*) FROM classification_rule", new MapSqlParameterSource(), Integer.class);
        jdbcStore.seedIfEmpty(List.of());
        var after = jdbc.queryForObject("SELECT COUNT(*) FROM classification_rule", new MapSqlParameterSource(), Integer.class);
        assertThat(after).isEqualTo(before);
    }
}
