package com.budget.application.analysis;

import com.budget.application.categorization.CategoryClassifier;
import com.budget.domain.transaction.BankTransaction;
import com.budget.domain.transaction.NormalizedTransaction;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class TransactionNormalizer {
    private static final Pattern MERCHANT_SPLIT = Pattern.compile(" ZAKUP| BLIK| PRZELEW| PŁATNOŚĆ| PLATNOSC| WPŁATA| WPLATA| DATA ", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    private final CategoryClassifier classifier;

    public TransactionNormalizer(CategoryClassifier classifier) {
        this.classifier = classifier;
    }

    public List<NormalizedTransaction> normalize(List<BankTransaction> bankTransactions) {
        var normalized = new ArrayList<NormalizedTransaction>();
        int lp = 1;
        for (var row : bankTransactions) {
            double value = row.amount();
            var description = clean(row.description());
            var bankCategory = clean(row.bankCategory());
            var decision = classifier.classifyDecision(bankCategory, description, value);
            var correctedCategory = decision.category();
            var confidence = confidence(value, correctedCategory, decision.matchedByTitle());
            var notes = notes(bankCategory, correctedCategory, confidence);
            boolean realIncome = value > 0 && classifier.isRealIncome(correctedCategory);
            boolean spend = value < 0 && !classifier.isExcluded(correctedCategory);
            boolean excludedFlow = classifier.isExcluded(correctedCategory) || (value > 0 && !realIncome);
            double excluded = excludedFlow ? Math.abs(value) : 0;
            double excludedOutgoing = excludedFlow && value < 0 ? -value : 0;
            double excludedIncoming = excludedFlow && value > 0 ? value : 0;
            var month = row.date().toString().substring(0, 7);
            normalized.add(new NormalizedTransaction(
                    lp++,
                    row.date(),
                    month,
                    merchant(description),
                    description,
                    row.account(),
                    bankCategory,
                    correctedCategory,
                    classifier.budgetArea(correctedCategory),
                    classifier.group(correctedCategory),
                    classifier.subcategory(correctedCategory, description),
                    classifier.budgetBucket(correctedCategory),
                    classifier.fixedness(correctedCategory),
                    value >= 0 ? "Wpływ" : "Wydatek",
                    value,
                    realIncome ? value : 0,
                    spend ? -value : 0,
                    spend && classifier.isDiscretionary(correctedCategory) ? -value : 0,
                    round2(excluded),
                    round2(excludedOutgoing),
                    round2(excludedIncoming),
                    round2(excludedIncoming - excludedOutgoing),
                    confidence,
                    String.join("; ", notes),
                    decision.pattern()
            ));
        }
        return normalized;
    }

    private String confidence(double value, String correctedCategory, boolean matchedByTitle) {
        if ("Do sprawdzenia".equals(correctedCategory) || (Math.abs(value) >= 500 && "Marketplace i zakupy online".equals(correctedCategory))) {
            return "Niska";
        }
        if ("Marketplace i zakupy online".equals(correctedCategory) || !matchedByTitle) {
            return "Średnia";
        }
        return "Wysoka";
    }

    private List<String> notes(String bankCategory, String correctedCategory, String confidence) {
        var notes = new ArrayList<String>();
        if (!bankCategory.equals(correctedCategory)) {
            notes.add("Własna kategoria z tytułu/opisu");
        }
        if ("Niska".equals(confidence)) {
            notes.add("Do ręcznego sprawdzenia");
        }
        return notes;
    }

    private String merchant(String description) {
        var parts = MERCHANT_SPLIT.split(description, 2);
        var value = parts.length > 0 && !parts[0].isBlank() ? parts[0] : description;
        return value.length() > 60 ? value.substring(0, 60).trim() : value.trim();
    }

    private String clean(String value) {
        return value == null ? "" : value.replaceAll("\\s+", " ").trim();
    }

    private double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
