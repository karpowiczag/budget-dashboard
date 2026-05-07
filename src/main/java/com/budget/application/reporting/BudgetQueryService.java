package com.budget.application.reporting;

import com.budget.domain.importjob.ImportRun;
import com.budget.domain.report.BudgetSnapshot;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;

@Service
public class BudgetQueryService {
    private final BudgetReportStore repository;

    public BudgetQueryService(BudgetReportStore repository) {
        this.repository = repository;
    }

    public List<YearSummary> years() {
        return repository.findYears();
    }

    public BudgetSnapshot dashboard(int year) {
        return repository.findDashboard(year);
    }

    public CalendarReport calendar(int year, String month) {
        return repository.findCalendar(year, normalizeOptionalMonth(month));
    }

    public AnalyticsReport analytics(
            int year,
            String scope,
            String month,
            LocalDate date,
            String area,
            String group,
            String category,
            String subcategory
    ) {
        var query = new TransactionQuery(year, 0, 5_000, "spend,desc", month, date, null, null, null, area, group, category, subcategory);
        return repository.findAnalytics(queryForScope(scope, query));
    }

    public TransactionPage transactions(TransactionQuery query) {
        return repository.findTransactions(normalizeTransactionQuery(query));
    }

    public List<ImportRun> importRuns() {
        return repository.findImportRuns();
    }

    private TransactionQuery queryForScope(String scope, TransactionQuery query) {
        return switch (normalizeScope(scope)) {
            case "day" -> dayScopedQuery(query);
            case "month" -> new TransactionQuery(
                    query.year(), query.page(), query.size(), query.sort(),
                    requiredMonth(query.month(), "month"), null,
                    query.query(), query.flow(), query.bucket(), query.area(), query.group(), query.category(), query.subcategory(),
                    query.fixedness(), query.confidence()
            );
            case "year" -> new TransactionQuery(
                    query.year(), query.page(), query.size(), query.sort(),
                    null, null,
                    query.query(), query.flow(), query.bucket(), query.area(), query.group(), query.category(), query.subcategory(),
                    query.fixedness(), query.confidence()
            );
            default -> throw new IllegalArgumentException("Unsupported analytics scope: " + scope);
        };
    }

    private TransactionQuery dayScopedQuery(TransactionQuery query) {
        if (query.date() == null) {
            throw new IllegalArgumentException("date is required for day analytics scope");
        }
        var dateMonth = YearMonth.from(query.date()).toString();
        if (query.month() != null && !dateMonth.equals(requiredMonth(query.month(), "month"))) {
            throw new IllegalArgumentException("month must match date for day analytics scope");
        }
        return new TransactionQuery(
                query.year(), query.page(), query.size(), query.sort(),
                dateMonth, query.date(),
                query.query(), query.flow(), query.bucket(), query.area(), query.group(), query.category(), query.subcategory(),
                query.fixedness(), query.confidence()
        );
    }

    private TransactionQuery normalizeTransactionQuery(TransactionQuery query) {
        var month = normalizeOptionalMonth(query.month());
        if (month != null && query.date() != null && !month.equals(YearMonth.from(query.date()).toString())) {
            throw new IllegalArgumentException("month must match date");
        }
        return new TransactionQuery(
                query.year(), query.page(), query.size(), query.sort(),
                month, query.date(),
                query.query(), query.flow(), query.bucket(), query.area(), query.group(), query.category(), query.subcategory(),
                query.fixedness(), query.confidence()
        );
    }

    private String normalizeScope(String scope) {
        return scope == null || scope.isBlank() ? "year" : scope.toLowerCase(Locale.ROOT);
    }

    private String requiredMonth(String month, String parameterName) {
        if (month == null || month.isBlank()) {
            throw new IllegalArgumentException(parameterName + " is required");
        }
        return parseMonth(month, parameterName);
    }

    private String normalizeOptionalMonth(String month) {
        return month == null || month.isBlank() ? null : parseMonth(month, "month");
    }

    private String parseMonth(String month, String parameterName) {
        try {
            return YearMonth.parse(month).toString();
        } catch (DateTimeException e) {
            throw new IllegalArgumentException(parameterName + " must use YYYY-MM format", e);
        }
    }
}
