package com.budget.application.importing;

import com.budget.application.analysis.BudgetAnalysisService;
import com.budget.application.reporting.BudgetReportStore;
import com.budget.domain.report.BudgetAnalysisResult;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class BudgetImportService {
    private static final Pattern YEAR_FOLDER = Pattern.compile("20\\d{2}");

    private final ImportSettings settings;
    private final BankTransactionReader csvReader;
    private final BudgetAnalysisService analysisService;
    private final BudgetReportStore repository;
    private final ImportAuditService auditService;
    private final TransactionTemplate transactionTemplate;

    public BudgetImportService(
            ImportSettings settings,
            BankTransactionReader csvReader,
            BudgetAnalysisService analysisService,
            BudgetReportStore repository,
            ImportAuditService auditService,
            PlatformTransactionManager transactionManager
    ) {
        this.settings = settings;
        this.csvReader = csvReader;
        this.analysisService = analysisService;
        this.repository = repository;
        this.auditService = auditService;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    public ImportSummary importUpload(TransactionImportFile file) {
        var fileName = sanitize(file.originalFileName());
        try {
            validateUpload(file, fileName);
            var result = importUploadStream(file, fileName);
            auditService.record(result.year(), fileName, "ok", "uploaded and imported");
            return new ImportSummary("ok", List.of(result.year()), result.transactionCount(), result.income(), result.spend(), "CSV imported; raw file was not retained");
        } catch (Exception e) {
            auditService.record(null, fileName, "error", e.getMessage());
            throw new IllegalArgumentException("Import failed: " + e.getMessage(), e);
        }
    }

    public ImportSummary rebuildLocal(Integer requestedYear) {
        if (!settings.localRebuildEnabled()) {
            throw new IllegalArgumentException("Local CSV rebuild is disabled in this environment");
        }
        var files = findLocalCsvs(requestedYear);
        if (files.isEmpty()) {
            throw new IllegalArgumentException("No local CSV files found for rebuild");
        }
        var years = new ArrayList<Integer>();
        var transactions = 0;
        var income = BigDecimal.ZERO;
        var spend = BigDecimal.ZERO;
        for (var path : files) {
            try (var input = Files.newInputStream(path)) {
                var year = requestedYear != null ? requestedYear : inferYearFromPath(path);
                var result = importStreamInTransaction(input, path.getFileName().toString(), year);
                auditService.record(result.year(), path.toString(), "ok", "local rebuild");
                years.add(result.year());
                transactions += result.transactionCount();
                income = income.add(result.income());
                spend = spend.add(result.spend());
            } catch (Exception e) {
                auditService.record(requestedYear, path.toString(), "error", e.getMessage());
                throw new IllegalArgumentException("Rebuild failed for " + path + ": " + e.getMessage(), e);
            }
        }
        return new ImportSummary("ok", years, transactions, income, spend, "Local CSV rebuild completed");
    }

    private BudgetAnalysisResult importStream(InputStream input, String fileName, Integer requestedYear) throws IOException {
        var budgetInput = csvReader.read(input, fileName, requestedYear);
        var result = analysisService.analyze(budgetInput);
        repository.save(result);
        return result;
    }

    private BudgetAnalysisResult importUploadStream(TransactionImportFile file, String fileName) throws IOException {
        try (var input = file.openStream()) {
            return importStreamInTransaction(input, fileName, null);
        }
    }

    private BudgetAnalysisResult importStreamInTransaction(InputStream input, String fileName, Integer requestedYear) {
        return Objects.requireNonNull(transactionTemplate.execute(status -> {
            try {
                return importStream(input, fileName, requestedYear);
            } catch (IOException e) {
                throw new IllegalArgumentException("Cannot read CSV: " + e.getMessage(), e);
            }
        }));
    }

    private void validateUpload(TransactionImportFile file, String fileName) {
        if (file.size() <= 0) {
            throw new IllegalArgumentException("Upload is empty");
        }
        if (file.size() > settings.maxUploadBytes()) {
            throw new IllegalArgumentException("CSV is larger than allowed limit");
        }
        if (!fileName.toLowerCase().endsWith(".csv")) {
            throw new IllegalArgumentException("Only CSV files are accepted");
        }
        if (!isCsvCompatibleContentType(file.contentType())) {
            throw new IllegalArgumentException("Only CSV-compatible content types are accepted");
        }
    }

    private List<Path> findLocalCsvs(Integer requestedYear) {
        var root = settings.localRoot();
        if (requestedYear != null) {
            var folder = root.resolve(String.valueOf(requestedYear));
            if (!Files.isDirectory(folder)) {
                return List.of();
            }
            return latestCsv(folder).map(List::of).orElse(List.of());
        }
        try (var stream = Files.list(root)) {
            return stream
                    .filter(Files::isDirectory)
                    .filter(path -> YEAR_FOLDER.matcher(path.getFileName().toString()).matches())
                    .flatMap(path -> latestCsv(path).stream())
                    .sorted()
                    .toList();
        } catch (IOException e) {
            throw new IllegalArgumentException("Cannot scan local import folders: " + e.getMessage(), e);
        }
    }

    private java.util.Optional<Path> latestCsv(Path folder) {
        try (var files = Files.list(folder)) {
            return files
                    .filter(path -> path.getFileName().toString().toLowerCase().endsWith(".csv"))
                    .max(Comparator.comparingLong(this::lastModified));
        } catch (IOException e) {
            throw new IllegalArgumentException("Cannot scan folder " + folder + ": " + e.getMessage(), e);
        }
    }

    private long lastModified(Path path) {
        try {
            return Files.getLastModifiedTime(path).toMillis();
        } catch (IOException e) {
            return 0;
        }
    }

    private Integer inferYearFromPath(Path path) {
        for (var part : path) {
            var value = part.toString();
            if (YEAR_FOLDER.matcher(value).matches()) {
                return Integer.parseInt(value);
            }
        }
        return null;
    }

    private String sanitize(String fileName) {
        var value = fileName == null || fileName.isBlank() ? "upload.csv" : fileName;
        return Path.of(value).getFileName().toString();
    }

    private boolean isCsvCompatibleContentType(String contentType) {
        var value = contentType == null ? "" : contentType.split(";", 2)[0].trim().toLowerCase();
        return value.isBlank()
                || "text/csv".equals(value)
                || "text/plain".equals(value)
                || "application/csv".equals(value)
                || "application/vnd.ms-excel".equals(value)
                || "application/octet-stream".equals(value);
    }

}
