package com.budget.application.fire;

import com.budget.domain.fire.FirePortfolioPosition;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;

final class FirePositionAnalyzer {
    private static final BigDecimal CONCENTRATION_HIGH = new BigDecimal("0.25");
    private static final BigDecimal CONCENTRATION_MEDIUM = new BigDecimal("0.15");
    private static final BigDecimal SINGLE_SECURITY_MEDIUM = new BigDecimal("0.10");
    private static final BigDecimal TAX_GAIN_MEDIUM = new BigDecimal("0.05");

    private final FireInstrumentClassifier classifier;

    FirePositionAnalyzer(FireInstrumentClassifier classifier) {
        this.classifier = classifier;
    }

    List<FireSummary.FirePositionAnalysis> analyze(
            FireSettings settings,
            List<FirePortfolioPosition> positions,
            BigDecimal currentValue,
            BigDecimal investmentPortfolioValue,
            List<FireSummary.FireRebalanceAction> rebalancing
    ) {
        var grouped = new LinkedHashMap<String, List<FirePortfolioPosition>>();
        for (var position : positions) {
            grouped.computeIfAbsent(positionKey(position), ignored -> new ArrayList<>()).add(position);
        }
        var rebalanceByClass = new LinkedHashMap<String, FireSummary.FireRebalanceAction>();
        for (var row : rebalancing) {
            rebalanceByClass.put(row.assetClass(), row);
        }
        return grouped.values().stream()
                .map(rows -> analyzeGroup(settings, rows, currentValue, investmentPortfolioValue, rebalanceByClass))
                .sorted(Comparator.comparingInt((FireSummary.FirePositionAnalysis row) -> riskRank(row.riskLevel()))
                        .thenComparing(FireSummary.FirePositionAnalysis::shareOfInvestments, Comparator.reverseOrder())
                        .thenComparing(FireSummary.FirePositionAnalysis::value, Comparator.reverseOrder()))
                .toList();
    }

    private FireSummary.FirePositionAnalysis analyzeGroup(
            FireSettings settings,
            List<FirePortfolioPosition> rows,
            BigDecimal currentValue,
            BigDecimal investmentPortfolioValue,
            LinkedHashMap<String, FireSummary.FireRebalanceAction> rebalanceByClass
    ) {
        var first = rows.getFirst();
        var profile = classifier.classify(first);
        var value = money(sum(rows, FirePortfolioPosition::valuePln));
        var costBasis = money(sum(rows, FirePortfolioPosition::costBasisPln));
        var gain = money(sum(rows, FirePortfolioPosition::gainPln));
        var returnPct = costBasis.signum() == 0
                ? rows.stream()
                .map(FirePortfolioPosition::returnPct)
                .filter(java.util.Objects::nonNull)
                .findFirst()
                .orElse(BigDecimal.ZERO)
                : gain.divide(costBasis, 6, RoundingMode.HALF_UP);
        var shareOfPortfolio = share(value, currentValue);
        var shareOfInvestments = profile.emergency()
                ? BigDecimal.ZERO
                : share(value, investmentPortfolioValue);
        var rebalance = rebalanceByClass.get(profile.assetClass());
        var riskDrivers = riskDrivers(profile, first, shareOfInvestments, value, gain);
        var decision = decision(settings, profile, first, shareOfInvestments, rebalance, gain);
        var reason = decisionReason(profile, shareOfInvestments, rebalance, riskDrivers);
        var checklist = checklist(profile, first, shareOfInvestments, rebalance);
        return new FireSummary.FirePositionAnalysis(
                first.instrument(),
                first.isin(),
                first.portfolio(),
                profile.assetClass(),
                profile.instrumentType(),
                profile.fireRole(),
                profile.wrapper(),
                accountLabel(rows),
                currencyLabel(rows),
                rows.stream().map(FirePortfolioPosition::priceDate).filter(java.util.Objects::nonNull).max(LocalDate::compareTo).orElse(null),
                value,
                costBasis,
                gain,
                returnPct,
                shareOfPortfolio,
                shareOfInvestments,
                riskLevel(profile, shareOfInvestments, value, gain),
                reviewFocus(settings, profile, shareOfInvestments, rebalance),
                decision,
                reason,
                decision,
                perspective(settings, profile),
                riskDrivers,
                checklist
        );
    }

