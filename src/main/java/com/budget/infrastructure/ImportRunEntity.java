package com.budget.infrastructure;

import java.time.OffsetDateTime;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("import_runs")
public class ImportRunEntity {
    @Id
    @Column("id")
    private Long id;
    @Column("report_year")
    private Integer reportYear;
    @Column("input_csv")
    private String inputCsv;
    @Column("status")
    private String status;
    @Column("message")
    private String message;
    @Column("created_at")
    private OffsetDateTime createdAt;

    public ImportRunEntity() {
    }

    public ImportRunEntity(Long id, Integer reportYear, String inputCsv, String status, String message, OffsetDateTime createdAt) {
        this.id = id;
        this.reportYear = reportYear;
        this.inputCsv = inputCsv;
        this.status = status;
        this.message = message;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getReportYear() {
        return reportYear;
    }

    public void setReportYear(Integer reportYear) {
        this.reportYear = reportYear;
    }

    public String getInputCsv() {
        return inputCsv;
    }

    public void setInputCsv(String inputCsv) {
        this.inputCsv = inputCsv;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
