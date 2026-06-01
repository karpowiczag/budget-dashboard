package com.budget.application.categorization;

import static org.assertj.core.api.Assertions.assertThat;

import com.budget.domain.category.CategoryRule;
import java.util.List;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class CategoryClassifierTest {
    private final CategoryClassifier classifier = new CategoryClassifier();

    @Test
    void everyTaxonomyRuleTargetsAValidCategory() {
        assertThat(BudgetTaxonomy.RULES)
                .isNotEmpty()
                .allSatisfy(rule -> assertThat(BudgetTaxonomy.CATEGORIES).containsKey(rule.categoryId()));
    }

    @Test
    void everyCategoryHasStableFlowAndBudgetGroup() {
        assertThat(BudgetTaxonomy.CATEGORIES.values())
                .allSatisfy(category -> {
                    assertThat(category.id()).matches("[a-z][A-Za-z0-9]*");
                    assertThat(category.label()).isNotBlank();
                    assertThat(BudgetTaxonomy.BUDGET_GROUPS).containsKey(category.budgetGroupId());
                    assertThat(category.flowType()).isIn(
                            "income",
                            "livingExpense",
                            "wealthTransfer",
                            "technicalTransfer",
                            "refundCorrection",
                            "review"
                    );
                });
    }

    @Test
    void classifiesSalaryFromTransferTitle() {
        var category = classifier.classify("", "PRZELEW EXPRESS ELIXIR PRZYCH. TEST EMPLOYER WYNAGRODZENIE", 18000);

        assertThat(category).isEqualTo("Pensja");
        assertThat(classifier.budgetArea(category)).isEqualTo("Przychody");
    }

    @Test
    void classifiesSalaryFromBankCategoryWhenTransferTitleHasNoSalaryKeyword() {
        var decision = classifier.classifyDecision("Wynagrodzenie", "PRZELEW EXPRESS ELIXIR PRZYCH.", 18000);

        assertThat(decision.category()).isEqualTo("Pensja");
        assertThat(decision.pattern()).isEqualTo("bank-category:Wynagrodzenie");
    }

    @Test
    void classifiesIncomingGooglePaymentsAsSalary() {
        var decision = classifier.classifyDecision("Bez kategorii", "PRZELEW EXPRESS ELIXIR PRZYCH. GOOGLE", 18000);

        assertThat(decision.category()).isEqualTo("Pensja");
        assertThat(decision.pattern()).isEqualTo("employer-income:GOOGLE");
        assertThat(classifier.budgetBucket(decision.category())).isEqualTo("Przychody");
    }

    @Test
    void keepsGooglePlayExpensesAsMultimedia() {
        var decision = classifier.classifyDecision("Bez kategorii", "GOOGLE PLAY", -49.99);

        assertThat(decision.category()).isEqualTo("Multimedia, książki i prasa");
    }

    @Test
    void classifiesCardRepaymentAsTechnicalTransfer() {
        var category = classifier.classify("", "RĘCZNA SPŁATA KARTY KREDYT", -65000);

        assertThat(category).isEqualTo("Spłata karty kredytowej");
        assertThat(classifier.budgetBucket(category)).isEqualTo("Transfer techniczny");
        assertThat(classifier.isExcluded(category)).isTrue();
    }

    @Test
    void classifiesSavingsAccountTransfersSeparatelyFromInvestmentsAndTechnicalTransfers() {
        var category = classifier.classify("", "PRZELEW WŁASNY NA KONTO OSZCZĘDNOŚCIOWE", -2000);

        assertThat(category).isEqualTo("Konto oszczędnościowe");
        assertThat(classifier.budgetBucket(category)).isEqualTo("Konto oszczędnościowe");
        assertThat(classifier.budgetArea(category)).isEqualTo("Majątek i inwestycje");
        assertThat(classifier.fixedness(category)).isEqualTo("Oszczędności");
        assertThat(classifier.isExcluded(category)).isTrue();
        assertThat(classifier.subcategory(category, "PRZELEW WŁASNY NA KONTO OSZCZĘDNOŚCIOWE")).isEqualTo("Konto oszczędnościowe");
    }

    @Test
    void classifiesBankSavingsAccountCategoryAsSeparateSavingsFlow() {
        var decision = classifier.classifyDecision("Lokaty i konto oszcz.", "PRZELEW WŁASNY", -2000);

        assertThat(decision.category()).isEqualTo("Konto oszczędnościowe");
        assertThat(decision.pattern()).isEqualTo("bank-category:Lokaty i konto oszcz.");
    }

    @Test
    void fallsBackUnknownExpenseToManualReview() {
        var decision = classifier.classifyDecision("Bez kategorii", "NIEZNANY SKLEP XYZ", -123);

        assertThat(decision.category()).isEqualTo("Niesklasyfikowane");
        assertThat(decision.subcategory()).isBlank();
        assertThat(decision.reviewStatus()).isEqualTo("needsReview");
        assertThat(decision.matchedByTitle()).isFalse();
    }

    @Test
    void flagsMarketplaceAsSplitReviewWithoutUsingReviewAsCategory() {
        var decision = classifier.classifyDecision("Bez kategorii", "ALLEGRO ZAKUPY", -450);

        assertThat(decision.category()).isEqualTo("Marketplace i zakupy online");
        assertThat(decision.reviewStatus()).isEqualTo("needsSplit");
        assertThat(decision.budgetGroup()).isEqualTo("Do rozbicia");
    }

    @Test
    void splitsMedicalNeedsFromBeautyDiscretionarySpend() {
        var medical = classifier.classifyDecision("Bez kategorii", "APTEKA TEST", -80);
        var beauty = classifier.classifyDecision("Bez kategorii", "ROSSMANN TEST", -120);

        assertThat(medical.category()).isEqualTo("Lekarz i apteka");
        assertThat(medical.budgetGroup()).isEqualTo("Obowiązkowe zmienne");
        assertThat(beauty.category()).isEqualTo("Uroda i kosmetyki");
        assertThat(beauty.budgetGroup()).isEqualTo("Nieobowiązkowe");
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

    @Test
    void classifiesPlusEFakturaBillsAsPhoneBills() {
        for (var title : List.of("PLUS TATIANA, E-FAKTURA", "PLUS ALEKSANDER, E-FAKTURA")) {
            var category = classifier.classify("Bez kategorii", title, -123);

            assertThat(category).isEqualTo("TV, internet, telefon");
            assertThat(classifier.budgetBucket(category)).isEqualTo("Obowiązkowe stałe");
            assertThat(classifier.fixedness(category)).isEqualTo("Stałe");
            assertThat(classifier.subcategory(category, title)).isEqualTo("Telefon");
        }
    }

    @Test
    void appliesPersonalRulesBeforePublicFallbackRules() {
        var personalClassifier = new CategoryClassifier(new PersonalCategoryRules(List.of(
                new CategoryRule(Pattern.compile("PRIVATE OWN TRANSFER", Pattern.CASE_INSENSITIVE), "ownTransfers", "personal:PRIVATE OWN TRANSFER")
        )));

        var decision = personalClassifier.classifyDecision("Bez kategorii", "PRIVATE OWN TRANSFER", -500);

        assertThat(decision.category()).isEqualTo("Przelewy własne");
        assertThat(decision.pattern()).isEqualTo("personal:PRIVATE OWN TRANSFER");
        assertThat(decision.reviewStatus()).isEqualTo("ok");
    }

    @Test
    void classifiesResearchedPublicMerchantsFromMayImport() {
        assertDecision("NORWAYS BEST AS ZAKUP PRZY UŻYCIU KARTY - INTERNET", "Podróże i wyjazdy", "Atrakcje w podróży");
        assertDecision("TORPEKSPRESSEN ZAKUP PRZY UŻYCIU KARTY - INTERNET", "Podróże i wyjazdy", "Transport w podróży");
        assertDecision("OEN TURISTSENTE ZAKUP PRZY UŻYCIU KARTY - INTERNET", "Podróże i wyjazdy", "Noclegi");
        assertDecision("housebrand.com ZAKUP PRZY UŻYCIU KARTY - INTERNET", "Odzież i obuwie", "Ubrania");
        assertDecision("DM DROGERIE MARKT K.2. ZAKUP PRZY UŻYCIU KARTY W KRAJU", "Uroda i kosmetyki", "Kosmetyki");
        assertDecision("PATRYCJA PILECKA ., EL GORDITO . BLIK P2P-WYCHODZĄCY", "Jedzenie poza domem", "Restauracje");
    }

    @Test
    void treatsForeignParkingAndRentalAsTravelInsteadOfLocalTransport() {
        assertDecision("PARKOVISTE CENTRUM ZAKUP PRZY UŻYCIU KARTY - INTERNET", "Podróże i wyjazdy", "Parking w podróży");
        assertDecision("SIXT RENT A CAR ZAKUP PRZY UŻYCIU KARTY - INTERNET", "Podróże i wyjazdy", "Wynajem auta/campera");

        var local = classifier.classifyDecision("Bez kategorii", "PARKOMAT WROCLAW ZAKUP PRZY UŻYCIU KARTY W KRAJU", -20);

        assertThat(local.category()).isEqualTo("Transport i parking");
        assertThat(local.subcategory()).isEqualTo("Parking");
    }

    private void assertDecision(String title, String category, String subcategory) {
        var decision = classifier.classifyDecision("Bez kategorii", title, -100);

        assertThat(decision.category()).isEqualTo(category);
        assertThat(decision.subcategory()).isEqualTo(subcategory);
        assertThat(decision.reviewStatus()).isEqualTo("ok");
    }
}
