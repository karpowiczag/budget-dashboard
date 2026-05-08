package com.budget.application.fire;

import com.budget.domain.fire.FirePortfolioPosition;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;

final class FireRiskAnalyzer {
    private static final BigDecimal EQUITY_HIGH_SHARE = new BigDecimal("0.85");
    private static final BigDecimal SINGLE_POSITION_MEDIUM_SHARE = new BigDecimal("0.15");
    private static final BigDecimal SINGLE_POSITION_HIGH_SHARE = new BigDecimal("0.25");
    private static final BigDecimal RETIREMENT_LOCKED_MEDIUM_SHARE = new BigDecimal("0.30");
    private static final BigDecimal RETIREMENT_LOCKED_HIGH_SHARE = new BigDecimal("0.50");
    private static final BigDecimal FX_MEDIUM_SHARE = new BigDecimal("0.60");

    List<FireSummary.FireRisk> analyze(Context context) {
        var risks = new ArrayList<FireSummary.FireRisk>();
        if (!context.spendTargetConfigured()) {
            risks.add(risk(
                    "fireTargetMissing",
                    "high",
                    "Plan FIRE",
                    "Brak celu wydatków FIRE",
                    "Cel miesięczny",
                    "nieustawiony",
                    "wymagany",
                    "Bez jawnego celu nie da się policzyć FIRE number, luki kapitału ani realnej wpłaty wymaganej do wieku " + context.settings().targetAge() + ".",
                    "Ustaw miesięczny koszt życia po FIRE oddzielnie od obecnego budżetu domowego."
            ));
        }

        if (!"ok".equals(context.dataQuality().status())) {
            risks.add(risk(
                    "dataQuality",
                    "high",
                    "Dane",
                    "Dane portfela wymagają przeglądu",
                    "Status",
                    context.dataQuality().status(),
                    "ok",
                    context.dataQuality().note(),
                    "Odśwież raporty MyFund i uzupełnij mapowanie klas aktywów/opakowań przed traktowaniem projekcji jako decyzyjnych."
            ));
        }

        if (context.investmentPortfolioValue().signum() > 0) {
            addAllocationRisks(context, risks);
            addConcentrationRisk(context, risks);
            addCurrencyRisk(context, risks);
        }

        addLiquidityRisk(context, risks);
        addBridgeRisk(context, risks);
        addEmergencyReserveRisk(context, risks);
        addTaxRisk(context, risks);

        return risks.stream()
                .sorted(Comparator.comparingInt((FireSummary.FireRisk row) -> riskRank(row.level()))
                        .thenComparing(FireSummary.FireRisk::area)
                        .thenComparing(FireSummary.FireRisk::title))
                .toList();
    }

    private void addAllocationRisks(Context context, List<FireSummary.FireRisk> risks) {
        context.allocation().stream()
                .filter(row -> row.assetClass().equals("Akcje"))
                .findFirst()
                .ifPresent(row -> {
                    var equityThreshold = context.settings().targetEquityShare().add(context.settings().rebalanceBand());
                    if (row.share().compareTo(EQUITY_HIGH_SHARE) >= 0 || row.share().compareTo(equityThreshold) > 0) {
                        var level = row.share().compareTo(EQUITY_HIGH_SHARE) >= 0 ? "high" : "medium";
                        risks.add(risk(
                                "equityConcentration",
                                level,
                                "Alokacja",
                                "Portfel jest mocno akcyjny",
                                "Akcje",
                                percentLabel(row.share()),
                                percentLabel(equityThreshold),
                                "Akcje stanowią " + percentLabel(row.share()) + " portfela inwestycyjnego przy celu " + percentLabel(row.targetShare()) + " i paśmie " + percentLabel(context.settings().rebalanceBand()) + ".",
                                "Nowe wpłaty kieruj w niedoważone klasy. Sprzedaż na rachunku opodatkowanym rozważ dopiero po uwzględnieniu podatku i kosztów."
                        ));
                    }
                });

        context.rebalancing().stream()
                .filter(row -> row.priority().equals("Wysoki"))
                .max(Comparator.comparing(row -> row.drift().abs()))
                .ifPresent(row -> risks.add(risk(
                        "rebalanceDrift",
                        "medium",
                        "Rebalancing",
                        "Alokacja wyszła poza pasmo",
                        row.assetClass(),
                        percentLabel(row.currentShare()),
                        percentLabel(row.targetShare()),
                        "Największe odchylenie dotyczy klasy " + row.assetClass() + ": " + percentLabel(row.drift()) + " względem celu.",
                        "Przez najbliższe wpłaty doważaj klasy poniżej celu; sprzedaż wygranych pozycji traktuj jako opcję podatkowo wrażliwą."
                )));
    }

