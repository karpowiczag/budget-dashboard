package com.budget.application.fire;

import com.budget.application.reporting.BudgetReportStore;
import com.budget.application.reporting.ReportNotFoundException;
import com.budget.domain.fire.FirePortfolioPosition;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class FireQueryService {
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    private static final BigDecimal TWELVE = BigDecimal.valueOf(12);

    private final FirePortfolioReader portfolioReader;
    private final BudgetReportStore budgetStore;
    private final FireSettingsService settingsService;

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
                .filter(position -> position.wrapper().equals("Poduszka bezpieczeństwa"))
                .toList(), FirePortfolioPosition::valuePln);
        var retirementLocked = sum(positions.stream()
                .filter(position -> position.wrapper().equals("Emerytalne długoterminowe"))
                .toList(), FirePortfolioPosition::valuePln);
        var liquidFireCapital = currentValue.subtract(retirementLocked).max(BigDecimal.ZERO);
        var monthlySpendTarget = settings.monthlySpendOverride() == null
                ? latestMonthlySpendTarget()
                : money(settings.monthlySpendOverride());
        var annualSpendTarget = monthlySpendTarget.multiply(TWELVE);
        var fireNumber = divide(annualSpendTarget, settings.safeWithdrawalRate(), 2);
        var yearsToFire = settings.targetAge() - settings.currentAge();
        var monthlyContribution = settings.monthlyContributionOverride() == null
                ? latestMonthlyWealthContribution()
                : money(settings.monthlyContributionOverride());
        var scenarios = scenarios(settings, currentValue, fireNumber, monthlyContribution, yearsToFire);
        var allocation = allocation(settings, positions, currentValue);
        var wrappers = wrappers(positions, currentValue);
        var rebalancing = rebalancing(settings, allocation, currentValue);
        var bridgeTo60 = bridgeCapital(settings, annualSpendTarget, 60);
        var bridgeTo65 = bridgeCapital(settings, annualSpendTarget, 65);
        var taxablePositions = positions.stream()
                .filter(position -> position.wrapper().equals("Rachunek opodatkowany"))
                .toList();
        var taxableCapital = sum(taxablePositions, FirePortfolioPosition::valuePln);
        var taxableGain = sum(taxablePositions, FirePortfolioPosition::gainPln);
        var estimatedTax = money(taxableGain.max(BigDecimal.ZERO).multiply(new BigDecimal("0.19")));
        var liquidBridgeGapTo60 = money(bridgeTo60.subtract(liquidFireCapital).max(BigDecimal.ZERO));
        var liquidBridgeGapTo65 = money(bridgeTo65.subtract(liquidFireCapital).max(BigDecimal.ZERO));
        var baseScenario = scenarios.stream()
                .filter(scenario -> scenario.id().equals("base"))
                .findFirst()
                .orElseGet(scenarios::getFirst);
        var contributionPlan = contributionPlan(monthlyContribution, baseScenario);
        var withdrawalPlan = withdrawalPlan(monthlySpendTarget, annualSpendTarget, liquidFireCapital, bridgeTo60, bridgeTo65, estimatedTax);
        var dataQuality = dataQuality(snapshot, positions);
        var actionItems = actionItems(baseScenario, contributionPlan, liquidBridgeGapTo60, dataQuality, rebalancing);

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
                money(annualSpendTarget),
                money(monthlySpendTarget),
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
                contributionPlan,
                withdrawalPlan,
                dataQuality,
                scenarios,
                allocation,
                wrappers,
                rebalancing,
                actionItems,
                milestones(settings, annualSpendTarget, fireNumber, bridgeTo60, bridgeTo65),
                legalRules(),
                sources(positions)
        );
    }

    private com.budget.domain.fire.FirePortfolioSnapshot readSnapshot(FireSettings settings) {
        try {
            return portfolioReader.read(settings.reportsPath());
        } catch (IOException e) {
            return new com.budget.domain.fire.FirePortfolioSnapshot(null, List.of(), List.of());
        }
    }

    private BigDecimal latestMonthlySpendTarget() {
        try {
            var years = budgetStore.findYears();
            if (years.isEmpty()) {
                return BigDecimal.valueOf(14_000);
            }
            var latestYear = years.stream().mapToInt(com.budget.application.reporting.YearSummary::year).max().orElseThrow();
            var dashboard = budgetStore.findDashboard(latestYear);
            return money(dashboard.savingsPlan().targetMonthlySpend());
        } catch (ReportNotFoundException | IllegalStateException e) {
            return BigDecimal.valueOf(14_000);
        }
    }

    private BigDecimal latestMonthlyWealthContribution() {
        try {
            var years = budgetStore.findYears();
            if (years.isEmpty()) {
                return BigDecimal.ZERO;
            }
            var latestYear = years.stream().mapToInt(com.budget.application.reporting.YearSummary::year).max().orElseThrow();
            var dashboard = budgetStore.findDashboard(latestYear);
            var months = BigDecimal.valueOf(Math.max(1, dashboard.activeMonths()));
            return money(dashboard.kpis().realSavingsOutgoing().divide(months, 2, RoundingMode.HALF_UP));
        } catch (ReportNotFoundException | IllegalStateException e) {
            return BigDecimal.ZERO;
        }
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
        var projected = futureValue(currentValue, currentMonthly, monthlyReturn, months);
        var gap = fireNumber.subtract(projected).max(BigDecimal.ZERO);
        var required = requiredMonthlyContribution(currentValue, fireNumber, monthlyReturn, months);
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

    private BigDecimal futureValue(BigDecimal principal, BigDecimal monthlyContribution, BigDecimal monthlyReturn, int months) {
        if (months <= 0) {
            return principal;
        }
        var rate = monthlyReturn.doubleValue();
        var factor = Math.pow(1 + rate, months);
        var contributionFactor = rate == 0 ? months : (factor - 1) / rate;
        return BigDecimal.valueOf(principal.doubleValue() * factor + monthlyContribution.doubleValue() * contributionFactor);
    }

    private BigDecimal requiredMonthlyContribution(BigDecimal principal, BigDecimal target, BigDecimal monthlyReturn, int months) {
        if (months <= 0) {
            return BigDecimal.ZERO;
        }
        var rate = monthlyReturn.doubleValue();
        var factor = Math.pow(1 + rate, months);
        var remaining = target.doubleValue() - principal.doubleValue() * factor;
        if (remaining <= 0) {
            return BigDecimal.ZERO;
        }
        var contributionFactor = rate == 0 ? months : (factor - 1) / rate;
        return BigDecimal.valueOf(remaining / contributionFactor);
    }

    private List<FireSummary.FireAllocation> allocation(FireSettings settings, List<FirePortfolioPosition> positions, BigDecimal total) {
        var grouped = new LinkedHashMap<String, BigDecimal>();
        for (var position : positions) {
            grouped.merge(position.assetClass(), position.valuePln(), BigDecimal::add);
        }
        var targets = targetAllocation(settings);
        return grouped.entrySet().stream()
                .map(entry -> {
                    var share = share(entry.getValue(), total);
                    var target = targets.getOrDefault(entry.getKey(), BigDecimal.ZERO);
                    var drift = share.subtract(target);
                    return new FireSummary.FireAllocation(
                            entry.getKey(),
                            money(entry.getValue()),
                            share,
                            target,
                            drift,
                            drift.abs().compareTo(settings.rebalanceBand()) > 0 ? "Poza pasmem" : "OK"
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

    private List<FireSummary.FireRebalanceAction> rebalancing(FireSettings settings, List<FireSummary.FireAllocation> allocation, BigDecimal total) {
        return allocation.stream()
                .map(row -> {
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

    private FireSummary.FireContributionPlan contributionPlan(BigDecimal currentMonthly, FireSummary.FireScenario baseScenario) {
        var required = money(baseScenario.requiredMonthlyContribution());
        var additional = money(required.subtract(currentMonthly).max(BigDecimal.ZERO));
        var ikeCapacity = BigDecimal.valueOf(28_260L * 2);
        var ikzeCapacity = BigDecimal.valueOf(11_304L * 2);
        var monthlyWrapperCapacity = money(ikeCapacity.add(ikzeCapacity).divide(TWELVE, 2, RoundingMode.HALF_UP));
        var recommendation = additional.signum() == 0
                ? "Plan bazowy domyka cel FIRE przy obecnym tempie wpłat. Utrzymaj automatyczne wpłaty i rebalansuj nowymi środkami."
                : "Brakującą miesięczną kwotę kieruj najpierw w roczne limity IKE/IKZE, a nadwyżkę w płynny portfel pomostowy do wieku 60/65.";
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
                .filter(position -> position.assetClass().equals("Inne"))
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
            List<FireSummary.FireRebalanceAction> rebalancing
    ) {
        var items = new ArrayList<FireSummary.FireActionItem>();
        if (!dataQuality.status().equals("ok")) {
            items.add(new FireSummary.FireActionItem(
                    "P1",
                    "data",
                    "Doprowadź dane MyFund do stanu produkcyjnego",
                    dataQuality.note(),
                    BigDecimal.ZERO
            ));
        }
        if (contributionPlan.additionalMonthlyNeeded().signum() > 0) {
            items.add(new FireSummary.FireActionItem(
                    "P1",
                    "contribution",
                    "Zwiększ miesięczne wpłaty do planu FIRE",
                    "Scenariusz bazowy wymaga większego tempa niż obecny plan wpłat.",
                    contributionPlan.additionalMonthlyNeeded()
            ));
        }
        if (liquidBridgeGapTo60.signum() > 0) {
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
                new FireSummary.FireLegalRule("ike-limit", "Limit IKE 2026", "28 260 zł / osoba", "Wypłata z zachowaniem zwolnienia podatkowego zasadniczo po 60 r.ż. albo 55 r.ż. przy uprawnieniach emerytalnych.", "https://www.gov.pl/web/rodzina/ike-limit-wplat"),
                new FireSummary.FireLegalRule("ikze-limit", "Limit IKZE 2026", "11 304 zł / osoba", "Dla JDG limit 16 956 zł; kwalifikowana wypłata po 65 r.ż. i 5 latach wpłat, zryczałtowany podatek 10%.", "https://www.knf.gov.pl/?articleId=81022&p_id=18"),
                new FireSummary.FireLegalRule("zus-age", "Powszechny wiek emerytalny", "60 K / 65 M", "FIRE w wieku 50 lat wymaga osobnego kapitału pomostowego przed świadczeniami ustawowymi.", "https://www.zus.pl/swiadczenia/emerytury/emerytura-dla-osob-urodzonych-po-31-grudnia-1948/emerytura-w-wieku-powszechnym"),
                new FireSummary.FireLegalRule("rebalance", "Rebalancing", "pasmo 5 p.p.", "Domyślnie doważanie nowymi wpłatami; sprzedaż w rachunkach opodatkowanych tylko przy istotnym odchyleniu.", "https://investor.vanguard.com/investor-resources-education/portfolio-management/rebalancing-your-portfolio")
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
            case "Poduszka bezpieczeństwa" -> "płynne";
            case "Emerytalne długoterminowe" -> "ograniczone do wieku emerytalnego";
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

    private BigDecimal sum(List<FirePortfolioPosition> positions, java.util.function.Function<FirePortfolioPosition, BigDecimal> extractor) {
        return positions.stream()
                .map(extractor)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
