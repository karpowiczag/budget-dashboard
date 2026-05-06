package com.budget.application.importing;

import com.budget.application.reporting.BudgetReportStore;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ImportAuditService {
    private static final int MAX_MESSAGE_LENGTH = 1_000;

    private final BudgetReportStore repository;

    public ImportAuditService(BudgetReportStore repository) {
        this.repository = repository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(Integer year, String inputCsv, String status, String message) {
        repository.recordImportRun(year, inputCsv, status, sanitize(message));
    }

    private String sanitize(String message) {
        if (message == null || message.isBlank()) {
            return "";
        }
        var normalized = message.replaceAll("\\R+", " ").trim();
        return normalized.length() <= MAX_MESSAGE_LENGTH ? normalized : normalized.substring(0, MAX_MESSAGE_LENGTH);
    }
}
