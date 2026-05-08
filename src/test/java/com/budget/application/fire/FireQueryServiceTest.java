package com.budget.application.fire;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.budget.application.reporting.BudgetReportStore;
import com.budget.application.reporting.YearSummary;
import com.budget.domain.fire.FirePortfolioPosition;
import com.budget.domain.fire.FirePortfolioSnapshot;
import com.budget.domain.report.BudgetSnapshot;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
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
                null,
                null,
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

        var summary = new FireQueryService(portfolioReader, budgetStore, new FireSettingsService(settings, new MemoryStore())).summary();

        assertThat(summary.reportsLoaded()).isTrue();
        assertThat(summary.currentPortfolioValue()).isEqualByComparingTo("200000.00");
        assertThat(summary.retirementLockedValue()).isEqualByComparingTo("50000.00");
        assertThat(summary.liquidFireCapital()).isEqualByComparingTo("150000.00");
        assertThat(summary.fireNumber()).isEqualByComparingTo("4800000.00");
        assertThat(summary.contributionPlan().annualIkeCapacityForHousehold()).isEqualByComparingTo("56520");
        assertThat(summary.withdrawalPlan().estimatedTaxReserve()).isEqualByComparingTo("0.00");
        assertThat(summary.dataQuality().status()).isEqualTo("ok");
        assertThat(summary.scenarios()).hasSize(3);
        assertThat(summary.allocation()).extracting(FireSummary.FireAllocation::assetClass).contains("Akcje", "Obligacje");
        assertThat(summary.allocation()).extracting(FireSummary.FireAllocation::assetClass).doesNotContain("Gotówka");
        assertThat(summary.wrappers()).extracting(FireSummary.FireWrapper::wrapper).contains("Poduszka bezpieczeństwa");
        assertThat(summary.legalRules()).extracting(FireSummary.FireLegalRule::id).contains("ike-limit", "ikze-limit", "zus-age");
    }

    @Test
    void linksHouseholdBudgetAndExcludesLoanOverpaymentsFromFireContribution() throws Exception {
        var settings = defaultSettings();
        when(portfolioReader.read(settings.reportsPath())).thenReturn(new FirePortfolioSnapshot(
                LocalDate.parse("2026-05-08"),
                List.of(position("Akcje", "Rachunek opodatkowany", "200000")),
                List.of("fake.csv")
        ));
        when(budgetStore.findYears()).thenReturn(List.of(new YearSummary(2026, OffsetDateTime.parse("2026-05-08T10:00:00Z"), 100, new BigDecimal("100000"), new BigDecimal("50000"), "local.csv")));
        when(budgetStore.findDashboard(2026)).thenReturn(budgetSnapshot());

        var summary = new FireQueryService(portfolioReader, budgetStore, new FireSettingsService(settings, new MemoryStore())).summary();

        assertThat(summary.budgetLink().linked()).isTrue();
        assertThat(summary.budgetLink().budgetYear()).isEqualTo(2026);
        assertThat(summary.monthlySpendTarget()).isEqualByComparingTo("14000.00");
        assertThat(summary.budgetLink().targetMonthlySpend()).isEqualByComparingTo("12000.00");
        assertThat(summary.currentMonthlyWealthContribution()).isEqualByComparingTo("9000.00");
        assertThat(summary.budgetLink().actualMonthlyInvestments()).isEqualByComparingTo("8000.00");
        assertThat(summary.budgetLink().savingsAccountMonthlyNet()).isEqualByComparingTo("1000.00");
        assertThat(summary.budgetLink().loanOverpaymentMonthly()).isEqualByComparingTo("4000.00");
        assertThat(summary.contributionPlan().recommendation()).contains("Nadpłaty kredytu");
    }

    @Test
    void keepsFireSpendTargetIndependentFromHouseholdBudgetTarget() throws Exception {
        var settings = new FireSettings(
                Path.of("fire/investments_reports"),
                36,
                50,
                new BigDecimal("9500.00"),
                null,
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
                List.of(position("Akcje", "Rachunek opodatkowany", "200000")),
                List.of("fake.csv")
        ));
        when(budgetStore.findYears()).thenReturn(List.of(new YearSummary(2026, OffsetDateTime.parse("2026-05-08T10:00:00Z"), 100, new BigDecimal("100000"), new BigDecimal("50000"), "local.csv")));
        when(budgetStore.findDashboard(2026)).thenReturn(budgetSnapshot());

        var summary = new FireQueryService(portfolioReader, budgetStore, new FireSettingsService(settings, new MemoryStore())).summary();

        assertThat(summary.monthlySpendTarget()).isEqualByComparingTo("9500.00");
        assertThat(summary.budgetLink().targetMonthlySpend()).isEqualByComparingTo("12000.00");
        assertThat(summary.budgetLink().firePortfolioMonthlyContribution()).isEqualByComparingTo("9000.00");
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

    private FireSettings defaultSettings() {
        return new FireSettings(
                Path.of("fire/investments_reports"),
                36,
                50,
                null,
                null,
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
    }

    private BudgetSnapshot budgetSnapshot() {
        return new BudgetSnapshot(
                2026,
                "2026-01-01 - 2026-05-08",
                LocalDate.parse("2026-01-01"),
                LocalDate.parse("2026-05-08"),
                5,
                new BudgetSnapshot.Kpis(
                        new BigDecimal("100000.00"),
                        new BigDecimal("50000.00"),
                        BigDecimal.ZERO,
                        new BigDecimal("50000.00"),
                        new BigDecimal("0.50"),
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        new BigDecimal("65000.00"),
                        new BigDecimal("5000.00"),
                        100,
                        0,
                        0,
                        0,
                        BigDecimal.ZERO,
                        new BigDecimal("5000.00"),
                        new BigDecimal("10000.00"),
                        new BigDecimal("15000.00"),
                        new BigDecimal("10000.00")
                ),
                List.of(),
                List.of(),
                List.of(),
                List.of(
                        new BudgetSnapshot.BudgetMixItem("Inwestycje", new BigDecimal("40000.00"), new BigDecimal("8000.00"), BigDecimal.ZERO, ""),
                        new BudgetSnapshot.BudgetMixItem("Konto oszczędnościowe", new BigDecimal("10000.00"), new BigDecimal("2000.00"), BigDecimal.ZERO, ""),
                        new BudgetSnapshot.BudgetMixItem("Nadpłata kredytu", new BigDecimal("20000.00"), new BigDecimal("4000.00"), BigDecimal.ZERO, "")
                ),
                new BudgetSnapshot.SavingsPlan(
                        new BigDecimal("10000.00"),
                        new BigDecimal("20000.00"),
                        new BigDecimal("7000.00"),
                        new BigDecimal("12000.00"),
                        new BigDecimal("11000.00"),
                        new BigDecimal("8000.00"),
                        new BigDecimal("9000.00"),
                        BigDecimal.ZERO,
                        new BigDecimal("21000.00"),
                        new BigDecimal("42000.00"),
                        List.of(),
                        List.of()
                ),
                null,
                List.of(),
                List.of(),
                List.of(),
                List.of()
        );
    }

    private static final class MemoryStore implements FireSettingsStore {
        @Override
        public Optional<FireSettings> findDefaultSettings() {
            return Optional.empty();
        }

        @Override
        public FireSettings saveDefaultSettings(FireSettings settings) {
            return settings;
        }
    }
}
