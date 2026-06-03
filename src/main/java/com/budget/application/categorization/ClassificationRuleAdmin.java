package com.budget.application.categorization;

import com.budget.domain.category.ClassificationRule;
import java.util.List;

/**
 * Mutation/admin port for user-editable classification rules (Phase 3c-ii). Implemented by the same
 * infrastructure adapter as the read-side {@link ClassificationRuleStore}; kept separate so the
 * in-memory taxonomy store stays read-only. {@link #rawRules()} returns every rule (including
 * disabled ones) ordered by priority for the manager UI.
 */
public interface ClassificationRuleAdmin {
    List<ClassificationRule> rawRules();

    /** Create (blank ruleId) or update (existing ruleId) a rule. */
    void saveRule(ClassificationRule rule);

    void deleteRule(String ruleId);
}
