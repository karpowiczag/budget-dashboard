package com.budget.config;

import com.budget.application.importing.ImportSettings;
import java.nio.file.Path;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class ImportConfiguration {
    @Bean
    ImportSettings importSettings(BudgetProperties properties) {
        return new ImportSettings(properties.upload().maxBytes(), Path.of(properties.localImport().root()));
    }
}
