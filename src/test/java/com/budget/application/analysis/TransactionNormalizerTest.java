package com.budget.application.analysis;

import static org.assertj.core.api.Assertions.assertThat;

import com.budget.application.categorization.CategoryClassifier;
import com.budget.domain.transaction.BankTransaction;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class TransactionNormalizerTest {
    private final CategoryClassifier classifier = new CategoryClassifier();
    private final TransactionNormalizer normalizer = new TransactionNormalizer(classifier);

    @Test
    void marksLargeMarketplaceTransactionAsLowConfidenceReviewRisk() {
        var transactions = normalizer.normalize(List.of(
                new BankTransaction(LocalDate.of(2026, 2, 1), "konto", "ALLEGRO ZAKUPY", "Zakupy", -750)
        ));

        var tx = transactions.getFirst();
        assertThat(tx.correctedCategory()).isEqualTo("Marketplace i zakupy online");
        assertThat(tx.confidence()).isEqualTo("Niska");
        assertThat(tx.notes()).contains("Do ręcznego sprawdzenia");
        assertThat(tx.analysisSpend()).isEqualByComparingTo(BigDecimal.valueOf(750));
    }

    @Test
    void excludesOwnTransfersFromSpend() {
        var transactions = normalizer.normalize(List.of(
                new BankTransaction(LocalDate.of(2026, 2, 1), "konto", "PRZELEW WŁASNY NA DRUGIE KONTO", "", -1000)
        ));

        var tx = transactions.getFirst();
        assertThat(tx.correctedCategory()).isEqualTo("Przelewy własne");
        assertThat(tx.analysisSpend()).isZero();
        assertThat(tx.excludedOutgoing()).isEqualByComparingTo(BigDecimal.valueOf(1000));
    }

    @Test
    void excludesSavingsAccountTransfersAsSeparateFinancialFlow() {
        var transactions = normalizer.normalize(List.of(
                new BankTransaction(LocalDate.of(2026, 2, 1), "konto", "PRZELEW WŁASNY NA OSZCZĘDNOŚCI", "", -1000)
        ));

        var tx = transactions.getFirst();
        assertThat(tx.correctedCategory()).isEqualTo("Konto oszczędnościowe");
        assertThat(tx.analysisSpend()).isZero();
        assertThat(tx.excludedOutgoing()).isEqualByComparingTo(BigDecimal.valueOf(1000));
    }
}
