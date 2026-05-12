package com.budget.application.importing;

import static org.assertj.core.api.Assertions.assertThat;
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
import com.budget.application.reporting.TransactionPage;
import com.budget.application.reporting.TransactionRecord;
import com.budget.application.reporting.YearSummary;
import com.budget.domain.report.BudgetAnalysisResult;
import com.budget.domain.report.BudgetInput;
import com.budget.domain.transaction.BankTransaction;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
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
        var transaction = bankTransaction("2026-01-10", "New", -100);
        var input = new BudgetInput(2026, "upload.csv", List.of(transaction));
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
        when(analysis.analyze(any())).thenReturn(result);

        service.rebuildLocal(null);

        verify(audit).record(2026, csv.getFileName().toString(), "ok", "local rebuild", 0);
        verify(audit, never()).record(any(), eq(csv.toString()), any(), any());
    }

    @Test
    void localRebuildMergesAllCsvFilesForYearWithoutDoubleCountingOverlap() throws Exception {
        var folder = tempDir.resolve("2026");
        Files.createDirectories(folder);
        var firstCsv = folder.resolve("first.csv");
        var secondCsv = folder.resolve("second.csv");
        Files.writeString(firstCsv, "first");
        Files.writeString(secondCsv, "second");
        var january = bankTransaction("2026-01-10", "Market", -100);
        var overlap = bankTransaction("2026-05-02", "Shared", -50);
        var may = bankTransaction("2026-05-07", "New", -75);

        var csvReader = mock(BankTransactionReader.class);
        var analysis = mock(BudgetAnalysisService.class);
        var repository = mock(BudgetReportStore.class);
        var audit = mock(ImportAuditService.class);
        var inputCaptor = ArgumentCaptor.forClass(BudgetInput.class);
        var service = new BudgetImportService(
                new ImportSettings(1024, tempDir, true),
                csvReader,
                analysis,
                repository,
                audit,
                noOpTransactionManager()
        );

        when(csvReader.read(any(), eq("first.csv"), eq(2026))).thenReturn(new BudgetInput(2026, "first.csv", List.of(january, overlap)));
        when(csvReader.read(any(), eq("second.csv"), eq(2026))).thenReturn(new BudgetInput(2026, "second.csv", List.of(overlap, may)));
        when(analysis.analyze(inputCaptor.capture())).thenAnswer(invocation -> {
            var input = invocation.getArgument(0, BudgetInput.class);
            return new BudgetAnalysisResult(input.year(), input.fileName(), null, List.of(), input.transactions().size(), BigDecimal.TEN, BigDecimal.ONE);
        });

        var summary = service.rebuildLocal(2026);

        assertThat(summary.transactions()).isEqualTo(3);
        assertThat(summary.duplicatesRemoved()).isEqualTo(1);
        assertThat(inputCaptor.getValue().transactions()).containsExactly(january, overlap, may);
        verify(audit).record(2026, "2026 (2 CSV files)", "ok", "local rebuild; duplicates removed: 1", 1);
    }

    @Test
    void localRebuildDeduplicatesSettledAndUnsettledCardTransactionCopies() throws Exception {
        var folder = tempDir.resolve("2026");
        Files.createDirectories(folder);
        var firstCsv = folder.resolve("first.csv");
        var secondCsv = folder.resolve("second.csv");
        Files.writeString(firstCsv, "first");
        Files.writeString(secondCsv, "second");
        var unsettled = bankTransaction(
                "2026-05-05",
                "Merchant ZAKUP PRZY UŻYCIU KARTY - INTERNET transakcja nierozliczona",
                -1809
        );
        var settled = bankTransaction(
                "2026-05-05",
                "Merchant ZAKUP PRZY UŻYCIU KARTY - INTERNET",
                -1809
        );
        var separatePayment = bankTransaction(
                "2026-05-05",
                "Merchant ZAKUP PRZY UŻYCIU KARTY - INTERNET",
                -8.75
        );

        var csvReader = mock(BankTransactionReader.class);
        var analysis = mock(BudgetAnalysisService.class);
        var repository = mock(BudgetReportStore.class);
        var audit = mock(ImportAuditService.class);
        var inputCaptor = ArgumentCaptor.forClass(BudgetInput.class);
        var service = new BudgetImportService(
                new ImportSettings(1024, tempDir, true),
                csvReader,
                analysis,
                repository,
                audit,
                noOpTransactionManager()
        );

        when(csvReader.read(any(), eq("first.csv"), eq(2026))).thenReturn(new BudgetInput(2026, "first.csv", List.of(unsettled)));
        when(csvReader.read(any(), eq("second.csv"), eq(2026))).thenReturn(new BudgetInput(2026, "second.csv", List.of(settled, separatePayment)));
        when(analysis.analyze(inputCaptor.capture())).thenAnswer(invocation -> {
            var input = invocation.getArgument(0, BudgetInput.class);
            return new BudgetAnalysisResult(input.year(), input.fileName(), null, List.of(), input.transactions().size(), BigDecimal.TEN, BigDecimal.ONE);
        });

        var summary = service.rebuildLocal(2026);

        assertThat(summary.transactions()).isEqualTo(2);
        assertThat(summary.duplicatesRemoved()).isEqualTo(1);
        assertThat(inputCaptor.getValue().transactions()).containsExactly(settled, separatePayment);
    }

    @Test
    void uploadMergesWithExistingYearInsteadOfReplacingItWithPartialCsv() throws Exception {
        var existing = bankTransaction("2026-01-10", "Existing", -100);
        var newTransaction = bankTransaction("2026-05-07", "New", -75);
        var csvReader = mock(BankTransactionReader.class);
        var analysis = mock(BudgetAnalysisService.class);
        var repository = mock(BudgetReportStore.class);
        var audit = mock(ImportAuditService.class);
        var inputCaptor = ArgumentCaptor.forClass(BudgetInput.class);
        var service = new BudgetImportService(
                new ImportSettings(2048, tempDir, true),
                csvReader,
                analysis,
                repository,
                audit,
                noOpTransactionManager()
        );

        when(csvReader.read(any(), eq("partial.csv"), any())).thenReturn(new BudgetInput(2026, "partial.csv", List.of(newTransaction)));
        when(repository.findYears()).thenReturn(List.of(new YearSummary(2026, OffsetDateTime.now(), 1, BigDecimal.ZERO, BigDecimal.ZERO, "old.csv")));
        when(repository.findTransactions(any())).thenReturn(new TransactionPage(List.of(transactionRecord(existing)), 0, 200, 1, 1, "postedDate,asc"));
        when(analysis.analyze(inputCaptor.capture())).thenAnswer(invocation -> {
            var input = invocation.getArgument(0, BudgetInput.class);
            return new BudgetAnalysisResult(input.year(), input.fileName(), null, List.of(), input.transactions().size(), BigDecimal.TEN, BigDecimal.ONE);
        });

        var summary = service.importUpload(new TransactionImportFile(
                "partial.csv",
                12,
                "text/csv",
                () -> new ByteArrayInputStream("csv".getBytes(java.nio.charset.StandardCharsets.UTF_8))
        ));

        assertThat(summary.transactions()).isEqualTo(1);
        assertThat(inputCaptor.getValue().transactions()).containsExactly(existing, newTransaction);
    }

    @Test
    void uploadImportsOnlyRowsAfterLatestExistingTransaction() throws Exception {
        var oldExisting = bankTransaction("2026-01-10", "Old existing", -100);
        var latestExisting = bankTransaction("2026-05-07", "Latest existing", -50);
        var oldFromUpload = bankTransaction("2026-01-10", "Old existing", -100);
        var latestDuplicateFromUpload = bankTransaction("2026-05-07", "Latest existing", -50);
        var newTransaction = bankTransaction("2026-05-08", "New", -75);
        var csvReader = mock(BankTransactionReader.class);
        var analysis = mock(BudgetAnalysisService.class);
        var repository = mock(BudgetReportStore.class);
        var audit = mock(ImportAuditService.class);
        var inputCaptor = ArgumentCaptor.forClass(BudgetInput.class);
        var service = new BudgetImportService(
                new ImportSettings(2048, tempDir, true),
                csvReader,
                analysis,
                repository,
                audit,
                noOpTransactionManager()
        );

        when(csvReader.read(any(), eq("incremental.csv"), any())).thenReturn(new BudgetInput(
                2026,
                "incremental.csv",
                List.of(oldFromUpload, latestDuplicateFromUpload, newTransaction)
        ));
        when(repository.findYears()).thenReturn(List.of(new YearSummary(2026, OffsetDateTime.now(), 2, BigDecimal.valueOf(10_000), BigDecimal.valueOf(150), "old.csv")));
        when(repository.findTransactions(any())).thenReturn(new TransactionPage(List.of(
                transactionRecord(oldExisting),
                transactionRecord(latestExisting)
        ), 0, 200, 2, 1, "postedDate,asc"));
        when(analysis.analyze(inputCaptor.capture())).thenAnswer(invocation -> {
            var input = invocation.getArgument(0, BudgetInput.class);
            return new BudgetAnalysisResult(input.year(), input.fileName(), null, List.of(), input.transactions().size(), BigDecimal.TEN, BigDecimal.ONE);
        });

        var summary = service.importUpload(new TransactionImportFile(
                "incremental.csv",
                12,
                "text/csv",
                () -> new ByteArrayInputStream("csv".getBytes(java.nio.charset.StandardCharsets.UTF_8))
        ));

        assertThat(summary.transactions()).isEqualTo(1);
        assertThat(summary.duplicatesRemoved()).isEqualTo(2);
        assertThat(inputCaptor.getValue().transactions()).containsExactly(oldExisting, latestExisting, newTransaction);
        verify(audit).record(2026, "incremental.csv", "ok", "uploaded incrementally; new transactions: 1; existing/duplicate rows skipped: 2", 2);
    }

    @Test
    void uploadAllowsNewDifferentTransactionsOnLatestExistingDay() throws Exception {
        var latestExisting = bankTransaction("2026-05-07", "Restaurant", -23.60);
        var latestDuplicateFromUpload = bankTransaction("2026-05-07", "Restaurant", -23.60);
        var sameDayNewTransaction = bankTransaction("2026-05-07", "Pharmacy", -75);
        var csvReader = mock(BankTransactionReader.class);
        var analysis = mock(BudgetAnalysisService.class);
        var repository = mock(BudgetReportStore.class);
        var audit = mock(ImportAuditService.class);
        var inputCaptor = ArgumentCaptor.forClass(BudgetInput.class);
        var service = new BudgetImportService(
                new ImportSettings(2048, tempDir, true),
                csvReader,
                analysis,
                repository,
                audit,
                noOpTransactionManager()
        );

        when(csvReader.read(any(), eq("same-day.csv"), any())).thenReturn(new BudgetInput(
                2026,
                "same-day.csv",
                List.of(latestDuplicateFromUpload, sameDayNewTransaction)
        ));
        when(repository.findYears()).thenReturn(List.of(new YearSummary(2026, OffsetDateTime.now(), 1, BigDecimal.ZERO, BigDecimal.ZERO, "old.csv")));
        when(repository.findTransactions(any())).thenReturn(new TransactionPage(List.of(transactionRecord(latestExisting)), 0, 200, 1, 1, "postedDate,asc"));
        when(analysis.analyze(inputCaptor.capture())).thenAnswer(invocation -> {
            var input = invocation.getArgument(0, BudgetInput.class);
            return new BudgetAnalysisResult(input.year(), input.fileName(), null, List.of(), input.transactions().size(), BigDecimal.TEN, BigDecimal.ONE);
        });

        var summary = service.importUpload(new TransactionImportFile(
                "same-day.csv",
                12,
                "text/csv",
                () -> new ByteArrayInputStream("csv".getBytes(java.nio.charset.StandardCharsets.UTF_8))
        ));

        assertThat(summary.transactions()).isEqualTo(1);
        assertThat(summary.duplicatesRemoved()).isEqualTo(1);
        assertThat(inputCaptor.getValue().transactions()).containsExactly(sameDayNewTransaction, latestExisting);
    }

    @Test
    void uploadWithNoRowsAfterLatestExistingTransactionDoesNotRebuildReport() throws Exception {
        var existing = bankTransaction("2026-05-07", "Existing", -100);
        var csvReader = mock(BankTransactionReader.class);
        var analysis = mock(BudgetAnalysisService.class);
        var repository = mock(BudgetReportStore.class);
        var audit = mock(ImportAuditService.class);
        var service = new BudgetImportService(
                new ImportSettings(2048, tempDir, true),
                csvReader,
                analysis,
                repository,
                audit,
                noOpTransactionManager()
        );

        when(csvReader.read(any(), eq("old.csv"), any())).thenReturn(new BudgetInput(2026, "old.csv", List.of(existing)));
        when(repository.findYears()).thenReturn(List.of(new YearSummary(2026, OffsetDateTime.now(), 1, BigDecimal.valueOf(18_000), BigDecimal.valueOf(100), "old.csv")));
        when(repository.findTransactions(any())).thenReturn(new TransactionPage(List.of(transactionRecord(existing)), 0, 200, 1, 1, "postedDate,asc"));

        var summary = service.importUpload(new TransactionImportFile(
                "old.csv",
                12,
                "text/csv",
                () -> new ByteArrayInputStream("csv".getBytes(java.nio.charset.StandardCharsets.UTF_8))
        ));

        assertThat(summary.transactions()).isZero();
        assertThat(summary.duplicatesRemoved()).isEqualTo(1);
        assertThat(summary.income()).isEqualByComparingTo(BigDecimal.valueOf(18_000));
        assertThat(summary.spend()).isEqualByComparingTo(BigDecimal.valueOf(100));
        assertThat(summary.message()).isEqualTo("CSV checked; no new transactions after latest imported transaction");
        verifyNoInteractions(analysis);
        verify(audit).record(2026, "old.csv", "ok", "uploaded; no new transactions; existing/duplicate rows skipped: 1", 1);
    }

    @Test
    void uploadPreservesRepeatedSameFingerprintTransactionsFromOneCsv() throws Exception {
        var firstCharge = bankTransaction("2026-05-07", "RESTAURACJA CGP W VOLV", -23.60);
        var secondCharge = bankTransaction("2026-05-07", "RESTAURACJA CGP W VOLV", -23.60);
        var csvReader = mock(BankTransactionReader.class);
        var analysis = mock(BudgetAnalysisService.class);
        var repository = mock(BudgetReportStore.class);
        var audit = mock(ImportAuditService.class);
        var inputCaptor = ArgumentCaptor.forClass(BudgetInput.class);
        var service = new BudgetImportService(
                new ImportSettings(2048, tempDir, true),
                csvReader,
                analysis,
                repository,
                audit,
                noOpTransactionManager()
        );

        when(csvReader.read(any(), eq("partial.csv"), any())).thenReturn(new BudgetInput(2026, "partial.csv", List.of(firstCharge, secondCharge)));
        when(analysis.analyze(inputCaptor.capture())).thenAnswer(invocation -> {
            var input = invocation.getArgument(0, BudgetInput.class);
            return new BudgetAnalysisResult(input.year(), input.fileName(), null, List.of(), input.transactions().size(), BigDecimal.TEN, BigDecimal.ONE);
        });

        var summary = service.importUpload(new TransactionImportFile(
                "partial.csv",
                12,
                "text/csv",
                () -> new ByteArrayInputStream("csv".getBytes(java.nio.charset.StandardCharsets.UTF_8))
        ));

        assertThat(summary.transactions()).isEqualTo(2);
        assertThat(summary.duplicatesRemoved()).isZero();
        assertThat(inputCaptor.getValue().transactions()).containsExactly(firstCharge, secondCharge);
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

    private BankTransaction bankTransaction(String date, String description, double amount) {
        return new BankTransaction(LocalDate.parse(date), "account", description, "bank", BigDecimal.valueOf(amount).setScale(2));
    }

    private TransactionRecord transactionRecord(BankTransaction transaction) {
        return new TransactionRecord(
                1,
                1,
                transaction.date(),
                transaction.date().toString().substring(0, 7),
                transaction.description(),
                transaction.description(),
                transaction.account(),
                transaction.bankCategory(),
                "Kategoria",
                "Obszar",
                "Grupa",
                "Podkategoria",
                "Koszyk",
                "Stałe",
                "Wydatek",
                transaction.amount(),
                BigDecimal.ZERO,
                transaction.amount().abs(),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                "Wysoka",
                "",
                ""
        );
    }
}
