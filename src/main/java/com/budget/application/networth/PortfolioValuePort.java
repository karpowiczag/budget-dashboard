package com.budget.application.networth;

import java.math.BigDecimal;

/**
 * Supplies the household's current invested capital (e.g. the MyFund portfolio value) to
 * the net-worth calculation, without coupling net worth to the FIRE module's internals.
 * Implemented by {@code FireQueryService}; returns zero when no portfolio data is loaded.
 */
public interface PortfolioValuePort {
    BigDecimal currentPortfolioValue();
}
