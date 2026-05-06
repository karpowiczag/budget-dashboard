package com.budget.application.importing;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.budget.application.analysis.BudgetAnalysisService;
import com.budget.application.reporting.BudgetReportStore;
import com.budget.domain.report.BudgetAnalysisResult;
import com.budget.domain.report.BudgetInput;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;

class BudgetImportServiceTest {
    @TempDir
    private Path tempDir;

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
                audit,
                noOpTransactionManager()
        );

        assertThatThrownBy(() -> service.rebuildLocal(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("disabled");

        verifyNoInteractions(csvReader, analysis, repository, audit);
    }

    @Test
    void failedPersistenceDoesNotRecordSuccessfulUploadAudit() throws Exception {
        var csvReader = mock(BankTransactionReader.class);
        var analysis = mock(BudgetAnalysisService.class);
        var repository = mock(BudgetReportStore.class);
        var audit = mock(ImportAuditService.class);
        var input = new BudgetInput(2026, "upload.csv", List.of());
        var result = new BudgetAnalysisResult(2026, "upload.csv", null, List.of(), 1, BigDecimal.TEN, BigDecimal.ONE);
        var service = new BudgetImportService(
                new ImportSettings(1024, Path.of("."), true),
                csvReader,
                analysis,
                repository,
                audit,
                noOpTransactionManager()
        );

        when(csvReader.read(any(), eq("upload.csv"), any())).thenReturn(input);
        when(analysis.analyze(input)).thenReturn(result);
        doThrow(new IllegalStateException("database rejected commit")).when(repository).save(result);

        assertThatThrownBy(() -> service.importUpload(new TransactionImportFile(
                "upload.csv",
                10,
                "text/csv",
                () -> new ByteArrayInputStream("x".getBytes(java.nio.charset.StandardCharsets.UTF_8))
        )))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Import failed");

        verify(audit, never()).record(eq(2026), eq("upload.csv"), eq("ok"), any());
        verify(audit).record(eq(null), eq("upload.csv"), eq("error"), any());
    }

    @Test
    void localRebuildAuditUsesFileNameWithoutLocalPath() throws Exception {
        var folder = tempDir.resolve("2026");
        Files.createDirectories(folder);
        var csv = folder.resolve("lista_operacji_260101_260331_fake.csv");
        Files.writeString(csv, "fake csv");

        var csvReader = mock(BankTransactionReader.class);
        var analysis = mock(BudgetAnalysisService.class);
        var repository = mock(BudgetReportStore.class);
        var audit = mock(ImportAuditService.class);
        var input = new BudgetInput(2026, csv.getFileName().toString(), List.of());
        var result = new BudgetAnalysisResult(2026, csv.getFileName().toString(), null, List.of(), 1, BigDecimal.TEN, BigDecimal.ONE);
        var service = new BudgetImportService(
                new ImportSettings(1024, tempDir, true),
                csvReader,
                analysis,
                repository,
                audit,
                noOpTransactionManager()
        );

        when(csvReader.read(any(), eq(csv.getFileName().toString()), eq(2026))).thenReturn(input);
        when(analysis.analyze(input)).thenReturn(result);

        service.rebuildLocal(null);

        verify(audit).record(2026, csv.getFileName().toString(), "ok", "local rebuild");
        verify(audit, never()).record(any(), eq(csv.toString()), any(), any());
    }

    @Test
    void failedLocalRebuildErrorUsesFileNameWithoutLocalPath() throws Exception {
        var folder = tempDir.resolve("2026");
        Files.createDirectories(folder);
        var csv = folder.resolve("broken.csv");
        Files.writeString(csv, "broken");

        var csvReader = mock(BankTransactionReader.class);
        var analysis = mock(BudgetAnalysisService.class);
        var repository = mock(BudgetReportStore.class);
        var audit = mock(ImportAuditService.class);
        var service = new BudgetImportService(
                new ImportSettings(1024, tempDir, true),
                csvReader,
                analysis,
                repository,
                audit,
                noOpTransactionManager()
        );

        when(csvReader.read(any(), eq("broken.csv"), eq(2026))).thenThrow(new IllegalArgumentException("bad csv"));

        assertThatThrownBy(() -> service.rebuildLocal(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Rebuild failed for broken.csv")
                .hasMessageNotContaining(csv.toString());

        verify(audit).record(2026, "broken.csv", "error", "bad csv");
        verify(audit, never()).record(any(), eq(csv.toString()), any(), any());
    }

    private PlatformTransactionManager noOpTransactionManager() {
        return new PlatformTransactionManager() {
            @Override
            public TransactionStatus getTransaction(TransactionDefinition definition) {
                return new SimpleTransactionStatus();
            }

            @Override
            public void commit(TransactionStatus status) {
            }

            @Override
            public void rollback(TransactionStatus status) {
            }
        };
    }
}
