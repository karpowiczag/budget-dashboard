package com.budget.application.fire;

import com.budget.application.networth.PortfolioValuePort;
import com.budget.application.reporting.BudgetReportStore;
import com.budget.application.reporting.ReportNotFoundException;
import com.budget.application.reporting.YearSummary;
import com.budget.domain.fire.FirePortfolioPosition;
import com.budget.domain.report.BudgetSnapshot;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class FireQueryService implements PortfolioValuePort {
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    private static final BigDecimal TWELVE = BigDecimal.valueOf(12);
    private static final String WRAPPER_EMERGENCY = "Poduszka bezpieczeństwa";
    private static final String WRAPPER_RETIREMENT = "Emerytalne długoterminowe";
    private static final String WRAPPER_TAXABLE = "Rachunek opodatkowany";
    private static final String BUCKET_INVESTMENTS = "Inwestycje";
    private static final String BUCKET_LOAN_OVERPAYMENT = "Nadpłata kredytu";
    private static final BigDecimal ASSUMED_INFLATION = new BigDecimal("0.025");
    private static final BigDecimal EQUITY_VOLATILITY = new BigDecimal("0.16");
    private static final BigDecimal MIN_PORTFOLIO_VOLATILITY = new BigDecimal("0.03");
    private static final int MONTE_CARLO_PATHS = 2000;
    private static final long MONTE_CARLO_SEED = 20260101L;
    private static final int GLIDEPATH_YEARS = 10;
    private static final BigDecimal GLIDEPATH_FLOOR_EQUITY = new BigDecimal("0.50");
    private static final String PPK_RECOMMENDATION = "Wpłacaj na PPK tyle, by uzyskać pełną dopłatę pracodawcy (1,5% bazowo, do 4% z dopłatami) — darmowy kapitał emerytalny ponad IKE/IKZE.";

    private final FirePortfolioReader portfolioReader;
    private final BudgetReportStore budgetStore;
    private final FireSettingsService settingsService;
    private final FireInstrumentClassifier instrumentClassifier = new FireInstrumentClassifier();
    private final FirePositionAnalyzer positionAnalyzer = new FirePositionAnalyzer(instrumentClassifier);
    private final FireRiskAnalyzer riskAnalyzer = new FireRiskAnalyzer();

    public FireQueryService(FirePortfolioReader portfolioReader, BudgetReportStore budgetStore, FireSettingsService settingsService) {
        this.portfolioReader = portfolioReader;
        this.budgetStore = budgetStore;
        this.settingsService = settingsService;
    }

    public FireSummary summary() {
        var settings = settingsService.current();
        var snapshot = readSnapshot(settings);
        var positions = snapshot.positions();
        var currentValue = sum(positions, FirePortfolioPosition::valuePln);
        var costBasis = sum(positions, FirePortfolioPosition::costBasisPln);
        var gain = sum(positions, FirePortfolioPosition::gainPln);
        var emergencyFund = sum(positions.stream()
                .filter(position -> position.wrapper().equals(WRAPPER_EMERGENCY))
                .toList(), FirePortfolioPosition::valuePln);
        var retirementLocked = sum(positions.stream()
                .filter(position -> position.wrapper().equals(WRAPPER_RETIREMENT))
                .toList(), FirePortfolioPosition::valuePln);
        var liquidFireCapital = currentValue.subtract(retirementLocked).max(BigDecimal.ZERO);
        var budgetLink = budgetLink(settings);
        var spendTargetConfigured = settings.monthlySpendOverride() != null;
        var monthlySpendTarget = fireSpendTarget(settings);
        var annualSpendTarget = monthlySpendTarget.multiply(TWELVE);
        var fireNumber = divide(annualSpendTarget, settings.safeWithdrawalRate(), 2);
        var yearsToFire = settings.targetAge() - settings.currentAge();
        var monthlyContribution = settings.monthlyContributionOverride() == null
                ? budgetLink.firePortfolioMonthlyContribution()
                : money(settings.monthlyContributionOverride());
        budgetLink = applyOverridesToBudgetLink(budgetLink, monthlyContribution, settings);
        var scenarios = spendTargetConfigured
                ? scenarios(settings, currentValue, fireNumber, monthlyContribution, yearsToFire)
                : List.<FireSummary.FireScenario>of();
        var investmentPositions = positions.stream()
                .filter(position -> !position.wrapper().equals(WRAPPER_EMERGENCY))
                .toList();
        var investmentPortfolioValue = sum(investmentPositions, FirePortfolioPosition::valuePln);
        var allocation = allocation(settings, investmentPositions, investmentPortfolioValue);
        var wrappers = wrappers(positions, currentValue);
        var portfolios = portfolios(positions, currentValue);
        var rebalancing = rebalancing(settings, allocation, investmentPortfolioValue);
        var bridgeTo60 = bridgeCapital(settings, annualSpendTarget, 60);
        var bridgeTo65 = bridgeCapital(settings, annualSpendTarget, 65);
        var taxablePositions = positions.stream()
                .filter(position -> position.wrapper().equals(WRAPPER_TAXABLE))
                .toList();
        var taxableCapital = sum(taxablePositions, FirePortfolioPosition::valuePln);
        var taxableGain = sum(taxablePositions, FirePortfolioPosition::gainPln);
        var estimatedTax = money(taxableGain.max(BigDecimal.ZERO).multiply(new BigDecimal("0.19")));
        var bridgeableEmergencyExcess = emergencyFund.subtract(budgetLink.emergencyReserveTarget()).max(BigDecimal.ZERO);
        var bridgeableLiquidCapital = money(taxableCapital.add(bridgeableEmergencyExcess));
        var liquidBridgeGapTo60 = money(bridgeTo60.subtract(bridgeableLiquidCapital).max(BigDecimal.ZERO));
        var liquidBridgeGapTo65 = money(bridgeTo65.subtract(bridgeableLiquidCapital).max(BigDecimal.ZERO));
        var baseScenario = scenarios.stream()
                .filter(scenario -> scenario.id().equals("base"))
                .findFirst()
                .orElse(null);
        var contributionPlan = contributionPlan(monthlyContribution, baseScenario, budgetLink, spendTargetConfigured);
        var withdrawalPlan = withdrawalPlan(monthlySpendTarget, annualSpendTarget, liquidFireCapital, bridgeTo60, bridgeTo65, estimatedTax);
        var dataQuality = dataQuality(snapshot, positions);
        var risks = riskAnalyzer.analyze(new FireRiskAnalyzer.Context(
                settings,
                investmentPositions,
                allocation,
                rebalancing,
                budgetLink,
                dataQuality,
                investmentPortfolioValue,
                currentValue,
                retirementLocked,
                emergencyFund,
                taxableCapital,
                taxableGain,
                estimatedTax,
                spendTargetConfigured,
                liquidBridgeGapTo60
        ));
        var actionItems = actionItems(baseScenario, contributionPlan, liquidBridgeGapTo60, dataQuality, rebalancing, budgetLink, spendTargetConfigured);
        var positionAnalyses = positionAnalyzer.analyze(settings, positions, currentValue, investmentPortfolioValue, rebalancing);

        var assumedInflation = ASSUMED_INFLATION;
        var fireNumberNominalAtTarget = money(BigDecimal.valueOf(
                fireNumber.doubleValue() * Math.pow(1 + assumedInflation.doubleValue(), Math.max(0, yearsToFire))));
        var suggestedEquityShare = glidepathEquityShare(settings.targetEquityShare(), yearsToFire);
        var portfolioVolatility = EQUITY_VOLATILITY.multiply(settings.targetEquityShare()).max(MIN_PORTFOLIO_VOLATILITY);
        var monteCarloSuccessRate = spendTargetConfigured
                ? BigDecimal.valueOf(FireProjection.monteCarloSuccessRate(
                        currentValue, money(monthlyContribution), settings.expectedRealReturn(), portfolioVolatility,
                        Math.max(0, yearsToFire) * 12, fireNumber, MONTE_CARLO_PATHS, MONTE_CARLO_SEED)).setScale(4, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        return new FireSummary(
                snapshot.asOf(),
                !positions.isEmpty(),
                settings.reportsPath().toString(),
                snapshot.sourceFiles().size(),
                positions.size(),
                settings.currentAge(),
                settings.targetAge(),
                yearsToFire,
                money(currentValue),
                money(costBasis),
                money(gain),
                money(emergencyFund),
                money(retirementLocked),
                money(liquidFireCapital),
                money(bridgeableLiquidCapital),
                money(budgetLink.emergencyReserveTarget()),
                money(annualSpendTarget),
                money(monthlySpendTarget),
                spendTargetConfigured,
                settings.safeWithdrawalRate(),
                money(fireNumber),
                money(fireNumber.subtract(currentValue).max(BigDecimal.ZERO)),
                bridgeTo60,
                bridgeTo65,
                liquidBridgeGapTo60,
                liquidBridgeGapTo65,
                money(taxableCapital),
                money(taxableGain),
                estimatedTax,
                money(monthlyContribution),
                assumedInflation,
                fireNumberNominalAtTarget,
                suggestedEquityShare,
                monteCarloSuccessRate,
                PPK_RECOMMENDATION,
                budgetLink,
                contributionPlan,
                withdrawalPlan,
                dataQuality,
                scenarios,
                allocation,
                wrappers,
                portfolios,
                rebalancing,
                risks,
                actionItems,
                positionAnalyses,
                spendTargetConfigured ? milestones(settings, annualSpendTarget, fireNumber, bridgeTo60, bridgeTo65) : List.of(),
                legalRules(),
                sources(positions)
        );
    }

    /**
     * Lightweight invested-capital read for the net-worth calculation: only sums position
     * values, skipping the full FIRE projection. Returns zero when no portfolio is loaded.
     */
    @Override
    public BigDecimal currentPortfolioValue() {
        var snapshot = readSnapshot(settingsService.current());
        return money(sum(snapshot.positions(), FirePortfolioPosition::valuePln));
    }

    private com.budget.domain.fire.FirePortfolioSnapshot readSnapshot(FireSettings settings) {
        try {
            return portfolioReader.read(settings.reportsPath());
        } catch (IOException e) {
            return new com.budget.domain.fire.FirePortfolioSnapshot(null, List.of(), List.of());
        }
    }

    private FireSummary.FireBudgetLink budgetLink(FireSettings settings) {
        try {
            var years = budgetStore.findYears();
            if (years.isEmpty()) {
                return FireSummary.FireBudgetLink.empty();
            }
            var latestYear = years.stream().mapToInt(YearSummary::year).max().orElseThrow();
            var dashboard = budgetStore.findDashboard(latestYear);
            var months = BigDecimal.valueOf(Math.max(1, dashboard.activeMonths()));
            var actualMonthlyInvestments = monthlyAverage(dashboard, BUCKET_INVESTMENTS);
            var savingsAccountMonthlyNet = money(dashboard.kpis().savingsAccountNetChange().divide(months, 2, RoundingMode.HALF_UP));
            var savingsAccountMonthlyGross = money(dashboard.kpis().savingsAccountGrossDeposits().divide(months, 2, RoundingMode.HALF_UP));
            var loanOverpaymentMonthly = monthlyAverage(dashboard, BUCKET_LOAN_OVERPAYMENT);
            var firePortfolioContribution = money(actualMonthlyInvestments.add(savingsAccountMonthlyNet.max(BigDecimal.ZERO)));
            var unassignedSurplusMonthly = money(dashboard.kpis().unassignedSurplus().divide(months, 2, RoundingMode.HALF_UP));
            return new FireSummary.FireBudgetLink(
                    true,
                    latestYear,
                    dashboard.activeMonths(),
                    money(dashboard.savingsPlan().currentMonthlyIncome()),
                    money(dashboard.savingsPlan().currentMonthlySpend()),
                    money(dashboard.savingsPlan().targetMonthlySpend()),
                    actualMonthlyInvestments,
                    savingsAccountMonthlyNet,
                    savingsAccountMonthlyGross,
                    loanOverpaymentMonthly,
                    firePortfolioContribution,
                    money(dashboard.savingsPlan().targetInvestmentTransfer()),
                    unassignedSurplusMonthly,
                    money(dashboard.savingsPlan().emergencyFundComfort()),
                    settings.monthlySpendOverride() != null,
                    settings.monthlyContributionOverride() != null,
                    "Budżet domowy zasila FIRE tylko tempem wpłat: inwestycje + netto konto oszczędnościowe. Cel wydatków FIRE jest osobnym ustawieniem, a nadpłaty kredytu są osobnym strumieniem redukcji długu."
            );
        } catch (ReportNotFoundException | IllegalStateException e) {
            return FireSummary.FireBudgetLink.empty();
        }
    }

    private BigDecimal fireSpendTarget(FireSettings settings) {
        return settings.monthlySpendOverride() == null
                ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                : money(settings.monthlySpendOverride());
    }

    private BigDecimal monthlyAverage(BudgetSnapshot dashboard, String bucket) {
        return dashboard.budgetMix().stream()
                .filter(row -> bucket.equals(row.bucket()))
                .findFirst()
                .map(BudgetSnapshot.BudgetMixItem::monthlyAverage)
                .map(this::money)
                .orElse(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
    }

    private FireSummary.FireBudgetLink applyOverridesToBudgetLink(
            FireSummary.FireBudgetLink budgetLink,
            BigDecimal monthlyContribution,
            FireSettings settings
    ) {
        return new FireSummary.FireBudgetLink(
                budgetLink.linked(),
                budgetLink.budgetYear(),
                budgetLink.activeMonths(),
                budgetLink.monthlyIncome(),
                budgetLink.currentMonthlyLivingSpend(),
                budgetLink.targetMonthlySpend(),
                budgetLink.actualMonthlyInvestments(),
                budgetLink.savingsAccountMonthlyNet(),
                budgetLink.savingsAccountMonthlyGrossDeposits(),
                budgetLink.loanOverpaymentMonthly(),
                money(monthlyContribution),
                budgetLink.targetInvestableSurplus(),
                budgetLink.unassignedSurplusMonthly(),
                budgetLink.emergencyReserveTarget(),
                settings.monthlySpendOverride() != null,
                settings.monthlyContributionOverride() != null,
                budgetLink.note()
        );
    }

    private List<FireSummary.FireScenario> scenarios(FireSettings settings, BigDecimal currentValue, BigDecimal fireNumber, BigDecimal currentMonthly, int yearsToFire) {
        return List.of(
                scenario("low", "Ostrożny", settings.pessimisticRealReturn(), currentValue, fireNumber, currentMonthly, yearsToFire),
                scenario("base", "Bazowy", settings.expectedRealReturn(), currentValue, fireNumber, currentMonthly, yearsToFire),
                scenario("high", "Dobry rynek", settings.optimisticRealReturn(), currentValue, fireNumber, currentMonthly, yearsToFire)
        );
    }

    private FireSummary.FireScenario scenario(String id, String label, BigDecimal annualReturn, BigDecimal currentValue, BigDecimal fireNumber, BigDecimal currentMonthly, int yearsToFire) {
        var months = yearsToFire * 12;
        var monthlyReturn = annualReturn.divide(TWELVE, 12, RoundingMode.HALF_UP);
        var projected = FireProjection.futureValue(currentValue, currentMonthly, monthlyReturn, months);
        var gap = fireNumber.subtract(projected).max(BigDecimal.ZERO);
        var required = FireProjection.requiredMonthlyContribution(currentValue, fireNumber, monthlyReturn, months);
        return new FireSummary.FireScenario(
                id,
                label,
                annualReturn,
                money(projected),
                money(gap),
                money(required),
                money(currentMonthly),
                gap.compareTo(BigDecimal.ZERO) == 0
        );
    }

    private List<FireSummary.FireAllocation> allocation(FireSettings settings, List<FirePortfolioPosition> positions, BigDecimal total) {
        var grouped = new LinkedHashMap<String, BigDecimal>();
        for (var position : positions) {
            grouped.merge(instrumentClassifier.classify(position).assetClass(), position.valuePln(), BigDecimal::add);
        }
        var targets = targetAllocation(settings);
        var assetClasses = new LinkedHashSet<String>();
        assetClasses.addAll(grouped.keySet());
        targets.forEach((assetClass, targetShare) -> {
            if (targetShare.signum() > 0) {
                assetClasses.add(assetClass);
            }
        });
        return assetClasses.stream()
                .map(assetClass -> {
                    var value = grouped.getOrDefault(assetClass, BigDecimal.ZERO);
                    var share = share(value, total);
                    var target = targets.getOrDefault(assetClass, BigDecimal.ZERO);
                    var drift = share.subtract(target);
                    return new FireSummary.FireAllocation(
                            assetClass,
                            money(value),
                            share,
                            target,
                            drift,
                            "Mieszane".equals(assetClass)
                                    ? "Wymaga look-through"
                                    : drift.abs().compareTo(settings.rebalanceBand()) > 0 ? "Poza pasmem" : "OK"
                    );
                })
                .sorted(Comparator.comparing(FireSummary.FireAllocation::value).reversed())
                .toList();
    }

    private Map<String, BigDecimal> targetAllocation(FireSettings settings) {
        var targets = new LinkedHashMap<String, BigDecimal>();
        targets.put("Akcje", settings.targetEquityShare());
        targets.put("Obligacje", settings.targetBondShare());
        targets.put("Gotówka", settings.targetCashShare());
        targets.put("Alternatywne", settings.targetAlternativeShare());
        targets.put("Mieszane", BigDecimal.ZERO);
        targets.put("Inne", BigDecimal.ZERO);
        return targets;
    }

    private List<FireSummary.FireWrapper> wrappers(List<FirePortfolioPosition> positions, BigDecimal total) {
        var grouped = new LinkedHashMap<String, List<FirePortfolioPosition>>();
        for (var position : positions) {
            grouped.computeIfAbsent(position.wrapper(), ignored -> new ArrayList<>()).add(position);
        }
        return grouped.entrySet().stream()
                .map(entry -> {
                    var value = sum(entry.getValue(), FirePortfolioPosition::valuePln);
                    return new FireSummary.FireWrapper(
                            entry.getKey(),
                            money(value),
                            share(value, total),
                            entry.getValue().size(),
                            liquidity(entry.getKey())
                    );
                })
                .sorted(Comparator.comparing(FireSummary.FireWrapper::value).reversed())
                .toList();
    }

    private List<FireSummary.FirePortfolioBreakdown> portfolios(List<FirePortfolioPosition> positions, BigDecimal total) {
        var grouped = new LinkedHashMap<String, List<FirePortfolioPosition>>();
        for (var position : positions) {
            grouped.computeIfAbsent(portfolioLabel(position), ignored -> new ArrayList<>()).add(position);
        }
        return grouped.entrySet().stream()
                .map(entry -> {
                    var rows = entry.getValue();
                    var value = money(sum(rows, FirePortfolioPosition::valuePln));
                    var emergency = money(sum(rows.stream()
                            .filter(position -> WRAPPER_EMERGENCY.equals(position.wrapper()))
                            .toList(), FirePortfolioPosition::valuePln));
                    var retirement = money(sum(rows.stream()
                            .filter(position -> WRAPPER_RETIREMENT.equals(position.wrapper()))
                            .toList(), FirePortfolioPosition::valuePln));
                    var taxable = money(sum(rows.stream()
                            .filter(position -> WRAPPER_TAXABLE.equals(position.wrapper()))
                            .toList(), FirePortfolioPosition::valuePln));
                    var investment = money(value.subtract(emergency).max(BigDecimal.ZERO));
                    var role = portfolioRole(value, emergency, retirement, taxable);
                    return new FireSummary.FirePortfolioBreakdown(
                            entry.getKey(),
                            value,
                            share(value, total),
                            investment,
                            emergency,
                            retirement,
                            taxable,
                            rows.size(),
                            role,
                            portfolioNote(role)
                    );
                })
                .sorted(Comparator.comparing(FireSummary.FirePortfolioBreakdown::value).reversed())
                .toList();
    }

    private String portfolioLabel(FirePortfolioPosition position) {
        var value = position.portfolio() == null ? "" : position.portfolio().trim();
        return value.isBlank() ? "Nieznany portfel" : value;
    }

    private String portfolioRole(BigDecimal total, BigDecimal emergency, BigDecimal retirement, BigDecimal taxable) {
        if (total.signum() == 0) {
            return "Brak wartości";
        }
        if (share(emergency, total).compareTo(new BigDecimal("0.80")) >= 0) {
            return "Poduszka";
        }
        if (share(retirement, total).compareTo(new BigDecimal("0.80")) >= 0) {
            return "Emerytalny";
        }
        if (share(taxable, total).compareTo(new BigDecimal("0.80")) >= 0) {
            return "Płynny inwestycyjny";
        }
        return "Mieszany";
    }

    private String portfolioNote(String role) {
        return switch (role) {
            case "Poduszka" -> "Nie traktuj jako ryzykowny portfel FIRE; chroni płynność i awarie.";
            case "Emerytalny" -> "Dobre miejsce na długi horyzont, ale nie finansuje automatycznie pomostu 50-60/65.";
            case "Płynny inwestycyjny" -> "Może finansować pomost, ale sprzedaż z zyskiem może być podatkowo wrażliwa.";
            default -> "Sprawdź segmenty w środku, bo portfel miesza płynność, podatki albo różne cele.";
        };
    }

    private List<FireSummary.FireRebalanceAction> rebalancing(FireSettings settings, List<FireSummary.FireAllocation> allocation, BigDecimal total) {
        return allocation.stream()
                .map(row -> {
                    if ("Mieszane".equals(row.assetClass())) {
                        return new FireSummary.FireRebalanceAction(
                                row.assetClass(),
                                row.share(),
                                row.targetShare(),
                                row.drift(),
                                BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                                "Rozbij look-through na akcje/obligacje przed decyzją",
                                row.share().signum() > 0 ? "Wysoki" : "Normalny"
                        );
                    }
                    var amountToTarget = row.targetShare().subtract(row.share()).multiply(total);
                    var action = amountToTarget.compareTo(BigDecimal.ZERO) > 0
                            ? "Doważyć nowymi wpłatami"
                            : amountToTarget.compareTo(BigDecimal.ZERO) < 0 ? "Nie dokupować / rozważyć przy rebalansie" : "Bez zmian";
                    var priority = row.drift().abs().compareTo(settings.rebalanceBand()) > 0 ? "Wysoki" : "Normalny";
                    return new FireSummary.FireRebalanceAction(
                            row.assetClass(),
                            row.share(),
                            row.targetShare(),
                            row.drift(),
                            money(amountToTarget),
                            action,
                            priority
                    );
                })
                .sorted(Comparator.comparing((FireSummary.FireRebalanceAction row) -> row.priority().equals("Wysoki") ? 0 : 1)
                        .thenComparing(FireSummary.FireRebalanceAction::assetClass))
                .toList();
    }

    private BigDecimal glidepathEquityShare(BigDecimal targetEquity, int yearsToFire) {
        if (yearsToFire >= GLIDEPATH_YEARS) {
            return targetEquity.setScale(4, RoundingMode.HALF_UP);
        }
        var floor = GLIDEPATH_FLOOR_EQUITY.min(targetEquity);
        var fraction = BigDecimal.valueOf(Math.max(0, yearsToFire))
                .divide(BigDecimal.valueOf(GLIDEPATH_YEARS), 6, RoundingMode.HALF_UP);
        return floor.add(targetEquity.subtract(floor).multiply(fraction)).setScale(4, RoundingMode.HALF_UP);
    }

    private BigDecimal bridgeCapital(FireSettings settings, BigDecimal annualSpendTarget, int accessAge) {
        var years = Math.max(0, accessAge - settings.targetAge());
        return money(annualSpendTarget.multiply(BigDecimal.valueOf(years)));
    }

    private List<FireSummary.FireMilestone> milestones(FireSettings settings, BigDecimal annualSpendTarget, BigDecimal fireNumber, BigDecimal bridgeTo60, BigDecimal bridgeTo65) {
        return List.of(
                new FireSummary.FireMilestone(settings.targetAge(), "FIRE target", "Kapitał generujący planowany roczny budżet według SWR.", money(fireNumber)),
                new FireSummary.FireMilestone(60, "Dostęp do IKE / wiek ZUS kobiet", "Pomost 50-60 powinien być pokryty płynnym kapitałem poza IKZE.", bridgeTo60),
                new FireSummary.FireMilestone(65, "IKZE i wiek ZUS mężczyzn", "Konserwatywny pomost do wieku 65 lat dla środków emerytalnych i ZUS.", bridgeTo65),
                new FireSummary.FireMilestone(0, "Roczny koszt życia", "Cel wydatków rocznych użyty w modelu FIRE.", money(annualSpendTarget))
        );
    }

    private FireSummary.FireContributionPlan contributionPlan(
            BigDecimal currentMonthly,
            FireSummary.FireScenario baseScenario,
            FireSummary.FireBudgetLink budgetLink,
            boolean spendTargetConfigured
    ) {
        if (!spendTargetConfigured || baseScenario == null) {
            return new FireSummary.FireContributionPlan(
                    money(currentMonthly),
                    BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                    BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                    BigDecimal.valueOf(28_260L * 2),
                    BigDecimal.valueOf(11_304L * 2),
                    money(BigDecimal.valueOf(28_260L * 2 + 11_304L * 2).divide(TWELVE, 2, RoundingMode.HALF_UP)),
                    "Ustaw docelowe miesięczne wydatki FIRE. Budżet będzie dalej dostarczał tylko obecne tempo wpłat inwestycyjnych."
            );
        }
        var required = money(baseScenario.requiredMonthlyContribution());
        var additional = money(required.subtract(currentMonthly).max(BigDecimal.ZERO));
        var ikeCapacity = BigDecimal.valueOf(28_260L * 2);
        var ikzeCapacity = BigDecimal.valueOf(11_304L * 2);
        var monthlyWrapperCapacity = money(ikeCapacity.add(ikzeCapacity).divide(TWELVE, 2, RoundingMode.HALF_UP));
        var source = budgetLink.contributionOverrideUsed()
                ? "override użytkownika"
                : "budżet domowy: inwestycje + netto konto oszczędnościowe";
        var debtNote = budgetLink.loanOverpaymentMonthly().signum() > 0
                ? " Nadpłaty kredytu (" + monthlyLabel(budgetLink.loanOverpaymentMonthly()) + ") są osobnym strumieniem redukcji długu, nie wpłatą do portfela inwestycyjnego."
                : "";
        var recommendation = additional.signum() == 0
                ? "Plan bazowy domyka cel FIRE przy obecnym tempie wpłat (" + source + "). Utrzymaj automatyczne wpłaty i rebalansuj nowymi środkami." + debtNote
                : "Brakuje " + monthlyLabel(additional) + " względem scenariusza bazowego. Źródło obecnej wpłaty: " + source + ". Najpierw wypełniaj IKE/IKZE, nadwyżkę kieruj w płynny portfel pomostowy." + debtNote;
        return new FireSummary.FireContributionPlan(
                money(currentMonthly),
                required,
                additional,
                ikeCapacity,
                ikzeCapacity,
                monthlyWrapperCapacity,
                recommendation
        );
    }

    private FireSummary.FireWithdrawalPlan withdrawalPlan(
            BigDecimal monthlySpendTarget,
            BigDecimal annualSpendTarget,
            BigDecimal liquidFireCapital,
            BigDecimal bridgeTo60,
            BigDecimal bridgeTo65,
            BigDecimal estimatedTax
    ) {
        var yearsCovered = annualSpendTarget.signum() == 0
                ? BigDecimal.ZERO
                : liquidFireCapital.divide(annualSpendTarget, 2, RoundingMode.HALF_UP);
        return new FireSummary.FireWithdrawalPlan(
                money(monthlySpendTarget),
                money(annualSpendTarget),
                money(liquidFireCapital),
                yearsCovered,
                bridgeTo60,
                bridgeTo65,
                estimatedTax,
                "Najpierw poduszka i portfel opodatkowany na pomost 50-60/65, IKE nie ruszać przed warunkami wypłaty, IKZE traktować jako kapitał po 65 r.ż."
        );
    }

    private FireSummary.FireDataQuality dataQuality(
            com.budget.domain.fire.FirePortfolioSnapshot snapshot,
            List<FirePortfolioPosition> positions
    ) {
        var newest = positions.stream()
                .map(FirePortfolioPosition::priceDate)
                .filter(java.util.Objects::nonNull)
                .max(LocalDate::compareTo)
                .orElse(snapshot.asOf());
        var staleThreshold = LocalDate.now().minusDays(45);
        var staleSources = sources(positions).stream()
                .filter(source -> source.asOf() == null || source.asOf().isBefore(staleThreshold))
                .count();
        var unknownAssets = positions.stream()
                .filter(position -> instrumentClassifier.classify(position).unknown())
                .toList();
        var unknownWrappers = positions.stream()
                .filter(position -> position.wrapper().equals("Inne"))
                .toList();
        var status = positions.isEmpty()
                ? "missing"
                : staleSources > 0 || !unknownAssets.isEmpty() || !unknownWrappers.isEmpty() ? "needsReview" : "ok";
        var note = switch (status) {
            case "missing" -> "Brak lokalnych raportów MyFund, więc projekcja nie ma bazy portfela.";
            case "needsReview" -> "Część raportów jest stara albo wymaga ręcznej mapy aktywów/opakowań.";
            default -> "Raporty są aktualne i wszystkie pozycje mają rozpoznaną klasę oraz segment płynności.";
        };
        return new FireSummary.FireDataQuality(
                newest,
                snapshot.sourceFiles().size(),
                positions.size(),
                Math.toIntExact(staleSources),
                unknownAssets.size(),
                money(sum(unknownAssets, FirePortfolioPosition::valuePln)),
                unknownWrappers.size(),
                money(sum(unknownWrappers, FirePortfolioPosition::valuePln)),
                status,
                note
        );
    }

    private List<FireSummary.FireActionItem> actionItems(
            FireSummary.FireScenario baseScenario,
            FireSummary.FireContributionPlan contributionPlan,
            BigDecimal liquidBridgeGapTo60,
            FireSummary.FireDataQuality dataQuality,
            List<FireSummary.FireRebalanceAction> rebalancing,
            FireSummary.FireBudgetLink budgetLink,
            boolean spendTargetConfigured
    ) {
        var items = new ArrayList<FireSummary.FireActionItem>();
        if (!spendTargetConfigured) {
            items.add(new FireSummary.FireActionItem(
                    "P1",
                    "planning",
                    "Ustaw miesięczny cel wydatków FIRE",
                    "Bez tej liczby model nie liczy FIRE number, luki ani wymaganej wpłaty. Budżet domowy nie zgaduje przyszłych kosztów życia.",
                    BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
            ));
        }
        if (!dataQuality.status().equals("ok")) {
            items.add(new FireSummary.FireActionItem(
                    "P1",
                    "data",
                    "Doprowadź dane MyFund do stanu produkcyjnego",
                    dataQuality.note(),
                    BigDecimal.ZERO
            ));
        }
        if (spendTargetConfigured && contributionPlan.additionalMonthlyNeeded().signum() > 0) {
            items.add(new FireSummary.FireActionItem(
                    "P1",
                    "contribution",
                    "Zwiększ miesięczne wpłaty inwestycyjne",
                    "Scenariusz bazowy porównuje wymagane wpłaty z budżetem domowym: inwestycje + netto konto oszczędnościowe. Nadpłaty kredytu są liczone osobno.",
                    contributionPlan.additionalMonthlyNeeded()
            ));
        }
        if (spendTargetConfigured && budgetLink.loanOverpaymentMonthly().compareTo(BigDecimal.ZERO) > 0 && contributionPlan.additionalMonthlyNeeded().signum() > 0) {
            items.add(new FireSummary.FireActionItem(
                    "P2",
                    "debt",
                    "Zdecyduj ile nadpłat kredytu ma konkurować z FIRE",
                    "Budżet pokazuje regularne nadpłaty kredytu. To poprawia majątek netto, ale nie buduje płynnego portfela na wiek 50.",
                    budgetLink.loanOverpaymentMonthly()
            ));
        }
        if (spendTargetConfigured && liquidBridgeGapTo60.signum() > 0) {
            items.add(new FireSummary.FireActionItem(
                    "P1",
                    "bridge",
                    "Zbuduj płynny kapitał pomostowy do 60 r.ż.",
                    "Część kapitału emerytalnego może być niedostępna w wieku 50 lat, więc sam FIRE number nie wystarczy.",
                    liquidBridgeGapTo60
            ));
        }
        rebalancing.stream()
                .filter(row -> row.priority().equals("Wysoki"))
                .findFirst()
                .ifPresent(row -> items.add(new FireSummary.FireActionItem(
                        "P2",
                        "rebalance",
                        "Skoryguj alokację nowymi wpłatami",
                        row.assetClass() + ": " + row.action(),
                        row.amountToTarget()
                )));
        if (items.isEmpty() && baseScenario.onTrack()) {
            items.add(new FireSummary.FireActionItem(
                    "P3",
                    "maintenance",
                    "Utrzymaj automatyzację",
                    "Model bazowy jest na ścieżce. Najważniejsze jest utrzymanie wpłat, kontroli wydatków i okresowego rebalancingu.",
                    BigDecimal.ZERO
            ));
        }
        return items;
    }

    private List<FireSummary.FireLegalRule> legalRules() {
        return List.of(
                new FireSummary.FireLegalRule("belka", "Podatek od zysków kapitałowych", "19%", "Dotyczy m.in. sprzedaży akcji, udziału w funduszach kapitałowych i PIT-38 poza opakowaniami emerytalnymi.", "https://www.podatki.gov.pl/podatki-osobiste/pit/informacje-podstawowe/co-jest-opodatkowane/zbycie-akcji/"),
                new FireSummary.FireLegalRule("ike-limit", "Limit IKE 2026", "28 260 zł / osoba", "Wypłata z zachowaniem zwolnienia podatkowego zasadniczo po 60 r.ż. albo 55 r.ż. przy uprawnieniach emerytalnych.", "https://www.knf.gov.pl/?articleId=81021&p_id=18"),
                new FireSummary.FireLegalRule("ikze-limit", "Limit IKZE 2026", "11 304 zł / osoba", "Dla JDG limit 16 956 zł; kwalifikowana wypłata po 65 r.ż. i 5 latach wpłat, zryczałtowany podatek 10%.", "https://www.knf.gov.pl/?articleId=81022&p_id=18"),
                new FireSummary.FireLegalRule("zus-age", "Powszechny wiek emerytalny", "60 K / 65 M", "FIRE w wieku 50 lat wymaga osobnego kapitału pomostowego przed świadczeniami ustawowymi.", "https://www.zus.pl/swiadczenia/emerytury/emerytura-dla-osob-urodzonych-po-31-grudnia-1948/emerytura-w-wieku-powszechnym"),
                new FireSummary.FireLegalRule("rebalance", "Rebalancing", "pasmo 5 p.p.", "Domyślnie doważanie nowymi wpłatami; sprzedaż w rachunkach opodatkowanych tylko przy istotnym odchyleniu.", "https://www.sec.gov/investor/pubs/assetallocation.htm"),
                new FireSummary.FireLegalRule("kid-cost-risk", "KID, koszty i ryzyko funduszy", "SRI / koszty / benchmark", "Dla funduszy i ETF sprawdzaj dokument KID, koszty, wskaźnik ryzyka i historyczne stopy zwrotu; sama nazwa waloru nie wystarcza do decyzji.", "https://wybieramfundusze.knf.gov.pl/"),
                new FireSummary.FireLegalRule("diversification", "Dywersyfikacja", "nie jedna inwestycja", "Model flaguje koncentrację, bo KNF zaleca rozpraszanie środków między różne produkty i unikanie zależności od jednej inwestycji.", "https://www.knf.gov.pl/dla_konsumenta/kampanie_informacyjne/inwestuj_swiadomie")
        );
    }

    private List<FireSummary.FireSource> sources(List<FirePortfolioPosition> positions) {
        var grouped = new LinkedHashMap<String, List<FirePortfolioPosition>>();
        for (var position : positions) {
            grouped.computeIfAbsent(position.sourceFile(), ignored -> new ArrayList<>()).add(position);
        }
        return grouped.entrySet().stream()
                .map(entry -> {
                    var rows = entry.getValue();
                    return new FireSummary.FireSource(
                            entry.getKey(),
                            rows.getFirst().portfolio(),
                            rows.stream().map(FirePortfolioPosition::priceDate).filter(java.util.Objects::nonNull).max(LocalDate::compareTo).orElse(null),
                            rows.size(),
                            money(sum(rows, FirePortfolioPosition::valuePln))
                    );
                })
                .toList();
    }

    private String liquidity(String wrapper) {
        return switch (wrapper) {
            case WRAPPER_EMERGENCY -> "płynne";
            case WRAPPER_RETIREMENT -> "ograniczone do wieku emerytalnego";
            default -> "płynne inwestycyjnie, podatkowo wrażliwe";
        };
    }

    private BigDecimal share(BigDecimal value, BigDecimal total) {
        if (total == null || total.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return value.divide(total, 6, RoundingMode.HALF_UP);
    }

    private BigDecimal divide(BigDecimal left, BigDecimal right, int scale) {
        if (right == null || right.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return left.divide(right, scale, RoundingMode.HALF_UP);
    }

    private BigDecimal money(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP);
    }

    private String monthlyLabel(BigDecimal amount) {
        var formatter = NumberFormat.getNumberInstance(Locale.forLanguageTag("pl-PL"));
        formatter.setMinimumFractionDigits(0);
        formatter.setMaximumFractionDigits(0);
        return formatter.format(money(amount)) + " zł/mies.";
    }

    private BigDecimal sum(List<FirePortfolioPosition> positions, java.util.function.Function<FirePortfolioPosition, BigDecimal> extractor) {
        return positions.stream()
                .map(extractor)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

}
