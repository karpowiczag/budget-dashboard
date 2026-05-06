package com.budget.config;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class GoogleAccountAllowlist {
    private final BudgetProperties properties;

    public GoogleAccountAllowlist(BudgetProperties properties) {
        this.properties = properties;
    }

    public boolean allowed(String email, boolean emailVerified) {
        var allowed = properties.security().allowedGoogleEmail();
        return StringUtils.hasText(allowed)
                && emailVerified
                && allowed.equalsIgnoreCase(email);
    }
}
