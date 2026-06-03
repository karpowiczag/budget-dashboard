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
 * <p>{@code statementBalance} at {@code statementDate} is an optional later, independently
 * observed balance (e.g. from a bank statement) used for reconciliation: the derived
 * balance at that date is compared against it and drift is flagged.
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
        LocalDate anchorDate,
        BigDecimal statementBalance,
        LocalDate statementDate
) {
}
