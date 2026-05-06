package com.budget.application.reporting;

import com.budget.domain.importjob.ImportRun;
import com.budget.domain.report.BudgetAnalysisResult;
import com.budget.domain.report.BudgetSnapshot;
import java.util.List;

public interface BudgetReportStore {
    void save(BudgetAnalysisResult result);

    List<YearSummary> findYears();

    BudgetSnapshot findDashboard(int year);

    CalendarReport findCalendar(int year, String month);

    AnalyticsReport findAnalytics(TransactionQuery query);

    TransactionPage findTransactions(TransactionQuery query);

    void recordImportRun(Integer year, String inputCsv, String status, String message);

    List<ImportRun> findImportRuns();
}
