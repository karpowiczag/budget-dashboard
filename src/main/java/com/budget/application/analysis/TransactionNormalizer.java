package com.budget.application.analysis;

import com.budget.application.categorization.CategoryClassifier;
import com.budget.domain.transaction.BankTransaction;
import com.budget.domain.transaction.NormalizedTransaction;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class TransactionNormalizer {
    private static final BigDecimal LARGE_MARKETPLACE_AMOUNT = BigDecimal.valueOf(500);
    private static final Pattern MERCHANT_SPLIT = Pattern.compile(" ZAKUP| BLIK| PRZELEW| PŁATNOŚĆ| PLATNOSC| WPŁATA| WPLATA| DATA ", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    private final CategoryClassifier classifier;

    public TransactionNormalizer(CategoryClassifier classifier) {
        this.classifier = classifier;
    }

    public List<NormalizedTransaction> normalize(List<BankTransaction> bankTransactions) {
        var normalized = new ArrayList<NormalizedTransaction>();
        int lp = 1;
        for (var row : bankTransactions) {
            var value = money(row.amount());
            var description = clean(row.description());
            var bankCategory = clean(row.bankCategory());
            var decision = classifier.classifyDecision(bankCategory, description, value);
            var correctedCategory = decision.category();
            var confidence = confidence(value, correctedCategory, decision.matchedByTitle());
            var notes = notes(bankCategory, correctedCategory, confidence);
            var realIncome = value.signum() > 0 && classifier.isRealIncome(correctedCategory);
            var spend = value.signum() < 0 && !classifier.isExcluded(correctedCategory);
            var excludedFlow = classifier.isExcluded(correctedCategory) || (value.signum() > 0 && !realIncome);
            var excluded = excludedFlow ? value.abs() : BigDecimal.ZERO;
            var excludedOutgoing = excludedFlow && value.signum() < 0 ? value.negate() : BigDecimal.ZERO;
            var excludedIncoming = excludedFlow && value.signum() > 0 ? value : BigDecimal.ZERO;
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
                    value.signum() >= 0 ? "Wpływ" : "Wydatek",
                    value,
                    realIncome ? value : BigDecimal.ZERO,
                    spend ? value.negate() : BigDecimal.ZERO,
                    spend && classifier.isDiscretionary(correctedCategory) ? value.negate() : BigDecimal.ZERO,
                    money(excluded),
                    money(excludedOutgoing),
                    money(excludedIncoming),
                    money(excludedIncoming.subtract(excludedOutgoing)),
                    confidence,
                    String.join("; ", notes),
                    decision.pattern()
            ));
        }
        return normalized;
    }

    private String confidence(BigDecimal value, String correctedCategory, boolean matchedByTitle) {
        if ("Do sprawdzenia".equals(correctedCategory)
                || (value.abs().compareTo(LARGE_MARKETPLACE_AMOUNT) >= 0 && "Marketplace i zakupy online".equals(correctedCategory))) {
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

    private BigDecimal money(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP);
    }
}
