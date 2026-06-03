package com.budget.application.importing;

import com.budget.application.categorization.CategoryCatalog;
import com.budget.application.categorization.TransactionOverrideStore;
import com.budget.application.reporting.BudgetReportStore;
import com.budget.domain.transaction.TransactionContentKey;
import org.springframework.stereotype.Service;

/**
 * Manual recategorize use case (Phase 3c-iii). Resolves the UI's surrogate transaction id to the
 * transaction's identity, records a content-keyed override (highest precedence, rebuild-stable), then
 * re-analyzes the year so the persisted category_id and all report rollups refresh immediately.
 */
@Service
public class RecategorizeService {
    private final BudgetReportStore repository;
    private final TransactionOverrideStore overrideStore;
    private final CategoryCatalog catalog;
    private final BudgetImportService importService;

    public RecategorizeService(
            BudgetReportStore repository,
            TransactionOverrideStore overrideStore,
            CategoryCatalog catalog,
            BudgetImportService importService
    ) {
        this.repository = repository;
        this.overrideStore = overrideStore;
        this.catalog = catalog;
        this.importService = importService;
    }

    public void recategorize(int year, long transactionId, String categoryId) {
        if (categoryId == null || categoryId.isBlank()) {
            throw new IllegalArgumentException("Category id is required");
        }
        // Validate the target category exists (throws IllegalArgumentException -> HTTP 400).
        catalog.definitionById(categoryId);
        var record = repository.findTransactionById(year, transactionId)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found: " + transactionId));
        var key = TransactionContentKey.of(record.postedDate(), record.account(), record.amount(), record.description());
        overrideStore.setOverride(key, categoryId, record.postedDate(), record.account(), record.amount(), record.description());
        importService.reanalyzeYear(year);
    }
}
