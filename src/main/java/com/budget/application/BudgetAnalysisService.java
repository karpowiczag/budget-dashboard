package com.budget.application;

import com.budget.domain.BudgetAnalysisResult;
import com.budget.domain.BudgetInput;
import com.budget.domain.NormalizedTransaction;
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
import java.util.function.ToDoubleFunction;
import org.springframework.stereotype.Service;

@Service
public class BudgetAnalysisService {
    private static final DateTimeFormatter MONTH_LABEL = DateTimeFormatter.ofPattern("MM.yyyy");
    private static final Set<String> REAL_SAVING_CATEGORIES = Set.of("Oszczędności i inwestycje", "Nadpłata kredytu");
    private static final Map<String, CategoryLimit> CATEGORY_LIMITS = Map.ofEntries(
            Map.entry("Żywność i chemia", new CategoryLimit(2400, "Plan posiłków, większe zakupy z listą, mniej awaryjnych wizyt.")),
            Map.entry("Jedzenie poza domem", new CategoryLimit(600, "Limit na restauracje, kawę i dostawy; zostawić tylko celowe wyjścia.")),
            Map.entry("Odzież i obuwie", new CategoryLimit(800, "Limit kwartalny i lista braków zamiast zakupów impulsowych.")),
            Map.entry("Marketplace i zakupy online", new CategoryLimit(400, "Rozbijać Allegro/Amazon/Temu po historii zamówień; limit koszyka online.")),
            Map.entry("Podróże i wyjazdy", new CategoryLimit(1500, "Traktować jako fundusz celowy, nie zwykły koszt miesiąca.")),
            Map.entry("Wyjścia i wydarzenia", new CategoryLimit(500, "Roczny limit biletów/eventów, decyzje przed zakupem.")),
            Map.entry("Sport i hobby", new CategoryLimit(500, "Limit na hobby i sprzęt; większe zakupy tylko z funduszu celowego.")),
            Map.entry("Elektronika", new CategoryLimit(300, "Tylko planowane zakupy; większe rzeczy osobny fundusz.")),
            Map.entry("Prezenty i wsparcie", new CategoryLimit(300, "Miesięczny fundusz prezentowy, nie zakup ad hoc.")),
            Map.entry("Zdrowie i uroda", new CategoryLimit(1400, "Oddzielić leczenie od kosmetyków/usług i ciąć tylko część uznaniową.")),
            Map.entry("Zwierzęta", new CategoryLimit(650, "Stały fundusz na karmę/weterynarza; porównać większe opakowania.")),
            Map.entry("Multimedia, książki i prasa", new CategoryLimit(250, "Przegląd subskrypcji i zakupów cyfrowych.")),
            Map.entry("Dom i wyposażenie", new CategoryLimit(500, "Zakupy domowe tylko z listy; większe rzeczy jako fundusz celowy."))
    );

    private final TransactionNormalizer normalizer;
    private final CategoryClassifier classifier;

