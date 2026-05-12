package com.budget.infrastructure.persistence.jdbc;

import com.budget.application.reporting.AnalyticsReport;
import com.budget.application.reporting.BudgetReportStore;
import com.budget.application.reporting.CalendarReport;
import com.budget.application.reporting.ReportNotFoundException;
import com.budget.application.reporting.TransactionPage;
import com.budget.application.reporting.TransactionQuery;
import com.budget.application.reporting.TransactionRecord;
import com.budget.application.reporting.YearSummary;
import com.budget.domain.importjob.ImportRun;
import com.budget.domain.report.BudgetAnalysisResult;
import com.budget.domain.report.BudgetSnapshot;
import com.budget.domain.transaction.NormalizedTransaction;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.jdbc.core.JdbcAggregateTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class BudgetReportRepository implements BudgetReportStore {
    private static final java.util.Set<String> FINANCIAL_FLOW_CATEGORIES = java.util.Set.of("Inwestycje", "Konto oszczędnościowe", "Nadpłata kredytu");

    private final ReportDataJdbcRepository reports;
    private final BudgetTransactionDataJdbcRepository transactions;
    private final ImportRunDataJdbcRepository importRuns;
    private final JdbcAggregateTemplate aggregateTemplate;
    private final NamedParameterJdbcTemplate jdbc;

    public BudgetReportRepository(
            ReportDataJdbcRepository reports,
            BudgetTransactionDataJdbcRepository transactions,
            ImportRunDataJdbcRepository importRuns,
            JdbcAggregateTemplate aggregateTemplate,
            NamedParameterJdbcTemplate jdbc
    ) {
        this.reports = reports;
        this.transactions = transactions;
        this.importRuns = importRuns;
        this.aggregateTemplate = aggregateTemplate;
        this.jdbc = jdbc;
    }

    @Override
    @Transactional
    public void save(BudgetAnalysisResult result) {
        reports.deleteById(result.year());
        var snapshot = result.snapshot();
        aggregateTemplate.insert(new ReportEntity(
                result.year(),
                snapshot.period(),
                snapshot.periodStart(),
                snapshot.periodEnd(),
                result.fileName(),
                OffsetDateTime.now(),
                result.transactionCount(),
                result.income(),
                result.spend()
        ));

        saveKpis(result.year(), snapshot.kpis());
        saveMonthly(result.year(), snapshot.monthly());
        saveCategories(result.year(), snapshot.categories());
        saveHierarchy(result.year(), snapshot.hierarchy());
        saveBudgetMix(result.year(), snapshot.budgetMix());
        saveSavingsPlan(result.year(), snapshot.savingsPlan());
        saveMonthControl(result.year(), snapshot.monthControl());
        saveFixedness(result.year(), snapshot.fixedness());
        saveMerchants(result.year(), snapshot.topMerchants());
        saveRecurring(result.year(), snapshot.recurring());
        saveLargeOneOffs(result.year(), snapshot.largeOneoffs());

        transactions.saveAll(result.transactions().stream()
                .map(tx -> toEntity(result.year(), tx))
                .toList());
    }

    @Override
    public List<YearSummary> findYears() {
        return reports.findAll().stream()
                .sorted(Comparator.comparingInt(ReportEntity::getReportYear))
                .map(report -> new YearSummary(
                        report.getReportYear(),
                        report.getImportedAt(),
                        report.getTransactionCount(),
                        report.getIncome(),
                        report.getSpend(),
                        report.getInputCsv()
                ))
                .toList();
    }

    @Override
    public BudgetSnapshot findDashboard(int year) {
        var report = reports.findById(year).orElseThrow(() -> new ReportNotFoundException(year));
        var kpis = enrichSavingsAccountKpis(
                queryOne("SELECT * FROM report_kpis WHERE report_year = :year", params(year), this::mapKpis),
                year
        );
        return new BudgetSnapshot(
                year,
                report.getPeriodLabel(),
                report.getPeriodStart(),
                report.getPeriodEnd(),
                activeMonths(year),
                kpis,
                query("SELECT * FROM report_monthly_summaries WHERE report_year = :year ORDER BY month_key", params(year), this::mapMonthly),
                query("SELECT * FROM report_category_summaries WHERE report_year = :year ORDER BY spend DESC, category", params(year), this::mapCategory),
                query("SELECT * FROM report_hierarchy_summaries WHERE report_year = :year ORDER BY budget_area, spend DESC, budget_group, category, subcategory", params(year), this::mapHierarchy),
                query("SELECT * FROM report_budget_mix WHERE report_year = :year ORDER BY id", params(year), this::mapBudgetMix),
                savingsPlan(year),
                monthControl(year),
                query("SELECT * FROM report_fixedness_summaries WHERE report_year = :year ORDER BY id", params(year), this::mapFixedness),
                query("SELECT * FROM report_merchant_summaries WHERE report_year = :year ORDER BY total_amount DESC, merchant", params(year), this::mapMerchant),
                query("SELECT * FROM report_recurring_merchants WHERE report_year = :year ORDER BY total_amount DESC, merchant", params(year), this::mapRecurring),
                query("SELECT * FROM report_large_oneoffs WHERE report_year = :year ORDER BY amount DESC, posted_date DESC", params(year), this::mapLargeOneOff)
        );
    }

    private BudgetSnapshot.Kpis enrichSavingsAccountKpis(BudgetSnapshot.Kpis row, int year) {
        var turnover = savingsAccountTurnover(year);
        var grossDeposits = decimal("""
                SELECT COALESCE(SUM(excluded_outgoing), 0)
                FROM budget_transactions
                WHERE report_year = :year
                  AND corrected_category = 'Konto oszczędnościowe'
                """, params(year));
        return new BudgetSnapshot.Kpis(
                row.income(),
                row.spend(),
                row.discretionary(),
                row.operatingSurplus(),
                row.savingsRate(),
                row.excludedGross(),
                row.excludedOutgoing(),
                row.excludedIncoming(),
                row.excludedNet(),
                row.realSavingsOutgoing(),
                row.unassignedSurplus(),
                row.transactions(),
                row.corrections(),
                row.lowConfidence(),
                row.toCheck(),
                row.toCheckAmount(),
                turnover.netChange(),
                grossDeposits,
                turnover.inflows(),
                turnover.outflows()
        );
    }

    private SavingsAccountTurnover savingsAccountTurnover(int year) {
        var latestSavingsAccountDate = jdbc.queryForObject("""
                SELECT MAX(posted_date)
                FROM budget_transactions
                WHERE report_year = :year
                  AND (LOWER(account) LIKE '%oszcz%' OR LOWER(account) LIKE '%lokat%' OR LOWER(account) LIKE '%saving%')
                """, params(year), LocalDate.class);
        var accountInflows = decimal("""
                SELECT COALESCE(SUM(amount), 0)
                FROM budget_transactions
                WHERE report_year = :year
                  AND amount > 0
                  AND (LOWER(account) LIKE '%oszcz%' OR LOWER(account) LIKE '%lokat%' OR LOWER(account) LIKE '%saving%')
                """, params(year));
        var accountOutflows = decimal("""
                SELECT COALESCE(SUM(-amount), 0)
                FROM budget_transactions
                WHERE report_year = :year
                  AND amount < 0
                  AND (LOWER(account) LIKE '%oszcz%' OR LOWER(account) LIKE '%lokat%' OR LOWER(account) LIKE '%saving%')
                """, params(year));
        var pendingDeposits = latestSavingsAccountDate == null
                ? decimal("""
                        SELECT COALESCE(SUM(excluded_outgoing), 0)
                        FROM budget_transactions
                        WHERE report_year = :year
                          AND corrected_category = 'Konto oszczędnościowe'
                        """, params(year))
                : decimal("""
                        SELECT COALESCE(SUM(excluded_outgoing), 0)
                        FROM budget_transactions
                        WHERE report_year = :year
                          AND corrected_category = 'Konto oszczędnościowe'
                          AND posted_date > :latestSavingsAccountDate
                        """, params(year).addValue("latestSavingsAccountDate", latestSavingsAccountDate));
        var inflows = accountInflows.add(pendingDeposits);
        return new SavingsAccountTurnover(inflows.subtract(accountOutflows), inflows, accountOutflows);
    }

    private record SavingsAccountTurnover(BigDecimal netChange, BigDecimal inflows, BigDecimal outflows) {
    }

    @Override
    public CalendarReport findCalendar(int year, String month) {
        ensureReport(year);
        var monthKey = month == null || month.isBlank() ? latestMonth(year) : month;
        var ym = YearMonth.parse(monthKey);
        var query = new TransactionQuery(year, 0, 5_000, "spend,desc", monthKey, null, null, null, null, null, null, null, null);
        var rows = findTransactionRows(query, false);
        var byDate = new LinkedHashMap<LocalDate, List<TransactionRecord>>();
        for (var row : rows) {
            byDate.computeIfAbsent(row.postedDate(), ignored -> new ArrayList<>()).add(row);
        }

        var days = new ArrayList<CalendarReport.CalendarDay>();
        for (var day = 1; day <= ym.lengthOfMonth(); day++) {
            var date = ym.atDay(day);
            var items = byDate.getOrDefault(date, List.of());
            var spend = items.stream().map(TransactionRecord::spend).reduce(BigDecimal.ZERO, BigDecimal::add);
            var income = items.stream().map(TransactionRecord::income).reduce(BigDecimal.ZERO, BigDecimal::add);
            var biggest = items.stream().max(Comparator.comparing(TransactionRecord::spend)).orElse(null);
            days.add(new CalendarReport.CalendarDay(date, day, spend, income, items.size(), biggest));
        }
        return new CalendarReport(year, monthKey, days);
    }

    @Override
    public AnalyticsReport findAnalytics(TransactionQuery query) {
        ensureReport(query.year());
        var rows = findTransactionRows(query, false);
        var spend = rows.stream().map(TransactionRecord::spend).reduce(BigDecimal.ZERO, BigDecimal::add);
        var income = rows.stream().map(TransactionRecord::income).reduce(BigDecimal.ZERO, BigDecimal::add);
        var oneoffs = rows.stream()
                .filter(row -> row.spend().compareTo(BigDecimal.valueOf(500)) >= 0)
                .sorted(Comparator.comparing(TransactionRecord::spend).reversed())
                .limit(120)
                .toList();
        return new AnalyticsReport(
                query.year(),
                query.date() != null ? "day" : query.month() != null ? "month" : "year",
                query.month(),
                query.date(),
                spend,
                income,
                rows.size(),
                aggregate(rows, TransactionRecord::area, AnalyticsReport.AreaSpend::new),
                aggregate(rows, TransactionRecord::group, AnalyticsReport.GroupSpend::new),
                aggregate(rows, TransactionRecord::correctedCategory, AnalyticsReport.CategorySpend::new),
                aggregateSubcategories(rows),
                aggregateHierarchy(rows),
                financialFlows(rows),
                aggregate(rows, TransactionRecord::merchant, AnalyticsReport.MerchantSpend::new),
                oneoffs,
                monthlyCategoryTrends(rows),
                monthlyHierarchyTrends(rows),
                monthlyBucketTrends(rows),
                monthlyMerchantTrends(rows),
                fixednessBreakdown(rows),
                confidenceBreakdown(rows),
                amountBands(rows)
        );
    }

    @Override
    public TransactionPage findTransactions(TransactionQuery query) {
        ensureReport(query.year());
        var total = countTransactions(query);
        var rows = findTransactionRows(query, true);
        return TransactionPage.of(rows, query, total);
    }

    @Override
    public void recordImportRun(Integer year, String inputCsv, String status, String message, int duplicatesRemoved) {
        importRuns.save(new ImportRunEntity(null, year, inputCsv, status, message, Math.max(0, duplicatesRemoved), OffsetDateTime.now()));
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
                        row.getDuplicatesRemoved() == null ? 0 : row.getDuplicatesRemoved(),
                        row.getCreatedAt()
                ))
                .toList();
    }

    private void saveKpis(int year, BudgetSnapshot.Kpis kpis) {
        update("""
                INSERT INTO report_kpis (
                    report_year, income, spend, discretionary, operating_surplus, savings_rate,
                    excluded_gross, excluded_outgoing, excluded_incoming, excluded_net,
                    real_savings_outgoing, unassigned_surplus, transactions, corrections,
                    low_confidence, to_check, to_check_amount
                ) VALUES (
                    :year, :income, :spend, :discretionary, :operatingSurplus, :savingsRate,
                    :excludedGross, :excludedOutgoing, :excludedIncoming, :excludedNet,
                    :realSavingsOutgoing, :unassignedSurplus, :transactions, :corrections,
                    :lowConfidence, :toCheck, :toCheckAmount
                )
                """, params(year)
                .addValue("income", kpis.income())
                .addValue("spend", kpis.spend())
                .addValue("discretionary", kpis.discretionary())
                .addValue("operatingSurplus", kpis.operatingSurplus())
                .addValue("savingsRate", kpis.savingsRate())
                .addValue("excludedGross", kpis.excludedGross())
                .addValue("excludedOutgoing", kpis.excludedOutgoing())
                .addValue("excludedIncoming", kpis.excludedIncoming())
                .addValue("excludedNet", kpis.excludedNet())
                .addValue("realSavingsOutgoing", kpis.realSavingsOutgoing())
                .addValue("unassignedSurplus", kpis.unassignedSurplus())
                .addValue("transactions", kpis.transactions())
                .addValue("corrections", kpis.corrections())
                .addValue("lowConfidence", kpis.lowConfidence())
                .addValue("toCheck", kpis.toCheck())
                .addValue("toCheckAmount", kpis.toCheckAmount()));
    }

    private void saveMonthly(int year, List<BudgetSnapshot.MonthlySummary> rows) {
        for (var row : rows) {
            update("""
                    INSERT INTO report_monthly_summaries (
                        report_year, month_label, month_key, income, spend, discretionary,
                        excluded, savings_investments, net_flow, savings_rate, transactions, spend_per_day
                    ) VALUES (
                        :year, :month, :monthKey, :income, :spend, :discretionary,
                        :excluded, :savingsInvestments, :netFlow, :savingsRate, :transactions, :spendPerDay
                    )
                    """, params(year)
                    .addValue("month", row.month())
                    .addValue("monthKey", row.monthKey())
                    .addValue("income", row.income())
                    .addValue("spend", row.spend())
                    .addValue("discretionary", row.discretionary())
                    .addValue("excluded", row.excluded())
                    .addValue("savingsInvestments", row.savingsInvestments())
                    .addValue("netFlow", row.netFlow())
                    .addValue("savingsRate", row.savingsRate())
                    .addValue("transactions", row.transactions())
                    .addValue("spendPerDay", row.spendPerDay()));
        }
    }

    private void saveCategories(int year, List<BudgetSnapshot.CategorySummary> rows) {
        for (var row : rows) {
            update("""
                    INSERT INTO report_category_summaries (
                        report_year, category, budget_group, spend, income, excluded,
                        monthly_average, max_month, max_amount, discretionary, transaction_count, merchant_examples
                    ) VALUES (
                        :year, :category, :group, :spend, :income, :excluded,
                        :monthlyAverage, :maxMonth, :maxAmount, :discretionary, :count, :merchantExamples
                    )
                    """, params(year)
                    .addValue("category", row.category())
                    .addValue("group", row.group())
                    .addValue("spend", row.spend())
                    .addValue("income", row.income())
                    .addValue("excluded", row.excluded())
                    .addValue("monthlyAverage", row.monthlyAverage())
                    .addValue("maxMonth", row.maxMonth())
                    .addValue("maxAmount", row.maxAmount())
                    .addValue("discretionary", row.discretionary())
                    .addValue("count", row.count())
                    .addValue("merchantExamples", encodeExamples(row.merchantExamples())));
        }
    }

    private void saveHierarchy(int year, List<BudgetSnapshot.HierarchySummary> rows) {
        for (var row : rows) {
            update("""
                    INSERT INTO report_hierarchy_summaries (
                        report_year, budget_area, budget_group, category, subcategory, spend,
                        income, excluded, monthly_average, transaction_count, discretionary
                    ) VALUES (
                        :year, :area, :group, :category, :subcategory, :spend,
                        :income, :excluded, :monthlyAverage, :count, :discretionary
                    )
                    """, params(year)
                    .addValue("area", row.area())
                    .addValue("group", row.group())
                    .addValue("category", row.category())
                    .addValue("subcategory", row.subcategory())
                    .addValue("spend", row.spend())
                    .addValue("income", row.income())
                    .addValue("excluded", row.excluded())
                    .addValue("monthlyAverage", row.monthlyAverage())
                    .addValue("count", row.count())
                    .addValue("discretionary", row.discretionary()));
        }
    }

    private void saveBudgetMix(int year, List<BudgetSnapshot.BudgetMixItem> rows) {
        for (var row : rows) {
            update("""
                    INSERT INTO report_budget_mix (
                        report_year, bucket, total_amount, monthly_average, income_share, note
                    ) VALUES (
                        :year, :bucket, :sum, :monthlyAverage, :incomeShare, :note
                    )
                    """, params(year)
                    .addValue("bucket", row.bucket())
                    .addValue("sum", row.sum())
                    .addValue("monthlyAverage", row.monthlyAverage())
                    .addValue("incomeShare", row.incomeShare())
                    .addValue("note", row.note()));
        }
    }

    private void saveSavingsPlan(int year, BudgetSnapshot.SavingsPlan plan) {
        update("""
                INSERT INTO report_savings_plan (
                    report_year, current_monthly_spend, current_monthly_income, core_monthly_cost,
                    target_monthly_spend, aggressive_monthly_spend, target_investment_transfer,
                    aggressive_investment_transfer, monthly_cut_needed, emergency_fund_min, emergency_fund_comfort
                ) VALUES (
                    :year, :currentMonthlySpend, :currentMonthlyIncome, :coreMonthlyCost,
                    :targetMonthlySpend, :aggressiveMonthlySpend, :targetInvestmentTransfer,
                    :aggressiveInvestmentTransfer, :monthlyCutNeeded, :emergencyFundMin, :emergencyFundComfort
                )
                """, params(year)
                .addValue("currentMonthlySpend", plan.currentMonthlySpend())
                .addValue("currentMonthlyIncome", plan.currentMonthlyIncome())
                .addValue("coreMonthlyCost", plan.coreMonthlyCost())
                .addValue("targetMonthlySpend", plan.targetMonthlySpend())
                .addValue("aggressiveMonthlySpend", plan.aggressiveMonthlySpend())
                .addValue("targetInvestmentTransfer", plan.targetInvestmentTransfer())
                .addValue("aggressiveInvestmentTransfer", plan.aggressiveInvestmentTransfer())
                .addValue("monthlyCutNeeded", plan.monthlyCutNeeded())
                .addValue("emergencyFundMin", plan.emergencyFundMin())
                .addValue("emergencyFundComfort", plan.emergencyFundComfort()));

        var limitRows = new ArrayList<BudgetSnapshot.CategoryLimit>();
        limitRows.addAll(plan.parentLimits());
        limitRows.addAll(plan.categoryLimits());
        for (var row : limitRows) {
            update("""
                    INSERT INTO report_category_limits (
                        report_year, limit_scope, limit_name, is_parent, category, bucket,
                        current_monthly, limit_amount, potential_monthly, potential_yearly, priority, action
                    ) VALUES (
                        :year, :scope, :name, :parent, :category, :bucket,
                        :currentMonthly, :limit, :potentialMonthly, :potentialYearly, :priority, :action
                    )
                    """, params(year)
                    .addValue("scope", row.scope())
                    .addValue("name", row.name())
                    .addValue("parent", row.parent())
                    .addValue("category", row.category())
                    .addValue("bucket", row.bucket())
                    .addValue("currentMonthly", row.currentMonthly())
                    .addValue("limit", row.limit())
                    .addValue("potentialMonthly", row.potentialMonthly())
                    .addValue("potentialYearly", row.potentialYearly())
                    .addValue("priority", row.priority())
                    .addValue("action", row.action()));
        }
    }

    private void saveMonthControl(int year, BudgetSnapshot.MonthControl control) {
        update("""
                INSERT INTO report_month_control (
                    report_year, month_label, month_key, elapsed_days, remaining_days, days_in_month,
                    income_to_date, spend_to_date, projected_spend, target_spend, remaining_budget,
                    daily_allowed, projected_delta
                ) VALUES (
                    :year, :month, :monthKey, :elapsedDays, :remainingDays, :daysInMonth,
                    :incomeToDate, :spendToDate, :projectedSpend, :targetSpend, :remainingBudget,
                    :dailyAllowed, :projectedDelta
                )
                """, params(year)
                .addValue("month", control.month())
                .addValue("monthKey", control.monthKey())
                .addValue("elapsedDays", control.elapsedDays())
                .addValue("remainingDays", control.remainingDays())
                .addValue("daysInMonth", control.daysInMonth())
                .addValue("incomeToDate", control.incomeToDate())
                .addValue("spendToDate", control.spendToDate())
                .addValue("projectedSpend", control.projectedSpend())
                .addValue("targetSpend", control.targetSpend())
                .addValue("remainingBudget", control.remainingBudget())
                .addValue("dailyAllowed", control.dailyAllowed())
                .addValue("projectedDelta", control.projectedDelta()));

        for (var row : control.categoryStatus()) {
            update("""
                    INSERT INTO report_month_category_status (
                        report_year, category, bucket, current_monthly, limit_amount,
                        potential_monthly, potential_yearly, priority, action, current_month_spend,
                        current_month_projection, remaining_this_month, projected_delta, usage
                    ) VALUES (
                        :year, :category, :bucket, :currentMonthly, :limit,
                        :potentialMonthly, :potentialYearly, :priority, :action, :currentMonthSpend,
                        :currentMonthProjection, :remainingThisMonth, :projectedDelta, :usage
                    )
                    """, params(year)
                    .addValue("category", row.category())
                    .addValue("bucket", row.bucket())
                    .addValue("currentMonthly", row.currentMonthly())
                    .addValue("limit", row.limit())
                    .addValue("potentialMonthly", row.potentialMonthly())
                    .addValue("potentialYearly", row.potentialYearly())
                    .addValue("priority", row.priority())
                    .addValue("action", row.action())
                    .addValue("currentMonthSpend", row.currentMonthSpend())
                    .addValue("currentMonthProjection", row.currentMonthProjection())
                    .addValue("remainingThisMonth", row.remainingThisMonth())
                    .addValue("projectedDelta", row.projectedDelta())
                    .addValue("usage", row.usage()));
        }

        for (var row : control.alerts()) {
            update("""
                    INSERT INTO report_month_alerts (report_year, alert_type, severity, message)
                    VALUES (:year, :type, :severity, :message)
                    """, params(year)
                    .addValue("type", row.type())
                    .addValue("severity", row.severity())
                    .addValue("message", row.message()));
        }

        for (var row : control.sinkingFunds()) {
            update("""
                    INSERT INTO report_sinking_funds (
                        report_year, name, category, monthly_set_aside, yearly_need, note
                    ) VALUES (
                        :year, :name, :category, :monthlySetAside, :yearlyNeed, :note
                    )
                    """, params(year)
                    .addValue("name", row.name())
                    .addValue("category", row.category())
                    .addValue("monthlySetAside", row.monthlySetAside())
                    .addValue("yearlyNeed", row.yearlyNeed())
                    .addValue("note", row.note()));
        }
    }

    private void saveFixedness(int year, List<BudgetSnapshot.FixednessSummary> rows) {
        for (var row : rows) {
            update("""
                    INSERT INTO report_fixedness_summaries (
                        report_year, fixedness_type, spend, excluded_outgoing, monthly_average, transaction_count
                    ) VALUES (
                        :year, :type, :spend, :excludedOutgoing, :monthlyAverage, :count
                    )
                    """, params(year)
                    .addValue("type", row.type())
                    .addValue("spend", row.spend())
                    .addValue("excludedOutgoing", row.excludedOutgoing())
                    .addValue("monthlyAverage", row.monthlyAverage())
                    .addValue("count", row.count()));
        }
    }

    private void saveMerchants(int year, List<BudgetSnapshot.MerchantSummary> rows) {
        for (var row : rows) {
            update("""
                    INSERT INTO report_merchant_summaries (
                        report_year, merchant, category, total_amount, transaction_count, average_amount
                    ) VALUES (
                        :year, :merchant, :category, :sum, :count, :average
                    )
                    """, params(year)
                    .addValue("merchant", row.merchant())
                    .addValue("category", row.category())
                    .addValue("sum", row.sum())
                    .addValue("count", row.count())
                    .addValue("average", row.average()));
        }
    }

    private void saveRecurring(int year, List<BudgetSnapshot.RecurringItem> rows) {
        for (var row : rows) {
            update("""
                    INSERT INTO report_recurring_merchants (
                        report_year, merchant, category, bucket, total_amount, months,
                        transaction_count, monthly_average, avg_day, last_date
                    ) VALUES (
                        :year, :merchant, :category, :bucket, :sum, :months,
                        :count, :monthlyAverage, :avgDay, :lastDate
                    )
                    """, params(year)
                    .addValue("merchant", row.merchant())
                    .addValue("category", row.category())
                    .addValue("bucket", row.bucket())
                    .addValue("sum", row.sum())
                    .addValue("months", row.months())
                    .addValue("count", row.count())
                    .addValue("monthlyAverage", row.monthlyAverage())
                    .addValue("avgDay", row.avgDay())
                    .addValue("lastDate", row.lastDate()));
        }
    }

    private void saveLargeOneOffs(int year, List<BudgetSnapshot.LargeOneOff> rows) {
        for (var row : rows) {
            update("""
                    INSERT INTO report_large_oneoffs (
                        report_year, posted_date, merchant, category, bucket, amount,
                        month_key, confidence, description
                    ) VALUES (
                        :year, :postedDate, :merchant, :category, :bucket, :amount,
                        :month, :confidence, :description
                    )
                    """, params(year)
                    .addValue("postedDate", row.date())
                    .addValue("merchant", row.merchant())
                    .addValue("category", row.category())
                    .addValue("bucket", row.bucket())
                    .addValue("amount", row.amount())
                    .addValue("month", row.month())
                    .addValue("confidence", row.confidence())
                    .addValue("description", row.description()));
        }
    }

    private BudgetSnapshot.SavingsPlan savingsPlan(int year) {
        var plan = queryOne("SELECT * FROM report_savings_plan WHERE report_year = :year", params(year), this::mapSavingsPlanWithoutLimits);
        var limits = query("SELECT * FROM report_category_limits WHERE report_year = :year ORDER BY potential_monthly DESC, category", params(year), this::mapCategoryLimit);
        var parentLimits = limits.stream().filter(BudgetSnapshot.CategoryLimit::parent).toList();
        var categoryLimits = limits.stream().filter(row -> !row.parent()).toList();
        return new BudgetSnapshot.SavingsPlan(
                plan.currentMonthlySpend(),
                plan.currentMonthlyIncome(),
                plan.coreMonthlyCost(),
                plan.targetMonthlySpend(),
                plan.aggressiveMonthlySpend(),
                plan.targetInvestmentTransfer(),
                plan.aggressiveInvestmentTransfer(),
                plan.monthlyCutNeeded(),
                plan.emergencyFundMin(),
                plan.emergencyFundComfort(),
                parentLimits,
                categoryLimits
        );
    }

    private BudgetSnapshot.MonthControl monthControl(int year) {
        var control = queryOne("SELECT * FROM report_month_control WHERE report_year = :year", params(year), this::mapMonthControlWithoutChildren);
        var statuses = query("SELECT * FROM report_month_category_status WHERE report_year = :year ORDER BY potential_monthly DESC, category", params(year), this::mapCategoryStatus);
        var alerts = query("SELECT * FROM report_month_alerts WHERE report_year = :year ORDER BY id", params(year), this::mapAlert);
        var sinkingFunds = query("SELECT * FROM report_sinking_funds WHERE report_year = :year ORDER BY id", params(year), this::mapSinkingFund);
        return new BudgetSnapshot.MonthControl(
                control.month(),
                control.monthKey(),
                control.elapsedDays(),
                control.remainingDays(),
                control.daysInMonth(),
                control.incomeToDate(),
                control.spendToDate(),
                control.projectedSpend(),
                control.targetSpend(),
                control.remainingBudget(),
                control.dailyAllowed(),
                control.projectedDelta(),
                statuses,
                alerts,
                sinkingFunds
        );
    }

    private int activeMonths(int year) {
        var value = jdbc.queryForObject(
                "SELECT COUNT(*) FROM report_monthly_summaries WHERE report_year = :year AND transactions > 0",
                params(year),
                Integer.class
        );
        return value == null ? 0 : value;
    }

    private String latestMonth(int year) {
        return queryOne("""
                SELECT month_key
                FROM report_monthly_summaries
                WHERE report_year = :year AND transactions > 0
                ORDER BY month_key DESC
                LIMIT 1
                """, params(year), (rs, rowNum) -> rs.getString("month_key"));
    }

    private long countTransactions(TransactionQuery query) {
        var params = params(query.year());
        var where = transactionWhere(query, params);
        var value = jdbc.queryForObject("SELECT COUNT(*) FROM budget_transactions " + where, params, Long.class);
        return value == null ? 0 : value;
    }

    private List<TransactionRecord> findTransactionRows(TransactionQuery query, boolean paged) {
        var params = params(query.year());
        var where = transactionWhere(query, params);
        var sql = new StringBuilder("SELECT * FROM budget_transactions ")
                .append(where)
                .append(" ORDER BY ")
                .append(query.orderByClause());
        if (paged) {
            sql.append(" LIMIT :limit OFFSET :offset");
            params.addValue("limit", query.size());
            params.addValue("offset", query.offset());
        }
        return query(sql.toString(), params, this::mapTransaction);
    }

    private String transactionWhere(TransactionQuery query, MapSqlParameterSource params) {
        var clauses = new ArrayList<String>();
        clauses.add("WHERE report_year = :year");
        if (query.month() != null) {
            clauses.add("month_key = :month");
            params.addValue("month", query.month());
        }
        if (query.date() != null) {
            clauses.add("posted_date = :date");
            params.addValue("date", query.date());
        }
        if (query.flow() != null) {
            switch (query.flow()) {
                case "income" -> clauses.add("income > 0");
                case "livingExpense" -> clauses.add("analysis_spend > 0 AND flow_type = 'livingExpense'");
                case "excluded" -> clauses.add("excluded > 0");
                case "technicalTransfer" -> clauses.add("excluded > 0 AND flow_type = 'technicalTransfer'");
                case "wealthTransfer" -> {
                    clauses.add("excluded_outgoing > 0 AND flow_type = 'wealthTransfer'");
                }
                case "refundCorrection" -> clauses.add("flow_type = 'refundCorrection'");
                case "review" -> clauses.add("review_status <> 'ok'");
                default -> throw new IllegalArgumentException("Unsupported transaction flow: " + query.flow());
            }
        }
        if (query.bucket() != null) {
            clauses.add("budget_bucket = :bucket");
            params.addValue("bucket", query.bucket());
        }
        if (query.area() != null) {
            clauses.add("budget_area = :area");
            params.addValue("area", query.area());
        }
        if (query.group() != null) {
            clauses.add("budget_group = :group");
            params.addValue("group", query.group());
        }
        if (query.category() != null) {
            clauses.add("corrected_category = :category");
            params.addValue("category", query.category());
        }
        if (query.subcategory() != null) {
            clauses.add("(subcategory = :subcategory OR corrected_category || ' · ' || subcategory = :subcategory)");
            params.addValue("subcategory", query.subcategory());
        }
        if (query.fixedness() != null) {
            clauses.add("fixedness = :fixedness");
            params.addValue("fixedness", query.fixedness());
        }
        if (query.confidence() != null) {
            clauses.add("confidence = :confidence");
            params.addValue("confidence", query.confidence());
        }
        if (query.reviewStatus() != null) {
            clauses.add("review_status = :reviewStatus");
            params.addValue("reviewStatus", query.reviewStatus());
        }
        if (query.query() != null) {
            clauses.add("""
                    LOWER(
                        COALESCE(merchant, '') || ' ' ||
                        COALESCE(description, '') || ' ' ||
                        COALESCE(corrected_category, '') || ' ' ||
                        COALESCE(subcategory, '') || ' ' ||
                        COALESCE(review_reason, '')
                    ) LIKE :needle
                    """);
            params.addValue("needle", "%" + query.query().toLowerCase() + "%");
        }
        return String.join(" AND ", clauses);
    }

    private void ensureReport(int year) {
        if (!reports.existsById(year)) {
            throw new ReportNotFoundException(year);
        }
    }

    private BudgetTransactionEntity toEntity(int year, NormalizedTransaction tx) {
        var entity = new BudgetTransactionEntity();
        entity.setReportYear(year);
        entity.setLp(tx.lp());
        entity.setPostedDate(tx.date());
        entity.setMonthKey(tx.month());
        entity.setMerchant(tx.merchant());
        entity.setDescription(tx.description());
        entity.setAccount(tx.account());
        entity.setBankCategory(tx.bankCategory());
        entity.setCategoryId(tx.categoryId());
        entity.setCorrectedCategory(tx.correctedCategory());
        entity.setSubcategoryId(tx.subcategoryId());
        entity.setBudgetArea(tx.budgetArea());
        entity.setBudgetGroup(tx.group());
        entity.setSubcategory(tx.subcategory());
        entity.setFlowType(tx.flowType());
        entity.setBudgetGroupId(tx.budgetGroupId());
        entity.setBudgetGroupLabel(tx.budgetGroup());
        entity.setReviewStatus(tx.reviewStatus());
        entity.setReviewReason(tx.reviewReason());
        entity.setBudgetBucket(tx.budgetBucket());
        entity.setFixedness(tx.fixedness());
        entity.setTransactionType(tx.type());
        entity.setAmount(tx.amount());
        entity.setIncome(tx.income());
        entity.setAnalysisSpend(tx.analysisSpend());
        entity.setDiscretionary(tx.discretionary());
        entity.setExcluded(tx.excluded());
        entity.setExcludedOutgoing(tx.excludedOutgoing());
        entity.setExcludedIncoming(tx.excludedIncoming());
        entity.setExcludedNet(tx.excludedNet());
        entity.setConfidence(tx.confidence());
        entity.setNotes(tx.notes());
        entity.setMatchedRule(tx.matchedRule());
        return entity;
    }

    private BudgetSnapshot.Kpis mapKpis(ResultSet rs, int rowNum) throws SQLException {
        return new BudgetSnapshot.Kpis(
                rs.getBigDecimal("income"),
                rs.getBigDecimal("spend"),
                rs.getBigDecimal("discretionary"),
                rs.getBigDecimal("operating_surplus"),
                rs.getBigDecimal("savings_rate"),
                rs.getBigDecimal("excluded_gross"),
                rs.getBigDecimal("excluded_outgoing"),
                rs.getBigDecimal("excluded_incoming"),
                rs.getBigDecimal("excluded_net"),
                rs.getBigDecimal("real_savings_outgoing"),
                rs.getBigDecimal("unassigned_surplus"),
                rs.getInt("transactions"),
                rs.getInt("corrections"),
                rs.getInt("low_confidence"),
                rs.getInt("to_check"),
                rs.getBigDecimal("to_check_amount")
        );
    }

    private BudgetSnapshot.MonthlySummary mapMonthly(ResultSet rs, int rowNum) throws SQLException {
        return new BudgetSnapshot.MonthlySummary(
                rs.getString("month_label"),
                rs.getString("month_key"),
                rs.getBigDecimal("income"),
                rs.getBigDecimal("spend"),
                rs.getBigDecimal("discretionary"),
                rs.getBigDecimal("excluded"),
                rs.getBigDecimal("savings_investments"),
                rs.getBigDecimal("net_flow"),
                rs.getBigDecimal("savings_rate"),
                rs.getInt("transactions"),
                rs.getBigDecimal("spend_per_day")
        );
    }

    private BudgetSnapshot.CategorySummary mapCategory(ResultSet rs, int rowNum) throws SQLException {
        return new BudgetSnapshot.CategorySummary(
                rs.getString("category"),
                rs.getString("budget_group"),
                rs.getBigDecimal("spend"),
                rs.getBigDecimal("income"),
                rs.getBigDecimal("excluded"),
                rs.getBigDecimal("monthly_average"),
                rs.getString("max_month"),
                rs.getBigDecimal("max_amount"),
                rs.getBoolean("discretionary"),
                rs.getInt("transaction_count"),
                decodeExamples(rs.getString("merchant_examples"))
        );
    }

    private BudgetSnapshot.HierarchySummary mapHierarchy(ResultSet rs, int rowNum) throws SQLException {
        return new BudgetSnapshot.HierarchySummary(
                rs.getString("budget_area"),
                rs.getString("budget_group"),
                rs.getString("category"),
                rs.getString("subcategory"),
                rs.getBigDecimal("spend"),
                rs.getBigDecimal("income"),
                rs.getBigDecimal("excluded"),
                rs.getBigDecimal("monthly_average"),
                rs.getInt("transaction_count"),
                rs.getBoolean("discretionary")
        );
    }

    private BudgetSnapshot.BudgetMixItem mapBudgetMix(ResultSet rs, int rowNum) throws SQLException {
        return new BudgetSnapshot.BudgetMixItem(
                rs.getString("bucket"),
                rs.getBigDecimal("total_amount"),
                rs.getBigDecimal("monthly_average"),
                rs.getBigDecimal("income_share"),
                rs.getString("note")
        );
    }

    private BudgetSnapshot.SavingsPlan mapSavingsPlanWithoutLimits(ResultSet rs, int rowNum) throws SQLException {
        return new BudgetSnapshot.SavingsPlan(
                rs.getBigDecimal("current_monthly_spend"),
                rs.getBigDecimal("current_monthly_income"),
                rs.getBigDecimal("core_monthly_cost"),
                rs.getBigDecimal("target_monthly_spend"),
                rs.getBigDecimal("aggressive_monthly_spend"),
                rs.getBigDecimal("target_investment_transfer"),
                rs.getBigDecimal("aggressive_investment_transfer"),
                rs.getBigDecimal("monthly_cut_needed"),
                rs.getBigDecimal("emergency_fund_min"),
                rs.getBigDecimal("emergency_fund_comfort"),
                List.of(),
                List.of()
        );
    }

    private BudgetSnapshot.CategoryLimit mapCategoryLimit(ResultSet rs, int rowNum) throws SQLException {
        return new BudgetSnapshot.CategoryLimit(
                rs.getString("limit_scope"),
                rs.getString("limit_name"),
                rs.getBoolean("is_parent"),
                rs.getString("category"),
                rs.getString("bucket"),
                rs.getBigDecimal("current_monthly"),
                rs.getBigDecimal("limit_amount"),
                rs.getBigDecimal("potential_monthly"),
                rs.getBigDecimal("potential_yearly"),
                rs.getString("priority"),
                rs.getString("action")
        );
    }

    private BudgetSnapshot.MonthControl mapMonthControlWithoutChildren(ResultSet rs, int rowNum) throws SQLException {
        return new BudgetSnapshot.MonthControl(
                rs.getString("month_label"),
                rs.getString("month_key"),
                rs.getInt("elapsed_days"),
                rs.getInt("remaining_days"),
                rs.getInt("days_in_month"),
                rs.getBigDecimal("income_to_date"),
                rs.getBigDecimal("spend_to_date"),
                rs.getBigDecimal("projected_spend"),
                rs.getBigDecimal("target_spend"),
                rs.getBigDecimal("remaining_budget"),
                rs.getBigDecimal("daily_allowed"),
                rs.getBigDecimal("projected_delta"),
                List.of(),
                List.of(),
                List.of()
        );
    }

    private BudgetSnapshot.CategoryStatus mapCategoryStatus(ResultSet rs, int rowNum) throws SQLException {
        return new BudgetSnapshot.CategoryStatus(
                rs.getString("category"),
                rs.getString("bucket"),
                rs.getBigDecimal("current_monthly"),
                rs.getBigDecimal("limit_amount"),
                rs.getBigDecimal("potential_monthly"),
                rs.getBigDecimal("potential_yearly"),
                rs.getString("priority"),
                rs.getString("action"),
                rs.getBigDecimal("current_month_spend"),
                rs.getBigDecimal("current_month_projection"),
                rs.getBigDecimal("remaining_this_month"),
                rs.getBigDecimal("projected_delta"),
                rs.getBigDecimal("usage")
        );
    }

    private BudgetSnapshot.Alert mapAlert(ResultSet rs, int rowNum) throws SQLException {
        return new BudgetSnapshot.Alert(rs.getString("alert_type"), rs.getString("severity"), rs.getString("message"));
    }

    private BudgetSnapshot.SinkingFund mapSinkingFund(ResultSet rs, int rowNum) throws SQLException {
        return new BudgetSnapshot.SinkingFund(
                rs.getString("name"),
                rs.getString("category"),
                rs.getBigDecimal("monthly_set_aside"),
                rs.getBigDecimal("yearly_need"),
                rs.getString("note")
        );
    }

    private BudgetSnapshot.FixednessSummary mapFixedness(ResultSet rs, int rowNum) throws SQLException {
        return new BudgetSnapshot.FixednessSummary(
                rs.getString("fixedness_type"),
                rs.getBigDecimal("spend"),
                rs.getBigDecimal("excluded_outgoing"),
                rs.getBigDecimal("monthly_average"),
                rs.getInt("transaction_count")
        );
    }

    private BudgetSnapshot.MerchantSummary mapMerchant(ResultSet rs, int rowNum) throws SQLException {
        return new BudgetSnapshot.MerchantSummary(
                rs.getString("merchant"),
                rs.getString("category"),
                rs.getBigDecimal("total_amount"),
                rs.getInt("transaction_count"),
                rs.getBigDecimal("average_amount")
        );
    }

    private BudgetSnapshot.RecurringItem mapRecurring(ResultSet rs, int rowNum) throws SQLException {
        return new BudgetSnapshot.RecurringItem(
                rs.getString("merchant"),
                rs.getString("category"),
                rs.getString("bucket"),
                rs.getBigDecimal("total_amount"),
                rs.getInt("months"),
                rs.getInt("transaction_count"),
                rs.getBigDecimal("monthly_average"),
                rs.getInt("avg_day"),
                rs.getObject("last_date", LocalDate.class)
        );
    }

    private BudgetSnapshot.LargeOneOff mapLargeOneOff(ResultSet rs, int rowNum) throws SQLException {
        return new BudgetSnapshot.LargeOneOff(
                rs.getObject("posted_date", LocalDate.class),
                rs.getString("merchant"),
                rs.getString("category"),
                rs.getString("bucket"),
                rs.getBigDecimal("amount"),
                rs.getString("month_key"),
                rs.getString("confidence"),
                rs.getString("description")
        );
    }

    private TransactionRecord mapTransaction(ResultSet rs, int rowNum) throws SQLException {
        return new TransactionRecord(
                rs.getLong("id"),
                rs.getInt("lp"),
                rs.getObject("posted_date", LocalDate.class),
                rs.getString("month_key"),
                rs.getString("merchant"),
                rs.getString("description"),
                rs.getString("account"),
                rs.getString("bank_category"),
                rs.getString("category_id"),
                rs.getString("corrected_category"),
                rs.getString("subcategory_id"),
                rs.getString("budget_area"),
                rs.getString("budget_group"),
                rs.getString("subcategory"),
                rs.getString("flow_type"),
                rs.getString("budget_group_id"),
                rs.getString("budget_group_label"),
                rs.getString("review_status"),
                rs.getString("review_reason"),
                rs.getString("budget_bucket"),
                rs.getString("fixedness"),
                rs.getString("transaction_type"),
                rs.getBigDecimal("amount"),
                rs.getBigDecimal("income"),
                rs.getBigDecimal("analysis_spend"),
                rs.getBigDecimal("discretionary"),
                rs.getBigDecimal("excluded"),
                rs.getBigDecimal("excluded_outgoing"),
                rs.getBigDecimal("excluded_incoming"),
                rs.getBigDecimal("excluded_net"),
                rs.getString("confidence"),
                rs.getString("notes"),
                rs.getString("matched_rule")
        );
    }

    private <T> List<T> query(String sql, MapSqlParameterSource params, RowMapper<T> mapper) {
        return jdbc.query(sql, params, mapper);
    }

    private <T> T queryOne(String sql, MapSqlParameterSource params, RowMapper<T> mapper) {
        try {
            return jdbc.queryForObject(sql, params, mapper);
        } catch (EmptyResultDataAccessException e) {
            throw new IllegalStateException("Stored budget snapshot is incomplete", e);
        }
    }

    private void update(String sql, MapSqlParameterSource params) {
        jdbc.update(sql, params);
    }

    private BigDecimal decimal(String sql, MapSqlParameterSource params) {
        var value = jdbc.queryForObject(sql, params, BigDecimal.class);
        return value == null ? BigDecimal.ZERO : value;
    }

    private MapSqlParameterSource params(int year) {
        return new MapSqlParameterSource("year", year);
    }

    private <T> List<T> aggregate(List<TransactionRecord> rows, Function<TransactionRecord, String> classifier, TriFactory<T> factory) {
        var totals = new LinkedHashMap<String, Aggregate>();
        for (var row : rows) {
            if (row.spend().signum() <= 0) {
                continue;
            }
            var key = classifier.apply(row);
            var agg = totals.computeIfAbsent(key == null || key.isBlank() ? "Inne" : key, ignored -> new Aggregate());
            agg.spend = agg.spend.add(row.spend());
            agg.count++;
        }
        return totals.entrySet().stream()
                .map(entry -> factory.create(entry.getKey(), entry.getValue().spend, entry.getValue().count))
                .sorted((left, right) -> extractSpend(right).compareTo(extractSpend(left)))
                .limit(50)
                .toList();
    }

    private List<AnalyticsReport.SubcategorySpend> aggregateSubcategories(List<TransactionRecord> rows) {
        var totals = new LinkedHashMap<String, SubcategoryAggregate>();
        for (var row : rows) {
            if (row.spend().signum() <= 0) {
                continue;
            }
            if (row.subcategory() == null || row.subcategory().isBlank()) {
                continue;
            }
            var key = row.correctedCategory() + " · " + row.subcategory();
            var agg = totals.computeIfAbsent(key, ignored -> new SubcategoryAggregate(row.correctedCategory(), row.subcategory()));
            agg.spend = agg.spend.add(row.spend());
            agg.count++;
        }
        return totals.values().stream()
                .map(agg -> new AnalyticsReport.SubcategorySpend(agg.category + " · " + agg.subcategory, agg.category, agg.spend, agg.count))
                .sorted(Comparator.comparing(AnalyticsReport.SubcategorySpend::spend).reversed())
                .limit(50)
                .toList();
    }

    private List<AnalyticsReport.HierarchySpend> aggregateHierarchy(List<TransactionRecord> rows) {
        var totals = new LinkedHashMap<String, HierarchyAggregate>();
        for (var row : rows) {
            if (row.spend().signum() <= 0) {
                continue;
            }
            var key = row.area() + "\u001F" + row.group() + "\u001F" + row.correctedCategory() + "\u001F" + row.subcategory();
            var agg = totals.computeIfAbsent(key, ignored -> new HierarchyAggregate(row.area(), row.group(), row.correctedCategory(), row.subcategory()));
            agg.spend = agg.spend.add(row.spend());
            agg.count++;
        }
        return totals.values().stream()
                .map(agg -> new AnalyticsReport.HierarchySpend(agg.area, agg.group, agg.category, agg.subcategory, agg.spend, agg.count))
                .sorted(Comparator.comparing(AnalyticsReport.HierarchySpend::spend).reversed())
                .limit(80)
                .toList();
    }

    private List<AnalyticsReport.FinancialFlow> financialFlows(List<TransactionRecord> rows) {
        var totals = new LinkedHashMap<String, FinancialFlowAggregate>();
        for (var row : rows) {
            if (!FINANCIAL_FLOW_CATEGORIES.contains(row.correctedCategory()) || row.excludedOutgoing().signum() <= 0) {
                continue;
            }
            var agg = totals.computeIfAbsent(row.correctedCategory(), ignored -> new FinancialFlowAggregate());
            agg.outgoing = agg.outgoing.add(row.excludedOutgoing());
            agg.count++;
        }
        return totals.entrySet().stream()
                .map(entry -> new AnalyticsReport.FinancialFlow(entry.getKey(), entry.getValue().outgoing, entry.getValue().count))
                .sorted(Comparator.comparing(AnalyticsReport.FinancialFlow::outgoing).reversed())
                .toList();
    }

    private List<AnalyticsReport.MonthlyCategoryTrend> monthlyCategoryTrends(List<TransactionRecord> rows) {
        var totals = new LinkedHashMap<String, MonthlyDimensionAggregate>();
        for (var row : rows) {
            if (row.spend().signum() <= 0) {
                continue;
            }
            var category = normalizedLabel(row.correctedCategory());
            var key = row.month() + "\u001F" + category;
            var agg = totals.computeIfAbsent(key, ignored -> new MonthlyDimensionAggregate(row.month(), category));
            agg.spend = agg.spend.add(row.spend());
            agg.count++;
        }
        return totals.values().stream()
                .map(agg -> new AnalyticsReport.MonthlyCategoryTrend(monthLabel(agg.monthKey), agg.monthKey, agg.dimension, agg.spend, agg.count))
                .sorted(Comparator.comparing(AnalyticsReport.MonthlyCategoryTrend::monthKey)
                        .thenComparing(AnalyticsReport.MonthlyCategoryTrend::spend, Comparator.reverseOrder()))
                .toList();
    }

    private List<AnalyticsReport.MonthlyHierarchyTrend> monthlyHierarchyTrends(List<TransactionRecord> rows) {
        var totals = new LinkedHashMap<String, MonthlyHierarchyAggregate>();
        for (var row : rows) {
            if (row.spend().signum() <= 0) {
                continue;
            }
            var category = normalizedLabel(row.correctedCategory());
            var subcategory = visibleSubcategory(row.subcategory());
            var key = row.month() + "\u001F" + category + "\u001F" + subcategory;
            var agg = totals.computeIfAbsent(key, ignored -> new MonthlyHierarchyAggregate(row.month(), category, subcategory));
            agg.spend = agg.spend.add(row.spend());
            agg.count++;
        }
        return totals.values().stream()
                .map(agg -> new AnalyticsReport.MonthlyHierarchyTrend(monthLabel(agg.monthKey), agg.monthKey, agg.category, agg.subcategory, agg.spend, agg.count))
                .sorted(Comparator.comparing(AnalyticsReport.MonthlyHierarchyTrend::monthKey)
                        .thenComparing(AnalyticsReport.MonthlyHierarchyTrend::category)
                        .thenComparing(AnalyticsReport.MonthlyHierarchyTrend::subcategory))
                .toList();
    }

    private List<AnalyticsReport.MonthlyBucketTrend> monthlyBucketTrends(List<TransactionRecord> rows) {
        var totals = new LinkedHashMap<String, MonthlyDimensionAggregate>();
        for (var row : rows) {
            if (row.spend().signum() <= 0) {
                continue;
            }
            var bucket = normalizedLabel(row.bucket());
            var key = row.month() + "\u001F" + bucket;
            var agg = totals.computeIfAbsent(key, ignored -> new MonthlyDimensionAggregate(row.month(), bucket));
            agg.spend = agg.spend.add(row.spend());
            agg.count++;
        }
        return totals.values().stream()
                .map(agg -> new AnalyticsReport.MonthlyBucketTrend(monthLabel(agg.monthKey), agg.monthKey, agg.dimension, agg.spend, agg.count))
                .sorted(Comparator.comparing(AnalyticsReport.MonthlyBucketTrend::monthKey)
                        .thenComparing(AnalyticsReport.MonthlyBucketTrend::spend, Comparator.reverseOrder()))
                .toList();
    }

    private List<AnalyticsReport.MonthlyMerchantTrend> monthlyMerchantTrends(List<TransactionRecord> rows) {
        var topMerchants = rows.stream()
                .filter(row -> row.spend().signum() > 0)
                .collect(java.util.stream.Collectors.groupingBy(
                        row -> normalizedLabel(row.merchant()),
                        java.util.stream.Collectors.reducing(BigDecimal.ZERO, TransactionRecord::spend, BigDecimal::add)
                ))
                .entrySet().stream()
                .sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed())
                .limit(10)
                .map(Map.Entry::getKey)
                .collect(java.util.stream.Collectors.toSet());
        var totals = new LinkedHashMap<String, MonthlyDimensionAggregate>();
        for (var row : rows) {
            if (row.spend().signum() <= 0) {
                continue;
            }
            var merchant = normalizedLabel(row.merchant());
            if (!topMerchants.contains(merchant)) {
                continue;
            }
            var key = row.month() + "\u001F" + merchant;
            var agg = totals.computeIfAbsent(key, ignored -> new MonthlyDimensionAggregate(row.month(), merchant));
            agg.spend = agg.spend.add(row.spend());
            agg.count++;
        }
        return totals.values().stream()
                .map(agg -> new AnalyticsReport.MonthlyMerchantTrend(monthLabel(agg.monthKey), agg.monthKey, agg.dimension, agg.spend, agg.count))
                .sorted(Comparator.comparing(AnalyticsReport.MonthlyMerchantTrend::monthKey)
                        .thenComparing(AnalyticsReport.MonthlyMerchantTrend::spend, Comparator.reverseOrder()))
                .toList();
    }

    private List<AnalyticsReport.FixednessBreakdown> fixednessBreakdown(List<TransactionRecord> rows) {
        var totals = new LinkedHashMap<String, Aggregate>();
        for (var row : rows) {
            if (row.spend().signum() <= 0) {
                continue;
            }
            var agg = totals.computeIfAbsent(normalizedLabel(row.fixedness()), ignored -> new Aggregate());
            agg.spend = agg.spend.add(row.spend());
            agg.count++;
        }
        return totals.entrySet().stream()
                .map(entry -> new AnalyticsReport.FixednessBreakdown(entry.getKey(), entry.getValue().spend, entry.getValue().count))
                .sorted(Comparator.comparing(AnalyticsReport.FixednessBreakdown::spend).reversed())
                .toList();
    }

    private List<AnalyticsReport.ConfidenceBreakdown> confidenceBreakdown(List<TransactionRecord> rows) {
        var totals = new LinkedHashMap<String, ConfidenceAggregate>();
        for (var row : rows) {
            var agg = totals.computeIfAbsent(normalizedLabel(row.confidence()), ignored -> new ConfidenceAggregate());
            agg.spend = agg.spend.add(row.spend());
            agg.income = agg.income.add(row.income());
            agg.excluded = agg.excluded.add(row.excluded());
            agg.count++;
        }
        return totals.entrySet().stream()
                .map(entry -> new AnalyticsReport.ConfidenceBreakdown(
                        entry.getKey(),
                        entry.getValue().count,
                        entry.getValue().spend,
                        entry.getValue().income,
                        entry.getValue().excluded
                ))
                .sorted(Comparator.comparing(AnalyticsReport.ConfidenceBreakdown::count).reversed())
                .toList();
    }

    private List<AnalyticsReport.AmountBand> amountBands(List<TransactionRecord> rows) {
        var bands = List.of(
                new AmountBandDefinition("0-50", BigDecimal.ZERO, BigDecimal.valueOf(50)),
                new AmountBandDefinition("50-100", BigDecimal.valueOf(50), BigDecimal.valueOf(100)),
                new AmountBandDefinition("100-250", BigDecimal.valueOf(100), BigDecimal.valueOf(250)),
                new AmountBandDefinition("250-500", BigDecimal.valueOf(250), BigDecimal.valueOf(500)),
                new AmountBandDefinition("500-1000", BigDecimal.valueOf(500), BigDecimal.valueOf(1000)),
                new AmountBandDefinition("1000+", BigDecimal.valueOf(1000), null)
        );
        var totals = new LinkedHashMap<String, AmountBandAggregate>();
        bands.forEach(band -> totals.put(band.label, new AmountBandAggregate(band)));
        for (var row : rows) {
            if (row.spend().signum() <= 0) {
                continue;
            }
            var band = bands.stream()
                    .filter(candidate -> candidate.contains(row.spend()))
                    .findFirst()
                    .orElseThrow();
            var agg = totals.get(band.label);
            agg.spend = agg.spend.add(row.spend());
            agg.count++;
        }
        return totals.values().stream()
                .map(agg -> new AnalyticsReport.AmountBand(agg.definition.label, agg.definition.minAmount, agg.definition.maxAmount, agg.count, agg.spend))
                .toList();
    }

    private String normalizedLabel(String value) {
        return value == null || value.isBlank() ? "Inne" : value;
    }

    private String visibleSubcategory(String value) {
        return value == null || value.isBlank() || "Ogólne".equals(value) ? "" : value;
    }

    private String monthLabel(String monthKey) {
        return monthKey == null || monthKey.length() != 7 ? monthKey : monthKey.substring(5, 7) + "." + monthKey.substring(0, 4);
    }

    private String encodeExamples(List<String> examples) {
        return examples == null ? "" : String.join("\n", examples);
    }

    private List<String> decodeExamples(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return value.lines()
                .map(String::trim)
                .filter(line -> !line.isBlank())
                .toList();
    }

    private BigDecimal extractSpend(Object row) {
        return switch (row) {
            case AnalyticsReport.AreaSpend area -> area.spend();
            case AnalyticsReport.GroupSpend group -> group.spend();
            case AnalyticsReport.CategorySpend category -> category.spend();
            case AnalyticsReport.MerchantSpend merchant -> merchant.sum();
            default -> BigDecimal.ZERO;
        };
    }

    @FunctionalInterface
    private interface TriFactory<T> {
        T create(String name, BigDecimal spend, int count);
    }

    private static final class Aggregate {
        private BigDecimal spend = BigDecimal.ZERO;
        private int count;
    }

    private static final class SubcategoryAggregate {
        private final String category;
        private final String subcategory;
        private BigDecimal spend = BigDecimal.ZERO;
        private int count;

        private SubcategoryAggregate(String category, String subcategory) {
            this.category = category;
            this.subcategory = subcategory;
        }
    }

    private static final class HierarchyAggregate {
        private final String area;
        private final String group;
        private final String category;
        private final String subcategory;
        private BigDecimal spend = BigDecimal.ZERO;
        private int count;

        private HierarchyAggregate(String area, String group, String category, String subcategory) {
            this.area = area;
            this.group = group;
            this.category = category;
            this.subcategory = subcategory;
        }
    }

    private static final class FinancialFlowAggregate {
        private BigDecimal outgoing = BigDecimal.ZERO;
        private int count;
    }

    private static final class MonthlyDimensionAggregate {
        private final String monthKey;
        private final String dimension;
        private BigDecimal spend = BigDecimal.ZERO;
        private int count;

        private MonthlyDimensionAggregate(String monthKey, String dimension) {
            this.monthKey = monthKey;
            this.dimension = dimension;
        }
    }

    private static final class MonthlyHierarchyAggregate {
        private final String monthKey;
        private final String category;
        private final String subcategory;
        private BigDecimal spend = BigDecimal.ZERO;
        private int count;

        private MonthlyHierarchyAggregate(String monthKey, String category, String subcategory) {
            this.monthKey = monthKey;
            this.category = category;
            this.subcategory = subcategory;
        }
    }

    private static final class ConfidenceAggregate {
        private BigDecimal spend = BigDecimal.ZERO;
        private BigDecimal income = BigDecimal.ZERO;
        private BigDecimal excluded = BigDecimal.ZERO;
        private int count;
    }

    private record AmountBandDefinition(String label, BigDecimal minAmount, BigDecimal maxAmount) {
        private boolean contains(BigDecimal amount) {
            return amount.compareTo(minAmount) >= 0 && (maxAmount == null || amount.compareTo(maxAmount) < 0);
        }
    }

    private static final class AmountBandAggregate {
        private final AmountBandDefinition definition;
        private BigDecimal spend = BigDecimal.ZERO;
        private int count;

        private AmountBandAggregate(AmountBandDefinition definition) {
            this.definition = definition;
        }
    }
}