    private void addConcentrationRisk(Context context, List<FireSummary.FireRisk> risks) {
        var largestPositionShare = largestPositionShare(context.investmentPositions(), context.investmentPortfolioValue());
        if (largestPositionShare.compareTo(SINGLE_POSITION_MEDIUM_SHARE) > 0) {
            risks.add(risk(
                    "singlePositionConcentration",
                    largestPositionShare.compareTo(SINGLE_POSITION_HIGH_SHARE) > 0 ? "high" : "medium",
                    "Dywersyfikacja",
                    "Duża koncentracja pojedynczej pozycji",
                    "Największa pozycja",
                    percentLabel(largestPositionShare),
                    percentLabel(SINGLE_POSITION_MEDIUM_SHARE),
                    "Największy instrument ma " + percentLabel(largestPositionShare) + " portfela inwestycyjnego. Nie pokazuję nazwy instrumentu, żeby nie ujawniać prywatnego składu portfela.",
                    "Używaj nowych wpłat do rozcieńczania koncentracji i sprawdź, czy pozycja nie dubluje ekspozycji w innych funduszach/ETF-ach."
            ));
        }
    }

    private void addCurrencyRisk(Context context, List<FireSummary.FireRisk> risks) {
        var fxShare = nonPlnCurrencyShare(context.investmentPositions(), context.investmentPortfolioValue());
        if (fxShare.compareTo(FX_MEDIUM_SHARE) > 0) {
            risks.add(risk(
                    "currencyExposure",
                    "medium",
                    "Waluta",
                    "Wysoka ekspozycja walutowa w raportowanych walutach",
                    "Pozycje nie-PLN",
                    percentLabel(fxShare),
                    percentLabel(FX_MEDIUM_SHARE),
                    "Raportowane waluty pozycji pokazują " + percentLabel(fxShare) + " portfela inwestycyjnego poza PLN. To nie zastępuje pełnego look-through walutowego funduszy.",
                    "Pilnuj, żeby cel FIRE w PLN miał sensowny bufor na ryzyko kursowe i nie wymagał sprzedaży aktywów po słabym kursie."
            ));
        }
    }

    private void addLiquidityRisk(Context context, List<FireSummary.FireRisk> risks) {
        if (context.currentValue().signum() == 0) {
            return;
        }
        var lockedShare = share(context.retirementLocked(), context.currentValue());
        if (context.settings().targetAge() < 60 && lockedShare.compareTo(RETIREMENT_LOCKED_MEDIUM_SHARE) > 0) {
            risks.add(risk(
                    "retirementWrapperLiquidity",
                    lockedShare.compareTo(RETIREMENT_LOCKED_HIGH_SHARE) > 0 ? "high" : "medium",
                    "Płynność",
                    "Duża część kapitału jest emerytalnie ograniczona",
                    "Kapitał emerytalny",
                    percentLabel(lockedShare),
                    percentLabel(RETIREMENT_LOCKED_MEDIUM_SHARE),
                    "Kapitał w segmencie emerytalnym to " + percentLabel(lockedShare) + " całego portfela. Przy FIRE w wieku " + context.settings().targetAge() + " nie finansuje automatycznie okresu pomostowego.",
                    "Buduj osobny płynny portfel pomostowy na lata 50-60/65 i nie mieszaj go z IKE/IKZE w ocenie gotowości do FIRE."
            ));
        }
    }

