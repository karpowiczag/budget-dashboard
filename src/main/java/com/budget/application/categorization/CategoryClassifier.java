package com.budget.application.categorization;

import com.budget.domain.category.CategoryMatch;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class CategoryClassifier {
    private final List<CategoryRuleMatcher> matchers;
    private final RegexCategoryRuleMatcher regexMatcher;
    private final SubcategoryClassifier subcategoryClassifier;

    public CategoryClassifier() {
        this(PersonalCategoryRules.empty());
    }

    public CategoryClassifier(PersonalCategoryRules personalRules) {
        this(new RegexCategoryRuleMatcher(personalRules), new SubcategoryClassifier());
    }

    CategoryClassifier(RegexCategoryRuleMatcher regexMatcher, SubcategoryClassifier subcategoryClassifier) {
        this.regexMatcher = regexMatcher;
        this.subcategoryClassifier = subcategoryClassifier;
        this.matchers = List.of(
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
                .findFirst()
                .orElseThrow();
    }

    public String budgetArea(String category) {
        return BudgetCatalog.BUDGET_AREAS.getOrDefault(category, "Inne");
    }

    public String group(String category) {
        return BudgetCatalog.GROUPS.getOrDefault(category, "Inne");
    }

    public String budgetBucket(String category) {
        return BudgetCatalog.BUDGET_BUCKETS.getOrDefault(category, "Inne");
    }

    public String fixedness(String category) {
        if (BudgetCatalog.FIXEDNESS.containsKey(category)) {
            return BudgetCatalog.FIXEDNESS.get(category);
        }
        if (BudgetCatalog.DISCRETIONARY.contains(category)) {
            return "Uznaniowe";
        }
        if (BudgetCatalog.EXCLUDED.contains(category)) {
            return "Transfer/wyłączone";
        }
        return "Do oceny";
    }

    public String subcategory(String category, String description) {
        return subcategoryClassifier.subcategory(category, description);
    }

    public boolean isDiscretionary(String category) {
        return BudgetCatalog.DISCRETIONARY.contains(category);
    }

    public boolean isExcluded(String category) {
        return BudgetCatalog.EXCLUDED.contains(category);
    }

    public boolean isRealIncome(String category) {
        return BudgetCatalog.INCOME_CATEGORIES.contains(category);
    }
}
