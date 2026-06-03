package com.budget.application.networth;

import com.budget.domain.networth.Liability;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Use-case result: liquid accounts with derived balances, liabilities, the full net worth
 * (liquid + invested − liabilities) and emergency-fund coverage. Lives in the application
 * layer (like {@code FireSummary}); the web layer maps it to contract DTOs.
 */
public record NetWorthOverview(
        List<LiquidAccount> accounts,
        List<Liability> liabilities,
        BigDecimal liquidTotal,
        BigDecimal investedAssets,
        BigDecimal totalAssets,
        BigDecimal totalLiabilities,
        BigDecimal netWorth,
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
            BigDecimal derivedBalance,
            BigDecimal statementBalance,
            LocalDate statementDate,
            BigDecimal reconciledBalance,
            BigDecimal drift,
            Boolean reconciled
    ) {
    }
}
