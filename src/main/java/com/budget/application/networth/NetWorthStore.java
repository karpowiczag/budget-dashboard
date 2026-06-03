package com.budget.application.networth;

import com.budget.domain.networth.Account;
import com.budget.domain.networth.Liability;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Port for persisting account configuration and reading the transaction flows needed to
 * derive balances. Implemented by an adapter in {@code infrastructure.persistence.jdbc}.
 */
public interface NetWorthStore {
    /** Distinct account labels seen in imported transactions. */
    List<String> discoverAccountKeys();

    /** Saved account configuration (name, kind, liquidity, anchor). */
    List<Account> accounts();

    Account saveAccount(Account account);

    /** Net of signed transaction amounts for an account, posted strictly after the given date. */
    BigDecimal netFlowSince(String accountKey, LocalDate sinceExclusive);

    /** Net of signed transaction amounts posted in the range (afterExclusive, throughInclusive]. */
    BigDecimal netFlowBetween(String accountKey, LocalDate afterExclusive, LocalDate throughInclusive);

    List<Liability> liabilities();

    Liability saveLiability(Liability liability);

    void deleteLiability(String liabilityKey);
}
