package com.budget.application.categorization;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Phase 3a is a behaviour-preserving seam: {@link BudgetTaxonomyCatalog} must be byte-for-byte
 * equivalent to the prior direct {@link BudgetTaxonomy} reads, and {@link CategoryClassifier} must
 * route through it. These tests pin that equivalence so a future DB-backed catalog (Phase 3b) is
 * the only thing that can change behaviour.
 */
class CategoryCatalogTest {

    /** The daily-paced set as it lived in BudgetAnalysisService before the seam. */
    private static final Set<String> LEGACY_DAILY_PACED = Set.of(
            "Żywność i chemia",
            "Jedzenie poza domem"
    );

    private final CategoryCatalog catalog = new BudgetTaxonomyCatalog();

    @Test
    void definitionByLabelMatchesTaxonomyForEveryCategory() {
        for (var definition : BudgetTaxonomy.CATEGORIES.values()) {
            assertThat(catalog.definitionByLabel(definition.label()))
                    .as("metadata for label %s", definition.label())
                    .isEqualTo(BudgetTaxonomy.category(BudgetTaxonomy.categoryIdByLabel(definition.label())))
                    .isEqualTo(definition);
        }
    }

    @Test
    void definitionByIdMatchesTaxonomyForEveryCategory() {
        for (var entry : BudgetTaxonomy.CATEGORIES.entrySet()) {
            assertThat(catalog.definitionById(entry.getKey())).isEqualTo(entry.getValue());
        }
    }

    @Test
    void categoryIdByLabelDelegatesToTaxonomy() {
        for (var definition : BudgetTaxonomy.CATEGORIES.values()) {
            assertThat(catalog.categoryIdByLabel(definition.label()))
                    .isEqualTo(BudgetTaxonomy.categoryIdByLabel(definition.label()))
                    .isEqualTo(definition.id());
        }
    }

    @Test
    void wealthCategoryLabelsMatchTaxonomy() {
        assertThat(catalog.wealthCategoryLabels())
                .isEqualTo(BudgetTaxonomy.wealthCategoryLabels())
                .containsExactlyInAnyOrder("Inwestycje", "Konto oszczędnościowe", "Nadpłata kredytu");
    }

    @Test
    void dailyPacedMatchesTheLegacyHardcodedSetForEveryCategory() {
        for (var definition : BudgetTaxonomy.CATEGORIES.values()) {
            assertThat(catalog.isDailyPaced(definition.label()))
                    .as("daily-paced for %s", definition.label())
                    .isEqualTo(LEGACY_DAILY_PACED.contains(definition.label()));
        }
        assertThat(catalog.isDailyPaced("Żywność i chemia")).isTrue();
        assertThat(catalog.isDailyPaced("Jedzenie poza domem")).isTrue();
        assertThat(catalog.isDailyPaced("Nieznana kategoria")).isFalse();
    }

    @Test
    void classifierRoutesMetadataAndFlagsThroughCatalog() {
        var classifier = new CategoryClassifier();
        for (var definition : BudgetTaxonomy.CATEGORIES.values()) {
            var label = definition.label();
            assertThat(classifier.budgetArea(label)).isEqualTo(definition.area());
            assertThat(classifier.group(label)).isEqualTo(definition.analyticsGroup());
            assertThat(classifier.budgetBucket(label)).isEqualTo(definition.budgetBucketLabel());
            assertThat(classifier.fixedness(label)).isEqualTo(definition.fixedness());
            assertThat(classifier.isDiscretionary(label)).isEqualTo(definition.discretionary());
            assertThat(classifier.isExcluded(label)).isEqualTo(definition.excluded());
            assertThat(classifier.isRealIncome(label)).isEqualTo(definition.realIncome());
            assertThat(classifier.isDailyPaced(label)).isEqualTo(LEGACY_DAILY_PACED.contains(label));
        }
        assertThat(classifier.wealthCategoryLabels()).isEqualTo(BudgetTaxonomy.wealthCategoryLabels());
    }
}
