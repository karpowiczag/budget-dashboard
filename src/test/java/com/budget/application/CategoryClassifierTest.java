package com.budget.application;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CategoryClassifierTest {
    private final CategoryClassifier classifier = new CategoryClassifier();

    @Test
    void classifiesSalaryFromTransferTitle() {
        var category = classifier.classify("", "PRZELEW EXPRESS ELIXIR PRZYCH. ALEKSANDER KARPOWICZ SOFTWARE WYNAGRODZENIE", 18000);

        assertThat(category).isEqualTo("Pensja Aleksander");
        assertThat(classifier.budgetArea(category)).isEqualTo("Przychody");
    }

    @Test
    void classifiesCardRepaymentAsTechnicalTransfer() {
        var category = classifier.classify("", "RĘCZNA SPŁATA KARTY KREDYT", -65000);

        assertThat(category).isEqualTo("Spłata karty kredytowej");
        assertThat(classifier.budgetBucket(category)).isEqualTo("Transfer techniczny");
        assertThat(classifier.isExcluded(category)).isTrue();
    }
}
