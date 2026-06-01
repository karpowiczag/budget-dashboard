package com.budget.domain.fire;

import java.math.BigDecimal;
import java.time.LocalDate;

public record FirePortfolioPosition(
        String sourceFile,
        String portfolio,
        String wrapper,
        String assetClass,
        String instrument,
        String group,
        String account,
        String currency,
        String isin,
        LocalDate priceDate,
        BigDecimal quantity,
        BigDecimal costBasisPln,
        BigDecimal valuePln,
        BigDecimal gainPln,
        BigDecimal returnPct
) {
}
