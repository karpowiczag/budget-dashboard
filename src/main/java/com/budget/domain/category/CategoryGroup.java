package com.budget.domain.category;

/**
 * A user-owned budget group — the planning grouping a category belongs to. The {@code groupId} is a
 * stable slug; only the {@code label} is user-editable (rename is safe because categories reference
 * the id).
 */
public record CategoryGroup(
        String groupId,
        String label,
        int sortOrder
) {
}