    private void addBridgeRisk(Context context, List<FireSummary.FireRisk> risks) {
        if (context.spendTargetConfigured() && context.liquidBridgeGapTo60().signum() > 0) {
            risks.add(risk(
                    "bridgeGap",
                    "high",
                    "Pomost",
                    "Brakuje płynnego kapitału do 60 r.ż.",
                    "Luka 50-60",
                    moneyLabel(context.liquidBridgeGapTo60()),
                    "0 zł",
                    "Po zostawieniu poduszki i uwzględnieniu rachunku opodatkowanego model widzi lukę pomostową do wieku 60 lat.",
                    "Część nowych wpłat kieruj do płynnego portfela pomostowego, zanim zwiększysz udział kapitału zablokowanego emerytalnie."
            ));
        }
    }

    private void addEmergencyReserveRisk(Context context, List<FireSummary.FireRisk> risks) {
        if (context.budgetLink().emergencyReserveTarget().signum() == 0) {
            return;
        }
        var reserveCoverage = context.emergencyFund().divide(context.budgetLink().emergencyReserveTarget(), 6, RoundingMode.HALF_UP);
        if (reserveCoverage.compareTo(BigDecimal.ONE) < 0) {
            risks.add(risk(
                    "emergencyReserveShortfall",
                    "high",
                    "Poduszka",
                    "Poduszka poniżej celu",
                    "Pokrycie",
                    percentLabel(reserveCoverage),
                    "100%",
                    "Poduszka bezpieczeństwa jest poniżej celu z budżetu domowego.",
                    "Przed agresywnym FIRE uzupełnij poduszkę do celu, bo chroni przed sprzedażą aktywów w złym momencie."
            ));
        } else {
            risks.add(risk(
                    "emergencyReserveCovered",
                    "low",
                    "Poduszka",
                    "Poduszka pokrywa cel",
                    "Pokrycie",
                    percentLabel(reserveCoverage),
                    "100%",
                    "Poduszka bezpieczeństwa jest co najmniej na poziomie celu z budżetu domowego.",
                    "Trzymaj ją poza ryzykiem rynkowym; nadwyżkę ponad cel można traktować jako kapitał pomostowy."
            ));
        }
    }

    private void addTaxRisk(Context context, List<FireSummary.FireRisk> risks) {
        if (context.estimatedTax().signum() == 0 || context.taxableCapital().signum() == 0) {
            return;
        }
        var taxDrag = share(context.estimatedTax(), context.taxableCapital());
        risks.add(risk(
                "capitalGainsTaxDrag",
                taxDrag.compareTo(new BigDecimal("0.05")) > 0 ? "medium" : "info",
                "Podatki",
                "Rebalancing może uruchomić podatek",
                "Szac. podatek",
                moneyLabel(context.estimatedTax()),
                "19% zysku",
                "Niezrealizowany zysk w rachunku opodatkowanym to " + moneyLabel(context.taxableGain().max(BigDecimal.ZERO)) + ", a uproszczona rezerwa podatku to " + moneyLabel(context.estimatedTax()) + ".",
                "Preferuj rebalancing nowymi wpłatami; sprzedaż pozycji opodatkowanych rób świadomie z rezerwą podatkową."
        ));
    }

    private FireSummary.FireRisk risk(
            String id,
            String level,
            String area,
            String title,
            String metric,
            String value,
            String threshold,
            String detail,
            String recommendation
    ) {
        return new FireSummary.FireRisk(id, level, area, title, metric, value, threshold, detail, recommendation);
    }

    private int riskRank(String level) {
        return switch (level) {
            case "high" -> 0;
            case "medium" -> 1;
            case "low" -> 2;
            default -> 3;
        };
    }

