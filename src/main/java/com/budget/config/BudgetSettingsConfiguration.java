package com.budget.config;

import com.budget.application.settings.BudgetSettingsDefaults;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class BudgetSettingsConfiguration {
    @Bean
    BudgetSettingsDefaults budgetSettingsDefaults(BudgetProperties properties) {
        var settings = properties.budgetSettings();
        return new BudgetSettingsDefaults(
                settings.targetMonthlySpend(),
                settings.aggressiveMonthlySpend(),
                settings.emergencyFundMinMonths(),
                settings.emergencyFundComfortMonths()
        );
    }
}
