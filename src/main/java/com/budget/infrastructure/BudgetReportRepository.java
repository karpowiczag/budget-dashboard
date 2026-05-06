package com.budget.infrastructure;

import com.budget.domain.BudgetAnalysisResult;
import com.budget.domain.ImportRun;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class BudgetReportRepository {
    private final JdbcClient jdbc;
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public BudgetReportRepository(JdbcClient jdbc, JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbc = jdbc;
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    public void save(BudgetAnalysisResult result) {
        var payloadJson = toJson(result.payload());
        jdbc.sql("DELETE FROM reports WHERE report_year = :year")
                .param("year", result.year())
                .update();
        jdbc.sql("""
                INSERT INTO reports (report_year, payload_json, input_csv, imported_at, transaction_count, income, spend)
                VALUES (:year, :payload, :input, :importedAt, :transactions, :income, :spend)
                """)
                .param("year", result.year())
                .param("payload", payloadJson)
                .param("input", result.fileName())
                .param("importedAt", OffsetDateTime.now())
                .param("transactions", result.transactionCount())
                .param("income", result.income())
                .param("spend", result.spend())
                .update();

        jdbc.sql("DELETE FROM budget_transactions WHERE report_year = :year")
                .param("year", result.year())
                .update();
        jdbcTemplate.batchUpdate("""
                INSERT INTO budget_transactions
                    (report_year, lp, posted_date, month_key, merchant, description, corrected_category, subcategory, amount, analysis_spend, payload_json)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, result.transactions(), 250, (ps, tx) -> {
            ps.setInt(1, result.year());
            ps.setInt(2, tx.lp());
            ps.setObject(3, tx.date());
            ps.setString(4, tx.month());
            ps.setString(5, tx.merchant());
            ps.setString(6, tx.description());
            ps.setString(7, tx.correctedCategory());
            ps.setString(8, tx.subcategory());
            ps.setDouble(9, tx.amount());
            ps.setDouble(10, tx.analysisSpend());
            ps.setString(11, toJson(tx.toPayloadMap()));
        });
    }

    public List<Map<String, Object>> findYears() {
        return jdbc.sql("""
                        SELECT report_year AS report_year, imported_at, transaction_count, income, spend, input_csv
                        FROM reports
                        ORDER BY report_year
                        """)
                .query((rs, rowNum) -> Map.<String, Object>of(
                        "year", rs.getInt("report_year"),
                        "imported_at", rs.getObject("imported_at", OffsetDateTime.class).toString(),
                        "transactions", rs.getInt("transaction_count"),
                        "income", rs.getDouble("income"),
                        "spend", rs.getDouble("spend"),
                        "input_csv", rs.getString("input_csv")
                ))
                .list();
    }

    public Map<String, Object> findPayload(int year) {
        var json = jdbc.sql("SELECT payload_json FROM reports WHERE report_year = :year")
                .param("year", year)
                .query(String.class)
                .optional()
                .orElseThrow(() -> new ReportNotFoundException(year));
        try {
            return objectMapper.readValue(json, new TypeReference<>() {
            });
        } catch (JacksonException e) {
            throw new IllegalStateException("Stored report payload is invalid JSON", e);
        }
    }

    public void recordImportRun(Integer year, String inputCsv, String status, String message) {
        jdbc.sql("""
                INSERT INTO import_runs (report_year, input_csv, status, message, created_at)
                VALUES (:year, :input, :status, :message, :createdAt)
                """)
                .param("year", year)
                .param("input", inputCsv)
                .param("status", status)
                .param("message", message)
                .param("createdAt", OffsetDateTime.now())
                .update();
    }

    public List<ImportRun> findImportRuns() {
        return jdbc.sql("""
                        SELECT id, report_year, input_csv, status, message, created_at
                        FROM import_runs
                        ORDER BY id DESC
                        LIMIT 50
                        """)
                .query((rs, rowNum) -> new ImportRun(
                        rs.getLong("id"),
                        (Integer) rs.getObject("report_year"),
                        rs.getString("input_csv"),
                        rs.getString("status"),
                        rs.getString("message"),
                        rs.getObject("created_at", OffsetDateTime.class)
                ))
                .list();
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JacksonException e) {
            throw new IllegalStateException("Unable to serialize budget payload", e);
        }
    }
}