    private BigDecimal largestPositionShare(List<FirePortfolioPosition> positions, BigDecimal total) {
        if (total.signum() == 0) {
            return BigDecimal.ZERO;
        }
        var grouped = new LinkedHashMap<String, BigDecimal>();
        for (var position : positions) {
            var isin = position.isin() == null ? "" : position.isin().trim();
            var instrument = position.instrument() == null ? "" : position.instrument().trim();
            var account = position.account() == null ? "" : position.account().trim();
            var key = !isin.isBlank() ? isin : instrument + "|" + account;
            grouped.merge(key, position.valuePln() == null ? BigDecimal.ZERO : position.valuePln(), BigDecimal::add);
        }
        return grouped.values().stream()
                .max(BigDecimal::compareTo)
                .map(value -> share(value, total))
                .orElse(BigDecimal.ZERO);
    }

    private BigDecimal nonPlnCurrencyShare(List<FirePortfolioPosition> positions, BigDecimal total) {
        if (total.signum() == 0) {
            return BigDecimal.ZERO;
        }
        var nonPln = positions.stream()
                .filter(position -> {
                    var currency = position.currency() == null ? "" : position.currency().trim();
                    return !currency.isBlank() && !currency.equalsIgnoreCase("PLN");
                })
                .map(FirePortfolioPosition::valuePln)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return share(nonPln, total);
    }

    private BigDecimal share(BigDecimal value, BigDecimal total) {
        if (total == null || total.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return value.divide(total, 6, RoundingMode.HALF_UP);
    }

    private String moneyLabel(BigDecimal amount) {
        var formatter = NumberFormat.getNumberInstance(Locale.forLanguageTag("pl-PL"));
        formatter.setMinimumFractionDigits(0);
        formatter.setMaximumFractionDigits(0);
        return formatter.format(money(amount)) + " zł";
    }

    private String percentLabel(BigDecimal ratio) {
        var formatter = NumberFormat.getPercentInstance(Locale.forLanguageTag("pl-PL"));
        formatter.setMinimumFractionDigits(1);
        formatter.setMaximumFractionDigits(1);
        return formatter.format(ratio == null ? BigDecimal.ZERO : ratio);
    }

    private BigDecimal money(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP);
    }

    record Context(
            FireSettings settings,
            List<FirePortfolioPosition> investmentPositions,
            List<FireSummary.FireAllocation> allocation,
            List<FireSummary.FireRebalanceAction> rebalancing,
            FireSummary.FireBudgetLink budgetLink,
            FireSummary.FireDataQuality dataQuality,
            BigDecimal investmentPortfolioValue,
            BigDecimal currentValue,
            BigDecimal retirementLocked,
            BigDecimal emergencyFund,
            BigDecimal taxableCapital,
            BigDecimal taxableGain,
            BigDecimal estimatedTax,
            boolean spendTargetConfigured,
            BigDecimal liquidBridgeGapTo60
    ) {
        Context {
            investmentPositions = investmentPositions == null ? List.of() : List.copyOf(investmentPositions);
            allocation = allocation == null ? List.of() : List.copyOf(allocation);
            rebalancing = rebalancing == null ? List.of() : List.copyOf(rebalancing);
            investmentPortfolioValue = investmentPortfolioValue == null ? BigDecimal.ZERO : investmentPortfolioValue;
            currentValue = currentValue == null ? BigDecimal.ZERO : currentValue;
            retirementLocked = retirementLocked == null ? BigDecimal.ZERO : retirementLocked;
            emergencyFund = emergencyFund == null ? BigDecimal.ZERO : emergencyFund;
            taxableCapital = taxableCapital == null ? BigDecimal.ZERO : taxableCapital;
            taxableGain = taxableGain == null ? BigDecimal.ZERO : taxableGain;
            estimatedTax = estimatedTax == null ? BigDecimal.ZERO : estimatedTax;
            liquidBridgeGapTo60 = liquidBridgeGapTo60 == null ? BigDecimal.ZERO : liquidBridgeGapTo60;
        }
    }
}
