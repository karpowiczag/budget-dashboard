package com.budget.domain;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

public record NormalizedTransaction(
        int lp,
        LocalDate date,
        String month,
        String merchant,
        String description,
        String account,
        String bankCategory,
        String correctedCategory,
        String budgetArea,
        String group,
        String subcategory,
        String budgetBucket,
        String fixedness,
        String type,
        double amount,
        double income,
        double analysisSpend,
        double discretionary,
        double excluded,
        double excludedOutgoing,
        double excludedIncoming,
        double excludedNet,
        String confidence,
        String notes,
        String matchedRule
) {
    public Map<String, Object> toPayloadMap() {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("Lp", lp);
        row.put("Data", date.toString());
        row.put("Miesiąc", month);
        row.put("Sprzedawca", merchant);
        row.put("Opis", description);
        row.put("Rachunek", account);
        row.put("Kategoria banku", bankCategory);
        row.put("Kategoria skorygowana", correctedCategory);
        row.put("Obszar budżetu", budgetArea);
        row.put("Grupa", group);
        row.put("Podkategoria", subcategory);
        row.put("Koszyk budżetu", budgetBucket);
        row.put("Stałe/zmienne", fixedness);
        row.put("Typ", type);
        row.put("Kwota", amount);
        row.put("Wpływ", income);
        row.put("Wydatek analizy", analysisSpend);
        row.put("Uznaniowe", discretionary);
        row.put("Wyłączone", excluded);
        row.put("Wyłączone wychodzące", excludedOutgoing);
        row.put("Wyłączone przychodzące", excludedIncoming);
        row.put("Wyłączone netto", excludedNet);
        row.put("Pewność kategorii", confidence);
        row.put("Uwagi", notes);
        row.put("Reguła dopasowania", matchedRule);
        return row;
    }
}
