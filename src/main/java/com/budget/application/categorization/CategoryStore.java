package com.budget.application.categorization;

import com.budget.domain.category.Category;
import com.budget.domain.category.CategoryGroup;
import java.util.List;

/**
 * Mutation port for the user-owned category catalog (Phase 3b). Implemented by an adapter in
 * infrastructure, which also implements the read-side {@link CategoryCatalog} over the same tables.
 * Reads return every row (including archived) ordered by sort order, so the manager UI can show and
 * un-hide archived categories.
 */
public interface CategoryStore {
    List<CategoryGroup> groups();

    List<Category> categories();

    /** Create or update a category by id (upsert). */
    void saveCategory(Category category);

    /** Create or update a group by id (upsert). */
    void saveGroup(CategoryGroup group);

    /** Hide a category (soft delete); never a hard delete in Phase 3b. */
    void archive(String categoryId);

    /** Apply an explicit display order to the given group and category ids. */
    void reorder(List<String> groupIdsInOrder, List<String> categoryIdsInOrder);
}