    public BudgetAnalysisService(TransactionNormalizer normalizer, CategoryClassifier classifier) {
        this.normalizer = normalizer;
        this.classifier = classifier;
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
        int activeMonthCount = Math.max(1, activeMonths.size());

        var monthly = monthlyRows(transactions, months, monthLabels, periodStart, periodEnd);
        var categories = categoryRows(transactions, months, monthLabels, activeMonthCount);
        var hierarchy = hierarchyRows(transactions, activeMonthCount);
        var topMerchants = topMerchants(transactions);

        double incomeTotal = sum(transactions, NormalizedTransaction::income);
        double spendTotal = sum(transactions, NormalizedTransaction::analysisSpend);
        double discretionaryTotal = sum(transactions, NormalizedTransaction::discretionary);
        double excludedTotal = sum(transactions, NormalizedTransaction::excluded);
        double excludedOutgoingTotal = sum(transactions, NormalizedTransaction::excludedOutgoing);
        double excludedIncomingTotal = sum(transactions, NormalizedTransaction::excludedIncoming);
        double excludedNetTotal = sum(transactions, NormalizedTransaction::excludedNet);
        double realSavingsOut = sum(transactions.stream().filter(tx -> REAL_SAVING_CATEGORIES.contains(tx.correctedCategory())).toList(), NormalizedTransaction::excludedOutgoing);
        double operatingSurplus = round2(incomeTotal - spendTotal);
        double surplusRatio = incomeTotal == 0 ? 0 : operatingSurplus / incomeTotal;
        double unassignedSurplus = round2(incomeTotal - spendTotal - realSavingsOut);
        int corrections = (int) transactions.stream().filter(tx -> !tx.notes().isBlank()).count();
        int lowConfidenceCount = (int) transactions.stream().filter(tx -> "Niska".equals(tx.confidence())).count();
        int checkCount = (int) transactions.stream().filter(tx -> "Do sprawdzenia".equals(tx.correctedCategory())).count();
        double checkAmount = round2(transactions.stream()
                .filter(tx -> "Do sprawdzenia".equals(tx.correctedCategory()))
                .mapToDouble(tx -> Math.abs(tx.amount()))
                .sum());

        var budgetMixRows = budgetMixRows(transactions, incomeTotal, activeMonthCount, spendTotal, realSavingsOut, unassignedSurplus);
        var categoryPlanRows = categoryPlanRows(categories);

        double avgIncome = round2(incomeTotal / activeMonthCount);
        double avgSpend = round2(spendTotal / activeMonthCount);
        double needsTotal = sum(transactions.stream().filter(tx -> "Potrzeby".equals(tx.budgetBucket())).toList(), NormalizedTransaction::analysisSpend);
        double mixedNeedsTotal = sum(transactions.stream().filter(tx -> "Potrzeby mieszane".equals(tx.budgetBucket())).toList(), NormalizedTransaction::analysisSpend);
        double coreMonthlyCost = round2((needsTotal + mixedNeedsTotal) / activeMonthCount);
        double targetMonthlySpend = 14_000;
        double aggressiveMonthlySpend = 13_000;
        var latestMonthKey = activeMonths.getLast();
        var monthControl = monthControl(latestMonthKey, transactions, categoryPlanRows, targetMonthlySpend, periodStart, periodEnd, checkAmount, checkCount, categories);

        var payload = new LinkedHashMap<String, Object>();
        payload.put("year", input.year());
        payload.put("period", periodStart + " - " + periodEnd);
        payload.put("activeMonths", activeMonthCount);
        payload.put("kpis", mapOf(
                "income", incomeTotal,
                "spend", spendTotal,
                "discretionary", discretionaryTotal,
                "operatingSurplus", operatingSurplus,
                "savingsRate", surplusRatio,
                "excludedGross", excludedTotal,
                "excludedOutgoing", excludedOutgoingTotal,
                "excludedIncoming", excludedIncomingTotal,
                "excludedNet", excludedNetTotal,
                "realSavingsOutgoing", realSavingsOut,
                "unassignedSurplus", unassignedSurplus,
                "transactions", transactions.size(),
                "corrections", corrections,
                "lowConfidence", lowConfidenceCount,
                "toCheck", checkCount,
                "toCheckAmount", checkAmount
        ));
        payload.put("monthly", monthly);
        payload.put("categories", categories.stream().map(CategoryRow::toMap).toList());
        payload.put("hierarchy", hierarchy);
        payload.put("budgetMix", budgetMixRows);
        payload.put("savingsPlan", mapOf(
                "currentMonthlySpend", avgSpend,
                "currentMonthlyIncome", avgIncome,
                "coreMonthlyCost", coreMonthlyCost,
                "targetMonthlySpend", targetMonthlySpend,
                "aggressiveMonthlySpend", aggressiveMonthlySpend,
                "targetInvestmentTransfer", round2(Math.max(0, avgIncome - targetMonthlySpend)),
                "aggressiveInvestmentTransfer", round2(Math.max(0, avgIncome - aggressiveMonthlySpend)),
                "monthlyCutNeeded", round2(Math.max(0, avgSpend - targetMonthlySpend)),
                "emergencyFundMin", round2(coreMonthlyCost * 3),
                "emergencyFundComfort", round2(coreMonthlyCost * 6),
                "categoryLimits", categoryPlanRows
        ));
        payload.put("monthControl", monthControl.toMap());
        payload.put("fixedness", fixednessRows(transactions, activeMonthCount));
        payload.put("topMerchants", topMerchants);
        payload.put("recurring", recurringRows(transactions, activeMonthCount));
        payload.put("largeOneoffs", largeOneoffs(transactions));
        payload.put("transactions", transactions.stream().map(NormalizedTransaction::toPayloadMap).toList());

        return new BudgetAnalysisResult(input.year(), input.fileName(), payload, transactions, transactions.size(), incomeTotal, spendTotal);
    }

