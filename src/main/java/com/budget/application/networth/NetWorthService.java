package com.budget.application.networth;

import com.budget.application.reporting.BudgetReportStore;
import com.budget.domain.networth.Account;
import com.budget.domain.networth.Liability;
import com.budget.domain.report.BudgetSnapshot;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import org.springframework.stereotype.Service;

/**
 * Derives liquid account balances from a per-account anchor plus transaction flows, folds
 * in invested capital (MyFund portfolio) and liabilities, and reports the full net worth
 * plus emergency-fund coverage. The two asset sources are disjoint — liquid bank balances
 * come from imported transactions, invested capital is injected via {@link PortfolioValuePort}
 * — so they never double-count.
 */
@Service
public class NetWorthService {
    private final NetWorthStore store;
    private final BudgetReportStore reports;
    private final PortfolioValuePort portfolio;

    public NetWorthService(NetWorthStore store, BudgetReportStore reports, PortfolioValuePort portfolio) {
        this.store = store;
        this.reports = reports;
        this.portfolio = portfolio;
    }

    public NetWorthOverview overview() {
        var efMin = BigDecimal.ZERO;
        var efComfort = BigDecimal.ZERO;
        var years = reports.findYears();
        if (!years.isEmpty()) {
            var latest = years.stream().mapToInt(y -> y.year()).max().getAsInt();
            BudgetSnapshot snapshot = reports.findDashboard(latest);
            var plan = snapshot.savingsPlan();
            if (plan != null) {
                efMin = nz(plan.emergencyFundMin());
                efComfort = nz(plan.emergencyFundComfort());
            }
        }

        var saved = new LinkedHashMap<String, Account>();
        for (var account : store.accounts()) {
            saved.put(account.accountKey(), account);
        }
        var keys = new LinkedHashSet<String>();
        keys.addAll(store.discoverAccountKeys());
        keys.addAll(saved.keySet());

        var rows = new ArrayList<NetWorthOverview.LiquidAccount>();
        var liquidTotal = BigDecimal.ZERO;
        for (var key : keys) {
            var account = saved.get(key);
            var name = account != null ? account.name() : key;
            var kind = account != null ? account.kind() : guessKind(key);
            var liquid = account == null || account.liquid();
            var excluded = account != null && account.excludeFromNetWorth();
            var anchorBalance = account != null ? account.anchorBalance() : null;
            var anchorDate = account != null ? account.anchorDate() : null;
            var configured = anchorBalance != null && anchorDate != null;
            var netFlow = configured ? money(store.netFlowSince(key, anchorDate)) : null;
            var derived = configured ? money(anchorBalance.add(netFlow)) : null;
            if (configured && liquid && !excluded) {
                liquidTotal = liquidTotal.add(derived);
            }
            rows.add(new NetWorthOverview.LiquidAccount(
                    key, name, kind, liquid, excluded, configured,
                    anchorBalance, anchorDate, netFlow, derived));
        }
        liquidTotal = money(liquidTotal);

        var liabilities = store.liabilities();
        var totalLiabilities = money(liabilities.stream()
                .map(Liability::currentPrincipal)
                .map(NetWorthService::nz)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        var invested = money(investedCapital());
        var totalAssets = money(liquidTotal.add(invested));
        var netWorth = money(totalAssets.subtract(totalLiabilities));

        var progress = efComfort.signum() > 0
                ? liquidTotal.divide(efComfort, 4, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        return new NetWorthOverview(rows, liabilities, liquidTotal, invested, totalAssets,
                totalLiabilities, netWorth, money(efMin), money(efComfort), progress);
    }

    public NetWorthOverview saveAccount(Account account) {
        store.saveAccount(account);
        return overview();
    }

    public NetWorthOverview saveLiability(Liability liability) {
        store.saveLiability(liability);
        return overview();
    }

    public NetWorthOverview deleteLiability(String liabilityKey) {
        store.deleteLiability(liabilityKey);
        return overview();
    }

    private BigDecimal investedCapital() {
        try {
            return nz(portfolio.currentPortfolioValue());
        } catch (RuntimeException ignored) {
            return BigDecimal.ZERO;
        }
    }

    private static String guessKind(String accountKey) {
        var upper = accountKey == null ? "" : accountKey.toUpperCase();
        if (upper.contains("OSZCZ") || upper.contains("LOKAT") || upper.contains("SAVING")) {
            return "SAVINGS";
        }
        return "CHECKING";
    }

    private static BigDecimal nz(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private static BigDecimal money(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP);
    }
}
