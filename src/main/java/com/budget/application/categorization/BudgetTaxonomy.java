package com.budget.application.categorization;

import com.budget.domain.category.CategoryRule;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public final class BudgetTaxonomy {
    static final String FLOW_INCOME = "income";
    static final String FLOW_LIVING_EXPENSE = "livingExpense";
    static final String FLOW_WEALTH_TRANSFER = "wealthTransfer";
    static final String FLOW_TECHNICAL_TRANSFER = "technicalTransfer";
    static final String FLOW_REFUND_CORRECTION = "refundCorrection";
    static final String FLOW_REVIEW = "review";

    static final String GROUP_INCOME = "income";
    static final String GROUP_OBLIGATORY_FIXED = "obligatoryFixed";
    static final String GROUP_OBLIGATORY_VARIABLE = "obligatoryVariable";
    static final String GROUP_DISCRETIONARY = "discretionary";
    static final String GROUP_NON_MONTHLY = "nonMonthly";
    static final String GROUP_WEALTH_BUILDING = "wealthBuilding";
    static final String GROUP_REVIEW_SPLIT = "reviewSplit";
    static final String GROUP_TECHNICAL_EXCLUDED = "technicalExcluded";

    static final String REVIEW_OK = "ok";
    static final String REVIEW_NEEDS_REVIEW = "needsReview";
    static final String REVIEW_NEEDS_SPLIT = "needsSplit";

    static final String CATEGORY_UNKNOWN = "unknownReview";
    static final String CATEGORY_MARKETPLACE = "marketplaceOnline";
    static final String CATEGORY_SAVINGS_ACCOUNT = "savingsAccount";
    static final String CATEGORY_INVESTMENTS = "investments";
    static final String CATEGORY_LOAN_OVERPAYMENT = "loanOverpayment";

    static final Map<String, BudgetGroup> BUDGET_GROUPS = orderedMap(List.of(
            new BudgetGroup(GROUP_INCOME, "Przychody"),
            new BudgetGroup(GROUP_OBLIGATORY_FIXED, "Obowiązkowe stałe"),
            new BudgetGroup(GROUP_OBLIGATORY_VARIABLE, "Obowiązkowe zmienne"),
            new BudgetGroup(GROUP_DISCRETIONARY, "Nieobowiązkowe"),
            new BudgetGroup(GROUP_NON_MONTHLY, "Nieregularne"),
            new BudgetGroup(GROUP_WEALTH_BUILDING, "Budowanie majątku"),
            new BudgetGroup(GROUP_REVIEW_SPLIT, "Do rozbicia"),
            new BudgetGroup(GROUP_TECHNICAL_EXCLUDED, "Transfer techniczny")
    ), BudgetGroup::id);

    static final Map<String, CategoryDefinition> CATEGORIES = orderedMap(List.of(
            category("salary", "Pensja", "Przychody", "Wpływy", GROUP_INCOME, "Przychody", "Do oceny", FLOW_INCOME, false, false, true),
            category("refundCorrection", "Zwroty i korekty", "Transfery techniczne", "Transfery", GROUP_TECHNICAL_EXCLUDED, "Transfer techniczny", "Transfer/wyłączone", FLOW_REFUND_CORRECTION, false, true, false),
            category("taxRefund", "Zwrot podatku", "Transfery techniczne", "Transfery", GROUP_TECHNICAL_EXCLUDED, "Transfer techniczny", "Transfer/wyłączone", FLOW_REFUND_CORRECTION, false, true, false),
            category("interest", "Odsetki", "Transfery techniczne", "Transfery", GROUP_TECHNICAL_EXCLUDED, "Transfer techniczny", "Transfer/wyłączone", FLOW_REFUND_CORRECTION, false, true, false),
            category("ownTransfers", "Przelewy własne", "Transfery techniczne", "Transfery", GROUP_TECHNICAL_EXCLUDED, "Transfer techniczny", "Transfer/wyłączone", FLOW_TECHNICAL_TRANSFER, false, true, false),
            category("cardRepayment", "Spłata karty kredytowej", "Transfery techniczne", "Finanse", GROUP_TECHNICAL_EXCLUDED, "Transfer techniczny", "Transfer/wyłączone", FLOW_TECHNICAL_TRANSFER, false, true, false),
            category("cashToSettle", "Gotówka do rozliczenia", "Transfery techniczne", "Gotówka", GROUP_TECHNICAL_EXCLUDED, "Transfer techniczny", "Transfer/wyłączone", FLOW_TECHNICAL_TRANSFER, false, true, false),
            category("cashDeposit", "Wpłata gotówki", "Transfery techniczne", "Gotówka", GROUP_TECHNICAL_EXCLUDED, "Transfer techniczny", "Transfer/wyłączone", FLOW_TECHNICAL_TRANSFER, false, true, false),
            category("loanInstallment", "Rata kredytu", "Zobowiązania", "Finanse", GROUP_OBLIGATORY_FIXED, "Obowiązkowe stałe", "Stałe", FLOW_LIVING_EXPENSE, false, false, false),
            category("installmentDebt", "Spłaty i raty", "Zobowiązania", "Finanse", GROUP_OBLIGATORY_FIXED, "Obowiązkowe stałe", "Stałe", FLOW_LIVING_EXPENSE, false, false, false),
            category(CATEGORY_LOAN_OVERPAYMENT, "Nadpłata kredytu", "Majątek i inwestycje", "Finanse", GROUP_WEALTH_BUILDING, "Nadpłata kredytu", "Oszczędności", FLOW_WEALTH_TRANSFER, false, true, false),
            category(CATEGORY_INVESTMENTS, "Inwestycje", "Majątek i inwestycje", "Oszczędności", GROUP_WEALTH_BUILDING, "Inwestycje", "Oszczędności", FLOW_WEALTH_TRANSFER, false, true, false),
            category(CATEGORY_SAVINGS_ACCOUNT, "Konto oszczędnościowe", "Majątek i inwestycje", "Oszczędności", GROUP_WEALTH_BUILDING, "Konto oszczędnościowe", "Oszczędności", FLOW_WEALTH_TRANSFER, false, true, false),

            category("rent", "Czynsz i wynajem", "Dom i mieszkanie", "Mieszkanie", GROUP_OBLIGATORY_FIXED, "Obowiązkowe stałe", "Stałe", FLOW_LIVING_EXPENSE, false, false, false),
            category("electricity", "Prąd", "Koszty stałe", "Rachunki", GROUP_OBLIGATORY_FIXED, "Obowiązkowe stałe", "Stałe", FLOW_LIVING_EXPENSE, false, false, false),
            category("telecom", "TV, internet, telefon", "Koszty stałe", "Rachunki", GROUP_OBLIGATORY_FIXED, "Obowiązkowe stałe", "Stałe", FLOW_LIVING_EXPENSE, false, false, false),
            category("insurance", "Ubezpieczenia", "Koszty stałe", "Rachunki", GROUP_OBLIGATORY_FIXED, "Obowiązkowe stałe", "Stałe", FLOW_LIVING_EXPENSE, false, false, false),
            category("taxes", "Podatki", "Koszty stałe", "Podatki", GROUP_OBLIGATORY_FIXED, "Obowiązkowe stałe", "Stałe", FLOW_LIVING_EXPENSE, false, false, false),
            category("publicFees", "Opłaty publiczne", "Koszty stałe", "Opłaty", GROUP_OBLIGATORY_FIXED, "Obowiązkowe stałe", "Stałe", FLOW_LIVING_EXPENSE, false, false, false),
            category("bankFees", "Opłaty bankowe", "Zobowiązania", "Finanse", GROUP_OBLIGATORY_FIXED, "Obowiązkowe stałe", "Stałe", FLOW_LIVING_EXPENSE, false, false, false),

            category("groceries", "Żywność i chemia", "Koszty codzienne", "Potrzeby podstawowe", GROUP_OBLIGATORY_VARIABLE, "Obowiązkowe zmienne", "Zmienne konieczne", FLOW_LIVING_EXPENSE, false, false, false),
            category("medicalPharmacy", "Lekarz i apteka", "Koszty codzienne", "Zdrowie", GROUP_OBLIGATORY_VARIABLE, "Obowiązkowe zmienne", "Zmienne konieczne", FLOW_LIVING_EXPENSE, false, false, false),
            category("pets", "Zwierzęta", "Koszty codzienne", "Potrzeby podstawowe", GROUP_OBLIGATORY_VARIABLE, "Obowiązkowe zmienne", "Zmienne konieczne", FLOW_LIVING_EXPENSE, false, false, false),
            category("transportParking", "Transport i parking", "Transport", "Transport", GROUP_OBLIGATORY_VARIABLE, "Obowiązkowe zmienne", "Zmienne konieczne", FLOW_LIVING_EXPENSE, false, false, false),
            category("fuelCar", "Paliwo i auto", "Transport", "Transport", GROUP_OBLIGATORY_VARIABLE, "Obowiązkowe zmienne", "Zmienne konieczne", FLOW_LIVING_EXPENSE, false, false, false),

            category("diningOut", "Jedzenie poza domem", "Styl życia", "Styl życia", GROUP_DISCRETIONARY, "Nieobowiązkowe", "Uznaniowe", FLOW_LIVING_EXPENSE, true, false, false),
            category("beautyCosmetics", "Uroda i kosmetyki", "Styl życia", "Styl życia", GROUP_DISCRETIONARY, "Nieobowiązkowe", "Uznaniowe", FLOW_LIVING_EXPENSE, true, false, false),
            category("clothing", "Odzież i obuwie", "Styl życia", "Styl życia", GROUP_DISCRETIONARY, "Nieobowiązkowe", "Uznaniowe", FLOW_LIVING_EXPENSE, true, false, false),
            category("events", "Wyjścia i wydarzenia", "Styl życia", "Styl życia", GROUP_DISCRETIONARY, "Nieobowiązkowe", "Uznaniowe", FLOW_LIVING_EXPENSE, true, false, false),
            category("media", "Multimedia, książki i prasa", "Styl życia", "Styl życia", GROUP_DISCRETIONARY, "Nieobowiązkowe", "Uznaniowe", FLOW_LIVING_EXPENSE, true, false, false),
            category("sportHobby", "Sport i hobby", "Styl życia", "Styl życia", GROUP_DISCRETIONARY, "Nieobowiązkowe", "Uznaniowe", FLOW_LIVING_EXPENSE, true, false, false),
            category("giftsSupport", "Prezenty i wsparcie", "Styl życia", "Styl życia", GROUP_DISCRETIONARY, "Nieobowiązkowe", "Uznaniowe", FLOW_LIVING_EXPENSE, true, false, false),
            category("electronics", "Elektronika", "Styl życia", "Styl życia", GROUP_DISCRETIONARY, "Nieobowiązkowe", "Uznaniowe", FLOW_LIVING_EXPENSE, true, false, false),
            category("travel", "Podróże i wyjazdy", "Styl życia", "Styl życia", GROUP_NON_MONTHLY, "Nieregularne", "Nieregularne", FLOW_LIVING_EXPENSE, true, false, false),
            category("travelShopping", "Zakupy w podróży", "Styl życia", "Styl życia", GROUP_DISCRETIONARY, "Nieobowiązkowe", "Uznaniowe", FLOW_LIVING_EXPENSE, true, false, false),
            category("homeGoods", "Dom i wyposażenie", "Dom i mieszkanie", "Dom", GROUP_NON_MONTHLY, "Nieregularne", "Nieregularne", FLOW_LIVING_EXPENSE, false, false, false),
            category("renovationGarden", "Remont i ogród", "Dom i mieszkanie", "Dom", GROUP_NON_MONTHLY, "Nieregularne", "Nieregularne", FLOW_LIVING_EXPENSE, false, false, false),
            category(CATEGORY_MARKETPLACE, "Marketplace i zakupy online", "Zakupy mieszane", "Zakupy mieszane", GROUP_REVIEW_SPLIT, "Do rozbicia", "Do rozbicia", FLOW_LIVING_EXPENSE, true, false, false),
            category("personalOther", "Inne osobiste", "Styl życia", "Styl życia", GROUP_DISCRETIONARY, "Nieobowiązkowe", "Uznaniowe", FLOW_LIVING_EXPENSE, true, false, false),
            category(CATEGORY_UNKNOWN, "Niesklasyfikowane", "Audyt danych", "Jakość danych", GROUP_REVIEW_SPLIT, "Do rozbicia", "Do oceny", FLOW_REVIEW, false, false, false)
    ), CategoryDefinition::id);

    static final Set<String> WEALTH_CATEGORY_IDS = Set.of(CATEGORY_INVESTMENTS, CATEGORY_SAVINGS_ACCOUNT, CATEGORY_LOAN_OVERPAYMENT);

    private static final Set<String> WEALTH_CATEGORY_LABELS = WEALTH_CATEGORY_IDS.stream()
            .map(id -> CATEGORIES.get(id).label())
            .collect(Collectors.toUnmodifiableSet());

    private static final Map<String, String> LEGACY_CATEGORY_LABEL_ALIASES = Map.of(
            "Oszczędności i inwestycje", CATEGORY_INVESTMENTS,
            "Zdrowie i uroda", "medicalPharmacy",
            "Do sprawdzenia", CATEGORY_UNKNOWN
    );

    static final List<CategoryRule> RULES = List.of(
            rule("RĘCZNA SPŁATA KARTY KREDYT|RECZNA SPLATA KARTY KREDYT|SPŁATA KARTY|SPLATA KARTY|SPŁATA - PRZELEW WEWNĘTRZNY|SPLATA - PRZELEW WEWNETRZNY", "cardRepayment"),
            rule("KREDYT - (WCZEŚNIEJSZA SPŁATA|WCZESNIEJSZA SPLATA)", CATEGORY_LOAN_OVERPAYMENT),
            rule("KREDYT - (SPŁATA RATY|SPLATA RATY)", "loanInstallment"),
            rule("ONLINE\\.SANTANDERC|ONLINE\\.SANTANDERCONSUMER|SANTANDERCONSUMER", "installmentDebt"),
            rule("\\bRATA\\b|\\bRATY\\b|POŻYCZKA|POZYCZKA", "installmentDebt"),
            rule("KONTO OSZCZĘDNOŚCIOWE|KONTO OSZCZEDNOSCIOWE|RACHUNEK OSZCZĘDNOŚCIOWY|RACHUNEK OSZCZEDNOSCIOWY|OSZCZĘDNOŚCIOWE|OSZCZEDNOSCIOWE|OSZCZĘDNOŚCI|OSZCZEDNOSCI", CATEGORY_SAVINGS_ACCOUNT),
            rule("PRZELEW WŁASNY|PRZELEW WLASNY|PRZELEW ŚRODKÓW|PRZELEW SRODKOW|PRZELEW WEWNĘTRZNY|PRZELEW WEWNETRZNY", "ownTransfers"),
            rule("WYNAGRODZENIE|SALARY|PENSJA", "salary"),
            rule("ZWROT ZAKUPU|DOF DO OKULAROW|ADYEN|TOTALIZATOR SPORTOWY", "refundCorrection"),
            rule("KAPITALIZACJA ODSETEK", "interest"),
            rule("ZWROT Z PODATKU|URZĄD SKARBOWY|URZAD SKARBOWY", "taxRefund"),
            rule("PRZELEW DO BM MBANKU|IKZE|ZAKUP SFI|FUNDUSZ|SUBFUNDUSZ|MAKLERSK|BROKERAGE", CATEGORY_INVESTMENTS),
            rule("WYPŁATA GOTÓWKI|WYPLATA GOTOWKI|BANKOMAT|ATM", "cashToSettle"),
            rule("WPŁATA WE WPŁATOMACIE|WPLATA WE WPLATOMACIE", "cashDeposit"),
            rule("OPLATY, ZALICZKA|OPŁATY, ZALICZKA|CZYNSZ|WYNAJEM", "rent"),
            rule("TAURON|PGE|ENERGA|ENEA|PRĄD|PRAD|GAZ", "electricity"),
            rule("KORBANK|WWW\\.PLUS\\.PL|SSL\\.PLUSGSM\\.PL|PLUS\\s+(TATIANA|ALEKSANDER),?\\s*E-FAKTURA|ORANGE|T-MOBILE|UPC|VECTRA|NETIA|\\bPLAY\\b.*(TELEFON|ABONAMENT|DOŁADOWANIE|DOLADOWANIE)", "telecom"),
            rule("PROWIZJA OD WYPŁATY GOTÓWKI|PROWIZJA OD WYPLATY GOTOWKI|PRZELEW EXPRESS ELIXIR WYCH|MTRANSFER BLUE MEDIA", "bankFees"),
            rule("PLIP\\.WROCLAW\\.PL|PLIP\\.UM\\.WROC\\.PL|GMINA WROCŁAW|GMINA WROCLAW", "publicFees"),
            rule("SKŁADKA UB|SKLADKA UB|UBEZPIECZ|POLISA|PZU|UNIQA|WARTA|LINK4|ALLIANZ|GENERALI|VWFS UBEZPIECZENIA", "insurance"),
            rule("PODATEK|URZAD SKARBOWY|URZĄD SKARBOWY|ZUS", "taxes"),
            rule("BIEDRONKA|LIDL|CARREFOUR|AUCHAN|KAUFLAND|ALDI|ZABKA|ŻABKA|DINO|STOKROTKA|LEWIATAN|SPOLEM|SPOŁEM|DELIKATESY|SKLEP BLUMIS", "groceries"),
            rule("BILLA|TIGER|CARREF GIRONA|GROCERY|SUPERMARKET|MARKET|TESCO|PIEKARNIA|CUKIERNIA|SASIEDZI|PURI|SPAR|DELIKOMAT|GORĄCO POLECAM|GORACO POLECAM|KUCHNIE SWIATA|CZAS NA HERBAT|SUPERMERCAT|COALIMENT|CELLER PALOU|SKLEP 800|REKREATYWA|FAMILIJNA|SLODKI CHLOPAK|SŁODKI CHŁOPAK|EL MERCADET", "groceries"),
            rule("APTEKA|APOTHEKE|DOZ|SUPER-PHARM|LEKARZ|MEDICOVER|LUX MED|LUXMED|DENT|FARMAC|FIELMANN|M-DENT|ALAB|MEDICCENTRE|ZBADAJKLESZCZA|OPTYCZNY|VITA RECEPCJA|WITEK|OVO MEDICAL|ZETMED", "medicalPharmacy"),
            rule("ROSSMANN|HEBE|SEPHORA|DOUGLAS|NOTINO|FRISER|DM-DROGERIE|DM\\s+DROGERIE|DROGERIE MARKT|MIDNIGHT MEADO", "beautyCosmetics"),
            rule("RESTAUR|RESTAURACJA|CAFE|KAWA|KIOSSO|PIZZA|SUSHI|BURGER|BURGUER|MCDONALD|KFC|POPEYES|PYSZNE|UBER EATS|BOLT FOOD|BAR |KUCHNIA|GLOVO|WOLT|LEPIONE|EXPRESS ORIENTAL|ATO RAMEN|GASTHOF|WIENERWIRTSCHAFT|PIEROGARNIA|PASIB|KIOSK PASIB|KIOSK PASI|MANGO MAMA|TUTTI SANTI|HALA SWIEBODZKI|HALA ŚWIEBODZKI|THAI EXPRESS|PAN PRECEL|SODEXO|STARBUCKS|SB NAMESTI SVOBODY|SBX WROCLAW|AMREST|BISTROCYKL|COSTA COFFEE|KEBAP|DONER|FOODIES|WAGOZERCY|EUREST|BB BRNO|BB VACLAVSKE|TENDUR|BELLA BEAN|KAVY|ORIGINAL KRUMLOV WAF|JEDZENIE|JEDZONKO|QUESA|KOREAŃCZYK|KOREANCZYK|CHINGU|CHATKA PRZY JATKACH|KARCZMA|CZARNA MAGIA|MAISTERSZTYK|LA FABIRCA|ENKLAWA|MANIA AL TAGLIO|SLIMAK|EL GORDITO", "diningOut"),
            rule("VINTED|ZALANDO|CCC|H&M|\\bHM\\b|RESERVED|SINSAY|MOHITO|ZARA|CROPP|OBUWIE|TK MAXX|DEICHMANN|PRIMARK|MEDICINE|WEARMEDICINE|HALF PRICE|TRACHTENMODE|SKLEP Z ODZIEZA|SP MEMERY|\\bHOUSE\\b|HOUSEBRAND(?:\\.COM)?|STRADIVARIUS|RESTYLE|TERRANOVA|LOKAAH|VENA SP|MARGO SP|DZIKIZACHOD|BALAGANSTUDIO|NEW YORKER|ROCKMETALSHOP", "clothing"),
            rule("KICKET|TICKET|TICKETMASTER|BILET|KINO|TEATR|EVENT|MUZEUM|KONCERT|BRUTALASSAULT|FLYN|NFCTRON|GOPAY|TIQETS|TIXYAPP|VIENNACONCERTS|EET RESIS FESTIVAL|SCHMITTENHOEHEBAHN|LANOVA DRAHA|BANYS ARABS|KISWE|OZZY|KUPBILECIK|STAGE24|CINEMA|GOOUT|PNGS|LOTTO|ENTRITT|NHM|CHRAM SV|SEDLEC|ZAMEK GRODZIEC|KNOCK OUT PRODUCTIONS|WPE W DZIEKANOWICACH", "events"),
            rule("NETFLIX|SPOTIFY|DISNEY|STEAM|EMPIK|GOOGLE PLAY|PLAYSTATION|ADOBE|KRD|ZECCER|FOTOJOKER|KSIEGRALNIA|KSIĘGRALNIA", "media"),
            rule("E S CELRA|MOL |CS(?:P)?HM|SERWIS|CZĘŚCI|CZESCI|BOB|MOTORPOL|MYJNIA|ORLEN|BP |SHELL|CIRCLE K|STACJA", "fuelCar"),
            rule("TORPEKSPRESSEN|TORP-EKSPRESSEN|WIENER LINIEN|MPSA - A|PARKOVISTE|VIGNETTE|EDALNICE|DALNICKA|KARLSPLATZGARAGE|PARKUM|PARKPLATZ|AP RIERA SANT VICENC|AP EL GARRIGAL|AP SANTA CATERINA|PR CERNY MOST|SIXT|ARCTIC CAMPERS", "travel"),
            rule("UBER|BOLT|FREE NOW|JAKDOJADE|ZTM|MPK|PKP|KOLEO|INTERCITY|FLIXBUS|INFOBUS|PARKING|PARKOMAT|APCOA|AUTOSTRADA|SYSTEMY POB|REZERWACJA\\.AIRPORT|PORT LOTNICZY|GARAGE|GARAZE|PP BYSTRZYCA|KBU.*SSP|KBU.*SPP|GALERIA DOMINIKANSKA|DHL24|WOOD SP\\. Z O\\.O\\. WROCL", "transportParking"),
            rule("IKEA|CASTORAMA|LEROY|JYSK|AGATA|PEPCO|ACTION|OBI|BRICOMAN|ALL4HOM|ATASZEK|WOOLWORTH", "homeGoods"),
            rule("KAKADU|ZOOPLUS|MAXI ZOO|WETERYNAR|VET|CENTRUM WETERYNARII|SKLEP ZOOLOGICZNY|MEDUZA|AQUAELZOO|ALE ANIMALE", "pets"),
            rule("BOOKING|BKG\\*HOTEL|AIRBNB|HOTEL|PENSJONAT|BAJKOWA PRZYSTAŃ|RYANAIR|WIZZAIR|LOT\\.COM|LOT |TRAVEL|PODRÓŻ|SIXT|BOTIGA PUB FUND GALA DALI|SCHOENBRUNN|NORWAYS BEST|NORWAY'S BEST|OEN TURISTSENT|ØEN TURISTSENT|MS\\* THEHOUSE|ALPINCENTER|DOM ZU SALZBURG|VLTAVSKA POHADKA|PARK INN|ARCTIC CAMPERS|ARBIO", "travel"),
            rule("AVOLTA|DUTY FREE|WT TG DF", "travelShopping"),
            rule("DECATHLON|THOMANN|KNOCKOUTMUSICSTORE|BLM\\*KOKONKI|KOKONKI|DACHSTEINSPORT|GIKME|MILITARIA.PL|ASSARION|FAJKOWO|ULTRA SUNN", "sportHobby"),
            rule("MEDIA EXPERT|MEDIA MARKT|ME M02|CYFROWE|RTV|AGD|ELECTRO|EURO\\.COM|KOMPUTRONIK|X-KOM|NEEWER", "electronics"),
            rule("ZEGAROWNIA|SUVENYRY|SOUVENIRS|EWIKING|KHM-KHM|BOLESLAWIEC|LOVE POLAND|DBFLORIST|PREZENT|FENIKS|MAGIC SILVER|JAPIER", "giftsSupport"),
            rule("ALLEGRO|AMAZON|ALIEXPRESS|TEMU|PAYPAL \\*ETSY|ETSY|SELFIBOX|PAYPAL \\*", CATEGORY_MARKETPLACE)
    );

    private BudgetTaxonomy() {
    }

    static CategoryDefinition category(String id) {
        var definition = CATEGORIES.get(id);
        if (definition == null) {
            throw new IllegalArgumentException("Unknown budget category id: " + id);
        }
        return definition;
    }

    /**
     * Canonical labels of the wealth-building categories (investments, savings
     * account, loan overpayment). Single source of truth so persistence,
     * analysis, and FIRE linkage cannot silently drift if a label is renamed.
     */
    public static Set<String> wealthCategoryLabels() {
        return WEALTH_CATEGORY_LABELS;
    }

    public static String categoryIdByLabel(String label) {
        if (label == null || label.isBlank()) {
            return "";
        }
        var alias = LEGACY_CATEGORY_LABEL_ALIASES.get(label);
        if (alias != null) {
            return alias;
        }
        return CATEGORIES.values().stream()
                .filter(category -> category.label().equals(label))
                .map(CategoryDefinition::id)
                .findFirst()
                .orElse(label);
    }

    static String budgetGroupLabel(String id) {
        return BUDGET_GROUPS.getOrDefault(id, new BudgetGroup(id, id)).label();
    }

    private static CategoryDefinition category(
            String id,
            String label,
            String area,
            String analyticsGroup,
            String budgetGroupId,
            String budgetBucketLabel,
            String fixedness,
            String flowType,
            boolean discretionary,
            boolean excluded,
            boolean realIncome
    ) {
        return new CategoryDefinition(id, label, area, analyticsGroup, budgetGroupId, budgetBucketLabel, fixedness, flowType, discretionary, excluded, realIncome);
    }

    private static CategoryRule rule(String regex, String categoryId) {
        if (!CATEGORIES.containsKey(categoryId)) {
            throw new IllegalArgumentException("Rule references unknown category: " + categoryId);
        }
        return new CategoryRule(Pattern.compile(regex, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE), categoryId, regex);
    }

    private static <T> Map<String, T> orderedMap(List<T> values, java.util.function.Function<T, String> key) {
        var map = new LinkedHashMap<String, T>();
        values.forEach(value -> map.put(key.apply(value), value));
        return Collections.unmodifiableMap(map);
    }

    record BudgetGroup(String id, String label) {
    }

    record CategoryDefinition(
            String id,
            String label,
            String area,
            String analyticsGroup,
            String budgetGroupId,
            String budgetBucketLabel,
            String fixedness,
            String flowType,
            boolean discretionary,
            boolean excluded,
            boolean realIncome
    ) {
    }
}
