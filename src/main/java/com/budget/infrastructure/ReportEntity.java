package com.budget.infrastructure;

import java.time.OffsetDateTime;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("reports")
public class ReportEntity {
    @Id
    @Column("report_year")
    private Integer reportYear;
    @Column("payload_json")
    private String payloadJson;
    @Column("input_csv")
    private String inputCsv;
    @Column("imported_at")
    private OffsetDateTime importedAt;
    @Column("transaction_count")
    private int transactionCount;
    @Column("income")
    private double income;
    @Column("spend")
    private double spend;

    public ReportEntity() {
    }

    public ReportEntity(Integer reportYear, String payloadJson, String inputCsv, OffsetDateTime importedAt, int transactionCount, double income, double spend) {
        this.reportYear = reportYear;
        this.payloadJson = payloadJson;
        this.inputCsv = inputCsv;
        this.importedAt = importedAt;
        this.transactionCount = transactionCount;
        this.income = income;
        this.spend = spend;
    }

    public Integer getReportYear() {
        return reportYear;
    }

    public void setReportYear(Integer reportYear) {
        this.reportYear = reportYear;
    }

    public String getPayloadJson() {
        return payloadJson;
    }

    public void setPayloadJson(String payloadJson) {
        this.payloadJson = payloadJson;
    }

    public String getInputCsv() {
        return inputCsv;
    }

    public void setInputCsv(String inputCsv) {
        this.inputCsv = inputCsv;
    }

    public OffsetDateTime getImportedAt() {
        return importedAt;
    }

    public void setImportedAt(OffsetDateTime importedAt) {
        this.importedAt = importedAt;
    }

    public int getTransactionCount() {
        return transactionCount;
    }

    public void setTransactionCount(int transactionCount) {
        this.transactionCount = transactionCount;
    }

    public double getIncome() {
        return income;
    }

    public void setIncome(double income) {
        this.income = income;
    }

    public double getSpend() {
        return spend;
    }

    public void setSpend(double spend) {
        this.spend = spend;
    }
}
