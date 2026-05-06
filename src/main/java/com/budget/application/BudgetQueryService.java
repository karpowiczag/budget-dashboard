package com.budget.application;

import com.budget.domain.ImportRun;
import com.budget.infrastructure.BudgetReportRepository;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class BudgetQueryService {
    private final BudgetReportRepository repository;

    public BudgetQueryService(BudgetReportRepository repository) {
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
