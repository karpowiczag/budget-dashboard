package com.budget.config;

import com.budget.application.categorization.BudgetTaxonomy;
import com.budget.application.categorization.BudgetTaxonomyCatalog;
import com.budget.application.settings.BudgetSettingsService;
import com.budget.infrastructure.persistence.jdbc.JdbcCategoryCatalog;
import java.util.Set;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Seeds the user-owned category tables from {@link BudgetTaxonomy} on first boot (Phase 3b). Runs
 * once at startup; {@link JdbcCategoryCatalog#seedIfEmpty} is a no-op once any category exists, so a
 * user's edits are never overwritten on subsequent boots.
 *
 * <p>The behaviour flags are sourced from their authoritative owners so the seed cannot drift:
 * daily-paced from {@link BudgetTaxonomyCatalog}, sinking-fund from {@link BudgetSettingsService}.
 * {@code PROTECTED_LABELS} replicates the frontend {@code PROTECTED_CATEGORIES} set and is the new
 * server-side source of truth for the "never auto-cut" flag — keep the two in sync.
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

    private final JdbcCategoryCatalog catalog;

    public CategoryCatalogSeeder(JdbcCategoryCatalog catalog) {
        this.catalog = catalog;
    }

    @Override
    public void run(ApplicationArguments args) {
        catalog.seedIfEmpty(
                BudgetTaxonomy.budgetGroups(),
                BudgetTaxonomy.categories(),
                BudgetTaxonomyCatalog.dailyPacedLabels(),
                Set.copyOf(BudgetSettingsService.defaultSinkingFundCategories()),
                PROTECTED_LABELS);
    }
}
