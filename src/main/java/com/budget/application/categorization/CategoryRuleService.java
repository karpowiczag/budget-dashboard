package com.budget.application.categorization;

import com.budget.domain.category.ClassificationRule;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import org.springframework.stereotype.Service;

/**
 * Use case over user-editable classification rules (Phase 3c-ii). Validates that the pattern compiles
 * and the target category exists before persisting; invalid input throws {@link
 * IllegalArgumentException} (mapped to HTTP 400). A saved rule only re-classifies transactions on the
 * next analyze/rebuild.
 */
@Service
public class CategoryRuleService {
    private final ClassificationRuleAdmin ruleAdmin;
    private final CategoryCatalog catalog;

    public CategoryRuleService(ClassificationRuleAdmin ruleAdmin, CategoryCatalog catalog) {
        this.ruleAdmin = ruleAdmin;
        this.catalog = catalog;
    }

    public List<ClassificationRule> rules() {
        return ruleAdmin.rawRules();
    }

    public void saveRule(ClassificationRule rule) {
        if (rule.pattern() == null || rule.pattern().isBlank()) {
            throw new IllegalArgumentException("Rule pattern is required");
        }
        try {
            Pattern.compile(rule.pattern());
        } catch (PatternSyntaxException e) {
            throw new IllegalArgumentException("Invalid rule pattern: " + e.getMessage());
        }
        // Throws IllegalArgumentException when the target category id is unknown.
        catalog.definitionById(rule.categoryId());
        ruleAdmin.saveRule(rule);
    }

    public void deleteRule(String ruleId) {
        ruleAdmin.deleteRule(ruleId);
    }
}
