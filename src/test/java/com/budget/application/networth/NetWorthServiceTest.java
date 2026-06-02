package com.budget.application.networth;

import static org.assertj.core.api.Assertions.assertThat;

import com.budget.application.reporting.AnalyticsReport;
import com.budget.application.reporting.BudgetReportStore;
import com.budget.application.reporting.CalendarReport;
import com.budget.application.reporting.TransactionPage;
import com.budget.application.reporting.TransactionQuery;
import com.budget.application.reporting.YearSummary;
import com.budget.domain.importjob.ImportRun;
import com.budget.domain.networth.Account;
import com.budget.domain.report.BudgetAnalysisResult;
import com.budget.domain.report.BudgetSnapshot;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class NetWorthServiceTest {

    private static BudgetSnapshot snapshotWithEmergencyTargets(BigDecimal min, BigDecimal comfort) {
        var plan = new BudgetSnapshot.SavingsPlan(
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, min, comfort, List.of(), List.of());
        return new BudgetSnapshot(2026, "2026", null, null, 0, null,
                List.of(), List.of(), List.of(), List.of(), plan, null,
                List.of(), List.of(), List.of(), List.of());
    }

    @Test
    void derivesBalancesAndEmergencyProgress() {
        var store = new FakeStore()
                .discovered("mBank Osobiste", "mBank Oszczednosci", "Konto maklerskie")
                .account(new Account("mBank Osobiste", "Osobiste", "CHECKING", true, false, new BigDecimal("1000.00"), LocalDate.parse("2026-01-01")))
                .account(new Account("mBank Oszczednosci", "Oszczednosci", "SAVINGS", true, false, new BigDecimal("20000.00"), LocalDate.parse("2026-01-01")))
                .netFlow("mBank Osobiste", new BigDecimal("500.00"))
                .netFlow("mBank Oszczednosci", new BigDecimal("3000.00"));
        var reports = new FakeReports(
                List.of(new YearSummary(2026, null, 100, BigDecimal.ZERO, BigDecimal.ZERO, null)),
                snapshotWithEmergencyTargets(new BigDecimal("27000"), new BigDecimal("54000")));

        var overview = new NetWorthService(store, reports).overview();

        assertThat(overview.accounts()).hasSize(3);
        assertThat(overview.liquidTotal()).isEqualByComparingTo("24500.00");
        assertThat(overview.emergencyFundComfort()).isEqualByComparingTo("54000.00");
        assertThat(overview.emergencyProgressComfort()).isEqualByComparingTo("0.4537");

        var maklerskie = overview.accounts().stream()
                .filter(a -> a.accountKey().equals("Konto maklerskie")).findFirst().orElseThrow();
        assertThat(maklerskie.configured()).isFalse();
        assertThat(maklerskie.derivedBalance()).isNull();
        assertThat(maklerskie.kind()).isEqualTo("CHECKING");
    }

    @Test
    void excludesNonLiquidAndExcludedAccountsFromLiquidTotal() {
        var store = new FakeStore()
                .discovered("Bie", "Inv")
                .account(new Account("Bie", "Biezace", "CHECKING", true, false, new BigDecimal("1000.00"), LocalDate.parse("2026-01-01")))
                .account(new Account("Inv", "Maklerskie", "OTHER", false, true, new BigDecimal("50000.00"), LocalDate.parse("2026-01-01")))
                .netFlow("Bie", BigDecimal.ZERO)
                .netFlow("Inv", BigDecimal.ZERO);
        var reports = new FakeReports(
                List.of(new YearSummary(2026, null, 1, BigDecimal.ZERO, BigDecimal.ZERO, null)),
                snapshotWithEmergencyTargets(new BigDecimal("3000"), new BigDecimal("6000")));

        var overview = new NetWorthService(store, reports).overview();

        assertThat(overview.liquidTotal()).isEqualByComparingTo("1000.00");
    }

    @Test
    void zeroEmergencyTargetsWhenNoReports() {
        var store = new FakeStore().discovered("A")
                .account(new Account("A", "A", "CHECKING", true, false, new BigDecimal("100.00"), LocalDate.parse("2026-01-01")))
                .netFlow("A", BigDecimal.ZERO);
        var reports = new FakeReports(List.of(), null);

        var overview = new NetWorthService(store, reports).overview();

        assertThat(overview.emergencyFundComfort()).isEqualByComparingTo("0.00");
        assertThat(overview.emergencyProgressComfort()).isEqualByComparingTo("0");
        assertThat(overview.liquidTotal()).isEqualByComparingTo("100.00");
    }

    private static final class FakeStore implements NetWorthStore {
        private final List<String> discovered = new ArrayList<>();
        private final Map<String, Account> accounts = new LinkedHashMap<>();
        private final Map<String, BigDecimal> flows = new HashMap<>();

        FakeStore discovered(String... keys) {
            discovered.addAll(List.of(keys));
            return this;
        }

        FakeStore account(Account account) {
            accounts.put(account.accountKey(), account);
            return this;
        }

        FakeStore netFlow(String key, BigDecimal value) {
            flows.put(key, value);
            return this;
        }

        @Override
        public List<String> discoverAccountKeys() {
            return List.copyOf(discovered);
        }

        @Override
        public List<Account> accounts() {
            return List.copyOf(accounts.values());
        }

        @Override
        public Account saveAccount(Account account) {
            accounts.put(account.accountKey(), account);
            return account;
        }

        @Override
        public BigDecimal netFlowSince(String accountKey, LocalDate sinceExclusive) {
            return flows.getOrDefault(accountKey, BigDecimal.ZERO);
        }
    }

    private record FakeReports(List<YearSummary> years, BudgetSnapshot snapshot) implements BudgetReportStore {
        @Override
        public void save(BudgetAnalysisResult result) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<YearSummary> findYears() {
            return years;
        }

        @Override
        public BudgetSnapshot findDashboard(int year) {
            return snapshot;
        }

        @Override
        public CalendarReport findCalendar(int year, String month) {
            throw new UnsupportedOperationException();
        }

        @Override
        public AnalyticsReport findAnalytics(TransactionQuery query) {
            throw new UnsupportedOperationException();
        }

        @Override
        public TransactionPage findTransactions(TransactionQuery query) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void recordImportRun(Integer year, String inputCsv, String status, String message, int duplicatesRemoved) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<ImportRun> findImportRuns() {
            throw new UnsupportedOperationException();
        }
    }
}
