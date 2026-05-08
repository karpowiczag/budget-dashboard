package com.budget.application.fire;

import java.math.BigDecimal;
import java.nio.file.Path;

public record FireSettings(
        Path reportsPath,
        int currentAge,
        int targetAge,
        BigDecimal safeWithdrawalRate,
        BigDecimal pessimisticRealReturn,
        BigDecimal expectedRealReturn,
        BigDecimal optimisticRealReturn,
        BigDecimal targetEquityShare,
        BigDecimal targetBondShare,
        BigDecimal targetCashShare,
        BigDecimal targetAlternativeShare,
        BigDecimal rebalanceBand
) {
}
