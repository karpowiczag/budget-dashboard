package com.budget.application.analysis;

import static org.assertj.core.api.Assertions.assertThat;

import com.budget.application.categorization.CategoryClassifier;
import com.budget.application.categorization.TransactionOverrideStore;
import com.budget.domain.transaction.BankTransaction;
import com.budget.domain.transaction.NormalizedTransaction;
import com.budget.domain.transaction.TransactionContentKey;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Phase 3c-iii: a manual recategorize override is matched by stable content key, wins over the whole
 * matcher chain, re-derives every dimension from the chosen category, and re-applies on every
 * normalize pass — which is exactly what makes it survive a rebuild (analyze re-runs normalize each
 * import over freshly-recreated rows).
 */
class TransactionOverrideNormalizationTest {

    private static TransactionOverrideStore fixedOverrides(Map<String, String> byKey) {
        return new TransactionOverrideStore() {
            @Override
            public Map<String, String> overridesByKey() {
                return byKey;
            }

            @Override
            public void setOverride(String contentKey, String categoryId, LocalDate postedDate, String account, BigDecimal amount, String description) {
            }

            @Override
            public void clearOverride(String contentKey) {
            }
        };
    }

    @Test
    void overrideWinsOverRuleMatchAndReDerivesEveryDimensionById() {
        // BIEDRONKA normally classifies to groceries (a spend); override it to investments (excluded wealth).
        var row = new BankTransaction(LocalDate.of(2026, 1, 3), "konto", "BIEDRONKA ZAKUP", "", -200);
        var key = TransactionContentKey.of(row.date(), row.account(), row.amount(), row.description());
        var normalizer = new TransactionNormalizer(new CategoryClassifier(), fixedOverrides(Map.of(key, "investments")));

        var result = normalizer.normalize(List.of(row));

        assertThat(result).singleElement().satisfies(tx -> {
            assertThat(tx.categoryId()).isEqualTo("investments");
            assertThat(tx.correctedCategory()).isEqualTo("Inwestycje");
            assertThat(tx.budgetBucket()).isEqualTo("Inwestycje");
            // Investments is excluded from spend; the override must flip the flow accordingly.
            assertThat(tx.analysisSpend()).isZero();
            assertThat(tx.excludedOutgoing()).isEqualByComparingTo(BigDecimal.valueOf(200));
        });
    }

    @Test
    void overrideReAppliesOnEveryPassAndAcrossIdenticalRowsSoItSurvivesRebuild() {
        var row = new BankTransaction(LocalDate.of(2026, 1, 3), "konto", "BIEDRONKA ZAKUP", "", -200);
        var duplicate = new BankTransaction(LocalDate.of(2026, 1, 3), "konto", "BIEDRONKA ZAKUP", "", -200);
        var key = TransactionContentKey.of(row.date(), row.account(), row.amount(), row.description());
        var normalizer = new TransactionNormalizer(new CategoryClassifier(), fixedOverrides(Map.of(key, "investments")));

        // First pass (e.g. the recategorize re-analysis).
        assertThat(normalizer.normalize(List.of(row)))
                .allSatisfy(tx -> assertThat(tx.categoryId()).isEqualTo("investments"));
        // Second pass over freshly-built rows (the rebuild that recreates budget_transactions) — the
        // content key still matches, so the override re-attaches without any persisted id.
        assertThat(normalizer.normalize(List.of(duplicate)))
                .allSatisfy(tx -> assertThat(tx.categoryId()).isEqualTo("investments"));
        // Two rows with identical content both receive the override (the key is content, not id).
        assertThat(normalizer.normalize(List.of(row, duplicate)))
                .hasSize(2)
                .allSatisfy(tx -> assertThat(tx.categoryId()).isEqualTo("investments"));
    }

    @Test
    void withoutAnOverrideClassificationIsUnchanged() {
        var row = new BankTransaction(LocalDate.of(2026, 1, 3), "konto", "BIEDRONKA ZAKUP", "", -200);
        var normalizer = new TransactionNormalizer(new CategoryClassifier(), fixedOverrides(Map.of()));

        assertThat(normalizer.normalize(List.of(row)))
                .singleElement()
                .extracting(NormalizedTransaction::categoryId)
                .isEqualTo("groceries");
    }
}
