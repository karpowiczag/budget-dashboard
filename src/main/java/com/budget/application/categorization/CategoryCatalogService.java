package com.budget.application.categorization;

import com.budget.domain.category.Category;
import com.budget.domain.category.CategoryGroup;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/**
 * Use case over the user-owned category catalog (Phase 3b). Categories are fully editable, but the
 * bucket / area / fixedness / flow vocabularies stay fixed (they encode planning semantics): edits
 * are validated against the vocabularies derived from {@link BudgetTaxonomy}, and a category's group
 * must reference an existing {@link CategoryGroup}. Invalid input throws
 * {@link IllegalArgumentException} (mapped to HTTP 400).
 */
@Service
public class CategoryCatalogService {
    private static final Set<String> VALID_AREAS = distinct(BudgetTaxonomy.CategoryDefinition::area);
    private static final Set<String> VALID_BUCKETS = distinct(BudgetTaxonomy.CategoryDefinition::budgetBucketLabel);
    private static final Set<String> VALID_FIXEDNESS = distinct(BudgetTaxonomy.CategoryDefinition::fixedness);
    private static final Set<String> VALID_FLOWS = distinct(BudgetTaxonomy.CategoryDefinition::flowType);

    private final CategoryStore store;

    public CategoryCatalogService(CategoryStore store) {
        this.store = store;
    }

    public List<CategoryGroup> groups() {
        return store.groups();
    }

    public List<Category> categories() {
        return store.categories();
    }

    public void saveCategory(Category category) {
        if (category.categoryId() == null || category.categoryId().isBlank()) {
            throw new IllegalArgumentException("Category id is required");
        }
        if (category.label() == null || category.label().isBlank()) {
            throw new IllegalArgumentException("Category label is required");
        }
        requireMember("budget bucket", category.budgetBucket(), VALID_BUCKETS);
        requireMember("area", category.area(), VALID_AREAS);
        requireMember("fixedness", category.fixedness(), VALID_FIXEDNESS);
        requireMember("flow type", category.flowType(), VALID_FLOWS);
        if (category.groupId() != null && !category.groupId().isBlank()) {
            var groupIds = store.groups().stream().map(CategoryGroup::groupId).collect(Collectors.toSet());
            if (!groupIds.contains(category.groupId())) {
                throw new IllegalArgumentException("Unknown category group: " + category.groupId());
            }
        }
        store.saveCategory(category);
    }

    public void saveGroup(CategoryGroup group) {
        if (group.groupId() == null || group.groupId().isBlank()) {
            throw new IllegalArgumentException("Group id is required");
        }
        if (group.label() == null || group.label().isBlank()) {
            throw new IllegalArgumentException("Group label is required");
        }
        store.saveGroup(group);
    }

    public void archive(String categoryId) {
        store.archive(categoryId);
    }

    public void reorder(List<String> groupIdsInOrder, List<String> categoryIdsInOrder) {
        store.reorder(
                groupIdsInOrder == null ? List.of() : groupIdsInOrder,
                categoryIdsInOrder == null ? List.of() : categoryIdsInOrder);
    }

    private static void requireMember(String field, String value, Set<String> allowed) {
        if (value == null || !allowed.contains(value)) {
            throw new IllegalArgumentException("Invalid " + field + ": " + value + ". Allowed: " + allowed);
        }
    }

    private static Set<String> distinct(java.util.function.Function<BudgetTaxonomy.CategoryDefinition, String> field) {
        return BudgetTaxonomy.categories().stream().map(field).collect(Collectors.toUnmodifiableSet());
    }
}
