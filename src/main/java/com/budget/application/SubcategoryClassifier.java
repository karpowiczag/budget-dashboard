package com.budget.application;

import java.util.List;
import java.util.regex.Pattern;

final class SubcategoryClassifier {
    String subcategory(String category, String description) {
        var source = normalize(description).toUpperCase();
        for (var rule : subcategoryRules(category)) {
            if (rule.pattern().matcher(source).find()) {
                return rule.label();
            }
        }
        if ("Do sprawdzenia".equals(category)) {
            return "Wymaga ręcznej decyzji";
        }
        if (BudgetCatalog.EXCLUDED.contains(category)) {
            return "Przepływ wyłączony z kosztów życia";
        }
        return "Ogólne";
    }

    private String normalize(String value) {
        return value == null ? "" : value.replaceAll("\\s+", " ").trim();
    }

    private List<SubcategoryRule> subcategoryRules(String category) {
        return switch (category) {
            case "Żywność i chemia" -> List.of(
                    sub("ROSSMANN|HEBE|DM-DROGERIE", "Drogeria/chemia"),
                    sub("PIEKARNIA|CUKIERNIA|GORĄCO POLECAM|GORACO POLECAM|SLODKI CHLOPAK|SŁODKI CHŁOPAK", "Piekarnia/cukiernia"),
                    sub("BIEDRONKA|LIDL|CARREFOUR|AUCHAN|KAUFLAND|ALDI|DINO|STOKROTKA|LEWIATAN|SPOLEM|SPOŁEM|TESCO|BILLA|SPAR|SUPERMARKET|MARKET", "Market spożywczy")
            );
            case "Jedzenie poza domem" -> List.of(
                    sub("UBER EATS|BOLT FOOD|PYSZNE|GLOVO|WOLT", "Dostawy"),
                    sub("CAFE|KAWA|STARBUCKS|COSTA|KAVY|BELLA BEAN", "Kawa i kawiarnie"),
                    sub("MCDONALD|KFC|POPEYES|BURGER|BURGUER|KEBAP|DONER|PIZZA", "Fast food"),
                    sub("RESTAUR|SUSHI|RAMEN|THAI|KUCHNIA|PIEROGARNIA|BISTRO|KARCZMA", "Restauracje")
            );
            case "Zdrowie i uroda" -> List.of(
                    sub("APTEKA|APOTHEKE|DOZ|FARMAC|SUPER-PHARM", "Apteka"),
                    sub("LEKARZ|MEDICOVER|LUX MED|LUXMED|DENT|ALAB|MEDICCENTRE|OVO MEDICAL|ZETMED", "Lekarz/badania"),
                    sub("SEPHORA|DOUGLAS|NOTINO|FRISER|MIDNIGHT MEADO", "Kosmetyki/usługi urody"),
                    sub("FIELMANN|OPTYCZNY|OKULAR", "Optyk")
            );
            case "Podróże i wyjazdy" -> List.of(
                    sub("BOOKING|BKG\\*HOTEL|AIRBNB|HOTEL|PENSJONAT|PARK INN|ARBIO|DOM ZU SALZBURG|VLTAVSKA POHADKA", "Noclegi"),
                    sub("RYANAIR|WIZZAIR|LOT\\.COM|LOT ", "Loty"),
                    sub("SIXT|ARCTIC CAMPERS", "Wynajem auta/campera"),
                    sub("SCHOENBRUNN|ALPINCENTER|DALI|BAHN|LANOVA|BANYS", "Atrakcje w podróży")
            );
            case "Transport i parking" -> List.of(
                    sub("UBER|BOLT|FREE NOW", "Taxi/VTC"),
                    sub("ZTM|MPK|PKP|KOLEO|INTERCITY|FLIXBUS|INFOBUS|WIENER LINIEN", "Komunikacja i bilety"),
                    sub("PARKING|PARKOVISTE|PARKOMAT|APCOA|PARKUM|GARAGE|GARAZE|SPP|KBU", "Parking"),
                    sub("AUTOSTRADA|VIGNETTE|EDALNICE|DALNICKA", "Drogi i winiety")
            );
            case "Paliwo i auto" -> List.of(
                    sub("ORLEN|BP |SHELL|CIRCLE K|MOL |STACJA", "Paliwo"),
                    sub("SERWIS|CZĘŚCI|CZESCI|MOTORPOL|MYJNIA", "Serwis/myjnia/części")
            );
            case "Odzież i obuwie" -> List.of(
                    sub("VINTED", "Second hand/Vinted"),
                    sub("CCC|OBUWIE|DEICHMANN|BALAGANSTUDIO", "Buty"),
                    sub("ZALANDO|H&M|\\bHM\\b|RESERVED|SINSAY|MOHITO|ZARA|CROPP|PRIMARK|MEDICINE|HOUSE|STRADIVARIUS|NEW YORKER", "Ubrania")
            );
            case "Wyjścia i wydarzenia" -> List.of(
                    sub("KINO|CINEMA", "Kino"),
                    sub("KONCERT|OZZY|KNOCK OUT|BRUTALASSAULT|FESTIVAL", "Koncerty/festiwale"),
                    sub("MUZEUM|ZAMEK|NHM|SCHOENBRUNN|BANYS|ENTRITT", "Muzea/atrakcje"),
                    sub("TICKET|BILET|KICKET|TICKETMASTER|KUPBILECIK|STAGE24", "Bilety")
            );
            case "Marketplace i zakupy online" -> List.of(
                    sub("ALLEGRO", "Allegro - do rozbicia"),
                    sub("AMAZON", "Amazon - do rozbicia"),
                    sub("TEMU|ALIEXPRESS", "Marketplace tani - do kontroli"),
                    sub("PAYPAL \\*ETSY|ETSY", "Etsy/PayPal")
            );
            case "Oszczędności i inwestycje" -> List.of(
                    sub("IKZE", "IKZE"),
                    sub("FUNDUSZ|SUBFUNDUSZ|SFI", "Fundusze"),
                    sub("MAKLERSK|BROKERAGE|BM MBANKU", "Rachunek maklerski")
            );
            case "Spłata karty kredytowej" -> List.of(
                    sub("RĘCZNA SPŁATA|RECZNA SPLATA", "Wychodząca spłata karty"),
                    sub("SPŁATA - PRZELEW WEWNĘTRZNY|SPLATA - PRZELEW WEWNETRZNY", "Księgowanie na karcie")
            );
            case "Rata kredytu" -> List.of(sub("KREDYT -", "Rata hipoteczna/kredytowa"));
            case "Nadpłata kredytu" -> List.of(sub("KREDYT -", "Nadpłata kapitału"));
            default -> List.of();
        };
    }

    private SubcategoryRule sub(String regex, String label) {
        return new SubcategoryRule(Pattern.compile(regex, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE), label);
    }

    private record SubcategoryRule(Pattern pattern, String label) {
    }
}