    private List<String> riskDrivers(
            FireInstrumentClassifier.FireInstrumentProfile profile,
            FirePortfolioPosition position,
            BigDecimal shareOfInvestments,
            BigDecimal value,
            BigDecimal gain
    ) {
        var items = new ArrayList<String>();
        if (profile.unknown()) {
            items.add("Brak pewnej klasy aktywów lub segmentu płynności");
        }
        if (shareOfInvestments.compareTo(CONCENTRATION_HIGH) >= 0) {
            items.add("Koncentracja pozycji powyżej 25% portfela inwestycyjnego");
        } else if (shareOfInvestments.compareTo(CONCENTRATION_MEDIUM) >= 0) {
            items.add("Koncentracja pozycji powyżej 15% portfela inwestycyjnego");
        }
        if (profile.singleSecurity()) {
            items.add("Ryzyko pojedynczej spółki zamiast szerokiego indeksu");
        }
        switch (profile.assetClass()) {
            case FireInstrumentClassifier.ASSET_EQUITY -> items.add("Zmienność rynku akcji");
            case FireInstrumentClassifier.ASSET_BONDS -> items.add("Ryzyko stopy procentowej, inflacji i kredytowe");
            case FireInstrumentClassifier.ASSET_CASH -> items.add("Ryzyko inflacji i utraty realnej siły nabywczej");
            case FireInstrumentClassifier.ASSET_ALTERNATIVES -> items.add("Satelita: sprawdź limit, płynność i korelację z portfelem");
            case FireInstrumentClassifier.ASSET_MULTI_ASSET -> items.add("Produkt mieszany: rozbij look-through na akcje/obligacje przed decyzją alokacyjną");
            default -> {
            }
        }
        if (profile.factorTilt()) {
            items.add("ETF faktorowy/tematyczny: możliwe dublowanie lub odchylenie od szerokiego rynku");
        }
        if (profile.nonPln()) {
            items.add("Waluta instrumentu: " + currency(position));
        }
        if (profile.retirement()) {
            items.add("Ograniczona dostępność przed 60/65 r.ż.");
        }
        if (profile.taxable() && share(gain.max(BigDecimal.ZERO), value).compareTo(TAX_GAIN_MEDIUM) > 0) {
            items.add("Sprzedaż z zyskiem może uruchomić podatek 19%");
        }
        if (profile.fundLike() && !FireInstrumentClassifier.ASSET_CASH.equals(profile.assetClass())) {
            items.add("Fundusz/ETF: sprawdź KID, SRI, koszty i replikację");
        }
        return items.stream().distinct().limit(6).toList();
    }

    private String riskLevel(
            FireInstrumentClassifier.FireInstrumentProfile profile,
            BigDecimal shareOfInvestments,
            BigDecimal value,
            BigDecimal gain
    ) {
        if (profile.unknown() || shareOfInvestments.compareTo(CONCENTRATION_HIGH) >= 0) {
            return "high";
        }
        if (profile.singleSecurity() && shareOfInvestments.compareTo(SINGLE_SECURITY_MEDIUM) >= 0) {
            return "high";
        }
        if (profile.emergency()) {
            return "low";
        }
        if (shareOfInvestments.compareTo(CONCENTRATION_MEDIUM) >= 0
                || profile.nonPln()
                || profile.retirement()
                || profile.singleSecurity()
                || FireInstrumentClassifier.ASSET_ALTERNATIVES.equals(profile.assetClass())
                || FireInstrumentClassifier.ASSET_MULTI_ASSET.equals(profile.assetClass())
                || profile.taxable() && share(gain.max(BigDecimal.ZERO), value).compareTo(TAX_GAIN_MEDIUM) > 0) {
            return "medium";
        }
        return "info";
    }

    private String reviewFocus(
            FireSettings settings,
            FireInstrumentClassifier.FireInstrumentProfile profile,
            BigDecimal shareOfInvestments,
            FireSummary.FireRebalanceAction rebalance
    ) {
        if (profile.unknown()) {
            return "Klasyfikacja";
        }
        if (profile.emergency()) {
            return "Poduszka";
        }
        if (FireInstrumentClassifier.ASSET_MULTI_ASSET.equals(profile.assetClass())) {
            return "Look-through";
        }
        if (shareOfInvestments.compareTo(CONCENTRATION_MEDIUM) >= 0) {
            return "Koncentracja";
        }
        if (profile.retirement() && settings.targetAge() < 60) {
            return "Dostępność";
        }
        if (profile.singleSecurity() || FireInstrumentClassifier.ASSET_ALTERNATIVES.equals(profile.assetClass()) || profile.factorTilt()) {
            return "Satelita";
        }
        if (rebalance != null && "Wysoki".equals(rebalance.priority())) {
            return rebalance.amountToTarget().signum() > 0 ? "Doważenie klasy" : "Nadwaga klasy";
        }
        if (profile.taxable()) {
            return "Podatek/koszty";
        }
        return "Rola w portfelu";
    }

