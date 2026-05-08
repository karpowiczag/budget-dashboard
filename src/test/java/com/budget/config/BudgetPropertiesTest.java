package com.budget.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class BudgetPropertiesTest {
    @Test
    void appliesSafeDefaultsForMissingNestedConfig() {
        var properties = new BudgetProperties(null, null, null, null, null, null, null);

        assertThat(properties.security().oauthEnabled()).isFalse();
        assertThat(properties.security().allowedGoogleEmail()).isBlank();
        assertThat(properties.database().url()).isBlank();
        assertThat(properties.upload().maxBytes()).isEqualTo(12_582_912L);
        assertThat(properties.localImport().root()).isEqualTo(".");
        assertThat(properties.localImport().rebuildEnabled()).isTrue();
        assertThat(properties.fire().reportsPath()).isEqualTo("fire/investments_reports");
        assertThat(properties.fire().targetAge()).isEqualTo(50);
        assertThat(properties.categorization().personalRules()).isEmpty();
    }
}
