package com.budget.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class GoogleAccountAllowlistTest {
    @Test
    void acceptsOnlyConfiguredVerifiedEmail() {
        var properties = new BudgetProperties(
                new BudgetProperties.Security(true, "owner@example.test"),
                null,
                null,
                null,
                null,
                null
        );
        var allowlist = new GoogleAccountAllowlist(properties);

        assertThat(allowlist.allowed("owner@example.test", true)).isTrue();
        assertThat(allowlist.allowed("OWNER@example.test", true)).isTrue();
        assertThat(allowlist.allowed("other@example.test", true)).isFalse();
        assertThat(allowlist.allowed("owner@example.test", false)).isFalse();
        assertThat(allowlist.allowed("", true)).isFalse();
    }
}