    private String decision(
            FireSettings settings,
            FireInstrumentClassifier.FireInstrumentProfile profile,
            FirePortfolioPosition position,
            BigDecimal shareOfInvestments,
            FireSummary.FireRebalanceAction rebalance,
            BigDecimal gain
    ) {
        if (profile.unknown()) {
            return "Najpierw sklasyfikuj ręcznie; nie traktuj tej pozycji jako sygnału do rebalancingu.";
        }
        if (profile.emergency()) {
            return "Trzymaj do celu poduszki. Nadwyżkę ponad cel kieruj do pomostu albo alokacji docelowej.";
        }
        if (FireInstrumentClassifier.ASSET_MULTI_ASSET.equals(profile.assetClass())) {
            return profile.retirement() && settings.targetAge() < 60
                    ? "Traktuj jako kapitał po 60/65 i nie używaj do precyzyjnego rebalancingu bez rozbicia look-through."
                    : "Nie używaj do precyzyjnego rebalancingu, dopóki nie rozbijesz ekspozycji look-through na akcje i obligacje.";
        }
        if (profile.retirement() && settings.targetAge() < 60) {
            return "Traktuj jako kapitał po 60/65; nie finansuje automatycznie wieku 50-60.";
        }
        if (shareOfInvestments.compareTo(CONCENTRATION_HIGH) >= 0) {
            return "Nie zwiększaj ekspozycji. Nowe wpłaty kieruj w niedoważone klasy albo płynny pomost.";
        }
        if (profile.singleSecurity()) {
            return "Ustal limit dla pojedynczych spółek; nie traktuj tej pozycji jako rdzenia FIRE.";
        }
        if (rebalance != null && "Wysoki".equals(rebalance.priority())) {
            if (rebalance.amountToTarget().signum() > 0 && profile.broadMarket()) {
                return "Możliwy kandydat do nowych wpłat w niedoważonej klasie, po sprawdzeniu kosztów i KID.";
            }
            if (rebalance.amountToTarget().signum() > 0) {
                return "Nie doważaj automatycznie satelity tylko dlatego, że klasa jest pod celem.";
            }
            return "Wstrzymaj nowe wpłaty do tej klasy; sprzedaż rozważ dopiero po policzeniu podatku i kosztów.";
        }
        if (profile.taxable() && gain.signum() > 0) {
            return "Monitoruj i rebalansuj głównie nowymi wpłatami, bo sprzedaż może być podatkowo kosztowna.";
        }
        if (profile.broadMarket()) {
            return "Może pełnić rolę rdzenia, ale kupuj tylko według docelowej alokacji i pasma rebalancingu.";
        }
        return "Monitoruj rolę, koszty, walutę i duplikację ekspozycji przy okresowym przeglądzie.";
    }

    private String decisionReason(
            FireInstrumentClassifier.FireInstrumentProfile profile,
            BigDecimal shareOfInvestments,
            FireSummary.FireRebalanceAction rebalance,
            List<String> riskDrivers
    ) {
        var reasons = new ArrayList<String>();
        reasons.add(profile.fireRole());
        if (shareOfInvestments.signum() > 0) {
            reasons.add("udział " + percentLabel(shareOfInvestments) + " portfela inwestycyjnego");
        }
        if (rebalance != null && "Wysoki".equals(rebalance.priority())) {
            reasons.add("odchylenie klasy " + percentLabel(rebalance.drift()));
        }
        riskDrivers.stream().limit(2).forEach(reasons::add);
        return String.join(" · ", reasons);
    }

    private String perspective(FireSettings settings, FireInstrumentClassifier.FireInstrumentProfile profile) {
        var horizon = Math.max(0, settings.targetAge() - settings.currentAge());
        var role = switch (profile.assetClass()) {
            case FireInstrumentClassifier.ASSET_EQUITY -> profile.broadMarket()
                    ? "Rdzeń wzrostowy ma sens dla długiego horyzontu " + horizon + "+ lat, ale będzie mocno zmienny."
                    : "Ekspozycja akcyjna poza szerokim rdzeniem powinna mieć limit satelitów i jasny powód trzymania.";
            case FireInstrumentClassifier.ASSET_BONDS -> "Stabilizator zmienności i potencjalny element pomostu, ale wymaga kontroli duration, inflacji i ryzyka emitenta.";
            case FireInstrumentClassifier.ASSET_CASH -> "Płynność chroni przed wymuszoną sprzedażą aktywów, lecz nie buduje realnego wzrostu FIRE.";
            case FireInstrumentClassifier.ASSET_ALTERNATIVES -> "Satelita może dywersyfikować, ale nie powinien zastępować rdzenia bez świadomego limitu.";
            case FireInstrumentClassifier.ASSET_MULTI_ASSET -> "Produkt mieszany może być wygodny, ale zaciemnia realny udział akcji i obligacji w rebalancingu.";
            default -> "Bez klasyfikacji nie da się ocenić wpływu waloru na plan FIRE.";
        };
        if (profile.retirement()) {
            return role + " Segment emerytalny może być podatkowo korzystny, ale nie rozwiązuje pomostu przed 60/65.";
        }
        if (profile.taxable()) {
            return role + " Segment opodatkowany jest płynny, ale realizacja zysków może generować PIT-38/19%.";
        }
        return role;
    }