    private List<Map<String, Object>> monthlyRows(List<NormalizedTransaction> transactions, List<String> months, List<String> labels, LocalDate periodStart, LocalDate periodEnd) {
        var rows = new ArrayList<Map<String, Object>>();
        for (int i = 0; i < months.size(); i++) {
            var month = months.get(i);
            var items = transactions.stream().filter(tx -> tx.month().equals(month)).toList();
            double income = sum(items, NormalizedTransaction::income);
            double spend = sum(items, NormalizedTransaction::analysisSpend);
            double excluded = sum(items, NormalizedTransaction::excluded);
            double savings = sum(items.stream().filter(tx -> "Oszczędności i inwestycje".equals(tx.correctedCategory())).toList(), NormalizedTransaction::excluded);
            rows.add(mapOf(
                    "month", labels.get(i),
                    "income", income,
                    "spend", spend,
                    "discretionary", sum(items, NormalizedTransaction::discretionary),
                    "excluded", excluded,
                    "savingsInvestments", savings,
                    "netFlow", round2(income - spend),
                    "savingsRate", income == 0 ? 0 : (income - spend) / income,
                    "transactions", items.size(),
                    "spendPerDay", round2(spend / analysisDays(month, periodStart, periodEnd))
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
            double maxValue = monthValues.stream().mapToDouble(Double::doubleValue).max().orElse(0);
            var maxMonth = maxValue == 0 ? "" : labels.get(monthValues.indexOf(maxValue));
            rows.add(new CategoryRow(
                    category,
                    classifier.group(category),
                    sum(items, NormalizedTransaction::analysisSpend),
                    sum(items, NormalizedTransaction::income),
                    sum(items, NormalizedTransaction::excluded),
                    round2(sum(items, NormalizedTransaction::analysisSpend) / activeMonthCount),
                    maxMonth,
                    maxValue,
                    classifier.isDiscretionary(category) ? "Tak" : "Nie",
                    items.size()
            ));
        }
        rows.sort(Comparator.comparingDouble(CategoryRow::spend).reversed());
        return rows;
    }

    private List<Map<String, Object>> hierarchyRows(List<NormalizedTransaction> transactions, int activeMonthCount) {
        var keys = new TreeSet<String>();
        transactions.forEach(tx -> keys.add(tx.budgetArea() + "\u001F" + tx.group() + "\u001F" + tx.correctedCategory() + "\u001F" + tx.subcategory()));
        var rows = new ArrayList<Map<String, Object>>();
        for (var key : keys) {
            var parts = key.split("\u001F", -1);
            var items = transactions.stream()
                    .filter(tx -> tx.budgetArea().equals(parts[0]) && tx.group().equals(parts[1]) && tx.correctedCategory().equals(parts[2]) && tx.subcategory().equals(parts[3]))
                    .toList();
            double spend = sum(items, NormalizedTransaction::analysisSpend);
            rows.add(mapOf(
                    "area", parts[0],
                    "group", parts[1],
                    "category", parts[2],
                    "subcategory", parts[3],
                    "spend", spend,
                    "income", sum(items, NormalizedTransaction::income),
                    "excluded", sum(items, NormalizedTransaction::excluded),
                    "monthlyAverage", round2(spend / activeMonthCount),
                    "count", items.size(),
                    "discretionary", classifier.isDiscretionary(parts[2]) ? "Tak" : "Nie"
            ));
        }
        rows.sort(Comparator
                .comparing((Map<String, Object> row) -> String.valueOf(row.get("area")))
                .thenComparing(row -> -((Number) row.get("spend")).doubleValue())
                .thenComparing(row -> String.valueOf(row.get("group")))
                .thenComparing(row -> String.valueOf(row.get("category")))
                .thenComparing(row -> String.valueOf(row.get("subcategory"))));
        return rows;
    }

    private List<Map<String, Object>> topMerchants(List<NormalizedTransaction> transactions) {
        var agg = new LinkedHashMap<String, MerchantAgg>();
        for (var tx : transactions) {
            if (tx.analysisSpend() <= 0) {
                continue;
            }
            var entry = agg.computeIfAbsent(tx.merchant(), key -> new MerchantAgg(tx.correctedCategory()));
            entry.sum += tx.analysisSpend();
            entry.count++;
            entry.category = tx.correctedCategory();
        }
        return agg.entrySet().stream()
                .map(entry -> mapOf(
                        "merchant", entry.getKey(),
                        "category", entry.getValue().category,
                        "sum", round2(entry.getValue().sum),
                        "count", entry.getValue().count,
                        "average", round2(entry.getValue().sum / entry.getValue().count)
                ))
                .sorted(Comparator.comparingDouble((Map<String, Object> row) -> ((Number) row.get("sum")).doubleValue()).reversed())
                .limit(20)
                .toList();
    }

    private List<Map<String, Object>> budgetMixRows(List<NormalizedTransaction> transactions, double incomeTotal, int activeMonthCount, double spendTotal, double realSavingsOut, double unassignedSurplus) {
        double needs = sum(transactions.stream().filter(tx -> "Potrzeby".equals(tx.budgetBucket())).toList(), NormalizedTransaction::analysisSpend);
        double mixedNeeds = sum(transactions.stream().filter(tx -> "Potrzeby mieszane".equals(tx.budgetBucket())).toList(), NormalizedTransaction::analysisSpend);
        double wants = sum(transactions.stream().filter(tx -> Set.of("Zachcianki", "Zachcianki do rozbicia").contains(tx.budgetBucket())).toList(), NormalizedTransaction::analysisSpend);
        double unclear = sum(transactions.stream().filter(tx -> "Zachcianki do rozbicia".equals(tx.budgetBucket())).toList(), NormalizedTransaction::analysisSpend);
        return List.of(
                mixRow("Potrzeby", needs, activeMonthCount, incomeTotal, "Cel bazowy: do 50% dochodu netto"),
                mixRow("Potrzeby mieszane", mixedNeeds, activeMonthCount, incomeTotal, "Do ręcznego rozbicia: część może być konieczna, część uznaniowa"),
                mixRow("Zachcianki", wants, activeMonthCount, incomeTotal, "Cel bazowy: do 30% dochodu, przy agresywnym inwestowaniu niżej"),
                mixRow("Marketplace do rozbicia", unclear, activeMonthCount, incomeTotal, "Największe ryzyko błędnej interpretacji bez historii zamówień"),
                mixRow("Oszczędzanie/inwestycje wykonane", realSavingsOut, activeMonthCount, incomeTotal, "Przelewy na inwestycje/oszczędności i nadpłaty kredytu"),
                mixRow("Nadwyżka operacyjna po oszczędnościach", unassignedSurplus, activeMonthCount, incomeTotal, "Dochód minus wydatki analizowane minus rozpoznane inwestycje/nadpłaty")
        );
    }

    private Map<String, Object> mixRow(String bucket, double sum, int activeMonthCount, double incomeTotal, String note) {
        return mapOf("bucket", bucket, "sum", sum, "monthlyAverage", round2(sum / activeMonthCount), "incomeShare", incomeTotal == 0 ? 0 : sum / incomeTotal, "note", note);
    }

    private List<Map<String, Object>> categoryPlanRows(List<CategoryRow> categories) {
        var rows = new ArrayList<Map<String, Object>>();
        for (var category : categories) {
            if (category.monthlyAverage() <= 0) {
                continue;
            }
            var configured = CATEGORY_LIMITS.get(category.category());
            double limit;
            String action;
            if (configured != null) {
                limit = configured.limit();
                action = configured.action();
            } else if ("Tak".equals(category.discretionary())) {
                limit = round2(category.monthlyAverage() * 0.82);
                action = "Ustawić limit miesięczny i opóźnić zakupy uznaniowe o 24-48 godzin.";
            } else if ("Potrzeby".equals(classifier.budgetBucket(category.category()))) {
                limit = round2(category.monthlyAverage());
                action = "Monitorować, ale nie ciąć bez świadomej decyzji.";
            } else {
                limit = round2(category.monthlyAverage() * 0.95);
                action = "Sprawdzić największe transakcje i powtarzalność.";
            }
            double potential = round2(Math.max(0, category.monthlyAverage() - limit));
            rows.add(mapOf(
                    "category", category.category(),
                    "bucket", classifier.budgetBucket(category.category()),
                    "currentMonthly", category.monthlyAverage(),
                    "limit", limit,
                    "potentialMonthly", potential,
                    "potentialYearly", round2(potential * 12),
                    "priority", potential >= 500 ? "Wysoki" : potential >= 150 ? "Średni" : "Niski",
                    "action", action
            ));
        }
        rows.sort(Comparator.comparingDouble((Map<String, Object> row) -> ((Number) row.get("potentialMonthly")).doubleValue()).reversed());
        return rows;
    }

    private MonthControl monthControl(String latestMonthKey, List<NormalizedTransaction> transactions, List<Map<String, Object>> categoryPlanRows, double targetMonthlySpend, LocalDate periodStart, LocalDate periodEnd, double checkAmount, int checkCount, List<CategoryRow> categories) {
        var latest = YearMonth.parse(latestMonthKey);
        int daysTotal = latest.lengthOfMonth();
        int elapsedDays = analysisDays(latestMonthKey, periodStart, periodEnd);
        int remainingDays = Math.max(0, daysTotal - elapsedDays);
        var latestItems = transactions.stream().filter(tx -> tx.month().equals(latestMonthKey)).toList();
        double spend = sum(latestItems, NormalizedTransaction::analysisSpend);
        double income = sum(latestItems, NormalizedTransaction::income);
        double projection = elapsedDays == 0 ? spend : round2(spend / elapsedDays * daysTotal);
        double remainingBudget = round2(targetMonthlySpend - spend);
        double dailyAllowed = remainingDays == 0 ? 0 : round2(Math.max(0, remainingBudget) / remainingDays);
        double projectedDelta = round2(targetMonthlySpend - projection);

        var statuses = new ArrayList<Map<String, Object>>();
        for (var row : categoryPlanRows) {
            var category = String.valueOf(row.get("category"));
            double current = sum(latestItems.stream().filter(tx -> tx.correctedCategory().equals(category)).toList(), NormalizedTransaction::analysisSpend);
            double projected = elapsedDays == 0 ? current : round2(current / elapsedDays * daysTotal);
            double limit = ((Number) row.get("limit")).doubleValue();
            var copy = new LinkedHashMap<>(row);
            copy.put("currentMonthSpend", current);
            copy.put("currentMonthProjection", projected);
            copy.put("remainingThisMonth", round2(limit - current));
            copy.put("projectedDelta", round2(limit - projected));
            copy.put("usage", limit == 0 ? 0 : current / limit);
            statuses.add(copy);
        }

        var alerts = new ArrayList<Map<String, Object>>();
        if (projection > targetMonthlySpend) {
            alerts.add(mapOf("type", "Ryzyko przekroczenia targetu", "severity", "Wysoki", "message", "Prognoza " + latest.format(MONTH_LABEL) + " to " + roundedPln(projection) + " zł przy celu " + roundedPln(targetMonthlySpend) + " zł."));
        }
        statuses.stream().limit(14).forEach(row -> {
            double projected = ((Number) row.get("currentMonthProjection")).doubleValue();
            double limit = ((Number) row.get("limit")).doubleValue();
            double delta = ((Number) row.get("projectedDelta")).doubleValue();
            if (projected > limit && limit > 0) {
                alerts.add(mapOf("type", "Kategoria ponad limitem", "severity", delta > -500 ? "Średni" : "Wysoki", "message", row.get("category") + ": prognoza " + roundedPln(projected) + " zł vs limit " + roundedPln(limit) + " zł."));
            }
        });
        if (checkAmount > 0) {
            alerts.add(mapOf("type", "Dane do sprawdzenia", "severity", "Średni", "message", checkCount + " transakcji (" + roundedPln(checkAmount) + " zł) wymaga ręcznej decyzji."));
        }

        var categoryAverages = new LinkedHashMap<String, Double>();
        categories.forEach(row -> categoryAverages.put(row.category(), row.monthlyAverage()));
        var sinkingFunds = new ArrayList<Map<String, Object>>();
        for (var entry : Map.of(
                "Podróże i wyjazdy", "Podróże",
                "Ubezpieczenia", "Ubezpieczenia",
                "Zwierzęta", "Zwierzęta",
                "Paliwo i auto", "Auto",
                "Elektronika", "Elektronika",
                "Zdrowie i uroda", "Zdrowie/uroda"
        ).entrySet()) {
            double avg = categoryAverages.getOrDefault(entry.getKey(), 0.0);
            if (avg > 0) {
                sinkingFunds.add(mapOf("name", entry.getValue(), "category", entry.getKey(), "monthlySetAside", avg, "yearlyNeed", round2(avg * 12), "note", "Fundusz celowy na koszty nierówne w czasie."));
            }
        }

        return new MonthControl(latest.format(MONTH_LABEL), latestMonthKey, elapsedDays, remainingDays, daysTotal, income, spend, projection, targetMonthlySpend, remainingBudget, dailyAllowed, projectedDelta, statuses, alerts, sinkingFunds);
    }

    private List<Map<String, Object>> fixednessRows(List<NormalizedTransaction> transactions, int activeMonthCount) {
        var rows = new ArrayList<Map<String, Object>>();
        for (var label : List.of("Stałe", "Zmienne konieczne", "Uznaniowe", "Oszczędności", "Transfer/wyłączone", "Do oceny")) {
            var items = transactions.stream().filter(tx -> tx.fixedness().equals(label)).toList();
            double spend = sum(items, NormalizedTransaction::analysisSpend);
            double excludedOutgoing = sum(items, NormalizedTransaction::excludedOutgoing);
            if (spend != 0 || excludedOutgoing != 0) {
                rows.add(mapOf("type", label, "spend", spend, "excludedOutgoing", excludedOutgoing, "monthlyAverage", round2(spend / activeMonthCount), "count", items.size()));
            }
        }
        return rows;
    }

    private List<Map<String, Object>> recurringRows(List<NormalizedTransaction> transactions, int activeMonthCount) {
        var merchantMonths = new LinkedHashMap<String, RecurringAgg>();
        for (var tx : transactions) {
            if (tx.analysisSpend() <= 0) {
                continue;
            }
            var key = tx.merchant() + "\u001F" + tx.correctedCategory();
            var agg = merchantMonths.computeIfAbsent(key, ignored -> new RecurringAgg(tx.merchant(), tx.correctedCategory(), tx.budgetBucket()));
            agg.months.add(tx.month());
            agg.amount += tx.analysisSpend();
            agg.count++;
            agg.days.add(tx.date().getDayOfMonth());
            agg.lastDate = agg.lastDate == null || tx.date().isAfter(agg.lastDate) ? tx.date() : agg.lastDate;
        }
        int threshold = Math.max(3, Math.min(4, activeMonthCount));
        return merchantMonths.values().stream()
                .filter(agg -> agg.months.size() >= threshold || (agg.amount >= 1000 && agg.months.size() >= 2))
                .map(agg -> mapOf(
                        "merchant", agg.merchant,
                        "category", agg.category,
                        "bucket", agg.bucket,
                        "sum", round2(agg.amount),
                        "months", agg.months.size(),
                        "count", agg.count,
                        "monthlyAverage", round2(agg.amount / agg.months.size()),
                        "avgDay", Math.round((float) agg.days.stream().mapToInt(Integer::intValue).average().orElse(0)),
                        "lastDate", agg.lastDate == null ? null : agg.lastDate.toString()
                ))
                .sorted(Comparator.comparingDouble((Map<String, Object> row) -> ((Number) row.get("sum")).doubleValue()).reversed())
                .limit(120)
                .toList();
    }

    private List<Map<String, Object>> largeOneoffs(List<NormalizedTransaction> transactions) {
        return transactions.stream()
                .filter(tx -> tx.analysisSpend() >= 1000)
                .map(tx -> mapOf(
                        "date", tx.date().toString(),
                        "merchant", tx.merchant(),
                        "category", tx.correctedCategory(),
                        "bucket", tx.budgetBucket(),
                        "amount", tx.analysisSpend(),
                        "month", tx.month(),
                        "confidence", tx.confidence(),
                        "description", tx.description().length() > 120 ? tx.description().substring(0, 120) : tx.description()
                ))
                .sorted(Comparator.comparingDouble((Map<String, Object> row) -> ((Number) row.get("amount")).doubleValue()).reversed())
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
        for (int month = 1; month <= 12; month++) {
            months.add("%04d-%02d".formatted(year, month));
        }
        return months;
    }

    private List<String> monthLabels(int year) {
        var labels = new ArrayList<String>();
        for (int month = 1; month <= 12; month++) {
            labels.add("%02d.%04d".formatted(month, year));
        }
        return labels;
    }

    private double sum(List<NormalizedTransaction> items, ToDoubleFunction<NormalizedTransaction> extractor) {
        return round2(items.stream().mapToDouble(extractor).sum());
    }

    private double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private String roundedPln(double value) {
        return String.format("%,.0f", value).replace(",", " ");
    }

    private Map<String, Object> mapOf(Object... values) {
        var map = new LinkedHashMap<String, Object>();
        for (int i = 0; i < values.length; i += 2) {
            map.put(String.valueOf(values[i]), values[i + 1]);
        }
        return map;
    }

    private record CategoryLimit(double limit, String action) {
    }

    private record CategoryRow(String category, String group, double spend, double income, double excluded, double monthlyAverage, String maxMonth, double maxAmount, String discretionary, int count) {
        Map<String, Object> toMap() {
            var map = new LinkedHashMap<String, Object>();
            map.put("category", category);
            map.put("group", group);
            map.put("spend", spend);
            map.put("income", income);
            map.put("excluded", excluded);
            map.put("monthlyAverage", monthlyAverage);
            map.put("maxMonth", maxMonth);
            map.put("maxAmount", maxAmount);
            map.put("discretionary", discretionary);
            map.put("count", count);
            return map;
        }
    }

    private static final class MerchantAgg {
        private String category;
        private double sum;
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
        private double amount;
        private int count;
        private LocalDate lastDate;

        private RecurringAgg(String merchant, String category, String bucket) {
            this.merchant = merchant;
            this.category = category;
            this.bucket = bucket;
        }
    }

    private record MonthControl(
            String month,
            String monthKey,
            int elapsedDays,
            int remainingDays,
            int daysInMonth,
            double incomeToDate,
            double spendToDate,
            double projectedSpend,
            double targetSpend,
            double remainingBudget,
            double dailyAllowed,
            double projectedDelta,
            List<Map<String, Object>> categoryStatus,
            List<Map<String, Object>> alerts,
            List<Map<String, Object>> sinkingFunds
    ) {
        Map<String, Object> toMap() {
            var map = new LinkedHashMap<String, Object>();
            map.put("month", month);
            map.put("monthKey", monthKey);
            map.put("elapsedDays", elapsedDays);
            map.put("remainingDays", remainingDays);
            map.put("daysInMonth", daysInMonth);
            map.put("incomeToDate", incomeToDate);
            map.put("spendToDate", spendToDate);
            map.put("projectedSpend", projectedSpend);
            map.put("targetSpend", targetSpend);
            map.put("remainingBudget", remainingBudget);
            map.put("dailyAllowed", dailyAllowed);
            map.put("projectedDelta", projectedDelta);
            map.put("categoryStatus", categoryStatus);
            map.put("alerts", alerts);
            map.put("sinkingFunds", sinkingFunds);
            return map;
        }
    }
}
