package com.budget.config;

import com.budget.application.settings.BudgetSettingsDefaults;
import java.math.BigDecimal;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class BudgetSettingsConfiguration {
    @Bean
    BudgetSettingsDefaults budgetSettingsDefaults(BudgetProperties properties) {
        var settings = properties.budgetSettings();
        return new BudgetSettingsDefaults(
                BigDecimal.valueOf(settings.targetMonthlySpend()),
                BigDecimal.valueOf(settings.aggressiveMonthlySpend()),
                settings.emergencyFundMinMonths(),
                settings.emergencyFundComfortMonths()
        );
    }
}
