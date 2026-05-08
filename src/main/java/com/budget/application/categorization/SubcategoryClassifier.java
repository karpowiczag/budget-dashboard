package com.budget.application.categorization;

import java.util.List;
import java.util.regex.Pattern;

final class SubcategoryClassifier {
    SubcategoryDecision subcategory(String categoryId, String description) {
        var source = normalize(description).toUpperCase();
        for (var rule : subcategoryRules(categoryId)) {
            if (rule.pattern().matcher(source).find()) {
                return new SubcategoryDecision(rule.id(), rule.label());
            }
        }
        return SubcategoryDecision.none();
    }

    private String normalize(String value) {
        return value == null ? "" : value.replaceAll("\\s+", " ").trim();
    }

    private List<SubcategoryRule> subcategoryRules(String categoryId) {
        return switch (categoryId) {
            case "groceries" -> List.of(
                    sub("drugstoreChemistry", "ROSSMANN|HEBE|DM-DROGERIE", "Drogeria/chemia"),
                    sub("bakery", "PIEKARNIA|CUKIERNIA|GORĄCO POLECAM|GORACO POLECAM|SLODKI CHLOPAK|SŁODKI CHŁOPAK", "Piekarnia/cukiernia"),
                    sub("groceryMarket", "BIEDRONKA|LIDL|CARREFOUR|AUCHAN|KAUFLAND|ALDI|DINO|STOKROTKA|LEWIATAN|SPOLEM|SPOŁEM|TESCO|BILLA|SPAR|SUPERMARKET|MARKET", "Market spożywczy")
            );
            case "diningOut" -> List.of(
                    sub("delivery", "UBER EATS|BOLT FOOD|PYSZNE|GLOVO|WOLT", "Dostawy"),
                    sub("coffee", "CAFE|KAWA|STARBUCKS|COSTA|KAVY|BELLA BEAN", "Kawa i kawiarnie"),
                    sub("fastFood", "MCDONALD|KFC|POPEYES|BURGER|BURGUER|KEBAP|DONER|PIZZA", "Fast food"),
                    sub("restaurants", "RESTAUR|SUSHI|RAMEN|THAI|KUCHNIA|PIEROGARNIA|BISTRO|KARCZMA", "Restauracje")
            );
            case "medicalPharmacy" -> List.of(
                    sub("pharmacy", "APTEKA|APOTHEKE|DOZ|FARMAC|SUPER-PHARM", "Apteka"),
                    sub("doctorTests", "LEKARZ|MEDICOVER|LUX MED|LUXMED|DENT|ALAB|MEDICCENTRE|OVO MEDICAL|ZETMED|WITEK", "Lekarz/badania"),
                    sub("optician", "FIELMANN|OPTYCZNY|OKULAR", "Optyk")
            );
            case "beautyCosmetics" -> List.of(
                    sub("cosmetics", "SEPHORA|DOUGLAS|NOTINO|ROSSMANN|HEBE|DM-DROGERIE", "Kosmetyki"),
                    sub("beautyServices", "FRISER|MIDNIGHT MEADO", "Usługi urody")
            );
            case "travel" -> List.of(
                    sub("lodging", "BOOKING|BKG\\*HOTEL|AIRBNB|HOTEL|PENSJONAT|PARK INN|ARBIO|DOM ZU SALZBURG|VLTAVSKA POHADKA", "Noclegi"),
                    sub("flights", "RYANAIR|WIZZAIR|LOT\\.COM|LOT ", "Loty"),
                    sub("rentalCar", "SIXT|ARCTIC CAMPERS", "Wynajem auta/campera"),
                    sub("travelAttractions", "SCHOENBRUNN|ALPINCENTER|DALI|BAHN|LANOVA|BANYS", "Atrakcje w podróży")
            );
            case "transportParking" -> List.of(
                    sub("taxi", "UBER|BOLT|FREE NOW", "Taxi/VTC"),
                    sub("publicTransport", "ZTM|MPK|PKP|KOLEO|INTERCITY|FLIXBUS|INFOBUS|WIENER LINIEN", "Komunikacja i bilety"),
                    sub("parking", "PARKING|PARKOVISTE|PARKOMAT|APCOA|PARKUM|GARAGE|GARAZE|SPP|KBU", "Parking"),
                    sub("roads", "AUTOSTRADA|VIGNETTE|EDALNICE|DALNICKA", "Drogi i winiety")
            );
            case "fuelCar" -> List.of(
                    sub("fuel", "ORLEN|BP |SHELL|CIRCLE K|MOL |STACJA", "Paliwo"),
                    sub("service", "SERWIS|CZĘŚCI|CZESCI|MOTORPOL|MYJNIA", "Serwis/myjnia/części")
            );
            case "telecom" -> List.of(
                    sub("phone", "PLUS\\s+(TATIANA|ALEKSANDER),?\\s*E-FAKTURA|ORANGE|T-MOBILE|\\bPLAY\\b", "Telefon"),
                    sub("internetTv", "KORBANK|UPC|VECTRA|NETIA", "Internet/TV")
            );
            case "clothing" -> List.of(
                    sub("secondHand", "VINTED", "Second hand/Vinted"),
                    sub("shoes", "CCC|OBUWIE|DEICHMANN|BALAGANSTUDIO", "Buty"),
                    sub("clothes", "ZALANDO|H&M|\\bHM\\b|RESERVED|SINSAY|MOHITO|ZARA|CROPP|PRIMARK|MEDICINE|HOUSE|STRADIVARIUS|NEW YORKER", "Ubrania")
            );
            case "events" -> List.of(
                    sub("cinema", "KINO|CINEMA", "Kino"),
                    sub("concerts", "KONCERT|OZZY|KNOCK OUT|BRUTALASSAULT|FESTIVAL", "Koncerty/festiwale"),
                    sub("museumsAttractions", "MUZEUM|ZAMEK|NHM|SCHOENBRUNN|BANYS|ENTRITT", "Muzea/atrakcje"),
                    sub("tickets", "TICKET|BILET|KICKET|TICKETMASTER|KUPBILECIK|STAGE24", "Bilety")
            );
            case BudgetTaxonomy.CATEGORY_MARKETPLACE -> List.of(
                    sub("allegro", "ALLEGRO", "Allegro"),
                    sub("amazon", "AMAZON", "Amazon"),
                    sub("cheapMarketplace", "TEMU|ALIEXPRESS", "Marketplace tani"),
                    sub("etsyPaypal", "PAYPAL \\*ETSY|ETSY", "Etsy/PayPal")
            );
            case BudgetTaxonomy.CATEGORY_INVESTMENTS -> List.of(
                    sub("ikze", "IKZE", "IKZE"),
                    sub("funds", "FUNDUSZ|SUBFUNDUSZ|SFI", "Fundusze"),
                    sub("brokerage", "MAKLERSK|BROKERAGE|BM MBANKU", "Rachunek maklerski")
            );
            case BudgetTaxonomy.CATEGORY_SAVINGS_ACCOUNT -> List.of(
                    sub("savingsAccount", "KONTO OSZCZĘDNOŚCIOWE|KONTO OSZCZEDNOSCIOWE|RACHUNEK OSZCZĘDNOŚCIOWY|RACHUNEK OSZCZEDNOSCIOWY", "Konto oszczędnościowe"),
                    sub("savingsTransfer", "OSZCZĘDNOŚCI|OSZCZEDNOSCI", "Przelew oszczędnościowy")
            );
            case "cardRepayment" -> List.of(
                    sub("cardRepaymentOut", "RĘCZNA SPŁATA|RECZNA SPLATA", "Wychodząca spłata karty"),
                    sub("cardInternalPosting", "SPŁATA - PRZELEW WEWNĘTRZNY|SPLATA - PRZELEW WEWNETRZNY", "Księgowanie na karcie")
            );
            case "loanInstallment" -> List.of(sub("mortgageInstallment", "KREDYT -", "Rata hipoteczna/kredytowa"));
            case BudgetTaxonomy.CATEGORY_LOAN_OVERPAYMENT -> List.of(sub("capitalOverpayment", "KREDYT -", "Nadpłata kapitału"));
            default -> List.of();
        };
    }

    private SubcategoryRule sub(String id, String regex, String label) {
        return new SubcategoryRule(id, Pattern.compile(regex, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE), label);
    }

    record SubcategoryDecision(String id, String label) {
        static SubcategoryDecision none() {
            return new SubcategoryDecision("", "");
        }
    }

    private record SubcategoryRule(String id, Pattern pattern, String label) {
    }
}
