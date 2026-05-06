package com.budget.application;

import com.budget.domain.ImportRun;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class BudgetQueryService {
    private final BudgetReportStore repository;

    public BudgetQueryService(BudgetReportStore repository) {
        this.repository = repository;
    }

    public List<Map<String, Object>> years() {
        return repository.findYears();
    }

    public Map<String, Object> budget(int year) {
        return repository.findPayload(year);
    }

    public List<ImportRun> importRuns() {
        return repository.findImportRuns();
    }
}
