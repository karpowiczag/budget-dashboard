package com.budget.domain.category;

/**
 * A user-owned category. The {@code categoryId} is a stable slug stored on every transaction;
 * everything else is editable. The bucket / area / fixedness / flow values must come from the fixed
 * planning vocabularies (validated in the application layer); {@code groupId} references a
 * {@link CategoryGroup}. {@code protectedFlag} avoids the reserved word {@code protected}.
 */
public record Category(
        String categoryId,
        String label,
        String area,
        String analyticsGroup,
        String groupId,
        String budgetBucket,
        String fixedness,
        String flowType,
        boolean discretionary,
        boolean excluded,
        boolean realIncome,
        boolean dailyPaced,
        boolean protectedFlag,
        boolean sinkingFundEligible,
        boolean archived,
        int sortOrder
) {
}
