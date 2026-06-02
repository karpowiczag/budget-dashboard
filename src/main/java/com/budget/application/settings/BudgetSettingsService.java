package com.budget.application.settings;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BudgetSettingsService {
    private static final BigDecimal DEFAULT_NET_INCOME_RATIO = new BigDecimal("0.75");
    private static final List<String> DEFAULT_SINKING_FUND_CATEGORIES = List.of(
            "Podróże i wyjazdy", "Ubezpieczenia", "Zwierzęta", "Paliwo i auto",
            "Elektronika", "Lekarz i apteka", "Uroda i kosmetyki");
    private static final Set<String> LIMIT_SCOPES = Set.of("bucket", "area", "group", "category");
    private static final Set<String> BUCKET_OVERRIDES = Set.of(
            "Obowiązkowe stałe",
            "Obowiązkowe zmienne",
            "Do rozbicia",
            "Nieobowiązkowe",
            "Nieregularne",
            "Inwestycje",
            "Konto oszczędnościowe",
            "Nadpłata kredytu"
    );

    private final BudgetSettingsDefaults defaults;
    private final BudgetSettingsStore store;

    public BudgetSettingsService(BudgetSettingsDefaults defaults, BudgetSettingsStore store) {
        this.defaults = defaults;
        this.store = store;
    }

    public BudgetSettings current() {
        return mergeWithDefaults(store.findDefaultSettings().orElse(defaults.toSettings()));
    }

    @Transactional
    public BudgetSettings save(BudgetSettings settings) {
        return store.saveDefaultSettings(validate(settings));
    }

    private BudgetSettings mergeWithDefaults(BudgetSettings settings) {
        return new BudgetSettings(
                positiveOrDefault(settings.targetMonthlySpend(), defaults.targetMonthlySpend()),
                positiveOrDefault(settings.aggressiveMonthlySpend(), defaults.aggressiveMonthlySpend()),
                positiveOrDefault(settings.emergencyFundMinMonths(), defaults.emergencyFundMinMonths()),
                positiveOrDefault(settings.emergencyFundComfortMonths(), defaults.emergencyFundComfortMonths()),
                netIncomeRatioOrDefault(settings.netIncomeRatio()),
                sinkingFundCategoriesOrDefault(settings.sinkingFundCategories()),
                settings.categoryLimits()
        );
    }

    private BigDecimal netIncomeRatioOrDefault(BigDecimal ratio) {
        return ratio != null && ratio.signum() > 0 && ratio.compareTo(BigDecimal.ONE) <= 0
                ? ratio.setScale(4, RoundingMode.HALF_UP)
                : DEFAULT_NET_INCOME_RATIO;
    }

    private List<String> sinkingFundCategoriesOrDefault(List<String> categories) {
        if (categories == null || categories.isEmpty()) {
            return DEFAULT_SINKING_FUND_CATEGORIES;
        }
        var seen = new LinkedHashSet<String>();
        for (var category : categories) {
            if (category != null && !category.isBlank()) {
                seen.add(category.trim());
            }
        }
        return seen.isEmpty() ? DEFAULT_SINKING_FUND_CATEGORIES : List.copyOf(seen);
    }

    private BudgetSettings validate(BudgetSettings settings) {
        if (settings == null) {
            throw new IllegalArgumentException("Budget settings body is required");
        }
        var normalized = mergeWithDefaults(settings);
        if (normalized.aggressiveMonthlySpend().compareTo(normalized.targetMonthlySpend()) > 0) {
            throw new IllegalArgumentException("aggressiveMonthlySpend cannot be greater than targetMonthlySpend");
        }
        if (normalized.emergencyFundComfortMonths() < normalized.emergencyFundMinMonths()) {
            throw new IllegalArgumentException("emergencyFundComfortMonths cannot be lower than emergencyFundMinMonths");
        }
        var limitsByKey = new LinkedHashMap<String, BudgetSettings.CategoryLimitSetting>();
        for (var row : normalized.categoryLimits()) {
            var scope = normalizeScope(row.scope());
            var name = normalizeName(row);
            var bucketOverride = normalizeBucketOverride(row.bucketOverride());
            if (name.isBlank()) {
                continue;
            }
            if (row.limit().signum() < 0) {
                throw new IllegalArgumentException("Category limits cannot be negative");
            }
            var key = scope + "\u001F" + name;
            if (limitsByKey.containsKey(key)) {
                throw new IllegalArgumentException("Duplicate budget limit: " + scope + "/" + name);
            }
            limitsByKey.put(key, new BudgetSettings.CategoryLimitSetting(
                    scope,
                    name,
                    "category".equals(scope) ? name : "",
                    money(row.limit()),
                    row.action() == null ? "" : row.action().trim(),
                    "category".equals(scope) ? bucketOverride : ""
            ));
        }
        return new BudgetSettings(
                money(normalized.targetMonthlySpend()),
                money(normalized.aggressiveMonthlySpend()),
                normalized.emergencyFundMinMonths(),
                normalized.emergencyFundComfortMonths(),
                normalized.netIncomeRatio(),
                normalized.sinkingFundCategories(),
                java.util.List.copyOf(limitsByKey.values())
        );
    }

    private String normalizeScope(String scope) {
        var normalized = scope == null || scope.isBlank() ? "category" : scope.trim().toLowerCase(java.util.Locale.ROOT);
        if (!LIMIT_SCOPES.contains(normalized)) {
            throw new IllegalArgumentException("Unsupported budget limit scope: " + scope);
        }
        return normalized;
    }

    private String normalizeName(BudgetSettings.CategoryLimitSetting row) {
        var name = row.name();
        if (name == null || name.isBlank()) {
            name = row.category();
        }
        return name == null ? "" : name.trim();
    }

    private String normalizeBucketOverride(String bucketOverride) {
        var normalized = bucketOverride == null ? "" : bucketOverride.trim();
        if (normalized.isBlank()) {
            return "";
        }
        if (!BUCKET_OVERRIDES.contains(normalized)) {
            throw new IllegalArgumentException("Unsupported budget bucket override: " + bucketOverride);
        }
        return normalized;
    }

    private BigDecimal positiveOrDefault(BigDecimal value, BigDecimal fallback) {
        return value != null && value.signum() > 0 ? value : fallback;
    }

    private int positiveOrDefault(int value, int fallback) {
        return value > 0 ? value : fallback;
    }

    private BigDecimal money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }
}
