package com.budget.application.fire;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.budget.application.reporting.BudgetReportStore;
import com.budget.domain.fire.FirePortfolioPosition;
import com.budget.domain.fire.FirePortfolioSnapshot;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.Mock;

@ExtendWith(MockitoExtension.class)
class FireQueryServiceTest {
    @Mock
    FirePortfolioReader portfolioReader;

    @Mock
    BudgetReportStore budgetStore;

    @Test
    void buildsFireSummaryFromPortfolioSnapshotWithoutBudgetData() throws Exception {
        var settings = new FireSettings(
                Path.of("fire/investments_reports"),
                36,
                50,
                BigDecimal.valueOf(0.035),
                BigDecimal.valueOf(0.02),
                BigDecimal.valueOf(0.04),
                BigDecimal.valueOf(0.055),
                BigDecimal.valueOf(0.80),
                BigDecimal.valueOf(0.10),
                BigDecimal.valueOf(0.05),
                BigDecimal.valueOf(0.05),
                BigDecimal.valueOf(0.05)
        );
        when(portfolioReader.read(settings.reportsPath())).thenReturn(new FirePortfolioSnapshot(
                LocalDate.parse("2026-05-08"),
                List.of(
                        position("Akcje", "Rachunek opodatkowany", "120000"),
                        position("Gotówka", "Poduszka bezpieczeństwa", "30000"),
                        position("Obligacje", "Emerytalne długoterminowe", "50000")
                ),
                List.of("fake.csv")
        ));
        when(budgetStore.findYears()).thenReturn(List.of());

        var summary = new FireQueryService(portfolioReader, budgetStore, settings).summary();

        assertThat(summary.reportsLoaded()).isTrue();
        assertThat(summary.currentPortfolioValue()).isEqualByComparingTo("200000.00");
        assertThat(summary.retirementLockedValue()).isEqualByComparingTo("50000.00");
        assertThat(summary.liquidFireCapital()).isEqualByComparingTo("150000.00");
        assertThat(summary.fireNumber()).isEqualByComparingTo("4800000.00");
        assertThat(summary.scenarios()).hasSize(3);
        assertThat(summary.allocation()).extracting(FireSummary.FireAllocation::assetClass).contains("Akcje", "Gotówka", "Obligacje");
        assertThat(summary.legalRules()).extracting(FireSummary.FireLegalRule::id).contains("ike-limit", "ikze-limit", "zus-age");
    }

    private FirePortfolioPosition position(String assetClass, String wrapper, String value) {
        return new FirePortfolioPosition(
                "fake.csv",
                "Test",
                wrapper,
                assetClass,
                assetClass + " instrument",
                assetClass,
                "Test account",
                "PLN",
                "",
                LocalDate.parse("2026-05-08"),
                BigDecimal.ONE,
                new BigDecimal(value).setScale(2),
                new BigDecimal(value).setScale(2),
                BigDecimal.ZERO.setScale(2),
                BigDecimal.ZERO.setScale(2)
        );
    }
}
