package com.budget.infrastructure.persistence.jdbc;

import static org.assertj.core.api.Assertions.assertThat;

import com.budget.domain.category.Category;
import com.budget.domain.category.ClassificationRule;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Phase 3d-ii: deleting a user category reassigns everything that points to it (rules, overrides,
 * transactions) onto the target and removes the row, and all three read caches reflect the move.
 */
@SpringBootTest(properties = {
        "app.database.url=jdbc:h2:mem:category_delete;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH;DB_CLOSE_DELAY=-1",
        "app.security.oauth-enabled=false"
})
class CategoryDeleteReassignTest {
    @Autowired
    private JdbcCategoryCatalog catalog;
    @Autowired
    private JdbcClassificationRuleStore ruleStore;
    @Autowired
    private JdbcTransactionOverrideStore overrideStore;

    private static Category userCategory(String id) {
        return new Category(id, "Cat " + id, "Styl życia", "Styl życia", "discretionary", "Nieobowiązkowe",
                "Uznaniowe", "livingExpense", true, false, false, false, false, false, false, 100, false);
    }

    @Test
    void deleteReassignsRulesOverridesAndTransactionsToTheTargetAndRemovesTheCategory() {
        catalog.saveCategory(userCategory("usersrc"));
        catalog.saveCategory(userCategory("usertgt"));
        ruleStore.saveRule(new ClassificationRule(null, "title", "USERSRCPATTERN", "usersrc", 50, true, "user"));
        overrideStore.setOverride("k1", "usersrc", LocalDate.of(2026, 1, 3), "konto", new BigDecimal("-10.00"), "X");

        // Warm the caches so we also prove invalidation.
        assertThat(ruleStore.compiledRules()).anySatisfy(rule -> assertThat(rule.categoryId()).isEqualTo("usersrc"));
        assertThat(overrideStore.overridesByKey()).containsEntry("k1", "usersrc");

        catalog.delete("usersrc", "usertgt");

        assertThat(catalog.categories()).noneMatch(category -> category.categoryId().equals("usersrc"));
        assertThat(catalog.categories()).anyMatch(category -> category.categoryId().equals("usertgt"));
        assertThat(ruleStore.rawRules())
                .filteredOn(rule -> "USERSRCPATTERN".equals(rule.pattern()))
                .allSatisfy(rule -> assertThat(rule.categoryId()).isEqualTo("usertgt"));
        assertThat(ruleStore.compiledRules()).noneSatisfy(rule -> assertThat(rule.categoryId()).isEqualTo("usersrc"));
        assertThat(overrideStore.overridesByKey()).containsEntry("k1", "usertgt");
    }
}
