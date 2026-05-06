package com.budget.infrastructure.persistence.jdbc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("reports")
public class ReportEntity {
    @Id
    @Column("report_year")
    private Integer reportYear;
    @Column("period_label")
    private String periodLabel;
    @Column("period_start")
    private LocalDate periodStart;
    @Column("period_end")
    private LocalDate periodEnd;
    @Column("input_csv")
    private String inputCsv;
    @Column("imported_at")
    private OffsetDateTime importedAt;
    @Column("transaction_count")
    private int transactionCount;
    @Column("income")
    private BigDecimal income;
    @Column("spend")
    private BigDecimal spend;

    public ReportEntity() {
    }

    public ReportEntity(
            Integer reportYear,
            String periodLabel,
            LocalDate periodStart,
            LocalDate periodEnd,
            String inputCsv,
            OffsetDateTime importedAt,
            int transactionCount,
            BigDecimal income,
            BigDecimal spend
    ) {
        this.reportYear = reportYear;
        this.periodLabel = periodLabel;
        this.periodStart = periodStart;
        this.periodEnd = periodEnd;
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

    public String getPeriodLabel() {
        return periodLabel;
    }

    public void setPeriodLabel(String periodLabel) {
        this.periodLabel = periodLabel;
    }

    public LocalDate getPeriodStart() {
        return periodStart;
    }

    public void setPeriodStart(LocalDate periodStart) {
        this.periodStart = periodStart;
    }

    public LocalDate getPeriodEnd() {
        return periodEnd;
    }

    public void setPeriodEnd(LocalDate periodEnd) {
        this.periodEnd = periodEnd;
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

    public BigDecimal getIncome() {
        return income;
    }

    public void setIncome(BigDecimal income) {
        this.income = income;
    }

    public BigDecimal getSpend() {
        return spend;
    }

    public void setSpend(BigDecimal spend) {
        this.spend = spend;
    }
}
