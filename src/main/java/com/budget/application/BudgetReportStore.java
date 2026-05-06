package com.budget.application;

import com.budget.domain.BudgetAnalysisResult;
import com.budget.domain.ImportRun;
import java.util.List;
import java.util.Map;

public interface BudgetReportStore {
    void save(BudgetAnalysisResult result);

    List<Map<String, Object>> findYears();

    Map<String, Object> findPayload(int year);

    void recordImportRun(Integer year, String inputCsv, String status, String message);

    List<ImportRun> findImportRuns();
}
