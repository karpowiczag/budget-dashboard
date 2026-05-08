package com.budget.config;

import com.budget.application.fire.FireSettings;
import java.nio.file.Path;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class FireConfiguration {
    @Bean
    FireSettings fireSettings(BudgetProperties properties) {
        var fire = properties.fire();
        return new FireSettings(
                Path.of(fire.reportsPath()),
                fire.currentAge(),
                fire.targetAge(),
                fire.safeWithdrawalRate(),
                fire.pessimisticRealReturn(),
                fire.expectedRealReturn(),
                fire.optimisticRealReturn(),
                fire.targetEquityShare(),
                fire.targetBondShare(),
                fire.targetCashShare(),
                fire.targetAlternativeShare(),
                fire.rebalanceBand()
        );
    }
}
