package com.budget.domain.networth;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * A bank/cash account whose balance is tracked for net worth and emergency-fund
 * coverage. Balances are <em>derived</em>: {@code anchorBalance} is a known balance at
 * {@code anchorDate}, and the current balance is {@code anchorBalance} plus the net of all
 * transaction flows posted on this account after {@code anchorDate}. {@code anchorBalance}
 * and {@code anchorDate} are {@code null} until the user sets an opening balance.
 *
 * <p>{@code accountKey} equals the transaction {@code account} label, so derivation can
 * match flows directly.
 */
public record Account(
        String accountKey,
        String name,
        String kind,
        boolean liquid,
        boolean excludeFromNetWorth,
        BigDecimal anchorBalance,
        LocalDate anchorDate
) {
}
