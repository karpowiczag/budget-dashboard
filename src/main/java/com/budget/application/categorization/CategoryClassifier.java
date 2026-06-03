package com.budget.application.categorization;

import com.budget.domain.category.CategoryMatch;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class CategoryClassifier {
    private final List<CategoryRuleMatcher> matchers;
    private final RegexCategoryRuleMatcher regexMatcher;
    private final SubcategoryClassifier subcategoryClassifier;
    private final CategoryCatalog catalog;

    public CategoryClassifier() {
        this(PersonalCategoryRules.empty());
    }

    public CategoryClassifier(PersonalCategoryRules personalRules) {
        this(personalRules, new BudgetTaxonomyCatalog());
    }

    // Test convenience: explicit catalog (e.g. a renaming stub) with rules from the taxonomy + the
    // supplied personal rules.
    public CategoryClassifier(PersonalCategoryRules personalRules, CategoryCatalog catalog) {
        this(new RegexCategoryRuleMatcher(personalRules), new SubcategoryClassifier(), catalog);
    }

    @Autowired
    public CategoryClassifier(CategoryCatalog catalog, ClassificationRuleStore ruleStore) {
        this(new RegexCategoryRuleMatcher(ruleStore), new SubcategoryClassifier(), catalog);
    }

    CategoryClassifier(RegexCategoryRuleMatcher regexMatcher, SubcategoryClassifier subcategoryClassifier) {
        this(regexMatcher, subcategoryClassifier, new BudgetTaxonomyCatalog());
    }

    CategoryClassifier(RegexCategoryRuleMatcher regexMatcher, SubcategoryClassifier subcategoryClassifier, CategoryCatalog catalog) {
        this.regexMatcher = regexMatcher;
        this.subcategoryClassifier = subcategoryClassifier;
        this.catalog = catalog;
        this.matchers = List.of(
                new PositiveEmployerIncomeMatcher(),
                new BankCategoryRuleMatcher(),
                regexMatcher,
                new PositiveFlowCategoryMatcher(),
                new FallbackCategoryMatcher()
        );
    }

    public CategoryMatch matchRule(String description) {
        return regexMatcher.match(new CategoryInput("", description, BigDecimal.ZERO))
                .map(decision -> new CategoryMatch(decision.category(), decision.pattern()))
                .orElseGet(CategoryMatch::none);
    }

    public String classify(String bankCategory, String description, double amount) {
        return classify(bankCategory, description, BigDecimal.valueOf(amount));
    }

    public String classify(String bankCategory, String description, BigDecimal amount) {
        return classifyDecision(bankCategory, description, amount).category();
    }

    public CategoryDecision classifyDecision(String bankCategory, String description, double amount) {
        return classifyDecision(bankCategory, description, BigDecimal.valueOf(amount));
    }

    public CategoryDecision classifyDecision(String bankCategory, String description, BigDecimal amount) {
        var input = new CategoryInput(bankCategory, description, amount);
        return matchers.stream()
                .map(matcher -> matcher.match(input))
                .flatMap(java.util.Optional::stream)
                .map(decision -> enrich(decision, description))
                .findFirst()
                .orElseThrow();
    }

    private CategoryDecision enrich(CategoryDecision decision, String description) {
        var subcategory = subcategoryClassifier.subcategory(decision.categoryId(), description);
        var enriched = decision.withSubcategory(subcategory);
        if (BudgetTaxonomy.CATEGORY_UNKNOWN.equals(decision.categoryId())) {
            return enriched.withReview(BudgetTaxonomy.REVIEW_NEEDS_REVIEW, "Brak deterministycznej reguły kategorii");
        }
        if (BudgetTaxonomy.CATEGORY_MARKETPLACE.equals(decision.categoryId())) {
            return enriched.withReview(BudgetTaxonomy.REVIEW_NEEDS_SPLIT, "Marketplace wymaga rozbicia po historii zamówienia");
        }
        return enriched;
    }

    public String budgetArea(String category) {
        return metadata(category).area();
    }

    public String group(String category) {
        return metadata(category).analyticsGroup();
    }

    public String budgetBucket(String category) {
        return metadata(category).budgetBucketLabel();
    }

    public String fixedness(String category) {
        return metadata(category).fixedness();
    }

    public String subcategory(String category, String description) {
        return subcategoryClassifier.subcategory(catalog.categoryIdByLabel(category), description).label();
    }

    public boolean isDiscretionary(String category) {
        return metadata(category).discretionary();
    }

    public boolean isExcluded(String category) {
        return metadata(category).excluded();
    }

    public boolean isRealIncome(String category) {
        return metadata(category).realIncome();
    }

    public boolean isDailyPaced(String category) {
        return catalog.isDailyPaced(category);
    }

    public Set<String> wealthCategoryLabels() {
        return catalog.wealthCategoryLabels();
    }

    // --- Stable-id lookups (rename-safe). Analysis groups by category id and resolves the
    // current display label through these, so a rename never splits a category. ---

    public String labelForId(String categoryId) {
        return catalog.definitionById(categoryId).label();
    }

    public String categoryIdByLabel(String category) {
        return catalog.categoryIdByLabel(category);
    }

    public String budgetAreaById(String categoryId) {
        return catalog.definitionById(categoryId).area();
    }

    public String groupById(String categoryId) {
        return catalog.definitionById(categoryId).analyticsGroup();
    }

    public String budgetBucketById(String categoryId) {
        return catalog.definitionById(categoryId).budgetBucketLabel();
    }

    public String fixednessById(String categoryId) {
        return catalog.definitionById(categoryId).fixedness();
    }

    public boolean isDiscretionaryById(String categoryId) {
        return catalog.definitionById(categoryId).discretionary();
    }

    public boolean isExcludedById(String categoryId) {
        return catalog.definitionById(categoryId).excluded();
    }

    public boolean isRealIncomeById(String categoryId) {
        return catalog.definitionById(categoryId).realIncome();
    }

    public boolean isDailyPacedById(String categoryId) {
        return catalog.isDailyPacedById(categoryId);
    }

    public Set<String> wealthCategoryIds() {
        return catalog.wealthCategoryIds();
    }

    private BudgetTaxonomy.CategoryDefinition metadata(String category) {
        return catalog.definitionByLabel(category);
    }
}
