package com.budget.infrastructure.persistence.jdbc;

import java.math.BigDecimal;
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
    @Column("account")
    private String account;
    @Column("bank_category")
    private String bankCategory;
    @Column("category_id")
    private String categoryId;
    @Column("corrected_category")
    private String correctedCategory;
    @Column("subcategory_id")
    private String subcategoryId;
    @Column("budget_area")
    private String budgetArea;
    @Column("budget_group")
    private String budgetGroup;
    @Column("subcategory")
    private String subcategory;
    @Column("flow_type")
    private String flowType;
    @Column("budget_group_id")
    private String budgetGroupId;
    @Column("budget_group_label")
    private String budgetGroupLabel;
    @Column("review_status")
    private String reviewStatus;
    @Column("review_reason")
    private String reviewReason;
    @Column("budget_bucket")
    private String budgetBucket;
    @Column("fixedness")
    private String fixedness;
    @Column("transaction_type")
    private String transactionType;
    @Column("amount")
    private BigDecimal amount;
    @Column("income")
    private BigDecimal income;
    @Column("analysis_spend")
    private BigDecimal analysisSpend;
    @Column("discretionary")
    private BigDecimal discretionary;
    @Column("excluded")
    private BigDecimal excluded;
    @Column("excluded_outgoing")
    private BigDecimal excludedOutgoing;
    @Column("excluded_incoming")
    private BigDecimal excludedIncoming;
    @Column("excluded_net")
    private BigDecimal excludedNet;
    @Column("confidence")
    private String confidence;
    @Column("notes")
    private String notes;
    @Column("matched_rule")
    private String matchedRule;

    public BudgetTransactionEntity() {
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

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public String getBankCategory() {
        return bankCategory;
    }

    public void setBankCategory(String bankCategory) {
        this.bankCategory = bankCategory;
    }

    public String getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(String categoryId) {
        this.categoryId = categoryId;
    }

    public String getCorrectedCategory() {
        return correctedCategory;
    }

    public void setCorrectedCategory(String correctedCategory) {
        this.correctedCategory = correctedCategory;
    }

    public String getSubcategoryId() {
        return subcategoryId;
    }

    public void setSubcategoryId(String subcategoryId) {
        this.subcategoryId = subcategoryId;
    }

    public String getBudgetArea() {
        return budgetArea;
    }

    public void setBudgetArea(String budgetArea) {
        this.budgetArea = budgetArea;
    }

    public String getBudgetGroup() {
        return budgetGroup;
    }

    public void setBudgetGroup(String budgetGroup) {
        this.budgetGroup = budgetGroup;
    }

    public String getSubcategory() {
        return subcategory;
    }

    public void setSubcategory(String subcategory) {
        this.subcategory = subcategory;
    }

    public String getFlowType() {
        return flowType;
    }

    public void setFlowType(String flowType) {
        this.flowType = flowType;
    }

    public String getBudgetGroupId() {
        return budgetGroupId;
    }

    public void setBudgetGroupId(String budgetGroupId) {
        this.budgetGroupId = budgetGroupId;
    }

    public String getBudgetGroupLabel() {
        return budgetGroupLabel;
    }

    public void setBudgetGroupLabel(String budgetGroupLabel) {
        this.budgetGroupLabel = budgetGroupLabel;
    }

    public String getReviewStatus() {
        return reviewStatus;
    }

    public void setReviewStatus(String reviewStatus) {
        this.reviewStatus = reviewStatus;
    }

    public String getReviewReason() {
        return reviewReason;
    }

    public void setReviewReason(String reviewReason) {
        this.reviewReason = reviewReason;
    }

    public String getBudgetBucket() {
        return budgetBucket;
    }

    public void setBudgetBucket(String budgetBucket) {
        this.budgetBucket = budgetBucket;
    }

    public String getFixedness() {
        return fixedness;
    }

    public void setFixedness(String fixedness) {
        this.fixedness = fixedness;
    }

    public String getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(String transactionType) {
        this.transactionType = transactionType;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public BigDecimal getIncome() {
        return income;
    }

    public void setIncome(BigDecimal income) {
        this.income = income;
    }

    public BigDecimal getAnalysisSpend() {
        return analysisSpend;
    }

    public void setAnalysisSpend(BigDecimal analysisSpend) {
        this.analysisSpend = analysisSpend;
    }

    public BigDecimal getDiscretionary() {
        return discretionary;
    }

    public void setDiscretionary(BigDecimal discretionary) {
        this.discretionary = discretionary;
    }

    public BigDecimal getExcluded() {
        return excluded;
    }

    public void setExcluded(BigDecimal excluded) {
        this.excluded = excluded;
    }

    public BigDecimal getExcludedOutgoing() {
        return excludedOutgoing;
    }

    public void setExcludedOutgoing(BigDecimal excludedOutgoing) {
        this.excludedOutgoing = excludedOutgoing;
    }

    public BigDecimal getExcludedIncoming() {
        return excludedIncoming;
    }

    public void setExcludedIncoming(BigDecimal excludedIncoming) {
        this.excludedIncoming = excludedIncoming;
    }

    public BigDecimal getExcludedNet() {
        return excludedNet;
    }

    public void setExcludedNet(BigDecimal excludedNet) {
        this.excludedNet = excludedNet;
    }

    public String getConfidence() {
        return confidence;
    }

    public void setConfidence(String confidence) {
        this.confidence = confidence;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getMatchedRule() {
        return matchedRule;
    }

    public void setMatchedRule(String matchedRule) {
        this.matchedRule = matchedRule;
    }
}
