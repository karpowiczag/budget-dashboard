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
import com.budget.domain.networth.Liability;
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

    private static Account account(String key, String name, String kind, boolean liquid, boolean excluded, String anchor) {
        return new Account(key, name, kind, liquid, excluded, new BigDecimal(anchor), LocalDate.parse("2026-01-01"), null, null);
    }

    @Test
    void derivesNetWorthFromLiquidInvestedAndLiabilities() {
        var store = new FakeStore()
                .discovered("mBank Osobiste", "mBank Oszczednosci", "Konto maklerskie")
                .account(account("mBank Osobiste", "Osobiste", "CHECKING", true, false, "1000.00"))
                .account(account("mBank Oszczednosci", "Oszczednosci", "SAVINGS", true, false, "20000.00"))
                .netFlow("mBank Osobiste", new BigDecimal("500.00"))
                .netFlow("mBank Oszczednosci", new BigDecimal("3000.00"))
                .liability(new Liability("mortgage", "Hipoteka", "MORTGAGE", new BigDecimal("30000.00"), new BigDecimal("0.072"), new BigDecimal("1500.00"), LocalDate.parse("2026-01-01")));
        var reports = new FakeReports(
                List.of(new YearSummary(2026, null, 100, BigDecimal.ZERO, BigDecimal.ZERO, null)),
                snapshotWithEmergencyTargets(new BigDecimal("27000"), new BigDecimal("54000")));
        PortfolioValuePort portfolio = () -> new BigDecimal("100000.00");

        var overview = new NetWorthService(store, reports, portfolio).overview();

        assertThat(overview.liquidTotal()).isEqualByComparingTo("24500.00");
        assertThat(overview.investedAssets()).isEqualByComparingTo("100000.00");
        assertThat(overview.totalLiabilities()).isEqualByComparingTo("30000.00");
        assertThat(overview.totalAssets()).isEqualByComparingTo("124500.00");
        assertThat(overview.netWorth()).isEqualByComparingTo("94500.00");
        assertThat(overview.liabilities()).hasSize(1);
        assertThat(overview.emergencyProgressComfort()).isEqualByComparingTo("0.4537");

        var maklerskie = overview.accounts().stream()
                .filter(a -> a.accountKey().equals("Konto maklerskie")).findFirst().orElseThrow();
        assertThat(maklerskie.configured()).isFalse();
        assertThat(maklerskie.derivedBalance()).isNull();
        assertThat(maklerskie.reconciled()).isNull();
    }

    @Test
    void excludesNonLiquidAndExcludedAccountsFromLiquidTotal() {
        var store = new FakeStore()
                .discovered("Bie", "Inv")
                .account(account("Bie", "Biezace", "CHECKING", true, false, "1000.00"))
                .account(account("Inv", "Maklerskie", "OTHER", false, true, "50000.00"))
                .netFlow("Bie", BigDecimal.ZERO)
                .netFlow("Inv", BigDecimal.ZERO);
        var reports = new FakeReports(
                List.of(new YearSummary(2026, null, 1, BigDecimal.ZERO, BigDecimal.ZERO, null)),
                snapshotWithEmergencyTargets(new BigDecimal("3000"), new BigDecimal("6000")));

        var overview = new NetWorthService(store, reports, () -> BigDecimal.ZERO).overview();

        assertThat(overview.liquidTotal()).isEqualByComparingTo("1000.00");
        assertThat(overview.netWorth()).isEqualByComparingTo("1000.00");
    }

    @Test
    void zeroEmergencyTargetsWhenNoReports() {
        var store = new FakeStore().discovered("A")
                .account(account("A", "A", "CHECKING", true, false, "100.00"))
                .netFlow("A", BigDecimal.ZERO);
        var reports = new FakeReports(List.of(), null);

        var overview = new NetWorthService(store, reports, () -> BigDecimal.ZERO).overview();

        assertThat(overview.emergencyFundComfort()).isEqualByComparingTo("0.00");
        assertThat(overview.emergencyProgressComfort()).isEqualByComparingTo("0");
        assertThat(overview.liquidTotal()).isEqualByComparingTo("100.00");
        assertThat(overview.netWorth()).isEqualByComparingTo("100.00");
    }

    @Test
    void investedCapitalDefaultsToZeroWhenPortfolioFails() {
        var store = new FakeStore().discovered("A")
                .account(account("A", "A", "CHECKING", true, false, "100.00"))
                .netFlow("A", BigDecimal.ZERO);
        var reports = new FakeReports(List.of(), null);
        PortfolioValuePort portfolio = () -> {
            throw new RuntimeException("no portfolio data");
        };

        var overview = new NetWorthService(store, reports, portfolio).overview();

        assertThat(overview.investedAssets()).isEqualByComparingTo("0.00");
        assertThat(overview.netWorth()).isEqualByComparingTo("100.00");
    }

    @Test
    void reconcilesDerivedBalanceAgainstStatement() {
        var store = new FakeStore()
                .discovered("Match", "Drift")
                .account(new Account("Match", "Zgodne", "CHECKING", true, false, new BigDecimal("1000.00"), LocalDate.parse("2026-01-01"), new BigDecimal("1500.00"), LocalDate.parse("2026-03-31")))
                .account(new Account("Drift", "Rozjazd", "CHECKING", true, false, new BigDecimal("1000.00"), LocalDate.parse("2026-01-01"), new BigDecimal("1450.00"), LocalDate.parse("2026-03-31")))
                .netFlow("Match", new BigDecimal("800.00"))
                .netFlow("Drift", new BigDecimal("800.00"))
                .flowBetween("Match", new BigDecimal("500.00"))
                .flowBetween("Drift", new BigDecimal("500.00"));
        var reports = new FakeReports(List.of(), null);

        var overview = new NetWorthService(store, reports, () -> BigDecimal.ZERO).overview();

        var match = overview.accounts().stream().filter(a -> a.accountKey().equals("Match")).findFirst().orElseThrow();
        assertThat(match.reconciledBalance()).isEqualByComparingTo("1500.00");
        assertThat(match.drift()).isEqualByComparingTo("0.00");
        assertThat(match.reconciled()).isTrue();

        var drift = overview.accounts().stream().filter(a -> a.accountKey().equals("Drift")).findFirst().orElseThrow();
        assertThat(drift.reconciledBalance()).isEqualByComparingTo("1500.00");
        assertThat(drift.drift()).isEqualByComparingTo("50.00");
        assertThat(drift.reconciled()).isFalse();
    }

    private static final class FakeStore implements NetWorthStore {
        private final List<String> discovered = new ArrayList<>();
        private final Map<String, Account> accounts = new LinkedHashMap<>();
        private final Map<String, BigDecimal> flows = new HashMap<>();
        private final Map<String, BigDecimal> betweenFlows = new HashMap<>();
        private final Map<String, Liability> liabilities = new LinkedHashMap<>();

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

        FakeStore flowBetween(String key, BigDecimal value) {
            betweenFlows.put(key, value);
            return this;
        }

        FakeStore liability(Liability liability) {
            liabilities.put(liability.liabilityKey(), liability);
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

        @Override
        public BigDecimal netFlowBetween(String accountKey, LocalDate afterExclusive, LocalDate throughInclusive) {
            return betweenFlows.getOrDefault(accountKey, BigDecimal.ZERO);
        }

        @Override
        public List<Liability> liabilities() {
            return List.copyOf(liabilities.values());
        }

        @Override
        public Liability saveLiability(Liability liability) {
            liabilities.put(liability.liabilityKey(), liability);
            return liability;
        }

        @Override
        public void deleteLiability(String liabilityKey) {
            liabilities.remove(liabilityKey);
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
