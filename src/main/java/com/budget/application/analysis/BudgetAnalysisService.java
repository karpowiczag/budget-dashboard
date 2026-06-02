package com.budget.application.analysis;

import com.budget.application.categorization.BudgetTaxonomy;
import com.budget.application.categorization.CategoryClassifier;
import com.budget.application.settings.BudgetSettings;
import com.budget.application.settings.BudgetSettingsService;
import com.budget.domain.report.BudgetAnalysisResult;
import com.budget.domain.report.BudgetInput;
import com.budget.domain.report.BudgetSnapshot;
import com.budget.domain.transaction.NormalizedTransaction;
import java.math.BigDecimal;
import java.time.Clock;
import java.math.RoundingMode;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class BudgetAnalysisService {
    private static final DateTimeFormatter MONTH_LABEL = DateTimeFormatter.ofPattern("MM.yyyy");
    private static final String BUCKET_FIXED_OBLIGATORY = "Obowiązkowe stałe";
    private static final String BUCKET_VARIABLE_OBLIGATORY = "Obowiązkowe zmienne";
    private static final String BUCKET_REVIEW = "Do rozbicia";
    private static final String BUCKET_FLEXIBLE = "Nieobowiązkowe";
    private static final String BUCKET_NON_MONTHLY = "Nieregularne";
    private static final String BUCKET_INVESTMENTS = "Inwestycje";
    private static final String BUCKET_SAVINGS_ACCOUNT = "Konto oszczędnościowe";
    private static final String BUCKET_LOAN_OVERPAYMENT = "Nadpłata kredytu";
    private static final Set<String> OBLIGATORY_BUCKETS = Set.of(BUCKET_FIXED_OBLIGATORY, BUCKET_VARIABLE_OBLIGATORY);
    private static final Set<String> REAL_SAVING_CATEGORIES = BudgetTaxonomy.wealthCategoryLabels();
    private static final Set<String> DAILY_PACED_CATEGORIES = Set.of(
            "Żywność i chemia",
            "Jedzenie poza domem"
    );
    private static final Map<String, ConfiguredCategoryLimit> DEFAULT_CATEGORY_LIMITS = Map.ofEntries(
            Map.entry("Żywność i chemia", new ConfiguredCategoryLimit(2400, "Plan posiłków, większe zakupy z listą, mniej awaryjnych wizyt.")),
            Map.entry("Jedzenie poza domem", new ConfiguredCategoryLimit(600, "Limit na restauracje, kawę i dostawy; zostawić tylko celowe wyjścia.")),
            Map.entry("Odzież i obuwie", new ConfiguredCategoryLimit(800, "Limit kwartalny i lista braków zamiast zakupów impulsowych.")),
            Map.entry("Marketplace i zakupy online", new ConfiguredCategoryLimit(400, "Rozbijać Allegro/Amazon/Temu po historii zamówień; limit koszyka online.")),
            Map.entry("Podróże i wyjazdy", new ConfiguredCategoryLimit(1500, "Traktować jako fundusz celowy, nie zwykły koszt miesiąca.")),
            Map.entry("Wyjścia i wydarzenia", new ConfiguredCategoryLimit(500, "Roczny limit biletów/eventów, decyzje przed zakupem.")),
            Map.entry("Sport i hobby", new ConfiguredCategoryLimit(500, "Limit na hobby i sprzęt; większe zakupy tylko z funduszu celowego.")),
            Map.entry("Elektronika", new ConfiguredCategoryLimit(300, "Tylko planowane zakupy; większe rzeczy osobny fundusz.")),
            Map.entry("Prezenty i wsparcie", new ConfiguredCategoryLimit(300, "Miesięczny fundusz prezentowy, nie zakup ad hoc.")),
            Map.entry("Lekarz i apteka", new ConfiguredCategoryLimit(1400, "Konieczne zdrowie monitorować, bez automatycznego cięcia.")),
            Map.entry("Uroda i kosmetyki", new ConfiguredCategoryLimit(500, "Kontrolować osobno od leczenia i ciąć jak uznaniowe.")),
            Map.entry("Zwierzęta", new ConfiguredCategoryLimit(650, "Stały fundusz na karmę/weterynarza; porównać większe opakowania.")),
            Map.entry("Multimedia, książki i prasa", new ConfiguredCategoryLimit(250, "Przegląd subskrypcji i zakupów cyfrowych.")),
            Map.entry("Dom i wyposażenie", new ConfiguredCategoryLimit(500, "Zakupy domowe tylko z listy; większe rzeczy jako fundusz celowy."))
    );

    private final TransactionNormalizer normalizer;
    private final CategoryClassifier classifier;
    private final BudgetSettingsService settingsService;
    private final Clock clock;

    @Autowired
    public BudgetAnalysisService(TransactionNormalizer normalizer, CategoryClassifier classifier, BudgetSettingsService settingsService) {
        this(normalizer, classifier, settingsService, Clock.systemDefaultZone());
    }

    BudgetAnalysisService(TransactionNormalizer normalizer, CategoryClassifier classifier, BudgetSettingsService settingsService, Clock clock) {
        this.normalizer = normalizer;
        this.classifier = classifier;
        this.settingsService = settingsService;
        this.clock = clock;
    }

    public BudgetAnalysisResult analyze(BudgetInput input) {
        var settings = settingsService.current();
        var transactions = applyCategoryBucketOverrides(normalizer.normalize(input.transactions()), settings);
        if (transactions.isEmpty()) {
            throw new IllegalArgumentException("No transactions to analyze");
        }

        var periodStart = transactions.stream().map(NormalizedTransaction::date).min(LocalDate::compareTo).orElseThrow();
        var periodEnd = transactions.stream().map(NormalizedTransaction::date).max(LocalDate::compareTo).orElseThrow();
        var months = months(input.year());
        var monthLabels = monthLabels(input.year());
        var activeMonths = months.stream().filter(month -> transactions.stream().anyMatch(tx -> tx.month().equals(month))).toList();
        var activeMonthCount = Math.max(1, activeMonths.size());

        var monthly = monthlyRows(transactions, months, monthLabels, periodStart, periodEnd);
        var categories = categoryRows(transactions, months, monthLabels, activeMonthCount);
        var hierarchy = hierarchyRows(transactions, activeMonthCount);
        var topMerchants = topMerchants(transactions);

        var incomeTotal = sum(transactions, NormalizedTransaction::income);
        var spendTotal = sum(transactions, NormalizedTransaction::analysisSpend);
        var discretionaryTotal = sum(transactions, NormalizedTransaction::discretionary);
        var excludedTotal = sum(transactions, NormalizedTransaction::excluded);
        var excludedOutgoingTotal = sum(transactions, NormalizedTransaction::excludedOutgoing);
        var excludedIncomingTotal = sum(transactions, NormalizedTransaction::excludedIncoming);
        var excludedNetTotal = sum(transactions, NormalizedTransaction::excludedNet);
        var realSavingsOut = sum(transactions.stream()
                .filter(tx -> REAL_SAVING_CATEGORIES.contains(tx.correctedCategory()))
                .toList(), NormalizedTransaction::excludedOutgoing);
        var operatingSurplus = money(incomeTotal.subtract(spendTotal));
        var unassignedSurplus = money(incomeTotal.subtract(spendTotal).subtract(realSavingsOut));
        var corrections = (int) transactions.stream().filter(tx -> !tx.notes().isBlank()).count();
        var lowConfidenceCount = (int) transactions.stream().filter(tx -> "Niska".equals(tx.confidence())).count();
        var checkCount = (int) transactions.stream().filter(tx -> !"ok".equals(tx.reviewStatus())).count();
        var checkAmount = money(transactions.stream()
                .filter(tx -> !"ok".equals(tx.reviewStatus()))
                .map(tx -> tx.amount().abs())
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        var savingsAccount = savingsAccountKpis(transactions, periodEnd);

        var budgetMixRows = budgetMixRows(transactions, incomeTotal, activeMonthCount, realSavingsOut, unassignedSurplus);
        var parentPlanRows = parentPlanRows(categories, settings);
        var categoryPlanRows = categoryPlanRows(categories, settings);

        var avgIncome = divide(incomeTotal, activeMonthCount);
        var avgSpend = divide(spendTotal, activeMonthCount);
        var fixedObligatoryTotal = sum(transactions.stream().filter(tx -> BUCKET_FIXED_OBLIGATORY.equals(tx.budgetBucket())).toList(), NormalizedTransaction::analysisSpend);
        var variableObligatoryTotal = sum(transactions.stream().filter(tx -> BUCKET_VARIABLE_OBLIGATORY.equals(tx.budgetBucket())).toList(), NormalizedTransaction::analysisSpend);
        var coreMonthlyCost = divide(fixedObligatoryTotal.add(variableObligatoryTotal), activeMonthCount);
        var targetMonthlySpend = money(settings.targetMonthlySpend());
        var aggressiveMonthlySpend = money(settings.aggressiveMonthlySpend());
        var today = LocalDate.now(clock);
        var latestActiveMonth = YearMonth.parse(activeMonths.getLast());
        var currentMonth = YearMonth.from(today);
        // The planner is for the current month: when viewing the current year and the calendar
        // month is ahead of the latest imported month, control the actual current month
        // (anchored to today) even before its transactions are imported.
        var controlMonth = input.year() == today.getYear() && currentMonth.isAfter(latestActiveMonth)
                ? currentMonth
                : latestActiveMonth;
        var latestMonthKey = controlMonth.toString();
        var monthControl = monthControl(latestMonthKey, transactions, categoryPlanRows, targetMonthlySpend, periodStart, periodEnd, checkAmount, checkCount, categories, today, settings.sinkingFundCategories());

        var snapshot = new BudgetSnapshot(
                input.year(),
                periodStart + " - " + periodEnd,
                periodStart,
                periodEnd,
                activeMonthCount,
                new BudgetSnapshot.Kpis(
                        money(incomeTotal),
                        money(spendTotal),
                        money(discretionaryTotal),
                        money(operatingSurplus),
                        ratio(operatingSurplus, incomeTotal),
                        money(excludedTotal),
                        money(excludedOutgoingTotal),
                        money(excludedIncomingTotal),
                        money(excludedNetTotal),
                        money(realSavingsOut),
                        money(unassignedSurplus),
                        transactions.size(),
                        corrections,
                        lowConfidenceCount,
                        checkCount,
                        money(checkAmount),
                        savingsAccount.netChange(),
                        savingsAccount.grossDeposits(),
                        savingsAccount.inflows(),
                        savingsAccount.outflows()
                ),
                monthly,
                categories.stream().map(CategoryRow::toSnapshot).toList(),
                hierarchy,
                budgetMixRows,
                new BudgetSnapshot.SavingsPlan(
                        money(avgSpend),
                        money(avgIncome),
                        money(coreMonthlyCost),
                        money(targetMonthlySpend),
                        money(aggressiveMonthlySpend),
                        maxZero(avgIncome.subtract(targetMonthlySpend)),
                        maxZero(avgIncome.subtract(aggressiveMonthlySpend)),
                        maxZero(avgSpend.subtract(targetMonthlySpend)),
                        money(coreMonthlyCost.multiply(BigDecimal.valueOf(settings.emergencyFundMinMonths()))),
                        money(coreMonthlyCost.multiply(BigDecimal.valueOf(settings.emergencyFundComfortMonths()))),
                        parentPlanRows,
                        categoryPlanRows
                ),
                monthControl,
                fixednessRows(transactions, activeMonthCount),
                topMerchants,
                recurringRows(transactions, activeMonthCount),
                largeOneoffs(transactions)
        );

        return new BudgetAnalysisResult(input.year(), input.fileName(), snapshot, transactions, transactions.size(), money(incomeTotal), money(spendTotal));
    }

    private SavingsAccountKpis savingsAccountKpis(List<NormalizedTransaction> transactions, LocalDate periodEnd) {
        var fromDate = transactions.stream().map(NormalizedTransaction::date).min(LocalDate::compareTo).orElse(periodEnd);
        var savingsAccountTransactions = transactions.stream()
                .filter(tx -> !tx.date().isBefore(fromDate) && !tx.date().isAfter(periodEnd))
                .filter(this::isSavingsAccountTransaction)
                .toList();
        var latestSavingsAccountDate = savingsAccountTransactions.stream()
                .map(NormalizedTransaction::date)
                .max(LocalDate::compareTo)
                .orElse(null);
        var grossDeposits = sum(transactions.stream()
                .filter(tx -> !tx.date().isBefore(fromDate) && !tx.date().isAfter(periodEnd))
                .filter(tx -> "Konto oszczędnościowe".equals(tx.correctedCategory()))
                .toList(), NormalizedTransaction::excludedOutgoing);
        var pendingDeposits = sum(transactions.stream()
                .filter(tx -> !tx.date().isBefore(fromDate) && !tx.date().isAfter(periodEnd))
                .filter(tx -> latestSavingsAccountDate == null || tx.date().isAfter(latestSavingsAccountDate))
                .filter(tx -> "Konto oszczędnościowe".equals(tx.correctedCategory()))
                .toList(), NormalizedTransaction::excludedOutgoing);
        var accountInflows = sum(savingsAccountTransactions.stream()
                .filter(tx -> tx.amount().compareTo(BigDecimal.ZERO) > 0)
                .toList(), NormalizedTransaction::amount);
        var accountOutflows = sum(savingsAccountTransactions.stream()
                .filter(tx -> tx.amount().compareTo(BigDecimal.ZERO) < 0)
                .toList(), tx -> tx.amount().negate());
        var inflows = accountInflows.add(pendingDeposits);
        var netChange = inflows.subtract(accountOutflows);
        return new SavingsAccountKpis(
                money(netChange),
                money(grossDeposits),
                money(inflows),
                money(accountOutflows)
        );
    }

    private boolean isSavingsAccountTransaction(NormalizedTransaction tx) {
        var account = normalizeAscii(tx.account());
        return account.contains("OSZCZ") || account.contains("LOKAT") || account.contains("SAVING");
    }

    private String normalizeAscii(String value) {
        if (value == null) {
            return "";
        }
        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toUpperCase(java.util.Locale.ROOT);
    }

    private List<BudgetSnapshot.MonthlySummary> monthlyRows(List<NormalizedTransaction> transactions, List<String> months, List<String> labels, LocalDate periodStart, LocalDate periodEnd) {
        var rows = new ArrayList<BudgetSnapshot.MonthlySummary>();
        for (var i = 0; i < months.size(); i++) {
            var month = months.get(i);
            var items = transactions.stream().filter(tx -> tx.month().equals(month)).toList();
            var income = sum(items, NormalizedTransaction::income);
            var spend = sum(items, NormalizedTransaction::analysisSpend);
            var excluded = sum(items, NormalizedTransaction::excluded);
            var savings = sum(items.stream().filter(tx -> REAL_SAVING_CATEGORIES.contains(tx.correctedCategory())).toList(), NormalizedTransaction::excludedOutgoing);
            rows.add(new BudgetSnapshot.MonthlySummary(
                    labels.get(i),
                    month,
                    money(income),
                    money(spend),
                    money(sum(items, NormalizedTransaction::discretionary)),
                    money(excluded),
                    money(savings),
                    money(income.subtract(spend)),
                    ratio(income.subtract(spend), income),
                    items.size(),
                    divide(spend, analysisDays(month, periodStart, periodEnd))
            ));
        }
        return rows;
    }

    private List<CategoryRow> categoryRows(List<NormalizedTransaction> transactions, List<String> months, List<String> labels, int activeMonthCount) {
        var names = new TreeSet<String>();
        transactions.forEach(tx -> names.add(tx.correctedCategory()));
        var rows = new ArrayList<CategoryRow>();
        for (var category : names) {
            var items = transactions.stream().filter(tx -> tx.correctedCategory().equals(category)).toList();
            var monthValues = months.stream()
                    .map(month -> sum(items.stream().filter(tx -> tx.month().equals(month)).toList(), NormalizedTransaction::analysisSpend))
                    .toList();
            var maxValue = monthValues.stream().max(Comparator.naturalOrder()).orElse(BigDecimal.ZERO);
            var maxMonth = maxValue.signum() == 0 ? "" : labels.get(monthValues.indexOf(maxValue));
            var spend = sum(items, NormalizedTransaction::analysisSpend);
            var merchantExamples = merchantExamples(items);
            rows.add(new CategoryRow(
                    category,
                    classifier.budgetArea(category),
                    classifier.group(category),
                    effectiveBudgetBucket(category, items),
                    spend,
                    sum(items, NormalizedTransaction::income),
                    sum(items, NormalizedTransaction::excluded),
                    divide(spend, activeMonthCount),
                    maxMonth,
                    maxValue,
                    classifier.isDiscretionary(category),
                    items.size(),
                    merchantExamples
            ));
        }
        rows.sort(Comparator.comparing(CategoryRow::spend).reversed());
        return rows;
    }

    private List<NormalizedTransaction> applyCategoryBucketOverrides(List<NormalizedTransaction> transactions, BudgetSettings settings) {
        var overrides = categoryBucketOverrides(settings);
        if (overrides.isEmpty()) {
            return transactions;
        }
        return transactions.stream()
                .map(tx -> {
                    var override = overrides.get(tx.correctedCategory());
                    if (override == null || override.isBlank() || override.equals(tx.budgetBucket())) {
                        return tx;
                    }
                    return withBudgetBucket(tx, override);
                })
                .toList();
    }

    private Map<String, String> categoryBucketOverrides(BudgetSettings settings) {
        return settings.categoryLimits().stream()
                .filter(row -> "category".equals(row.scope()))
                .filter(row -> !row.displayName().isBlank())
                .filter(row -> !row.bucketOverride().isBlank())
                .collect(java.util.stream.Collectors.toMap(
                        BudgetSettings.CategoryLimitSetting::displayName,
                        BudgetSettings.CategoryLimitSetting::bucketOverride,
                        (first, second) -> second,
                        LinkedHashMap::new
                ));
    }

    private NormalizedTransaction withBudgetBucket(NormalizedTransaction tx, String budgetBucket) {
        return new NormalizedTransaction(
                tx.lp(),
                tx.date(),
                tx.month(),
                tx.merchant(),
                tx.description(),
                tx.account(),
                tx.bankCategory(),
                tx.categoryId(),
                tx.correctedCategory(),
                tx.subcategoryId(),
                tx.budgetArea(),
                tx.group(),
                tx.subcategory(),
                tx.flowType(),
                budgetGroupIdForBucket(budgetBucket),
                budgetGroupLabelForBucket(budgetBucket),
                tx.reviewStatus(),
                tx.reviewReason(),
                budgetBucket,
                tx.fixedness(),
                tx.type(),
                tx.amount(),
                tx.income(),
                tx.analysisSpend(),
                tx.discretionary(),
                tx.excluded(),
                tx.excludedOutgoing(),
                tx.excludedIncoming(),
                tx.excludedNet(),
                tx.confidence(),
                appendNote(tx.notes(), "Ręcznie zmieniony koszyk: " + budgetBucket),
                tx.matchedRule()
        );
    }

    private String budgetGroupIdForBucket(String bucket) {
        return switch (bucket) {
            case BUCKET_FIXED_OBLIGATORY -> "obligatoryFixed";
            case BUCKET_VARIABLE_OBLIGATORY -> "obligatoryVariable";
            case BUCKET_FLEXIBLE -> "discretionary";
            case BUCKET_NON_MONTHLY -> "nonMonthly";
            case BUCKET_REVIEW -> "reviewSplit";
            case BUCKET_INVESTMENTS, BUCKET_SAVINGS_ACCOUNT, BUCKET_LOAN_OVERPAYMENT -> "wealthBuilding";
            default -> "reviewSplit";
        };
    }

    private String budgetGroupLabelForBucket(String bucket) {
        return switch (budgetGroupIdForBucket(bucket)) {
            case "obligatoryFixed" -> BUCKET_FIXED_OBLIGATORY;
            case "obligatoryVariable" -> BUCKET_VARIABLE_OBLIGATORY;
            case "discretionary" -> BUCKET_FLEXIBLE;
            case "nonMonthly" -> BUCKET_NON_MONTHLY;
            case "wealthBuilding" -> "Budowanie majątku";
            default -> BUCKET_REVIEW;
        };
    }

    private String appendNote(String notes, String note) {
        return notes == null || notes.isBlank() ? note : notes + "; " + note;
    }

    private String effectiveBudgetBucket(String category, List<NormalizedTransaction> items) {
        return items.stream()
                .collect(java.util.stream.Collectors.groupingBy(NormalizedTransaction::budgetBucket, LinkedHashMap::new, java.util.stream.Collectors.counting()))
                .entrySet()
                .stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElseGet(() -> classifier.budgetBucket(category));
    }

    private List<String> merchantExamples(List<NormalizedTransaction> items) {
        var totals = new LinkedHashMap<String, BigDecimal>();
        for (var item : items) {
            if (item.analysisSpend().signum() <= 0 && item.excludedOutgoing().signum() <= 0) {
                continue;
            }
            var merchant = item.merchant() == null ? "" : item.merchant().trim();
            if (merchant.isBlank()) {
                continue;
            }
            totals.merge(merchant, item.analysisSpend().add(item.excludedOutgoing()), BigDecimal::add);
        }
        return totals.entrySet().stream()
                .sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed()
                        .thenComparing(Map.Entry::getKey))
                .limit(5)
                .map(Map.Entry::getKey)
                .toList();
    }

    private List<BudgetSnapshot.HierarchySummary> hierarchyRows(List<NormalizedTransaction> transactions, int activeMonthCount) {
        var keys = new TreeSet<String>();
        transactions.forEach(tx -> keys.add(tx.budgetArea() + "\u001F" + tx.group() + "\u001F" + tx.correctedCategory() + "\u001F" + tx.subcategory()));
        var rows = new ArrayList<BudgetSnapshot.HierarchySummary>();
        for (var key : keys) {
            var parts = key.split("\u001F", -1);
            var items = transactions.stream()
                    .filter(tx -> tx.budgetArea().equals(parts[0]) && tx.group().equals(parts[1]) && tx.correctedCategory().equals(parts[2]) && tx.subcategory().equals(parts[3]))
                    .toList();
            var spend = sum(items, NormalizedTransaction::analysisSpend);
            rows.add(new BudgetSnapshot.HierarchySummary(
                    parts[0],
                    parts[1],
                    parts[2],
                    parts[3],
                    money(spend),
                    money(sum(items, NormalizedTransaction::income)),
                    money(sum(items, NormalizedTransaction::excluded)),
                    divide(spend, activeMonthCount),
                    items.size(),
                    classifier.isDiscretionary(parts[2])
            ));
        }
        rows.sort(Comparator
                .comparing(BudgetSnapshot.HierarchySummary::area)
                .thenComparing((BudgetSnapshot.HierarchySummary row) -> row.spend().negate())
                .thenComparing(BudgetSnapshot.HierarchySummary::group)
                .thenComparing(BudgetSnapshot.HierarchySummary::category)
                .thenComparing(BudgetSnapshot.HierarchySummary::subcategory));
        return rows;
    }

    private List<BudgetSnapshot.MerchantSummary> topMerchants(List<NormalizedTransaction> transactions) {
        var agg = new LinkedHashMap<String, MerchantAgg>();
        for (var tx : transactions) {
            if (tx.analysisSpend().signum() <= 0) {
                continue;
            }
            var entry = agg.computeIfAbsent(tx.merchant(), key -> new MerchantAgg(tx.correctedCategory()));
            entry.sum = entry.sum.add(tx.analysisSpend());
            entry.count++;
            entry.category = tx.correctedCategory();
        }
        return agg.entrySet().stream()
                .map(entry -> new BudgetSnapshot.MerchantSummary(
                        entry.getKey(),
                        entry.getValue().category,
                        money(entry.getValue().sum),
                        entry.getValue().count,
                        divide(entry.getValue().sum, entry.getValue().count)
                ))
                .sorted(Comparator.comparing(BudgetSnapshot.MerchantSummary::sum).reversed())
                .limit(20)
                .toList();
    }

    private List<BudgetSnapshot.BudgetMixItem> budgetMixRows(List<NormalizedTransaction> transactions, BigDecimal incomeTotal, int activeMonthCount, BigDecimal realSavingsOut, BigDecimal unassignedSurplus) {
        var fixedObligatory = sum(transactions.stream().filter(tx -> BUCKET_FIXED_OBLIGATORY.equals(tx.budgetBucket())).toList(), NormalizedTransaction::analysisSpend);
        var variableObligatory = sum(transactions.stream().filter(tx -> BUCKET_VARIABLE_OBLIGATORY.equals(tx.budgetBucket())).toList(), NormalizedTransaction::analysisSpend);
        var review = sum(transactions.stream().filter(tx -> BUCKET_REVIEW.equals(tx.budgetBucket())).toList(), NormalizedTransaction::analysisSpend);
        var flexible = sum(transactions.stream().filter(tx -> BUCKET_FLEXIBLE.equals(tx.budgetBucket())).toList(), NormalizedTransaction::analysisSpend);
        var investments = sum(transactions.stream().filter(tx -> "Inwestycje".equals(tx.correctedCategory())).toList(), NormalizedTransaction::excludedOutgoing);
        var savingsAccount = sum(transactions.stream().filter(tx -> "Konto oszczędnościowe".equals(tx.correctedCategory())).toList(), NormalizedTransaction::excludedOutgoing);
        var loanOverpayments = sum(transactions.stream().filter(tx -> "Nadpłata kredytu".equals(tx.correctedCategory())).toList(), NormalizedTransaction::excludedOutgoing);
        return List.of(
                mixRow(BUCKET_FIXED_OBLIGATORY, fixedObligatory, activeMonthCount, incomeTotal, "Rachunki, raty i zobowiązania do zapłacenia w pierwszej kolejności"),
                mixRow(BUCKET_VARIABLE_OBLIGATORY, variableObligatory, activeMonthCount, incomeTotal, "Konieczne koszty zmienne: jedzenie, zdrowie, transport"),
                mixRow(BUCKET_REVIEW, review, activeMonthCount, incomeTotal, "Mieszane koszty do rozbicia przed decyzją o cięciu"),
                mixRow(BUCKET_FLEXIBLE, flexible, activeMonthCount, incomeTotal, "Nieobowiązkowe wydatki kontrolowane jednym limitem"),
                mixRow(BUCKET_INVESTMENTS, investments, activeMonthCount, incomeTotal, "Przelewy na inwestycje, fundusze i rachunek maklerski"),
                mixRow(BUCKET_SAVINGS_ACCOUNT, savingsAccount, activeMonthCount, incomeTotal, "Przelewy na własne konto oszczędnościowe"),
                mixRow(BUCKET_LOAN_OVERPAYMENT, loanOverpayments, activeMonthCount, incomeTotal, "Nadpłaty kapitału kredytu, osobno od inwestycji"),
                mixRow("Nadwyżka operacyjna po oszczędnościach", unassignedSurplus, activeMonthCount, incomeTotal, "Dochód minus wydatki analizowane minus rozpoznane inwestycje/nadpłaty")
        );
    }

    private BudgetSnapshot.BudgetMixItem mixRow(String bucket, BigDecimal sum, int activeMonthCount, BigDecimal incomeTotal, String note) {
        return new BudgetSnapshot.BudgetMixItem(bucket, money(sum), divide(sum, activeMonthCount), ratio(sum, incomeTotal), note);
    }

    private List<BudgetSnapshot.CategoryLimit> categoryPlanRows(List<CategoryRow> categories, BudgetSettings settings) {
        var overrides = settings.categoryLimits().stream()
                .filter(row -> "category".equals(row.scope()))
                .filter(row -> !row.displayName().isBlank())
                .collect(java.util.stream.Collectors.toMap(
                        BudgetSettings.CategoryLimitSetting::displayName,
                        row -> new ConfiguredCategoryLimit(money(row.limit()), row.action()),
                        (first, second) -> second,
                        LinkedHashMap::new
                ));
        var rows = new ArrayList<BudgetSnapshot.CategoryLimit>();
        for (var category : categories) {
            if (category.monthlyAverage().signum() <= 0) {
                continue;
            }
            var configured = overrides.getOrDefault(category.category(), DEFAULT_CATEGORY_LIMITS.get(category.category()));
            BigDecimal limit;
            String action;
            if (configured != null) {
                limit = configured.limit();
                action = configured.action();
            } else if (BUCKET_FLEXIBLE.equals(category.bucket())) {
                limit = money(category.monthlyAverage().multiply(BigDecimal.valueOf(0.82)));
                action = "Opcjonalny override; główny limit ustawiaj na poziomie Nieobowiązkowe.";
            } else if (BUCKET_REVIEW.equals(category.bucket())) {
                limit = money(category.monthlyAverage().multiply(BigDecimal.valueOf(0.95)));
                action = "Najpierw rozbić transakcje, potem zdecydować czy część jest nieobowiązkowa.";
            } else if (OBLIGATORY_BUCKETS.contains(category.bucket())) {
                limit = money(category.monthlyAverage().max(category.maxAmount()));
                action = "Zobowiązanie lub konieczny koszt; nie ciąć automatycznie.";
            } else {
                limit = money(category.monthlyAverage().multiply(BigDecimal.valueOf(0.95)));
                action = "Sprawdzić największe transakcje i powtarzalność.";
            }
            var potential = maxZero(category.monthlyAverage().subtract(limit));
            rows.add(new BudgetSnapshot.CategoryLimit(
                    "category",
                    category.category(),
                    false,
                    category.category(),
                    category.bucket(),
                    money(category.monthlyAverage()),
                    money(limit),
                    money(potential),
                    money(potential.multiply(BigDecimal.valueOf(12))),
                    priority(potential),
                    action
            ));
        }
        rows.sort(Comparator.comparing(BudgetSnapshot.CategoryLimit::potentialMonthly).reversed());
        return rows;
    }

    private List<BudgetSnapshot.CategoryLimit> parentPlanRows(List<CategoryRow> categories, BudgetSettings settings) {
        var configured = settings.categoryLimits().stream()
                .filter(row -> !"category".equals(row.scope()))
                .filter(row -> !row.displayName().isBlank())
                .collect(java.util.stream.Collectors.toMap(
                        row -> row.scope() + "\u001F" + row.displayName(),
                        row -> new ConfiguredCategoryLimit(money(row.limit()), row.action()),
                        (first, second) -> second,
                        LinkedHashMap::new
                ));
        var rows = new ArrayList<BudgetSnapshot.CategoryLimit>();
        rows.addAll(parentRowsForScope("bucket", categories, configured));
        rows.addAll(parentRowsForScope("area", categories, configured));
        rows.addAll(parentRowsForScope("group", categories, configured));
        rows.sort(Comparator
                .comparing(BudgetSnapshot.CategoryLimit::scope)
                .thenComparing(BudgetSnapshot.CategoryLimit::potentialMonthly, Comparator.reverseOrder())
                .thenComparing(BudgetSnapshot.CategoryLimit::name));
        return rows;
    }

    private List<BudgetSnapshot.CategoryLimit> parentRowsForScope(
            String scope,
            List<CategoryRow> categories,
            Map<String, ConfiguredCategoryLimit> configured
    ) {
        var grouped = new LinkedHashMap<String, List<CategoryRow>>();
        for (var row : categories) {
            var name = switch (scope) {
                case "bucket" -> row.bucket();
                case "area" -> row.area();
                case "group" -> row.group();
                default -> row.category();
            };
            if (name == null || name.isBlank() || "Inne".equals(name)) {
                continue;
            }
            grouped.computeIfAbsent(name, ignored -> new ArrayList<>()).add(row);
        }

        var rows = new ArrayList<BudgetSnapshot.CategoryLimit>();
        for (var entry : grouped.entrySet()) {
            var name = entry.getKey();
            var children = entry.getValue();
            var current = money(children.stream().map(CategoryRow::monthlyAverage).reduce(BigDecimal.ZERO, BigDecimal::add));
            if (current.signum() <= 0) {
                continue;
            }
            var setting = configured.get(scope + "\u001F" + name);
            var discretionaryShare = children.stream().filter(CategoryRow::discretionary).count() / (double) children.size();
            var limit = setting == null ? defaultParentLimit(scope, name, current, discretionaryShare) : setting.limit();
            var action = setting == null ? parentAction(scope, name, discretionaryShare) : setting.action();
            var potential = maxZero(current.subtract(limit));
            rows.add(new BudgetSnapshot.CategoryLimit(
                    scope,
                    name,
                    true,
                    name,
                    dominantBucket(children),
                    current,
                    money(limit),
                    money(potential),
                    money(potential.multiply(BigDecimal.valueOf(12))),
                    priority(potential),
                    action
            ));
        }
        return rows;
    }

    private BigDecimal defaultParentLimit(String scope, String name, BigDecimal current, double discretionaryShare) {
        if ("bucket".equals(scope) && BUCKET_FLEXIBLE.equals(name)) {
            return money(current.multiply(BigDecimal.valueOf(0.80)));
        }
        if ("bucket".equals(scope) && BUCKET_REVIEW.equals(name)) {
            return money(current.multiply(BigDecimal.valueOf(0.95)));
        }
        if ("bucket".equals(scope) && BUCKET_NON_MONTHLY.equals(name)) {
            return money(current.multiply(BigDecimal.valueOf(0.90)));
        }
        if ("bucket".equals(scope) && OBLIGATORY_BUCKETS.contains(name)) {
            return current;
        }
        if (discretionaryShare >= 0.5) {
            return money(current.multiply(BigDecimal.valueOf(0.85)));
        }
        return current;
    }

    private String parentAction(String scope, String name, double discretionaryShare) {
        if ("bucket".equals(scope)) {
            if (BUCKET_FLEXIBLE.equals(name)) {
                return "Główny limit do kontroli wydatków nieobowiązkowych.";
            }
            if (BUCKET_REVIEW.equals(name)) {
                return "Rozbić transakcje i przenieść część do obowiązkowych albo nieobowiązkowych.";
            }
            if (OBLIGATORY_BUCKETS.contains(name)) {
                return "Zobowiązania monitorować, bez automatycznego cięcia.";
            }
            return "Limit strategiczny dla koszyka; kategorie niżej służą jako override.";
        }
        if (discretionaryShare >= 0.5) {
            return "Ustawić limit nadrzędny i pilnować sumy kategorii pod spodem.";
        }
        return "Monitorować jako limit nadrzędny, bez automatycznego cięcia pojedynczych kategorii.";
    }

    private String dominantBucket(List<CategoryRow> rows) {
        return rows.stream()
                .collect(java.util.stream.Collectors.groupingBy(CategoryRow::bucket, LinkedHashMap::new, java.util.stream.Collectors.counting()))
                .entrySet()
                .stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("Inne");
    }

    private BudgetSnapshot.MonthControl monthControl(
            String latestMonthKey,
            List<NormalizedTransaction> transactions,
            List<BudgetSnapshot.CategoryLimit> categoryPlanRows,
            BigDecimal targetMonthlySpend,
            LocalDate periodStart,
            LocalDate periodEnd,
            BigDecimal checkAmount,
            int checkCount,
            List<CategoryRow> categories,
            LocalDate today,
            List<String> sinkingFundCategories
    ) {
        var latest = YearMonth.parse(latestMonthKey);
        var daysTotal = latest.lengthOfMonth();
        // For the live current month, anchor "elapsed" to today; for a closed/past month use
        // the span covered by its transactions.
        var elapsedDays = latest.equals(YearMonth.from(today))
                ? Math.min(today.getDayOfMonth(), daysTotal)
                : analysisDays(latestMonthKey, periodStart, periodEnd);
        var remainingDays = Math.max(0, daysTotal - elapsedDays);
        var latestItems = transactions.stream().filter(tx -> tx.month().equals(latestMonthKey)).toList();
        var spend = sum(latestItems, NormalizedTransaction::analysisSpend);
        var income = sum(latestItems, NormalizedTransaction::income);
        var remainingBudget = money(targetMonthlySpend.subtract(spend));
        var dailyAllowed = remainingDays == 0 ? BigDecimal.ZERO : divide(maxZero(remainingBudget), remainingDays);

        var statuses = new ArrayList<BudgetSnapshot.CategoryStatus>();
        for (var row : categoryPlanRows) {
            var current = sum(latestItems.stream().filter(tx -> tx.correctedCategory().equals(row.category())).toList(), NormalizedTransaction::analysisSpend);
            var projected = projectCategoryMonthEnd(row.category(), current, elapsedDays, daysTotal);
            var limit = row.limit();
            statuses.add(new BudgetSnapshot.CategoryStatus(
                    row.category(),
                    row.bucket(),
                    row.currentMonthly(),
                    row.limit(),
                    row.potentialMonthly(),
                    row.potentialYearly(),
                    row.priority(),
                    row.action(),
                    money(current),
                    money(projected),
                    money(limit.subtract(current)),
                    money(limit.subtract(projected)),
                    ratio(projected, limit)
            ));
        }
        var projection = statuses.stream()
                .map(BudgetSnapshot.CategoryStatus::currentMonthProjection)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        var projectedDelta = money(targetMonthlySpend.subtract(projection));

        var alerts = new ArrayList<BudgetSnapshot.Alert>();
        if (projection.compareTo(targetMonthlySpend) > 0) {
            alerts.add(new BudgetSnapshot.Alert(
                    "Ryzyko przekroczenia targetu",
                    "Wysoki",
                    "Prognoza " + latest.format(MONTH_LABEL) + " to " + roundedPln(projection) + " zł przy celu " + roundedPln(targetMonthlySpend) + " zł."
            ));
        }
        statuses.stream().limit(14).forEach(row -> {
            var projected = row.currentMonthProjection();
            var limit = row.limit();
            var delta = row.projectedDelta();
            if (projected.compareTo(limit) > 0 && limit.signum() > 0) {
                alerts.add(new BudgetSnapshot.Alert(
                        "Kategoria ponad limitem",
                        delta.compareTo(BigDecimal.valueOf(-500)) > 0 ? "Średni" : "Wysoki",
                        row.category() + ": prognoza " + roundedPln(projected) + " zł vs limit " + roundedPln(limit) + " zł."
                ));
            }
        });
        if (checkAmount.signum() > 0) {
            alerts.add(new BudgetSnapshot.Alert(
                    "Dane do sprawdzenia",
                    "Średni",
                    checkCount + " transakcji (" + roundedPln(checkAmount) + " zł) wymaga ręcznej decyzji."
            ));
        }

        var categoryAverages = new LinkedHashMap<String, BigDecimal>();
        categories.forEach(row -> categoryAverages.put(row.category(), row.monthlyAverage()));
        var sinkingFunds = new ArrayList<BudgetSnapshot.SinkingFund>();
        for (var category : sinkingFundCategories) {
            var avg = categoryAverages.getOrDefault(category, BigDecimal.ZERO);
            if (avg.signum() > 0) {
                sinkingFunds.add(new BudgetSnapshot.SinkingFund(
                        category,
                        category,
                        money(avg),
                        money(avg.multiply(BigDecimal.valueOf(12))),
                        "Fundusz celowy na koszty nierówne w czasie."
                ));
            }
        }

        return new BudgetSnapshot.MonthControl(
                latest.format(MONTH_LABEL),
                latestMonthKey,
                elapsedDays,
                remainingDays,
                daysTotal,
                money(income),
                money(spend),
                money(projection),
                money(targetMonthlySpend),
                money(remainingBudget),
                money(dailyAllowed),
                money(projectedDelta),
                statuses,
                alerts,
                sinkingFunds
        );
    }

    private List<BudgetSnapshot.FixednessSummary> fixednessRows(List<NormalizedTransaction> transactions, int activeMonthCount) {
        var rows = new ArrayList<BudgetSnapshot.FixednessSummary>();
        for (var label : List.of("Stałe", "Zmienne konieczne", "Uznaniowe", "Nieregularne", "Oszczędności", "Transfer/wyłączone", "Do rozbicia", "Do oceny")) {
            var items = transactions.stream().filter(tx -> tx.fixedness().equals(label)).toList();
            var spend = sum(items, NormalizedTransaction::analysisSpend);
            var excludedOutgoing = sum(items, NormalizedTransaction::excludedOutgoing);
            if (spend.signum() != 0 || excludedOutgoing.signum() != 0) {
                rows.add(new BudgetSnapshot.FixednessSummary(label, money(spend), money(excludedOutgoing), divide(spend, activeMonthCount), items.size()));
            }
        }
        return rows;
    }

    private List<BudgetSnapshot.RecurringItem> recurringRows(List<NormalizedTransaction> transactions, int activeMonthCount) {
        var merchantMonths = new LinkedHashMap<String, RecurringAgg>();
        for (var tx : transactions) {
            if (tx.analysisSpend().signum() <= 0) {
                continue;
            }
            var key = tx.merchant() + "\u001F" + tx.correctedCategory();
            var agg = merchantMonths.computeIfAbsent(key, ignored -> new RecurringAgg(tx.merchant(), tx.correctedCategory(), tx.budgetBucket()));
            agg.months.add(tx.month());
            agg.amount = agg.amount.add(tx.analysisSpend());
            agg.count++;
            agg.days.add(tx.date().getDayOfMonth());
            agg.lastDate = agg.lastDate == null || tx.date().isAfter(agg.lastDate) ? tx.date() : agg.lastDate;
        }
        var threshold = Math.max(3, Math.min(4, activeMonthCount));
        return merchantMonths.values().stream()
                .filter(agg -> agg.months.size() >= threshold || (agg.amount.compareTo(BigDecimal.valueOf(1000)) >= 0 && agg.months.size() >= 2))
                .map(agg -> new BudgetSnapshot.RecurringItem(
                        agg.merchant,
                        agg.category,
                        agg.bucket,
                        money(agg.amount),
                        agg.months.size(),
                        agg.count,
                        divide(agg.amount, agg.months.size()),
                        Math.round((float) agg.days.stream().mapToInt(Integer::intValue).average().orElse(0)),
                        agg.lastDate
                ))
                .sorted(Comparator.comparing(BudgetSnapshot.RecurringItem::sum).reversed())
                .limit(120)
                .toList();
    }

    private List<BudgetSnapshot.LargeOneOff> largeOneoffs(List<NormalizedTransaction> transactions) {
        return transactions.stream()
                .filter(tx -> tx.analysisSpend().compareTo(BigDecimal.valueOf(1000)) >= 0)
                .map(tx -> new BudgetSnapshot.LargeOneOff(
                        tx.date(),
                        tx.merchant(),
                        tx.correctedCategory(),
                        tx.budgetBucket(),
                        money(tx.analysisSpend()),
                        tx.month(),
                        tx.confidence(),
                        tx.description().length() > 120 ? tx.description().substring(0, 120) : tx.description()
                ))
                .sorted(Comparator.comparing(BudgetSnapshot.LargeOneOff::amount).reversed())
                .limit(160)
                .toList();
    }

    private int analysisDays(String monthKey, LocalDate periodStart, LocalDate periodEnd) {
        var month = YearMonth.parse(monthKey);
        var start = periodStart.isAfter(month.atDay(1)) ? periodStart : month.atDay(1);
        var end = periodEnd.isBefore(month.atEndOfMonth()) ? periodEnd : month.atEndOfMonth();
        if (end.isBefore(start)) {
            return month.lengthOfMonth();
        }
        return (int) (end.toEpochDay() - start.toEpochDay()) + 1;
    }

    private BigDecimal projectCategoryMonthEnd(String category, BigDecimal current, int elapsedDays, int daysTotal) {
        if (current == null || current.signum() <= 0 || elapsedDays <= 0 || elapsedDays >= daysTotal) {
            return money(current);
        }
        if (DAILY_PACED_CATEGORIES.contains(category)) {
            return money(divide(current, elapsedDays).multiply(BigDecimal.valueOf(daysTotal)));
        }
        return money(current);
    }

    private List<String> months(int year) {
        var months = new ArrayList<String>();
        for (var month = 1; month <= 12; month++) {
            months.add("%04d-%02d".formatted(year, month));
        }
        return months;
    }

    private List<String> monthLabels(int year) {
        var labels = new ArrayList<String>();
        for (var month = 1; month <= 12; month++) {
            labels.add("%02d.%04d".formatted(month, year));
        }
        return labels;
    }

    private BigDecimal sum(List<NormalizedTransaction> items, Function<NormalizedTransaction, BigDecimal> extractor) {
        return money(items.stream()
                .map(extractor)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    private BigDecimal divide(BigDecimal value, int divisor) {
        if (divisor == 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return money(value.divide(BigDecimal.valueOf(divisor), 2, RoundingMode.HALF_UP));
    }

    private BigDecimal money(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal ratio(BigDecimal numerator, BigDecimal denominator) {
        if (denominator == null || denominator.signum() == 0) {
            return BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
        }
        return numerator.divide(denominator, 4, RoundingMode.HALF_UP);
    }

    private BigDecimal maxZero(BigDecimal value) {
        return money(value.max(BigDecimal.ZERO));
    }

    private String priority(BigDecimal potential) {
        if (potential.compareTo(BigDecimal.valueOf(500)) >= 0) {
            return "Wysoki";
        }
        return potential.compareTo(BigDecimal.valueOf(150)) >= 0 ? "Średni" : "Niski";
    }

    private String roundedPln(BigDecimal value) {
        return String.format("%,.0f", value).replace(",", " ");
    }

    private record ConfiguredCategoryLimit(BigDecimal limit, String action) {
        private ConfiguredCategoryLimit(double limit, String action) {
            this(BigDecimal.valueOf(limit), action);
        }
    }

    private record SavingsAccountKpis(
            BigDecimal netChange,
            BigDecimal grossDeposits,
            BigDecimal inflows,
            BigDecimal outflows
    ) {
    }

    private record CategoryRow(
            String category,
            String area,
            String group,
            String bucket,
            BigDecimal spend,
            BigDecimal income,
            BigDecimal excluded,
            BigDecimal monthlyAverage,
            String maxMonth,
            BigDecimal maxAmount,
            boolean discretionary,
            int count,
            List<String> merchantExamples
    ) {
        BudgetSnapshot.CategorySummary toSnapshot() {
            return new BudgetSnapshot.CategorySummary(
                    category,
                    group,
                    spend,
                    income,
                    excluded,
                    monthlyAverage,
                    maxMonth,
                    maxAmount,
                    discretionary,
                    count,
                    merchantExamples
            );
        }
    }

    private static final class MerchantAgg {
        private String category;
        private BigDecimal sum = BigDecimal.ZERO;
        private int count;

        private MerchantAgg(String category) {
            this.category = category;
        }
    }

    private static final class RecurringAgg {
        private final String merchant;
        private final String category;
        private final String bucket;
        private final Set<String> months = new LinkedHashSet<>();
        private final List<Integer> days = new ArrayList<>();
        private BigDecimal amount = BigDecimal.ZERO;
        private int count;
        private LocalDate lastDate;

        private RecurringAgg(String merchant, String category, String bucket) {
            this.merchant = merchant;
            this.category = category;
            this.bucket = bucket;
        }
    }
}
