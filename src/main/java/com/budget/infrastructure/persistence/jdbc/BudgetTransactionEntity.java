package com.budget.infrastructure.persistence.jdbc;

import java.time.LocalDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("budget_transactions")
public class BudgetTransactionEntity {
    @Id
    @Column("id")
    private Long id;
    @Column("report_year")
    private int reportYear;
    @Column("lp")
    private int lp;
    @Column("posted_date")
    private LocalDate postedDate;
    @Column("month_key")
    private String monthKey;
    @Column("merchant")
    private String merchant;
    @Column("description")
    private String description;
    @Column("corrected_category")
    private String correctedCategory;
    @Column("subcategory")
    private String subcategory;
    @Column("amount")
    private double amount;
    @Column("analysis_spend")
    private double analysisSpend;
    @Column("payload_json")
    private String payloadJson;

    public BudgetTransactionEntity() {
    }

    public BudgetTransactionEntity(Long id, int reportYear, int lp, LocalDate postedDate, String monthKey, String merchant, String description, String correctedCategory, String subcategory, double amount, double analysisSpend, String payloadJson) {
        this.id = id;
        this.reportYear = reportYear;
        this.lp = lp;
        this.postedDate = postedDate;
        this.monthKey = monthKey;
        this.merchant = merchant;
        this.description = description;
        this.correctedCategory = correctedCategory;
        this.subcategory = subcategory;
        this.amount = amount;
        this.analysisSpend = analysisSpend;
        this.payloadJson = payloadJson;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public int getReportYear() {
        return reportYear;
    }

    public void setReportYear(int reportYear) {
        this.reportYear = reportYear;
    }

    public int getLp() {
        return lp;
    }

    public void setLp(int lp) {
        this.lp = lp;
    }

    public LocalDate getPostedDate() {
        return postedDate;
    }

    public void setPostedDate(LocalDate postedDate) {
        this.postedDate = postedDate;
    }

    public String getMonthKey() {
        return monthKey;
    }

    public void setMonthKey(String monthKey) {
        this.monthKey = monthKey;
    }

    public String getMerchant() {
        return merchant;
    }

    public void setMerchant(String merchant) {
        this.merchant = merchant;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCorrectedCategory() {
        return correctedCategory;
    }

    public void setCorrectedCategory(String correctedCategory) {
        this.correctedCategory = correctedCategory;
    }

    public String getSubcategory() {
        return subcategory;
    }

    public void setSubcategory(String subcategory) {
        this.subcategory = subcategory;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public double getAnalysisSpend() {
        return analysisSpend;
    }

    public void setAnalysisSpend(double analysisSpend) {
        this.analysisSpend = analysisSpend;
    }

    public String getPayloadJson() {
        return payloadJson;
    }

    public void setPayloadJson(String payloadJson) {
        this.payloadJson = payloadJson;
    }
}
