package com.budget.application.categorization;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CategoryClassifierTest {
    private final CategoryClassifier classifier = new CategoryClassifier();

    @Test
    void classifiesSalaryFromTransferTitle() {
        var category = classifier.classify("", "PRZELEW EXPRESS ELIXIR PRZYCH. TEST EMPLOYER WYNAGRODZENIE", 18000);

        assertThat(category).isEqualTo("Pensja");
        assertThat(classifier.budgetArea(category)).isEqualTo("Przychody");
    }

    @Test
    void classifiesCardRepaymentAsTechnicalTransfer() {
        var category = classifier.classify("", "RĘCZNA SPŁATA KARTY KREDYT", -65000);

        assertThat(category).isEqualTo("Spłata karty kredytowej");
        assertThat(classifier.budgetBucket(category)).isEqualTo("Transfer techniczny");
        assertThat(classifier.isExcluded(category)).isTrue();
    }

    @Test
    void fallsBackUnknownExpenseToManualReview() {
        var decision = classifier.classifyDecision("Bez kategorii", "NIEZNANY SKLEP XYZ", -123);

        assertThat(decision.category()).isEqualTo("Do sprawdzenia");
        assertThat(decision.matchedByTitle()).isFalse();
    }

    @Test
    void treatsPositiveUnmatchedFlowAsRefundCorrection() {
        var decision = classifier.classifyDecision("", "ZWROT LOSOWY", 42);

        assertThat(decision.category()).isEqualTo("Zwroty i korekty");
        assertThat(decision.pattern()).isEqualTo("positive-flow-fallback");
        assertThat(decision.matchedByTitle()).isTrue();
    }

    @Test
    void assignsNestedSubcategoryFromMerchantTitle() {
        assertThat(classifier.subcategory("Żywność i chemia", "BIEDRONKA ZAKUP WROCLAW")).isEqualTo("Market spożywczy");
        assertThat(classifier.subcategory("Spłata karty kredytowej", "RĘCZNA SPŁATA KARTY KREDYT")).isEqualTo("Wychodząca spłata karty");
    }
}
