package com.budget.config;

import com.budget.application.categorization.BudgetTaxonomy;
import com.budget.application.categorization.BudgetTaxonomyCatalog;
import com.budget.application.categorization.PersonalCategoryRules;
import com.budget.application.settings.BudgetSettingsService;
import com.budget.domain.category.ClassificationRule;
import com.budget.infrastructure.persistence.jdbc.JdbcCategoryCatalog;
import com.budget.infrastructure.persistence.jdbc.JdbcClassificationRuleStore;
import java.util.ArrayList;
import java.util.Set;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Seeds the user-owned category catalog and classification rules from {@link BudgetTaxonomy} on first
 * boot (Phases 3b/3c). Runs once at startup as a single ordered pass — categories first, then rules
 * (so the rule → category foreign key is satisfied). Both seeds are no-ops once any row exists, so a
 * user's edits are never overwritten on subsequent boots.
 *
 * <p>The behaviour flags are sourced from their authoritative owners so the seed cannot drift:
 * daily-paced from {@link BudgetTaxonomyCatalog}, sinking-fund from {@link BudgetSettingsService}.
 * {@code PROTECTED_LABELS} replicates the frontend {@code PROTECTED_CATEGORIES} set and is the new
 * server-side source of truth for the "never auto-cut" flag — keep the two in sync.
 *
 * <p>Rules seed built-in defaults (declared order as ascending priority) and private personal rules
 * from {@link PersonalCategoryRules} in a lower priority band, reproducing the historical
 * personal-before-built-in precedence. Personal patterns come from ignored config only (Data Safety)
 * — never hard-coded here.
 */
@Component
public class CategoryCatalogSeeder implements ApplicationRunner {
    private static final Set<String> PROTECTED_LABELS = Set.of(
            "Czynsz i wynajem",
            "Prąd",
            "TV, internet, telefon",
            "Ubezpieczenia",
            "Podatki",
            "Spłaty i raty",
            "Nadpłata kredytu",
            "Inwestycje",
            "Konto oszczędnościowe",
            "Pensja"
    );
    private static final int BUILTIN_PRIORITY_BASE = 1000;

    private final JdbcCategoryCatalog catalog;
    private final JdbcClassificationRuleStore ruleStore;
    private final PersonalCategoryRules personalRules;

    public CategoryCatalogSeeder(JdbcCategoryCatalog catalog, JdbcClassificationRuleStore ruleStore, PersonalCategoryRules personalRules) {
        this.catalog = catalog;
        this.ruleStore = ruleStore;
        this.personalRules = personalRules;
    }

    @Override
    public void run(ApplicationArguments args) {
        catalog.seedIfEmpty(
                BudgetTaxonomy.budgetGroups(),
                BudgetTaxonomy.categories(),
                BudgetTaxonomyCatalog.dailyPacedLabels(),
                Set.copyOf(BudgetSettingsService.defaultSinkingFundCategories()),
                PROTECTED_LABELS);
        ruleStore.seedIfEmpty(defaultRules());
    }

    private java.util.List<ClassificationRule> defaultRules() {
        var rules = new ArrayList<ClassificationRule>();
        var priority = 0;
        // Personal rules first (lower priority band) — they win over built-ins, matching the prior
        // Stream.concat(personalRules, RULES) order. The raw pattern comes from the compiled Pattern.
        for (var rule : personalRules.rules()) {
            rules.add(new ClassificationRule(null, "title", rule.pattern().pattern(), rule.categoryId(), priority++, true, "personal"));
        }
        var builtinPriority = BUILTIN_PRIORITY_BASE;
        for (var rule : BudgetTaxonomy.rules()) {
            rules.add(new ClassificationRule(null, "title", rule.sourcePattern(), rule.categoryId(), builtinPriority++, true, "builtin"));
        }
        return rules;
    }
}
