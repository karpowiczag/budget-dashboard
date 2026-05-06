package com.budget;

import com.budget.config.BudgetProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(BudgetProperties.class)
public class BudgetApplication {
    public static void main(String[] args) {
        SpringApplication.run(BudgetApplication.class, args);
    }
}
