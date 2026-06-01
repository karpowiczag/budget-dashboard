package com.budget.application.importing;

import com.budget.application.analysis.BudgetAnalysisService;
import com.budget.application.reporting.TransactionQuery;
import com.budget.application.reporting.BudgetReportStore;
import com.budget.application.reporting.YearSummary;
import com.budget.domain.report.BudgetInput;
import com.budget.domain.report.BudgetAnalysisResult;
import com.budget.domain.transaction.BankTransaction;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class BudgetImportService {
    private static final Pattern YEAR_FOLDER = Pattern.compile("20\\d{2}");
    private static final Pattern UNSETTLED_CARD_MARKER = Pattern.compile(
            "\\btransakcja\\s+nierozliczona\\b",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
    );

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
            auditService.record(result.year(), fileName, "ok", uploadImportMessage(result.newTransactions(), result.duplicatesRemoved()), result.duplicatesRemoved());
            return new ImportSummary(
                    "ok",
                    List.of(result.year()),
                    result.newTransactions(),
                    result.duplicatesRemoved(),
                    result.income(),
                    result.spend(),
                    result.message()
            );
        } catch (Exception e) {
            auditService.record(null, fileName, "error", e.getMessage());
            throw new IllegalArgumentException("Import failed: " + e.getMessage(), e);
        }
    }

    public ImportSummary rebuildLocal(Integer requestedYear) {
        if (!settings.localRebuildEnabled()) {
            throw new IllegalArgumentException("Local CSV rebuild is disabled in this environment");
        }
        var filesByYear = findLocalCsvsByYear(requestedYear);
        if (filesByYear.isEmpty()) {
            throw new IllegalArgumentException("No local CSV files found for rebuild");
        }
        var years = new ArrayList<Integer>();
        var transactions = 0;
        var duplicatesRemoved = 0;
        var income = BigDecimal.ZERO;
        var spend = BigDecimal.ZERO;
        for (var entry : filesByYear.entrySet()) {
            var year = entry.getKey();
            var files = entry.getValue();
            var auditName = localRebuildAuditName(year, files);
            try {
                var result = importLocalFilesInTransaction(year, files);
                auditService.record(result.analysis().year(), auditName, "ok", importMessage("local rebuild", result.duplicatesRemoved()), result.duplicatesRemoved());
                years.add(result.analysis().year());
                transactions += result.analysis().transactionCount();
                income = income.add(result.analysis().income());
                spend = spend.add(result.analysis().spend());
                duplicatesRemoved += result.duplicatesRemoved();
            } catch (Exception e) {
                auditService.record(year, auditName, "error", e.getMessage());
                throw new IllegalArgumentException("Rebuild failed for " + auditName + ": " + e.getMessage(), e);
            }
        }
        return new ImportSummary("ok", years, transactions, duplicatesRemoved, income, spend, "Local CSV rebuild completed");
    }

    private UploadImportResult importUploadStream(TransactionImportFile file, String fileName) throws IOException {
        try (var input = file.openStream()) {
            var upload = csvReader.read(input, fileName, null);
            var prepared = prepareIncrementalUpload(upload);
            if (prepared.newTransactions() == 0) {
                var existing = existingYearSummary(upload.year());
                return new UploadImportResult(
                        upload.year(),
                        0,
                        prepared.duplicatesRemoved(),
                        existing == null ? BigDecimal.ZERO : existing.income(),
                        existing == null ? BigDecimal.ZERO : existing.spend(),
                        "CSV checked; no new transactions"
                );
            }
            var analysis = importBudgetInputInTransaction(prepared.input());
            return new UploadImportResult(
                    analysis.year(),
                    prepared.newTransactions(),
                    prepared.duplicatesRemoved(),
                    analysis.income(),
                    analysis.spend(),
                    "CSV imported; raw file was not retained"
            );
        }
    }

    private ImportResult importLocalFilesInTransaction(int year, List<Path> paths) {
        return Objects.requireNonNull(transactionTemplate.execute(status -> {
            try {
                var sources = new ArrayList<List<BankTransaction>>();
                for (var path : paths) {
                    try (var input = Files.newInputStream(path)) {
                        sources.add(csvReader.read(input, path.getFileName().toString(), year).transactions());
                    }
                }
                var merged = mergeTransactionSources(sources);
                var input = new BudgetInput(year, "local-rebuild-" + year + "-" + paths.size() + "-files.csv", merged.transactions());
                return new ImportResult(importBudgetInput(input), merged.duplicatesRemoved());
            } catch (IOException e) {
                throw new IllegalArgumentException("Cannot read CSV: " + e.getMessage(), e);
            }
        }));
    }

    private BudgetAnalysisResult importBudgetInputInTransaction(BudgetInput input) {
        return Objects.requireNonNull(transactionTemplate.execute(status -> importBudgetInput(input)));
    }

    private BudgetAnalysisResult importBudgetInput(BudgetInput input) {
        var result = analysisService.analyze(input);
        repository.save(result);
        return result;
    }

    private PreparedInput prepareIncrementalUpload(BudgetInput upload) {
        var existing = existingRawTransactions(upload.year());
        if (existing.isEmpty()) {
            var merged = mergeTransactionSources(List.of(upload.transactions()));
            return new PreparedInput(
                    new BudgetInput(upload.year(), upload.fileName(), merged.transactions()),
                    merged.duplicatesRemoved(),
                    merged.transactions().size()
            );
        }

        var existingCounts = fingerprintCounts(existing);
        var seenUploadCounts = new LinkedHashMap<TransactionFingerprint, Integer>();
        var incremental = new ArrayList<BankTransaction>();
        var skippedExisting = 0;
        for (var transaction : upload.transactions()) {
            var fingerprint = fingerprint(transaction);
            var seen = seenUploadCounts.merge(fingerprint, 1, Integer::sum);
            if (seen <= existingCounts.getOrDefault(fingerprint, 0)) {
                skippedExisting++;
            } else {
                incremental.add(transaction);
            }
        }

        var newRows = mergeTransactionSources(List.of(incremental));
        if (newRows.transactions().isEmpty()) {
            return new PreparedInput(
                    new BudgetInput(upload.year(), upload.fileName(), existing),
                    skippedExisting + newRows.duplicatesRemoved(),
                    0
            );
        }

        var merged = new ArrayList<BankTransaction>();
        merged.addAll(existing);
        merged.addAll(newRows.transactions());
        merged.sort(Comparator.comparing(BankTransaction::date)
                .thenComparing(BankTransaction::account)
                .thenComparing(BankTransaction::description)
                .thenComparing(BankTransaction::amount));
        return new PreparedInput(
                new BudgetInput(upload.year(), upload.fileName(), merged),
                skippedExisting + newRows.duplicatesRemoved(),
                newRows.transactions().size()
        );
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

    private Map<Integer, List<Path>> findLocalCsvsByYear(Integer requestedYear) {
        var root = settings.localRoot();
        if (requestedYear != null) {
            var folder = root.resolve(String.valueOf(requestedYear));
            if (!Files.isDirectory(folder)) {
                return Map.of();
            }
            var files = csvFiles(folder);
            return files.isEmpty() ? Map.of() : Map.of(requestedYear, files);
        }
        try (var stream = Files.list(root)) {
            var result = new java.util.TreeMap<Integer, List<Path>>();
            stream
                    .filter(Files::isDirectory)
                    .filter(path -> YEAR_FOLDER.matcher(path.getFileName().toString()).matches())
                    .forEach(path -> {
                        var files = csvFiles(path);
                        if (!files.isEmpty()) {
                            result.put(Integer.parseInt(path.getFileName().toString()), files);
                        }
                    });
            return result;
        } catch (IOException e) {
            throw new IllegalArgumentException("Cannot scan local import folders: " + e.getMessage(), e);
        }
    }

    private List<Path> csvFiles(Path folder) {
        try (var files = Files.list(folder)) {
            return files
                    .filter(path -> path.getFileName().toString().toLowerCase().endsWith(".csv"))
                    .sorted(Comparator.comparing(Path::toString))
                    .toList();
        } catch (IOException e) {
            throw new IllegalArgumentException("Cannot scan folder " + folder + ": " + e.getMessage(), e);
        }
    }

    private MergedTransactions mergeTransactionSources(List<List<BankTransaction>> sources) {
        var grouped = new LinkedHashMap<TransactionFingerprint, List<List<BankTransaction>>>();
        var sourceCount = 0;
        for (var source : sources) {
            var sourceGroups = new LinkedHashMap<TransactionFingerprint, List<BankTransaction>>();
            for (var transaction : source) {
                sourceCount++;
                sourceGroups.computeIfAbsent(fingerprint(transaction), ignored -> new ArrayList<>()).add(transaction);
            }
            sourceGroups.forEach((fingerprint, transactions) ->
                    grouped.computeIfAbsent(fingerprint, ignored -> new ArrayList<>()).add(transactions)
            );
        }
        var merged = new ArrayList<BankTransaction>();
        grouped.values().forEach(sourceGroups -> merged.addAll(bestTransactions(sourceGroups)));
        merged.sort(Comparator.comparing(BankTransaction::date)
                .thenComparing(BankTransaction::account)
                .thenComparing(BankTransaction::description)
                .thenComparing(BankTransaction::amount));
        return new MergedTransactions(merged, sourceCount - merged.size());
    }

    private List<BankTransaction> bestTransactions(List<List<BankTransaction>> sourceGroups) {
        var keepCount = sourceGroups.stream().mapToInt(List::size).max().orElse(0);
        var candidates = sourceGroups.stream()
                .flatMap(List::stream)
                .sorted(this::compareTransactionQuality)
                .toList();
        return candidates.stream().limit(keepCount).toList();
    }

    private int compareTransactionQuality(BankTransaction left, BankTransaction right) {
        return Integer.compare(transactionQuality(right), transactionQuality(left));
    }

    private int transactionQuality(BankTransaction transaction) {
        var score = 0;
        if (!isUnsettled(transaction)) {
            score += 2;
        }
        if (!blank(transaction.bankCategory())) {
            score += 1;
        }
        return score;
    }

    private TransactionFingerprint fingerprint(BankTransaction transaction) {
        return new TransactionFingerprint(
                transaction.date(),
                normalizeKey(transaction.account()),
                money(transaction.amount()),
                normalizeDescriptionKey(transaction.description())
        );
    }

    private Map<TransactionFingerprint, Integer> fingerprintCounts(List<BankTransaction> transactions) {
        var counts = new LinkedHashMap<TransactionFingerprint, Integer>();
        for (var transaction : transactions) {
            counts.merge(fingerprint(transaction), 1, Integer::sum);
        }
        return counts;
    }

    private boolean isUnsettled(BankTransaction transaction) {
        return transaction.description() != null && UNSETTLED_CARD_MARKER.matcher(transaction.description()).find();
    }

    private String normalizeDescriptionKey(String value) {
        return normalizeKey(UNSETTLED_CARD_MARKER.matcher(value == null ? "" : value).replaceAll(""));
    }

    private String normalizeKey(String value) {
        return (value == null ? "" : value)
                .replaceAll("\\s+", " ")
                .trim()
                .toUpperCase(Locale.ROOT);
    }

    private BigDecimal money(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP);
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private List<BankTransaction> existingRawTransactions(int year) {
        var summaries = repository.findYears();
        if (summaries == null || summaries.stream().noneMatch(row -> row.year() == year)) {
            return List.of();
        }
        var page = 0;
        var transactions = new ArrayList<BankTransaction>();
        TransactionPageLoop:
        while (true) {
            var result = repository.findTransactions(new TransactionQuery(
                    year, page, 200, "postedDate,asc",
                    null, null, null, null, null, null, null, null, null
            ));
            for (var row : result.items()) {
                transactions.add(new BankTransaction(row.postedDate(), row.account(), row.description(), row.bankCategory(), row.amount()));
            }
            page++;
            if (page >= result.totalPages()) {
                break TransactionPageLoop;
            }
        }
        return transactions;
    }

    private YearSummary existingYearSummary(int year) {
        var summaries = repository.findYears();
        if (summaries == null) {
            return null;
        }
        return summaries.stream()
                .filter(row -> row.year() == year)
                .findFirst()
                .orElse(null);
    }

    private String sanitize(String fileName) {
        var value = fileName == null || fileName.isBlank() ? "upload.csv" : fileName;
        return Path.of(value).getFileName().toString();
    }

    private String displayName(Path path) {
        return sanitize(path == null ? null : path.getFileName().toString());
    }

    private String localRebuildAuditName(int year, List<Path> files) {
        return files.size() == 1 ? displayName(files.getFirst()) : year + " (" + files.size() + " CSV files)";
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

    private String importMessage(String base, int duplicatesRemoved) {
        return duplicatesRemoved > 0 ? base + "; duplicates removed: " + duplicatesRemoved : base;
    }

    private String uploadImportMessage(int newTransactions, int skippedRows) {
        var base = newTransactions == 0
                ? "uploaded; no new transactions"
                : "uploaded incrementally; new transactions: " + newTransactions;
        return skippedRows > 0 ? base + "; existing/duplicate rows skipped: " + skippedRows : base;
    }

    private record ImportResult(BudgetAnalysisResult analysis, int duplicatesRemoved) {
    }

    private record UploadImportResult(
            int year,
            int newTransactions,
            int duplicatesRemoved,
            BigDecimal income,
            BigDecimal spend,
            String message
    ) {
    }

    private record PreparedInput(BudgetInput input, int duplicatesRemoved, int newTransactions) {
    }

    private record MergedTransactions(List<BankTransaction> transactions, int duplicatesRemoved) {
    }

    private record TransactionFingerprint(
            java.time.LocalDate date,
            String account,
            BigDecimal amount,
            String description
    ) {
    }

}
