package com.budget.application.analysis;

import com.budget.application.categorization.CategoryClassifier;
import com.budget.application.settings.BudgetSettings;
import com.budget.application.settings.BudgetSettingsService;
import com.budget.domain.report.BudgetAnalysisResult;
import com.budget.domain.report.BudgetInput;
import com.budget.domain.report.BudgetSnapshot;
import com.budget.domain.transaction.NormalizedTransaction;
import java.math.BigDecimal;
import java.math.RoundingMode;
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
import org.springframework.stereotype.Service;

@Service
public class BudgetAnalysisService {
    private static final DateTimeFormatter MONTH_LABEL = DateTimeFormatter.ofPattern("MM.yyyy");
    private static final Set<String> REAL_SAVING_CATEGORIES = Set.of("Oszczędności i inwestycje", "Nadpłata kredytu");
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
            Map.entry("Zdrowie i uroda", new ConfiguredCategoryLimit(1400, "Oddzielić leczenie od kosmetyków/usług i ciąć tylko część uznaniową.")),
            Map.entry("Zwierzęta", new ConfiguredCategoryLimit(650, "Stały fundusz na karmę/weterynarza; porównać większe opakowania.")),
            Map.entry("Multimedia, książki i prasa", new ConfiguredCategoryLimit(250, "Przegląd subskrypcji i zakupów cyfrowych.")),
            Map.entry("Dom i wyposażenie", new ConfiguredCategoryLimit(500, "Zakupy domowe tylko z listy; większe rzeczy jako fundusz celowy."))
    );

    private final TransactionNormalizer normalizer;
    private final CategoryClassifier classifier;
    private final BudgetSettingsService settingsService;

    public BudgetAnalysisService(TransactionNormalizer normalizer, CategoryClassifier classifier, BudgetSettingsService settingsService) {
        this.normalizer = normalizer;
        this.classifier = classifier;
        this.settingsService = settingsService;
    }

    public BudgetAnalysisResult analyze(BudgetInput input) {
        var transactions = normalizer.normalize(input.transactions());
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
        var checkCount = (int) transactions.stream().filter(tx -> "Do sprawdzenia".equals(tx.correctedCategory())).count();
        var checkAmount = money(transactions.stream()
                .filter(tx -> "Do sprawdzenia".equals(tx.correctedCategory()))
                .map(tx -> tx.amount().abs())
                .reduce(BigDecimal.ZERO, BigDecimal::add));

        var budgetMixRows = budgetMixRows(transactions, incomeTotal, activeMonthCount, realSavingsOut, unassignedSurplus);
        var settings = settingsService.current();
        var categoryPlanRows = categoryPlanRows(categories, settings);

        var avgIncome = divide(incomeTotal, activeMonthCount);
        var avgSpend = divide(spendTotal, activeMonthCount);
        var needsTotal = sum(transactions.stream().filter(tx -> "Potrzeby".equals(tx.budgetBucket())).toList(), NormalizedTransaction::analysisSpend);
        var mixedNeedsTotal = sum(transactions.stream().filter(tx -> "Potrzeby mieszane".equals(tx.budgetBucket())).toList(), NormalizedTransaction::analysisSpend);
        var coreMonthlyCost = divide(needsTotal.add(mixedNeedsTotal), activeMonthCount);
        var targetMonthlySpend = money(settings.targetMonthlySpend());
        var aggressiveMonthlySpend = money(settings.aggressiveMonthlySpend());
        var latestMonthKey = activeMonths.getLast();
        var monthControl = monthControl(latestMonthKey, transactions, categoryPlanRows, targetMonthlySpend, periodStart, periodEnd, checkAmount, checkCount, categories);

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
                        money(checkAmount)
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

    private List<BudgetSnapshot.MonthlySummary> monthlyRows(List<NormalizedTransaction> transactions, List<String> months, List<String> labels, LocalDate periodStart, LocalDate periodEnd) {
        var rows = new ArrayList<BudgetSnapshot.MonthlySummary>();
        for (var i = 0; i < months.size(); i++) {
            var month = months.get(i);
            var items = transactions.stream().filter(tx -> tx.month().equals(month)).toList();
            var income = sum(items, NormalizedTransaction::income);
            var spend = sum(items, NormalizedTransaction::analysisSpend);
            var excluded = sum(items, NormalizedTransaction::excluded);
            var savings = sum(items.stream().filter(tx -> "Oszczędności i inwestycje".equals(tx.correctedCategory())).toList(), NormalizedTransaction::excluded);
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
            rows.add(new CategoryRow(
                    category,
                    classifier.group(category),
                    spend,
                    sum(items, NormalizedTransaction::income),
                    sum(items, NormalizedTransaction::excluded),
                    divide(spend, activeMonthCount),
                    maxMonth,
                    maxValue,
                    classifier.isDiscretionary(category),
                    items.size()
            ));
        }
        rows.sort(Comparator.comparing(CategoryRow::spend).reversed());
        return rows;
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
        var needs = sum(transactions.stream().filter(tx -> "Potrzeby".equals(tx.budgetBucket())).toList(), NormalizedTransaction::analysisSpend);
        var mixedNeeds = sum(transactions.stream().filter(tx -> "Potrzeby mieszane".equals(tx.budgetBucket())).toList(), NormalizedTransaction::analysisSpend);
        var wants = sum(transactions.stream().filter(tx -> Set.of("Zachcianki", "Zachcianki do rozbicia").contains(tx.budgetBucket())).toList(), NormalizedTransaction::analysisSpend);
        var unclear = sum(transactions.stream().filter(tx -> "Zachcianki do rozbicia".equals(tx.budgetBucket())).toList(), NormalizedTransaction::analysisSpend);
        return List.of(
                mixRow("Potrzeby", needs, activeMonthCount, incomeTotal, "Cel bazowy: do 50% dochodu netto"),
                mixRow("Potrzeby mieszane", mixedNeeds, activeMonthCount, incomeTotal, "Do ręcznego rozbicia: część może być konieczna, część uznaniowa"),
                mixRow("Zachcianki", wants, activeMonthCount, incomeTotal, "Cel bazowy: do 30% dochodu, przy agresywnym inwestowaniu niżej"),
                mixRow("Marketplace do rozbicia", unclear, activeMonthCount, incomeTotal, "Największe ryzyko błędnej interpretacji bez historii zamówień"),
                mixRow("Oszczędzanie/inwestycje wykonane", realSavingsOut, activeMonthCount, incomeTotal, "Przelewy na inwestycje/oszczędności i nadpłaty kredytu"),
                mixRow("Nadwyżka operacyjna po oszczędnościach", unassignedSurplus, activeMonthCount, incomeTotal, "Dochód minus wydatki analizowane minus rozpoznane inwestycje/nadpłaty")
        );
    }

    private BudgetSnapshot.BudgetMixItem mixRow(String bucket, BigDecimal sum, int activeMonthCount, BigDecimal incomeTotal, String note) {
        return new BudgetSnapshot.BudgetMixItem(bucket, money(sum), divide(sum, activeMonthCount), ratio(sum, incomeTotal), note);
    }

    private List<BudgetSnapshot.CategoryLimit> categoryPlanRows(List<CategoryRow> categories, BudgetSettings settings) {
        var overrides = settings.categoryLimits().stream()
                .filter(row -> row.category() != null && !row.category().isBlank())
                .collect(java.util.stream.Collectors.toMap(
                        BudgetSettings.CategoryLimitSetting::category,
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
            } else if (category.discretionary()) {
                limit = money(category.monthlyAverage().multiply(BigDecimal.valueOf(0.82)));
                action = "Ustawić limit miesięczny i opóźnić zakupy uznaniowe o 24-48 godzin.";
            } else if ("Potrzeby".equals(classifier.budgetBucket(category.category()))) {
                limit = money(category.monthlyAverage());
                action = "Monitorować, ale nie ciąć bez świadomej decyzji.";
            } else {
                limit = money(category.monthlyAverage().multiply(BigDecimal.valueOf(0.95)));
                action = "Sprawdzić największe transakcje i powtarzalność.";
            }
            var potential = maxZero(category.monthlyAverage().subtract(limit));
            rows.add(new BudgetSnapshot.CategoryLimit(
                    category.category(),
                    classifier.budgetBucket(category.category()),
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

    private BudgetSnapshot.MonthControl monthControl(
            String latestMonthKey,
            List<NormalizedTransaction> transactions,
            List<BudgetSnapshot.CategoryLimit> categoryPlanRows,
            BigDecimal targetMonthlySpend,
            LocalDate periodStart,
            LocalDate periodEnd,
            BigDecimal checkAmount,
            int checkCount,
            List<CategoryRow> categories
    ) {
        var latest = YearMonth.parse(latestMonthKey);
        var daysTotal = latest.lengthOfMonth();
        var elapsedDays = analysisDays(latestMonthKey, periodStart, periodEnd);
        var remainingDays = Math.max(0, daysTotal - elapsedDays);
        var latestItems = transactions.stream().filter(tx -> tx.month().equals(latestMonthKey)).toList();
        var spend = sum(latestItems, NormalizedTransaction::analysisSpend);
        var income = sum(latestItems, NormalizedTransaction::income);
        var projection = elapsedDays == 0 ? spend : divide(spend, elapsedDays).multiply(BigDecimal.valueOf(daysTotal));
        var remainingBudget = money(targetMonthlySpend.subtract(spend));
        var dailyAllowed = remainingDays == 0 ? BigDecimal.ZERO : divide(maxZero(remainingBudget), remainingDays);
        var projectedDelta = money(targetMonthlySpend.subtract(projection));

        var statuses = new ArrayList<BudgetSnapshot.CategoryStatus>();
        for (var row : categoryPlanRows) {
            var current = sum(latestItems.stream().filter(tx -> tx.correctedCategory().equals(row.category())).toList(), NormalizedTransaction::analysisSpend);
            var projected = elapsedDays == 0 ? current : divide(current, elapsedDays).multiply(BigDecimal.valueOf(daysTotal));
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
                    ratio(current, limit)
            ));
        }

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
        for (var entry : Map.of(
                "Podróże i wyjazdy", "Podróże",
                "Ubezpieczenia", "Ubezpieczenia",
                "Zwierzęta", "Zwierzęta",
                "Paliwo i auto", "Auto",
                "Elektronika", "Elektronika",
                "Zdrowie i uroda", "Zdrowie/uroda"
        ).entrySet()) {
            var avg = categoryAverages.getOrDefault(entry.getKey(), BigDecimal.ZERO);
            if (avg.signum() > 0) {
                sinkingFunds.add(new BudgetSnapshot.SinkingFund(
                        entry.getValue(),
                        entry.getKey(),
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
        for (var label : List.of("Stałe", "Zmienne konieczne", "Uznaniowe", "Oszczędności", "Transfer/wyłączone", "Do oceny")) {
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

    private record CategoryRow(
            String category,
            String group,
            BigDecimal spend,
            BigDecimal income,
            BigDecimal excluded,
            BigDecimal monthlyAverage,
            String maxMonth,
            BigDecimal maxAmount,
            boolean discretionary,
            int count
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
                    count
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
