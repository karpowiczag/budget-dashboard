package com.budget.infrastructure.persistence.jdbc;

import com.budget.application.reporting.BudgetReportStore;
import com.budget.application.reporting.ReportNotFoundException;
import com.budget.domain.report.BudgetAnalysisResult;
import com.budget.domain.importjob.ImportRun;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.springframework.data.jdbc.core.JdbcAggregateTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class BudgetReportRepository implements BudgetReportStore {
    private final ReportDataJdbcRepository reports;
    private final BudgetTransactionDataJdbcRepository transactions;
    private final ImportRunDataJdbcRepository importRuns;
    private final JdbcAggregateTemplate aggregateTemplate;
    private final ObjectMapper objectMapper;

    public BudgetReportRepository(
            ReportDataJdbcRepository reports,
            BudgetTransactionDataJdbcRepository transactions,
            ImportRunDataJdbcRepository importRuns,
            JdbcAggregateTemplate aggregateTemplate,
            ObjectMapper objectMapper
    ) {
        this.reports = reports;
        this.transactions = transactions;
        this.importRuns = importRuns;
        this.aggregateTemplate = aggregateTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public void save(BudgetAnalysisResult result) {
        transactions.deleteByReportYear(result.year());
        reports.deleteById(result.year());
        aggregateTemplate.insert(new ReportEntity(
                result.year(),
                toJson(result.payload()),
                result.fileName(),
                OffsetDateTime.now(),
                result.transactionCount(),
                result.income(),
                result.spend()
        ));

        var rows = result.transactions().stream()
                .map(tx -> new BudgetTransactionEntity(
                        null,
                        result.year(),
                        tx.lp(),
                        tx.date(),
                        tx.month(),
                        tx.merchant(),
                        tx.description(),
                        tx.correctedCategory(),
                        tx.subcategory(),
                        tx.amount(),
                        tx.analysisSpend(),
                        toJson(tx.toPayloadMap())
                ))
                .toList();
        transactions.saveAll(rows);
    }

    @Override
    public List<Map<String, Object>> findYears() {
        return reports.findAll().stream()
                .sorted(Comparator.comparingInt(ReportEntity::getReportYear))
                .map(report -> Map.<String, Object>of(
                        "year", report.getReportYear(),
                        "imported_at", report.getImportedAt().toString(),
                        "transactions", report.getTransactionCount(),
                        "income", report.getIncome(),
                        "spend", report.getSpend(),
                        "input_csv", report.getInputCsv()
                ))
                .toList();
    }

    @Override
    public Map<String, Object> findPayload(int year) {
        var json = reports.findById(year)
                .map(ReportEntity::getPayloadJson)
                .orElseThrow(() -> new ReportNotFoundException(year));
        try {
            return objectMapper.readValue(json, new TypeReference<>() {
            });
        } catch (JacksonException e) {
            throw new IllegalStateException("Stored report payload is invalid JSON", e);
        }
    }

    @Override
    public void recordImportRun(Integer year, String inputCsv, String status, String message) {
        importRuns.save(new ImportRunEntity(null, year, inputCsv, status, message, OffsetDateTime.now()));
    }

    @Override
    public List<ImportRun> findImportRuns() {
        return importRuns.findTop50ByOrderByIdDesc().stream()
                .map(row -> new ImportRun(
                        row.getId(),
                        row.getReportYear(),
                        row.getInputCsv(),
                        row.getStatus(),
                        row.getMessage(),
                        row.getCreatedAt()
                ))
                .toList();
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JacksonException e) {
            throw new IllegalStateException("Unable to serialize budget payload", e);
        }
    }
}
