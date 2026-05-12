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
        var settings = settingsWithSpend("14000.00");
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
        assertThat(summary.allocation()).extracting(FireSummary.FireAllocation::assetClass).contains("Akcje", "Obligacje", "Gotówka", "Alternatywne");
        assertThat(summary.allocation()).filteredOn(row -> row.assetClass().equals("Gotówka"))
                .singleElement()
                .satisfies(row -> {
                    assertThat(row.value()).isEqualByComparingTo("0.00");
                    assertThat(row.targetShare()).isEqualByComparingTo("0.05");
                    assertThat(row.status()).isEqualTo("OK");
                });
        assertThat(summary.rebalancing()).filteredOn(row -> row.assetClass().equals("Gotówka"))
                .singleElement()
                .satisfies(row -> {
                    assertThat(row.amountToTarget()).isEqualByComparingTo("8500.00");
                    assertThat(row.action()).isEqualTo("Doważyć nowymi wpłatami");
                });
        assertThat(summary.wrappers()).extracting(FireSummary.FireWrapper::wrapper).contains("Poduszka bezpieczeństwa");
        assertThat(summary.portfolios()).extracting(FireSummary.FirePortfolioBreakdown::portfolio).contains("Test");
        assertThat(summary.portfolios()).singleElement().satisfies(portfolio -> {
            assertThat(portfolio.value()).isEqualByComparingTo("200000.00");
            assertThat(portfolio.role()).isEqualTo("Mieszany");
        });
        assertThat(summary.risks()).extracting(FireSummary.FireRisk::id).contains("singlePositionConcentration");
        assertThat(summary.positionAnalyses()).hasSize(3);
        assertThat(summary.positionAnalyses()).extracting(FireSummary.FirePositionAnalysis::instrument)
                .contains("Akcje instrument", "Gotówka instrument", "Obligacje instrument");
        assertThat(summary.positionAnalyses().stream()
                .filter(row -> row.instrument().equals("Akcje instrument"))
                .findFirst()
                .orElseThrow()
                .decision()).contains("Nie zwiększaj ekspozycji");
        assertThat(summary.positionAnalyses().stream()
                .filter(row -> row.instrument().equals("Gotówka instrument"))
                .findFirst()
                .orElseThrow()
                .perspective()).contains("Płynność");
        assertThat(summary.legalRules()).extracting(FireSummary.FireLegalRule::id).contains("ike-limit", "ikze-limit", "zus-age");
    }

    @Test
    void linksHouseholdBudgetAndExcludesLoanOverpaymentsFromFireContribution() throws Exception {
        var settings = settingsWithSpend("14000.00");
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
    void requiresExplicitFireSpendTargetInsteadOfGuessingFromBudget() throws Exception {
        var settings = defaultSettings();
        when(portfolioReader.read(settings.reportsPath())).thenReturn(new FirePortfolioSnapshot(
                LocalDate.parse("2026-05-08"),
                List.of(position("Akcje", "Rachunek opodatkowany", "200000")),
                List.of("fake.csv")
        ));
        when(budgetStore.findYears()).thenReturn(List.of(new YearSummary(2026, OffsetDateTime.parse("2026-05-08T10:00:00Z"), 100, new BigDecimal("100000"), new BigDecimal("50000"), "local.csv")));
        when(budgetStore.findDashboard(2026)).thenReturn(budgetSnapshot());

        var summary = new FireQueryService(portfolioReader, budgetStore, new FireSettingsService(settings, new MemoryStore())).summary();

        assertThat(summary.spendTargetConfigured()).isFalse();
        assertThat(summary.monthlySpendTarget()).isEqualByComparingTo("0.00");
        assertThat(summary.fireNumber()).isEqualByComparingTo("0.00");
        assertThat(summary.scenarios()).isEmpty();
        assertThat(summary.budgetLink().targetMonthlySpend()).isEqualByComparingTo("12000.00");
        assertThat(summary.budgetLink().firePortfolioMonthlyContribution()).isEqualByComparingTo("9000.00");
        assertThat(summary.actionItems()).extracting(FireSummary.FireActionItem::title).contains("Ustaw miesięczny cel wydatków FIRE");
        assertThat(summary.risks()).extracting(FireSummary.FireRisk::id).contains("fireTargetMissing");
    }

    @Test
    void flagsPortfolioRisksFromCurrentAllocation() throws Exception {
        var settings = settingsWithSpend("10000.00");
        when(portfolioReader.read(settings.reportsPath())).thenReturn(new FirePortfolioSnapshot(
                LocalDate.parse("2026-05-08"),
                List.of(
                        position("Inne", "Rachunek opodatkowany", "180000", "iShares Core MSCI World UCITS ETF", "ETF-y zagraniczne", "EUR"),
                        position("Obligacje", "Rachunek opodatkowany", "20000")
                ),
                List.of("fake.csv")
        ));
        when(budgetStore.findYears()).thenReturn(List.of());

        var summary = new FireQueryService(portfolioReader, budgetStore, new FireSettingsService(settings, new MemoryStore())).summary();

        assertThat(summary.risks()).extracting(FireSummary.FireRisk::id)
                .contains("equityConcentration", "rebalanceDrift", "singlePositionConcentration");
        assertThat(summary.risks().stream()
                .filter(risk -> risk.id().equals("equityConcentration"))
                .findFirst()
                .orElseThrow()
                .level()).isEqualTo("high");
        assertThat(summary.positionAnalyses().stream()
                .filter(row -> row.assetClass().equals("Akcje"))
                .findFirst()
                .orElseThrow()
                .riskLevel()).isEqualTo("high");
        assertThat(summary.positionAnalyses().stream()
                .filter(row -> row.assetClass().equals("Akcje"))
                .findFirst()
                .orElseThrow()
                .checklist()).anyMatch(item -> item.contains("KID"));
        assertThat(summary.positionAnalyses().stream()
                .filter(row -> row.assetClass().equals("Akcje"))
                .findFirst()
                .orElseThrow()
                .instrumentType()).isEqualTo("ETF akcyjny szerokiego rynku");
    }

    @Test
    void normalizesMyFundInstrumentTaxonomyIntoFireRoles() throws Exception {
        var settings = settingsWithSpend("10000.00");
        when(portfolioReader.read(settings.reportsPath())).thenReturn(new FirePortfolioSnapshot(
                LocalDate.parse("2026-05-08"),
                List.of(
                        position("Inne", "Emerytalne długoterminowe", "50000", "Global UCITS ETF", "ETF-y zagraniczne", "EUR"),
                        position("Inne", "Emerytalne długoterminowe", "30000", "Vanguard LifeStrategy 80% Equity UCITS ETF", "ETF-y zagraniczne", "EUR"),
                        position("Inne", "Rachunek opodatkowany", "10000", "Gold ETC", "ETC zagraniczne", "USD"),
                        position("Inne", "Poduszka bezpieczeństwa", "20000", "Konto oszczędnościowe", "Konta oszczędnościowe", "PLN")
                ),
                List.of("fake.csv")
        ));
        when(budgetStore.findYears()).thenReturn(List.of());

        var summary = new FireQueryService(portfolioReader, budgetStore, new FireSettingsService(settings, new MemoryStore())).summary();

        assertThat(summary.allocation()).extracting(FireSummary.FireAllocation::assetClass)
                .contains("Akcje", "Alternatywne", "Mieszane")
                .doesNotContain("Inne");
        assertThat(summary.positionAnalyses()).filteredOn(row -> row.instrument().equals("Global UCITS ETF"))
                .singleElement()
                .satisfies(row -> {
                    assertThat(row.assetClass()).isEqualTo("Akcje");
                    assertThat(row.instrumentType()).isEqualTo("ETF akcyjny szerokiego rynku");
                    assertThat(row.fireRole()).isEqualTo("Kapitał po 60/65");
                    assertThat(row.decision()).contains("po 60/65");
                    assertThat(row.riskDrivers()).anyMatch(driver -> driver.contains("Ograniczona dostępność"));
                });
        assertThat(summary.positionAnalyses()).filteredOn(row -> row.instrument().equals("Vanguard LifeStrategy 80% Equity UCITS ETF"))
                .singleElement()
                .satisfies(row -> {
                    assertThat(row.assetClass()).isEqualTo("Mieszane");
                    assertThat(row.instrumentType()).isEqualTo("ETF mieszany 80/20");
                    assertThat(row.reviewFocus()).isEqualTo("Look-through");
                    assertThat(row.decision()).contains("look-through");
                    assertThat(row.riskDrivers()).anyMatch(driver -> driver.contains("Produkt mieszany"));
                });
        assertThat(summary.risks()).extracting(FireSummary.FireRisk::id).contains("mixedFundLookThrough");
        assertThat(summary.rebalancing()).filteredOn(row -> row.assetClass().equals("Mieszane"))
                .singleElement()
                .satisfies(row -> {
                    assertThat(row.action()).contains("look-through");
                    assertThat(row.amountToTarget()).isEqualByComparingTo("0.00");
                });
        assertThat(summary.positionAnalyses()).filteredOn(row -> row.instrument().equals("Gold ETC"))
                .singleElement()
                .satisfies(row -> {
                    assertThat(row.assetClass()).isEqualTo("Alternatywne");
                    assertThat(row.instrumentType()).isEqualTo("ETC/surowiec");
                    assertThat(row.fireRole()).isEqualTo("Satelita dywersyfikacyjny");
                });
        assertThat(summary.positionAnalyses()).filteredOn(row -> row.instrument().equals("Konto oszczędnościowe"))
                .singleElement()
                .satisfies(row -> {
                    assertThat(row.assetClass()).isEqualTo("Gotówka");
                    assertThat(row.riskLevel()).isEqualTo("low");
                    assertThat(row.decision()).contains("poduszki");
                });
        assertThat(summary.dataQuality().unknownAssetClassCount()).isZero();
    }

    @Test
    void keepsSeparateMyFundPortfoliosVisibleInFireSummary() throws Exception {
        var settings = settingsWithSpend("10000.00");
        when(portfolioReader.read(settings.reportsPath())).thenReturn(new FirePortfolioSnapshot(
                LocalDate.parse("2026-05-08"),
                List.of(
                        position("Akcje", "Emerytalne długoterminowe", "60000", "ETF emerytalny", "ETF-y zagraniczne", "EUR", "Emerytura Aleksander"),
                        position("Akcje", "Rachunek opodatkowany", "30000", "ETF płynny", "ETF-y zagraniczne", "USD", "Giełda"),
                        position("Gotówka", "Poduszka bezpieczeństwa", "20000", "Konto oszczędnościowe", "Konta oszczędnościowe", "PLN", "Poduszka")
                ),
                List.of("fake.csv")
        ));
        when(budgetStore.findYears()).thenReturn(List.of());

        var summary = new FireQueryService(portfolioReader, budgetStore, new FireSettingsService(settings, new MemoryStore())).summary();

        assertThat(summary.portfolios()).extracting(FireSummary.FirePortfolioBreakdown::portfolio)
                .containsExactly("Emerytura Aleksander", "Giełda", "Poduszka");
        assertThat(summary.portfolios()).filteredOn(portfolio -> portfolio.portfolio().equals("Emerytura Aleksander"))
                .singleElement()
                .satisfies(portfolio -> {
                    assertThat(portfolio.role()).isEqualTo("Emerytalny");
                    assertThat(portfolio.retirementLockedValue()).isEqualByComparingTo("60000.00");
                    assertThat(portfolio.investmentValue()).isEqualByComparingTo("60000.00");
                });
        assertThat(summary.portfolios()).filteredOn(portfolio -> portfolio.portfolio().equals("Poduszka"))
                .singleElement()
                .satisfies(portfolio -> {
                    assertThat(portfolio.role()).isEqualTo("Poduszka");
                    assertThat(portfolio.emergencyValue()).isEqualByComparingTo("20000.00");
                    assertThat(portfolio.investmentValue()).isEqualByComparingTo("0.00");
                });
        assertThat(summary.positionAnalyses()).filteredOn(row -> row.instrument().equals("ETF płynny"))
                .singleElement()
                .satisfies(row -> assertThat(row.portfolio()).isEqualTo("Giełda"));
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
        return position(assetClass, wrapper, value, assetClass + " instrument", assetClass, "PLN");
    }

    private FirePortfolioPosition position(String assetClass, String wrapper, String value, String instrument, String group, String currency) {
        return position(assetClass, wrapper, value, instrument, group, currency, "Test");
    }

    private FirePortfolioPosition position(String assetClass, String wrapper, String value, String instrument, String group, String currency, String portfolio) {
        return new FirePortfolioPosition(
                "fake.csv",
                portfolio,
                wrapper,
                assetClass,
                instrument,
                group,
                "Test account",
                currency,
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
        return settingsWithSpend(null);
    }

    private FireSettings settingsWithSpend(String monthlySpend) {
        return new FireSettings(
                Path.of("fire/investments_reports"),
                36,
                50,
                monthlySpend == null ? null : new BigDecimal(monthlySpend),
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
