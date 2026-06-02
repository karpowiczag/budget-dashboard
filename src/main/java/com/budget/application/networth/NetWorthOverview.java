package com.budget.application.networth;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Use-case result: the liquid accounts with their derived balances plus emergency-fund
 * coverage. Lives in the application layer (like {@code FireSummary}); the web layer maps
 * it to contract DTOs.
 */
public record NetWorthOverview(
        List<LiquidAccount> accounts,
        BigDecimal liquidTotal,
        BigDecimal emergencyFundMin,
        BigDecimal emergencyFundComfort,
        BigDecimal emergencyProgressComfort
) {
    public record LiquidAccount(
            String accountKey,
            String name,
            String kind,
            boolean liquid,
            boolean excludeFromNetWorth,
            boolean configured,
            BigDecimal anchorBalance,
            LocalDate anchorDate,
            BigDecimal netFlowSinceAnchor,
            BigDecimal derivedBalance
    ) {
    }
}
