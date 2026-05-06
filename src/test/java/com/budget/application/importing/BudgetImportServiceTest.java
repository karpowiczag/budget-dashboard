package com.budget.application.importing;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import com.budget.application.analysis.BudgetAnalysisService;
import com.budget.application.reporting.BudgetReportStore;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class BudgetImportServiceTest {
    @Test
    void disabledLocalRebuildFailsBeforeScanningOrPersisting() {
        var csvReader = mock(BankTransactionReader.class);
        var analysis = mock(BudgetAnalysisService.class);
        var repository = mock(BudgetReportStore.class);
        var audit = mock(ImportAuditService.class);
        var service = new BudgetImportService(
                new ImportSettings(1024, Path.of("missing-root"), false),
                csvReader,
                analysis,
                repository,
                audit
        );

        assertThatThrownBy(() -> service.rebuildLocal(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("disabled");

        verifyNoInteractions(csvReader, analysis, repository, audit);
    }
}
