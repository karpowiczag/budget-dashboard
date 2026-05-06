package com.budget.application.reporting;

import com.budget.domain.importjob.ImportRun;
import com.budget.domain.report.BudgetSnapshot;
import java.time.LocalDate;
import java.util.List;
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
        return repository.findCalendar(year, month);
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
        var query = new TransactionQuery(year, 0, 5_000, "spend,desc", month, date, null, null, area, group, category, subcategory);
        return repository.findAnalytics(queryForScope(scope, query));
    }

    public TransactionPage transactions(TransactionQuery query) {
        return repository.findTransactions(query);
    }

    public List<ImportRun> importRuns() {
        return repository.findImportRuns();
    }

    private TransactionQuery queryForScope(String scope, TransactionQuery query) {
        return switch (scope == null || scope.isBlank() ? "year" : scope) {
            case "day" -> new TransactionQuery(query.year(), query.page(), query.size(), query.sort(), query.month(), query.date(), query.query(), query.bucket(), query.area(), query.group(), query.category(), query.subcategory());
            case "month" -> new TransactionQuery(query.year(), query.page(), query.size(), query.sort(), query.month(), null, query.query(), query.bucket(), query.area(), query.group(), query.category(), query.subcategory());
            case "year" -> new TransactionQuery(query.year(), query.page(), query.size(), query.sort(), null, null, query.query(), query.bucket(), query.area(), query.group(), query.category(), query.subcategory());
            default -> throw new IllegalArgumentException("Unsupported analytics scope: " + scope);
        };
    }
}
