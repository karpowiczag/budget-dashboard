package com.budget.web.dto;

import com.budget.application.importing.ImportSummary;
import com.budget.application.reporting.AnalyticsReport;
import com.budget.application.reporting.CalendarReport;
import com.budget.application.reporting.TransactionPage;
import com.budget.application.reporting.TransactionRecord;
import com.budget.application.reporting.YearSummary;
import com.budget.application.settings.BudgetSettings;
import com.budget.domain.importjob.ImportRun;
import com.budget.domain.report.BudgetSnapshot;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class BudgetApiMapper {
    public BudgetApiDtos.YearSummaryResponse toYearSummary(YearSummary row) {
        return new BudgetApiDtos.YearSummaryResponse(
                row.year(),
                row.importedAt(),
                row.transactions(),
                row.income(),
                row.spend(),
                row.inputCsv()
        );
    }

    public BudgetApiDtos.DashboardResponse toDashboard(BudgetSnapshot snapshot) {
        return new BudgetApiDtos.DashboardResponse(
                snapshot.year(),
                snapshot.period(),
                snapshot.periodStart(),
                snapshot.periodEnd(),
                snapshot.activeMonths(),
                toKpis(snapshot.kpis()),
                map(snapshot.monthly(), this::toMonthly),
                map(snapshot.categories(), this::toCategory),
                map(snapshot.hierarchy(), this::toHierarchy),
                map(snapshot.budgetMix(), this::toBudgetMix),
                toSavingsPlan(snapshot.savingsPlan()),
                toMonthControl(snapshot.monthControl()),
                map(snapshot.fixedness(), this::toFixedness),
                map(snapshot.topMerchants(), this::toMerchant),
                map(snapshot.recurring(), this::toRecurring),
                map(snapshot.largeOneoffs(), this::toLargeOneOff)
        );
    }

    public BudgetApiDtos.CalendarResponse toCalendar(CalendarReport report) {
        return new BudgetApiDtos.CalendarResponse(
                report.year(),
                report.month(),
                map(report.days(), day -> new BudgetApiDtos.CalendarDayResponse(
                        day.date(),
                        day.day(),
                        day.spend(),
                        day.income(),
                        day.transactions(),
                        day.biggest() == null ? null : toTransaction(day.biggest())
                ))
        );
    }

    public BudgetApiDtos.AnalyticsResponse toAnalytics(AnalyticsReport report) {
        return new BudgetApiDtos.AnalyticsResponse(
                report.year(),
                report.scope(),
                report.month(),
                report.date(),
                report.spend(),
                report.income(),
                report.transactionCount(),
                map(report.areaTop(), row -> new BudgetApiDtos.AreaSpendResponse(row.area(), row.spend(), row.count())),
                map(report.groupTop(), row -> new BudgetApiDtos.GroupSpendResponse(row.group(), row.spend(), row.count())),
                map(report.categoryTop(), row -> new BudgetApiDtos.CategorySpendResponse(row.category(), row.spend(), row.count())),
                map(report.subcategoryTop(), row -> new BudgetApiDtos.SubcategorySpendResponse(row.subcategory(), row.category(), row.spend(), row.count())),
                map(report.hierarchyTop(), row -> new BudgetApiDtos.HierarchySpendResponse(row.area(), row.group(), row.category(), row.subcategory(), row.spend(), row.count())),
                map(report.financialFlows(), row -> new BudgetApiDtos.FinancialFlowResponse(row.category(), row.outgoing(), row.count())),
                map(report.merchants(), row -> new BudgetApiDtos.AnalyticsMerchantSpendResponse(row.merchant(), row.sum(), row.count())),
                map(report.oneoffs(), this::toTransaction),
                map(report.monthlyCategoryTrends(), row -> new BudgetApiDtos.MonthlyCategoryTrendResponse(row.month(), row.monthKey(), row.category(), row.spend(), row.count())),
                map(report.monthlyBucketTrends(), row -> new BudgetApiDtos.MonthlyBucketTrendResponse(row.month(), row.monthKey(), row.bucket(), row.spend(), row.count())),
                map(report.monthlyMerchantTrends(), row -> new BudgetApiDtos.MonthlyMerchantTrendResponse(row.month(), row.monthKey(), row.merchant(), row.spend(), row.count())),
                map(report.fixednessBreakdown(), row -> new BudgetApiDtos.FixednessBreakdownResponse(row.fixedness(), row.spend(), row.count())),
                map(report.confidenceBreakdown(), row -> new BudgetApiDtos.ConfidenceBreakdownResponse(row.confidence(), row.count(), row.spend(), row.income(), row.excluded())),
                map(report.amountBands(), row -> new BudgetApiDtos.AmountBandResponse(row.label(), row.minAmount(), row.maxAmount(), row.count(), row.spend()))
        );
    }

    public BudgetApiDtos.TransactionPageResponse toTransactionPage(TransactionPage page) {
        return new BudgetApiDtos.TransactionPageResponse(
                map(page.items(), this::toTransaction),
                page.page(),
                page.size(),
                page.totalItems(),
                page.totalPages(),
                page.sort()
        );
    }

    public BudgetApiDtos.TransactionResponse toTransaction(TransactionRecord row) {
        return new BudgetApiDtos.TransactionResponse(
                row.id(),
                row.lp(),
                row.postedDate(),
                row.month(),
                row.merchant(),
                row.description(),
                row.account(),
                row.bankCategory(),
                row.correctedCategory(),
                row.area(),
                row.group(),
                row.subcategory(),
                row.bucket(),
                row.fixedness(),
                row.type(),
                row.amount(),
                row.income(),
                row.spend(),
                row.discretionary(),
                row.excluded(),
                row.excludedOutgoing(),
                row.excludedIncoming(),
                row.excludedNet(),
                row.confidence(),
                row.notes(),
                row.matchedRule()
        );
    }

    public BudgetApiDtos.ImportSummaryResponse toImportSummary(ImportSummary summary) {
        return new BudgetApiDtos.ImportSummaryResponse(
                summary.status(),
                summary.years(),
                summary.transactions(),
                summary.duplicatesRemoved(),
                summary.income(),
                summary.spend(),
                summary.message()
        );
    }

    public BudgetApiDtos.ImportRunResponse toImportRun(ImportRun run) {
        return new BudgetApiDtos.ImportRunResponse(
                run.id(),
                run.year(),
                run.inputCsv(),
                run.status(),
                run.message(),
                run.duplicatesRemoved(),
                run.createdAt()
        );
    }

    public BudgetApiDtos.BudgetSettingsDto toBudgetSettings(BudgetSettings settings) {
        return new BudgetApiDtos.BudgetSettingsDto(
                settings.targetMonthlySpend(),
                settings.aggressiveMonthlySpend(),
                settings.emergencyFundMinMonths(),
                settings.emergencyFundComfortMonths(),
                map(settings.categoryLimits(), row -> new BudgetApiDtos.CategoryLimitSettingDto(row.scope(), row.name(), row.category(), row.limit(), row.action(), row.bucketOverride()))
        );
    }

    public BudgetSettings toBudgetSettings(BudgetApiDtos.BudgetSettingsDto dto) {
        if (dto == null) {
            return null;
        }
        return new BudgetSettings(
                dto.targetMonthlySpend(),
                dto.aggressiveMonthlySpend(),
                dto.emergencyFundMinMonths(),
                dto.emergencyFundComfortMonths(),
                map(dto.categoryLimits(), row -> new BudgetSettings.CategoryLimitSetting(row.scope(), row.name(), row.category(), row.limit(), row.action(), row.bucketOverride()))
        );
    }

    private BudgetApiDtos.KpisResponse toKpis(BudgetSnapshot.Kpis row) {
        return new BudgetApiDtos.KpisResponse(
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
                row.savingsAccountNetChange(),
                row.savingsAccountGrossDeposits(),
                row.savingsAccountInflows(),
                row.savingsAccountOutflows()
        );
    }

    private BudgetApiDtos.MonthlySummaryResponse toMonthly(BudgetSnapshot.MonthlySummary row) {
        return new BudgetApiDtos.MonthlySummaryResponse(
                row.month(),
                row.monthKey(),
                row.income(),
                row.spend(),
                row.discretionary(),
                row.excluded(),
                row.savingsInvestments(),
                row.netFlow(),
                row.savingsRate(),
                row.transactions(),
                row.spendPerDay()
        );
    }

    private BudgetApiDtos.CategorySummaryResponse toCategory(BudgetSnapshot.CategorySummary row) {
        return new BudgetApiDtos.CategorySummaryResponse(
                row.category(),
                row.group(),
                row.spend(),
                row.income(),
                row.excluded(),
                row.monthlyAverage(),
                row.maxMonth(),
                row.maxAmount(),
                row.discretionary(),
                row.count(),
                row.merchantExamples()
        );
    }

    private BudgetApiDtos.HierarchySummaryResponse toHierarchy(BudgetSnapshot.HierarchySummary row) {
        return new BudgetApiDtos.HierarchySummaryResponse(
                row.area(),
                row.group(),
                row.category(),
                row.subcategory(),
                row.spend(),
                row.income(),
                row.excluded(),
                row.monthlyAverage(),
                row.count(),
                row.discretionary()
        );
    }

    private BudgetApiDtos.BudgetMixItemResponse toBudgetMix(BudgetSnapshot.BudgetMixItem row) {
        return new BudgetApiDtos.BudgetMixItemResponse(row.bucket(), row.sum(), row.monthlyAverage(), row.incomeShare(), row.note());
    }

    private BudgetApiDtos.SavingsPlanResponse toSavingsPlan(BudgetSnapshot.SavingsPlan row) {
        return new BudgetApiDtos.SavingsPlanResponse(
                row.currentMonthlySpend(),
                row.currentMonthlyIncome(),
                row.coreMonthlyCost(),
                row.targetMonthlySpend(),
                row.aggressiveMonthlySpend(),
                row.targetInvestmentTransfer(),
                row.aggressiveInvestmentTransfer(),
                row.monthlyCutNeeded(),
                row.emergencyFundMin(),
                row.emergencyFundComfort(),
                map(row.parentLimits(), this::toCategoryLimit),
                map(row.categoryLimits(), this::toCategoryLimit)
        );
    }

    private BudgetApiDtos.CategoryLimitResponse toCategoryLimit(BudgetSnapshot.CategoryLimit row) {
        return new BudgetApiDtos.CategoryLimitResponse(
                row.scope(),
                row.name(),
                row.parent(),
                row.category(),
                row.bucket(),
                row.currentMonthly(),
                row.limit(),
                row.potentialMonthly(),
                row.potentialYearly(),
                row.priority(),
                row.action()
        );
    }

    private BudgetApiDtos.MonthControlResponse toMonthControl(BudgetSnapshot.MonthControl row) {
        return new BudgetApiDtos.MonthControlResponse(
                row.month(),
                row.monthKey(),
                row.elapsedDays(),
                row.remainingDays(),
                row.daysInMonth(),
                row.incomeToDate(),
                row.spendToDate(),
                row.projectedSpend(),
                row.targetSpend(),
                row.remainingBudget(),
                row.dailyAllowed(),
                row.projectedDelta(),
                map(row.categoryStatus(), this::toCategoryStatus),
                map(row.alerts(), item -> new BudgetApiDtos.AlertResponse(item.type(), item.severity(), item.message())),
                map(row.sinkingFunds(), item -> new BudgetApiDtos.SinkingFundResponse(item.name(), item.category(), item.monthlySetAside(), item.yearlyNeed(), item.note()))
        );
    }

    private BudgetApiDtos.CategoryStatusResponse toCategoryStatus(BudgetSnapshot.CategoryStatus row) {
        return new BudgetApiDtos.CategoryStatusResponse(
                row.category(),
                row.bucket(),
                row.currentMonthly(),
                row.limit(),
                row.potentialMonthly(),
                row.potentialYearly(),
                row.priority(),
                row.action(),
                row.currentMonthSpend(),
                row.currentMonthProjection(),
                row.remainingThisMonth(),
                row.projectedDelta(),
                row.usage()
        );
    }

    private BudgetApiDtos.FixednessSummaryResponse toFixedness(BudgetSnapshot.FixednessSummary row) {
        return new BudgetApiDtos.FixednessSummaryResponse(row.type(), row.spend(), row.excludedOutgoing(), row.monthlyAverage(), row.count());
    }

    private BudgetApiDtos.MerchantSummaryResponse toMerchant(BudgetSnapshot.MerchantSummary row) {
        return new BudgetApiDtos.MerchantSummaryResponse(row.merchant(), row.category(), row.sum(), row.count(), row.average());
    }

    private BudgetApiDtos.RecurringItemResponse toRecurring(BudgetSnapshot.RecurringItem row) {
        return new BudgetApiDtos.RecurringItemResponse(
                row.merchant(),
                row.category(),
                row.bucket(),
                row.sum(),
                row.months(),
                row.count(),
                row.monthlyAverage(),
                row.avgDay(),
                row.lastDate()
        );
    }

    private BudgetApiDtos.LargeOneOffResponse toLargeOneOff(BudgetSnapshot.LargeOneOff row) {
        return new BudgetApiDtos.LargeOneOffResponse(
                row.date(),
                row.merchant(),
                row.category(),
                row.bucket(),
                row.amount(),
                row.month(),
                row.confidence(),
                row.description()
        );
    }

    private <T, R> List<R> map(List<T> rows, java.util.function.Function<T, R> mapper) {
        return rows == null ? List.of() : rows.stream().map(mapper).toList();
    }
}
