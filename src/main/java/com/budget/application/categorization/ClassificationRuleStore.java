package com.budget.application.categorization;

import com.budget.domain.category.CategoryRule;
import java.util.List;

/**
 * Read-side seam over the classification rules driving the regex/title matcher (Phase 3c). Returns
 * the enabled rules compiled to {@link CategoryRule}, ordered by ascending priority (then id), so the
 * matcher takes the first match deterministically. Phase 3c-i backs this with the static
 * {@link BudgetTaxonomy} rules (behaviour-identical); a DB-backed implementation makes rules
 * user-editable. Patterns are compiled once by the implementation so the import hot path never
 * recompiles.
 */
public interface ClassificationRuleStore {
    List<CategoryRule> compiledRules();
}
