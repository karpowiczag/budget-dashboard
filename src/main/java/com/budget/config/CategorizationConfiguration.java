package com.budget.config;

import com.budget.application.categorization.PersonalCategoryRules;
import com.budget.domain.category.CategoryRule;
import java.util.regex.Pattern;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class CategorizationConfiguration {
    @Bean
    PersonalCategoryRules personalCategoryRules(BudgetProperties properties) {
        var rules = properties.categorization().personalRules().stream()
                .filter(rule -> !rule.pattern().isBlank() && !rule.category().isBlank())
                .map(rule -> new CategoryRule(
                        Pattern.compile(rule.pattern(), Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE),
                        rule.category(),
                        "personal:" + rule.pattern()
                ))
                .toList();
        return new PersonalCategoryRules(rules);
    }
}
