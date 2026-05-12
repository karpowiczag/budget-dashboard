package com.budget.application.fire;

import com.budget.domain.fire.FirePortfolioPosition;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class FireInstrumentClassifier {
    static final String ASSET_EQUITY = "Akcje";
    static final String ASSET_BONDS = "Obligacje";
    static final String ASSET_CASH = "Gotówka";
    static final String ASSET_ALTERNATIVES = "Alternatywne";
    static final String ASSET_MULTI_ASSET = "Mieszane";
    static final String ASSET_UNKNOWN = "Inne";

    static final String WRAPPER_EMERGENCY = "Poduszka bezpieczeństwa";
    static final String WRAPPER_RETIREMENT = "Emerytalne długoterminowe";
    static final String WRAPPER_TAXABLE = "Rachunek opodatkowany";

    FireInstrumentProfile classify(FirePortfolioPosition position) {
        var text = normalizedText(position);
        var rawAssetClass = clean(position.assetClass());
        var wrapper = clean(position.wrapper());
        var emergency = WRAPPER_EMERGENCY.equals(wrapper);
        var retirement = WRAPPER_RETIREMENT.equals(wrapper);
        var taxable = WRAPPER_TAXABLE.equals(wrapper);
        var fundLike = containsAny(text, "etf", "ucits", "fundusz", "fundusze", "fio", "sfio", "tfi", "index", "indeks");
        var etf = containsAny(text, "etf", "ucits");
        var multiAsset = containsAny(text,
                "lifestrategy",
                "life strategy",
                "multi-asset",
                "multi asset",
                "mieszany",
                "mieszane",
                "gotowe strategie",
                "strategia na min",
                "80% equity",
                "80 equity"
        );
        var factorTilt = containsAny(text,
                "minimum volatility",
                "min volatility",
                "quality",
                "momentum",
                "value",
                "factor",
                "edge",
                "nasdaq",
                "sector",
                "sektor"
        );
        var broadMarket = containsAny(text,
                "msci world",
                "ftse all-world",
                "ftse all world",
                "acwi",
                "s&p 500",
                "sp500",
                "s p 500",
                "emerging markets",
                "developed markets",
                "global",
                "world",
                "all-world",
                "all world"
        );
        var reasons = new ArrayList<String>();
        var assetClass = assetClass(text, rawAssetClass, emergency, multiAsset, reasons);
        var singleSecurity = ASSET_EQUITY.equals(assetClass) && !fundLike && containsAny(text, "akcje", "gpw", "bourse", "börse", "stock");
        var instrumentType = instrumentType(assetClass, text, etf, fundLike, broadMarket, singleSecurity, multiAsset, factorTilt);
        var fireRole = fireRole(assetClass, wrapper, broadMarket, singleSecurity);
        var currency = clean(position.currency()).toUpperCase(Locale.ROOT);
        var nonPln = !currency.isBlank() && !"PLN".equals(currency);
        var unknown = ASSET_UNKNOWN.equals(assetClass) || wrapper.equals(ASSET_UNKNOWN) || wrapper.isBlank();
        return new FireInstrumentProfile(
                assetClass,
                instrumentType,
                fireRole,
                wrapper.isBlank() ? ASSET_UNKNOWN : wrapper,
                fundLike,
                etf,
                broadMarket,
                multiAsset,
                factorTilt,
                singleSecurity,
                emergency,
                retirement,
                taxable,
                nonPln,
                unknown,
                List.copyOf(reasons)
        );
    }

    private String assetClass(String text, String rawAssetClass, boolean emergency, boolean multiAsset, List<String> reasons) {
        if (emergency || containsAny(text, "konto gotowkowe", "konto oszczednosciowe", "gotowka", "cash", "lokata", "pieniezny", "money market")) {
            reasons.add("płynność");
            return ASSET_CASH;
        }
        if (multiAsset) {
            reasons.add("produkt mieszany wymagający look-through");
            return ASSET_MULTI_ASSET;
        }
        if (containsAny(text, "oblig", "bond", "treasury", "tbill", "t-bill", "skarb", "edo", "tos", "coi", "rod", "ds", "ok")) {
            reasons.add("instrument dłużny");
            return ASSET_BONDS;
        }
        if (containsAny(text, "etc", "gold", "zloto", "złoto", "silver", "commodity", "surow", "reit", "crypto", "bitcoin", "ethereum")) {
            reasons.add("aktywo alternatywne");
            return ASSET_ALTERNATIVES;
        }
        if (containsAny(text, "akcje", "stock", "equity", "etf", "ucits", "fundusz", "fundusze", "fio", "sfio", "tfi", "msci", "ftse", "s&p", "sp500", "nasdaq")) {
            reasons.add("ekspozycja akcyjna/funduszowa");
            return ASSET_EQUITY;
        }
        if (isKnownAssetClass(rawAssetClass)) {
            reasons.add("klasa z importu MyFund");
            return rawAssetClass;
        }
        reasons.add("brak jednoznacznego słownika");
        return ASSET_UNKNOWN;
    }

    private String instrumentType(String assetClass, String text, boolean etf, boolean fundLike, boolean broadMarket, boolean singleSecurity, boolean multiAsset, boolean factorTilt) {
        if (ASSET_CASH.equals(assetClass)) {
            return containsAny(text, "oszczednosciowe", "oszczędnościowe") ? "Konto oszczędnościowe / gotówka" : "Gotówka / rachunek pieniężny";
        }
        if (ASSET_MULTI_ASSET.equals(assetClass)) {
            if (containsAny(text, "lifestrategy", "80% equity", "80 equity")) {
                return "ETF mieszany 80/20";
            }
            return etf || fundLike ? "Fundusz mieszany/strategia gotowa" : "Produkt mieszany";
        }
        if (ASSET_BONDS.equals(assetClass)) {
            return etf || fundLike ? "Fundusz/ETF obligacyjny" : "Obligacja lub instrument dłużny";
        }
        if (ASSET_ALTERNATIVES.equals(assetClass)) {
            if (containsAny(text, "gold", "zloto", "złoto", "etc")) {
                return "ETC/surowiec";
            }
            return "Satelita alternatywny";
        }
        if (ASSET_EQUITY.equals(assetClass)) {
            if (etf && factorTilt) {
                return "ETF akcyjny faktorowy/tematyczny";
            }
            if (etf && broadMarket) {
                return "ETF akcyjny szerokiego rynku";
            }
            if (etf) {
                return "ETF akcyjny tematyczny/regionalny";
            }
            if (fundLike) {
                return "Fundusz akcyjny";
            }
            if (singleSecurity) {
                return "Akcja pojedyncza";
            }
            return "Ekspozycja akcyjna";
        }
        return "Nieustalony typ instrumentu";
    }

    private String fireRole(String assetClass, String wrapper, boolean broadMarket, boolean singleSecurity) {
        if (WRAPPER_EMERGENCY.equals(wrapper)) {
            return "Płynność i poduszka";
        }
        if (WRAPPER_RETIREMENT.equals(wrapper)) {
            return "Kapitał po 60/65";
        }
        return switch (assetClass) {
            case ASSET_EQUITY -> broadMarket && !singleSecurity ? "Rdzeń wzrostowy FIRE" : "Satelita wzrostowy";
            case ASSET_BONDS -> "Stabilizator i pomost";
            case ASSET_CASH -> "Płynność/pomost";
            case ASSET_ALTERNATIVES -> "Satelita dywersyfikacyjny";
            case ASSET_MULTI_ASSET -> "Produkt mieszany do look-through";
            default -> "Do ręcznej klasyfikacji";
        };
    }

    private boolean isKnownAssetClass(String value) {
        return ASSET_EQUITY.equals(value)
                || ASSET_BONDS.equals(value)
                || ASSET_CASH.equals(value)
                || ASSET_ALTERNATIVES.equals(value)
                || ASSET_MULTI_ASSET.equals(value);
    }

    private boolean containsAny(String text, String... tokens) {
        for (var token : tokens) {
            if (text.contains(normalize(token))) {
                return true;
            }
        }
        return false;
    }

    private String normalizedText(FirePortfolioPosition position) {
        return normalize(String.join(" ",
                clean(position.assetClass()),
                clean(position.group()),
                clean(position.instrument()),
                clean(position.account()),
                clean(position.isin()),
                clean(position.currency())
        ));
    }

    private String normalize(String value) {
        var polishSafe = clean(value)
                .replace('ł', 'l')
                .replace('Ł', 'L');
        var noAccents = Normalizer.normalize(polishSafe, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return noAccents.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9&+\\-. ]", " ").replaceAll("\\s+", " ").trim();
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }

    record FireInstrumentProfile(
            String assetClass,
            String instrumentType,
            String fireRole,
            String wrapper,
            boolean fundLike,
            boolean etf,
            boolean broadMarket,
            boolean multiAsset,
            boolean factorTilt,
            boolean singleSecurity,
            boolean emergency,
            boolean retirement,
            boolean taxable,
            boolean nonPln,
            boolean unknown,
            List<String> classificationReasons
    ) {
        FireInstrumentProfile {
            classificationReasons = classificationReasons == null ? List.of() : List.copyOf(classificationReasons);
        }
    }
}
