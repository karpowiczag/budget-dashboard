package com.budget.domain.report;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.budget.domain.transaction.BankTransaction;
import com.budget.domain.transaction.NormalizedTransaction;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class DomainRecordImmutabilityTest {
    @Test
    void budgetInputDefensivelyCopiesTransactions() {
        var source = new ArrayList<>(List.of(bankTransaction()));

        var input = new BudgetInput(2026, "fixture.csv", source);
        source.clear();

        assertThat(input.transactions()).hasSize(1);
        assertThatThrownBy(() -> input.transactions().add(bankTransaction()))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void analysisResultDefensivelyCopiesTransactions() {
        var source = new ArrayList<>(List.of(normalizedTransaction()));

        var result = new BudgetAnalysisResult(2026, "fixture.csv", null, source, 1, money(10), money(1));
        source.clear();

        assertThat(result.transactions()).hasSize(1);
        assertThatThrownBy(() -> result.transactions().clear())
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void budgetSnapshotDefensivelyCopiesTopLevelAndNestedLists() {
        var monthly = new ArrayList<>(List.of(monthlySummary()));
        var limits = new ArrayList<>(List.of(categoryLimit()));
        var statuses = new ArrayList<>(List.of(categoryStatus()));

        var snapshot = snapshot(monthly, limits, statuses);
        monthly.clear();
        limits.clear();
        statuses.clear();

        assertThat(snapshot.monthly()).hasSize(1);
        assertThat(snapshot.savingsPlan().categoryLimits()).hasSize(1);
        assertThat(snapshot.monthControl().categoryStatus()).hasSize(1);
        assertThatThrownBy(() -> snapshot.monthly().clear())
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> snapshot.savingsPlan().categoryLimits().clear())
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> snapshot.monthControl().categoryStatus().clear())
                .isInstanceOf(UnsupportedOperationException.class);
    }

    private BudgetSnapshot snapshot(
            List<BudgetSnapshot.MonthlySummary> monthly,
            List<BudgetSnapshot.CategoryLimit> limits,
            List<BudgetSnapshot.CategoryStatus> statuses
    ) {
        return new BudgetSnapshot(
                2026,
                "2026-01-01 - 2026-01-31",
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 1, 31),
                1,
                kpis(),
                monthly,
                List.of(categorySummary()),
                List.of(hierarchySummary()),
                List.of(budgetMixItem()),
                new BudgetSnapshot.SavingsPlan(
                        money(100),
                        money(1000),
                        money(500),
                        money(800),
                        money(700),
                        money(200),
                        money(300),
                        money(0),
                        money(1500),
                        money(3000),
                        limits
                ),
                new BudgetSnapshot.MonthControl(
                        "01.2026",
                        "2026-01",
                        1,
                        30,
                        31,
                        money(1000),
                        money(100),
                        money(3100),
                        money(800),
                        money(700),
                        money(23),
                        money(2300),
                        statuses,
                        List.of(new BudgetSnapshot.Alert("budget", "info", "message")),
                        List.of(new BudgetSnapshot.SinkingFund("Fund", "Category", money(100), money(1200), "note"))
                ),
                List.of(new BudgetSnapshot.FixednessSummary("Variable", money(100), money(0), money(100), 1)),
                List.of(new BudgetSnapshot.MerchantSummary("Merchant", "Category", money(100), 1, money(100))),
                List.of(new BudgetSnapshot.RecurringItem("Merchant", "Category", "Needs", money(100), 1, 1, money(100), 1, LocalDate.of(2026, 1, 1))),
                List.of(new BudgetSnapshot.LargeOneOff(LocalDate.of(2026, 1, 1), "Merchant", "Category", "Needs", money(100), "2026-01", "High", "Description"))
        );
    }

    private BudgetSnapshot.Kpis kpis() {
        return new BudgetSnapshot.Kpis(
                money(1000),
                money(100),
                money(50),
                money(900),
                BigDecimal.valueOf(0.9),
                money(0),
                money(0),
                money(0),
                money(0),
                money(0),
                money(900),
                1,
                0,
                0,
                0,
                money(0)
        );
    }

    private BudgetSnapshot.MonthlySummary monthlySummary() {
        return new BudgetSnapshot.MonthlySummary("01.2026", "2026-01", money(1000), money(100), money(50), money(0), money(0), money(900), BigDecimal.valueOf(0.9), 1, money(3));
    }

    private BudgetSnapshot.CategorySummary categorySummary() {
        return new BudgetSnapshot.CategorySummary("Category", "Group", money(100), money(0), money(0), money(100), "01.2026", money(100), true, 1);
    }

    private BudgetSnapshot.HierarchySummary hierarchySummary() {
        return new BudgetSnapshot.HierarchySummary("Area", "Group", "Category", "Subcategory", money(100), money(0), money(0), money(100), 1, true);
    }

    private BudgetSnapshot.BudgetMixItem budgetMixItem() {
        return new BudgetSnapshot.BudgetMixItem("Needs", money(100), money(100), BigDecimal.valueOf(0.1), "note");
    }

    private BudgetSnapshot.CategoryLimit categoryLimit() {
        return new BudgetSnapshot.CategoryLimit("Category", "Needs", money(100), money(50), money(50), money(600), "high", "action");
    }

    private BudgetSnapshot.CategoryStatus categoryStatus() {
        return new BudgetSnapshot.CategoryStatus("Category", "Needs", money(100), money(50), money(50), money(600), "high", "action", money(10), money(20), money(30), money(-10), BigDecimal.valueOf(0.2));
    }

    private BankTransaction bankTransaction() {
        return new BankTransaction(LocalDate.of(2026, 1, 1), "account", "salary", "", money(10));
    }

    private NormalizedTransaction normalizedTransaction() {
        return new NormalizedTransaction(
                1,
                LocalDate.of(2026, 1, 1),
                "2026-01",
                "Merchant",
                "Description",
                "account",
                "",
                "Category",
                "Area",
                "Group",
                "Subcategory",
                "Needs",
                "Variable",
                "Expense",
                money(-10),
                money(0),
                money(10),
                money(10),
                money(0),
                money(0),
                money(0),
                money(0),
                "High",
                "",
                "rule"
        );
    }

    private BigDecimal money(long value) {
        return BigDecimal.valueOf(value).setScale(2);
    }
}