    private List<String> checklist(
            FireInstrumentClassifier.FireInstrumentProfile profile,
            FirePortfolioPosition position,
            BigDecimal shareOfInvestments,
            FireSummary.FireRebalanceAction rebalance
    ) {
        var items = new ArrayList<String>();
        if (profile.unknown()) {
            items.add("Uzupełnij ręczną klasyfikację klasy aktywów i segmentu płynności.");
        }
        if (clean(position.isin()).isBlank() && !FireInstrumentClassifier.ASSET_CASH.equals(profile.assetClass())) {
            items.add("Uzupełnij ISIN, żeby łatwiej sprawdzić KID, koszty, walutę i ekspozycję.");
        }
        if (profile.fundLike() && !FireInstrumentClassifier.ASSET_CASH.equals(profile.assetClass())) {
            items.add("Sprawdź KID/PRIIP: SRI, TER/koszty transakcyjne, walutę, replikację i benchmark.");
        }
        if (FireInstrumentClassifier.ASSET_MULTI_ASSET.equals(profile.assetClass())) {
            items.add("Zapisz przybliżony split look-through, np. 80/20, żeby alokacja nie udawała czystych akcji.");
        }
        if (profile.singleSecurity()) {
            items.add("Ustal maksymalny udział pojedynczej spółki w portfelu FIRE.");
        }
        if (shareOfInvestments.compareTo(CONCENTRATION_MEDIUM) >= 0) {
            items.add("Sprawdź, czy walor nie dubluje ekspozycji z innymi ETF/funduszami.");
        }
        if (rebalance != null && "Wysoki".equals(rebalance.priority())) {
            items.add("Porównaj z celem alokacji i pasmem rebalancingu; preferuj nowe wpłaty.");
        }
        if (profile.retirement()) {
            items.add("Nie licz tej pozycji jako płynnego finansowania wieku 50-60.");
        }
        if (profile.taxable()) {
            items.add("Przed sprzedażą policz podatek 19%, koszty transakcyjne i możliwość kompensacji strat.");
        }
        return items.stream().distinct().limit(5).toList();
    }

    private String positionKey(FirePortfolioPosition position) {
        var isin = clean(position.isin());
        var instrument = clean(position.instrument());
        var wrapper = clean(position.wrapper());
        var account = clean(position.account());
        return (!isin.isBlank() ? isin : instrument) + "|" + wrapper + "|" + account;
    }

    private String accountLabel(List<FirePortfolioPosition> rows) {
        var accounts = new LinkedHashSet<String>();
        for (var row : rows) {
            var account = clean(row.account());
            if (!account.isBlank()) {
                accounts.add(account);
            }
        }
        if (accounts.isEmpty()) {
            return "";
        }
        if (accounts.size() == 1) {
            return accounts.iterator().next();
        }
        return accounts.size() + " konta";
    }

    private String currencyLabel(List<FirePortfolioPosition> rows) {
        var currencies = new LinkedHashSet<String>();
        for (var row : rows) {
            var currency = currency(row);
            if (!currency.isBlank()) {
                currencies.add(currency);
            }
        }
        if (currencies.isEmpty()) {
            return "PLN";
        }
        if (currencies.size() == 1) {
            return currencies.iterator().next();
        }
        return String.join("/", currencies);
    }

    private String currency(FirePortfolioPosition row) {
        return clean(row.currency()).toUpperCase(Locale.ROOT);
    }

    private int riskRank(String level) {
        return switch (level) {
            case "high" -> 0;
            case "medium" -> 1;
            case "low" -> 2;
            default -> 3;
        };
    }

    private BigDecimal share(BigDecimal value, BigDecimal total) {
        if (total == null || total.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return value.divide(total, 6, RoundingMode.HALF_UP);
    }

    private String percentLabel(BigDecimal ratio) {
        return ratio == null ? "0,0%" : ratio.multiply(BigDecimal.valueOf(100)).setScale(1, RoundingMode.HALF_UP) + "%";
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

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
