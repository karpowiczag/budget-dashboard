package com.budget.application.categorization;

import com.budget.application.importing.BudgetImportService;
import com.budget.domain.category.Category;
import com.budget.domain.category.CategoryGroup;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
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
    // Hardcoded floor of ids that hardcoded matchers/analysis reproduce on rebuild — undeletable even
    // if the builtin column were somehow wrong. The builtin flag is the primary guard; this backstops
    // the crash-on-rebuild producer ids specifically: salary (employer-income/bank-category matcher),
    // refundCorrection (positive-flow catch-all), unknownReview (fallback), marketplaceOnline (enrich
    // split), groceries/diningOut (hardcoded daily-paced set), plus the wealth ids used by analysis.
    private static final Set<String> STRUCTURAL_FLOOR = Stream.concat(
            BudgetTaxonomy.wealthCategoryIds().stream(),
            Stream.of(BudgetTaxonomy.CATEGORY_UNKNOWN, BudgetTaxonomy.CATEGORY_MARKETPLACE,
                    "salary", "refundCorrection", "groceries", "diningOut")
    ).collect(Collectors.toUnmodifiableSet());

    private final CategoryStore store;
    private final BudgetImportService importService;

    public CategoryCatalogService(CategoryStore store, BudgetImportService importService) {
        this.store = store;
        this.importService = importService;
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

    /**
     * Hard-delete a user category, reassigning its rules/transactions/overrides onto {@code reassignToId}
     * (merge). Built-in / structurally-required categories are rejected (archive them instead). After the
     * cascade commits, the affected report years are re-analyzed so dashboards reflect the merge.
     */
    public void delete(String categoryId, String reassignToId) {
        if (categoryId == null || categoryId.isBlank()) {
            throw new IllegalArgumentException("Category id is required");
        }
        var source = store.categories().stream()
                .filter(category -> category.categoryId().equals(categoryId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown category: " + categoryId));
        if (source.builtin() || STRUCTURAL_FLOOR.contains(categoryId)) {
            throw new IllegalArgumentException("Kategoria jest wymagana przez system; zarchiwizuj ją zamiast usuwać");
        }
        if (reassignToId == null || reassignToId.isBlank()) {
            throw new IllegalArgumentException("Reassignment target is required to delete a category");
        }
        if (reassignToId.equals(categoryId)) {
            throw new IllegalArgumentException("Reassignment target must differ from the deleted category");
        }
        var target = store.categories().stream()
                .filter(category -> category.categoryId().equals(reassignToId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown reassignment target: " + reassignToId));
        if (target.archived()) {
            throw new IllegalArgumentException("Reassignment target is archived");
        }
        var affectedYears = store.delete(categoryId, reassignToId);
        for (var year : affectedYears) {
            importService.reanalyzeYear(year);
        }
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
