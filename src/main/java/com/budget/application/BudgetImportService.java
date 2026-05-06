package com.budget.application;

import com.budget.config.BudgetProperties;
import com.budget.domain.BudgetAnalysisResult;
import com.budget.domain.BudgetInput;
import com.budget.infrastructure.BankCsvReader;
import com.budget.infrastructure.BudgetReportRepository;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class BudgetImportService {
    private static final Pattern YEAR_FOLDER = Pattern.compile("20\\d{2}");

    private final BudgetProperties properties;
    private final BankCsvReader csvReader;
    private final BudgetAnalysisService analysisService;
    private final BudgetReportRepository repository;

    public BudgetImportService(BudgetProperties properties, BankCsvReader csvReader, BudgetAnalysisService analysisService, BudgetReportRepository repository) {
        this.properties = properties;
        this.csvReader = csvReader;
        this.analysisService = analysisService;
        this.repository = repository;
    }

    @Transactional
    public ImportSummary importUpload(MultipartFile file) {
        var fileName = sanitize(file.getOriginalFilename());
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Upload is empty");
        }
        if (file.getSize() > properties.upload().maxBytes()) {
            throw new IllegalArgumentException("CSV is larger than allowed limit");
        }
        if (!fileName.toLowerCase().endsWith(".csv")) {
            throw new IllegalArgumentException("Only CSV files are accepted");
        }
        try (var input = file.getInputStream()) {
            var result = importStream(input, fileName, null);
            repository.recordImportRun(result.year(), fileName, "ok", "uploaded and imported");
            return new ImportSummary("ok", List.of(result.year()), result.transactionCount(), result.income(), result.spend(), "CSV imported; raw file was not retained");
        } catch (Exception e) {
            repository.recordImportRun(null, fileName, "error", e.getMessage());
            throw new IllegalArgumentException("Import failed: " + e.getMessage(), e);
        }
    }

    @Transactional
    public ImportSummary rebuildLocal(Integer requestedYear) {
        var files = findLocalCsvs(requestedYear);
        if (files.isEmpty()) {
            throw new IllegalArgumentException("No local CSV files found for rebuild");
        }
        var years = new ArrayList<Integer>();
        int transactions = 0;
        double income = 0;
        double spend = 0;
        for (var path : files) {
            try (var input = Files.newInputStream(path)) {
                var year = requestedYear != null ? requestedYear : inferYearFromPath(path);
                var result = importStream(input, path.getFileName().toString(), year);
                repository.recordImportRun(result.year(), path.toString(), "ok", "local rebuild");
                years.add(result.year());
                transactions += result.transactionCount();
                income += result.income();
                spend += result.spend();
            } catch (Exception e) {
                repository.recordImportRun(requestedYear, path.toString(), "error", e.getMessage());
                throw new IllegalArgumentException("Rebuild failed for " + path + ": " + e.getMessage(), e);
            }
        }
        return new ImportSummary("ok", years, transactions, round2(income), round2(spend), "Local CSV rebuild completed");
    }

    private BudgetAnalysisResult importStream(InputStream input, String fileName, Integer requestedYear) throws IOException {
        var budgetInput = csvReader.read(input, fileName, requestedYear);
        var result = analysisService.analyze(budgetInput);
        repository.save(result);
        return result;
    }

    private List<Path> findLocalCsvs(Integer requestedYear) {
        var root = Path.of(properties.localImport().root());
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

    private double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
