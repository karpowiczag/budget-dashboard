package com.budget.application.categorization;

public record CategoryDecision(
        String categoryId,
        String category,
        String subcategoryId,
        String subcategory,
        String flowType,
        String budgetGroupId,
        String budgetGroup,
        String reviewStatus,
        String reviewReason,
        String pattern,
        boolean matchedByTitle
) {
    static CategoryDecision prelim(String categoryId, String pattern, boolean matchedByTitle) {
        var category = BudgetTaxonomy.category(categoryId);
        return new CategoryDecision(
                category.id(),
                category.label(),
                "",
                "",
                category.flowType(),
                category.budgetGroupId(),
                BudgetTaxonomy.budgetGroupLabel(category.budgetGroupId()),
                BudgetTaxonomy.REVIEW_OK,
                "",
                pattern,
                matchedByTitle
        );
    }

    CategoryDecision withSubcategory(SubcategoryClassifier.SubcategoryDecision subcategory) {
        return new CategoryDecision(
                categoryId,
                category,
                subcategory.id(),
                subcategory.label(),
                flowType,
                budgetGroupId,
                budgetGroup,
                reviewStatus,
                reviewReason,
                pattern,
                matchedByTitle
        );
    }

    CategoryDecision withReview(String status, String reason) {
        return new CategoryDecision(
                categoryId,
                category,
                subcategoryId,
                subcategory,
                flowType,
                budgetGroupId,
                budgetGroup,
                status,
                reason,
                pattern,
                matchedByTitle
        );
    }
}
