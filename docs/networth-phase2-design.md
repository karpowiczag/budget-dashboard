# Phase 2 design — Net worth, account balances & liabilities

Status: **proposal / not yet implemented**. Authored from the planning-domain review
(see `MEMORY` planning-domain-review) which found the app's biggest structural gap: it
tracks **flows, not balances**, so it can show an emergency-fund *target* but never
*progress*, and cannot produce a household **net worth = assets − liabilities**.

This document is the agreed-on shape before coding. It deliberately sequences the work so
the **first increment ships visible value** (emergency-fund progress) without the full
balance-sheet machinery.

---

## 1. Recommendation & rationale

**Adopt a hybrid sourcing strategy, manual-snapshot first.**

| Strategy | Verdict |
| --- | --- |
| **Manual periodic balance snapshots** (user types current balance per account) | ✅ **Primary for Phase 2a–2b.** Zero reconciliation, ships progress immediately, matches how YNAB/Personal Capital onboard. |
| **Derived from opening balance + cumulative transaction flow** | ✅ **Layer on in Phase 2c** as an *assist* (pre-fill the next snapshot, flag drift). The app already stores every transaction, so this is cheap once balances exist — but using it as the *only* source requires correct opening balances and clean, non-overlapping imports, which we can't assume yet. |
| **Imported balance CSV / bank API** | ❌ Out of scope; no bank API, and balance CSVs aren't part of the export the user has. |

**Why manual-first:** the value the review demands ("Am I on track for my emergency
fund?", "what's our net worth trend?") needs *a balance number*, not a *perfect* one. A
user-entered snapshot delivers that in the smallest possible change. Derivation and
reconciliation are accuracy improvements that ride on top, not prerequisites.

**Net worth definition (anti-double-count):**

```
netWorth = Σ(liquid account balances)        ← bank cash/checking/savings, manual or derived
         + investedCapital                    ← FireSummary.currentPortfolioValue (MyFund CSV)
         − Σ(liability current principal)      ← new Liability model
```

The two asset sources are **disjoint by construction**: liquid accounts come from the
bank world (the budget app's transactions), `currentPortfolioValue` comes from the MyFund
broker CSV (`FirePortfolioPosition.valuePln`). The only double-count risk is a user
entering a brokerage-cash balance that is *also* in MyFund — guarded by UI copy
("liquid bank/cash only; investments are read from MyFund") and an `excludeFromNetWorth`
flag on accounts. The FIRE module's internal `emergencyFundValue` sleeve stays a
FIRE-only concept; **budget emergency-fund progress uses the liquid bank savings balance.**

---

## 2. Domain model (`com.budget.domain.networth`)

Immutable records, no imports from application/infra/web/config (PackageBoundaryTest).

```java
public enum AccountKind { CHECKING, SAVINGS, CASH, BROKERAGE_CASH, OTHER_ASSET }

public record Account(
        String accountKey,      // stable slug, e.g. "mbank-osobiste"
        String name,
        AccountKind kind,
        String currency,        // "PLN"
        boolean liquid,         // counts toward emergency-fund coverage
        boolean excludeFromNetWorth) {}

public record AccountBalance(
        String accountKey,
        LocalDate asOf,
        BigDecimal balance) {}

public enum LiabilityKind { MORTGAGE, AUTO, CONSUMER, STUDENT, OTHER }

public record Liability(
        String liabilityKey,
        String name,
        LiabilityKind kind,
        String currency,
        BigDecimal currentPrincipal,
        BigDecimal annualInterestRate,   // 0.072 = 7.2%; nullable→unknown
        BigDecimal monthlyPayment,       // contractual; nullable
        LocalDate asOf) {}

public record NetWorthSnapshot(
        LocalDate asOf,
        BigDecimal liquidAssets,
        BigDecimal investedAssets,       // from FireSummary.currentPortfolioValue
        BigDecimal totalAssets,
        BigDecimal totalLiabilities,
        BigDecimal netWorth,
        BigDecimal emergencyFundMin,     // reused from SavingsPlan
        BigDecimal emergencyFundComfort,
        BigDecimal emergencyFundProgress // liquidAssets / emergencyFundComfort, clamped
) {}
```

Balances and liabilities are **time series** (one row per `asOf`); "current" = latest
`asOf`. This gives the net-worth *trend* for free and avoids destructive updates.

---

## 3. Persistence — Flyway **V12** + JDBC adapter

Mirror the existing settings-profile pattern (`V5__budget_settings.sql`,
`V11__fire_settings.sql`): a parent keyed table + child series with
`ON DELETE CASCADE`, all `NUMERIC(14,2)` money, written through a
`NamedParameterJdbcTemplate` adapter behind an application port — exactly like
`JdbcBudgetSettingsRepository` implements `BudgetSettingsStore`.

`src/main/resources/db/migration/V12__networth_balances.sql`:

```sql
CREATE TABLE networth_accounts (
    account_key            VARCHAR(64) PRIMARY KEY,
    name                   VARCHAR(160) NOT NULL,
    kind                   VARCHAR(32)  NOT NULL,
    currency               VARCHAR(8)   NOT NULL DEFAULT 'PLN',
    liquid                 BOOLEAN      NOT NULL DEFAULT TRUE,
    exclude_from_net_worth BOOLEAN      NOT NULL DEFAULT FALSE,
    updated_at             TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE networth_account_balances (
    id          BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    account_key VARCHAR(64) NOT NULL REFERENCES networth_accounts (account_key) ON DELETE CASCADE,
    as_of       DATE          NOT NULL,
    balance     NUMERIC(14,2) NOT NULL,
    UNIQUE (account_key, as_of)
);

CREATE TABLE networth_liabilities (
    liability_key        VARCHAR(64) PRIMARY KEY,
    name                 VARCHAR(160)  NOT NULL,
    kind                 VARCHAR(32)   NOT NULL,
    currency             VARCHAR(8)    NOT NULL DEFAULT 'PLN',
    current_principal    NUMERIC(14,2) NOT NULL,
    annual_interest_rate NUMERIC(8,6),
    monthly_payment      NUMERIC(14,2),
    as_of                DATE          NOT NULL,
    updated_at           TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_networth_balances_account ON networth_account_balances (account_key, as_of);
```

Adapter: `infrastructure.persistence.jdbc.JdbcNetWorthRepository` (upsert accounts/
liabilities by key; insert-or-replace balance by `(account_key, as_of)`). No Spring Data
`@Table` entity needed — the settings repos use raw `NamedParameterJdbcTemplate`, follow
that.

---

## 4. Application — port + net-worth service

```
application/networth/
  NetWorthStore.java       // PORT: load/save accounts, balances, liabilities
  NetWorthService.java     // use case: assemble NetWorthSnapshot
```

`NetWorthStore` (port, implemented by `JdbcNetWorthRepository`, wired in `config`):

```java
public interface NetWorthStore {
    List<Account> accounts();
    void saveAccount(Account a);
    void deleteAccount(String accountKey);
    Optional<AccountBalance> latestBalance(String accountKey);
    List<AccountBalance> balanceHistory();          // for the trend chart
    void recordBalance(AccountBalance b);
    List<Liability> liabilities();
    void saveLiability(Liability l);
    void deleteLiability(String liabilityKey);
}
```

`NetWorthService.snapshot(...)` composes the number. **Crucially, the invested side is
injected, not recomputed**, to avoid coupling and double-counting:

```java
NetWorthSnapshot snapshot(BigDecimal investedCapital,        // = FireSummary.currentPortfolioValue
                          BigDecimal emergencyFundMin,        // = SavingsPlan.emergencyFundMin
                          BigDecimal emergencyFundComfort) {
    var liquid = accounts.stream().filter(Account::liquid)
        .map(a -> latestBalance(a.accountKey())).flatMap(Optional::stream)
        .map(AccountBalance::balance).reduce(ZERO, BigDecimal::add);
    var assetsNonLiquid = /* OTHER_ASSET latest balances, !excludeFromNetWorth */;
    var liab = liabilities.stream().map(Liability::currentPrincipal).reduce(ZERO, ...);
    var totalAssets = liquid.add(assetsNonLiquid).add(investedCapital);
    return new NetWorthSnapshot(asOf, liquid, investedCapital, totalAssets,
        liab, totalAssets.subtract(liab), emergencyFundMin, emergencyFundComfort,
        ratioClamped(liquid, emergencyFundComfort));
}
```

The caller (a controller-facing service or the dashboard assembler) passes
`fireSummary.currentPortfolioValue()` and the already-computed emergency-fund targets
from `SavingsPlan` — no new financial math, just composition.

---

## 5. Contract (`web` + `docs/openapi/budget-api.yaml`)

New resource under `/api/v1`, contract-first (update YAML + `OpenApiContractTest` in the
same change). Mutations require the CSRF header (frontend `csrfHeaders()`).

- `GET  /api/v1/networth` → `NetWorthResponse { snapshot, accounts[], liabilities[], history[] }`
- `PUT  /api/v1/networth/accounts/{key}` ← `AccountUpsertRequest`
- `POST /api/v1/networth/accounts/{key}/balances` ← `{ asOf, balance }`
- `PUT  /api/v1/networth/liabilities/{key}` ← `LiabilityUpsertRequest`
- `DELETE` for account/liability.

DTOs are new `web.dto` records (`NetWorthResponse`, `AccountResponse`, …) mapped by
`BudgetApiMapper`; controller returns DTOs, never domain records (as today). Money/Ratio
reuse the existing OpenAPI component schemas.

---

## 6. Frontend — the **Majątek** section

The wealth view today says *"przepływy z importowanych transakcji, nie live saldo kont"* —
this feature is exactly the missing "saldo". Add to the Majątek section:

- **Net-worth header card**: `netWorth`, with `assets − liabilities` breakdown and a
  small history sparkline (`history[]`).
- **Emergency-fund progress**: a bar — `liquidAssets` vs `emergencyFundMin`/`Comfort`
  (e.g. "38 000 / 54 000 zł · 70%") — the headline answer to "am I on track?".
- **Accounts & balances editor**: list of accounts; "zaktualizuj saldo" records a new
  dated snapshot.
- **Liabilities editor**: principal/rate/payment; feeds net worth (and Phase 2d payoff).

New `api/networthApi.js` (with CSRF header), pure selectors in `budgetSelectors.js`
(`selectNetWorth`, `selectEmergencyFundProgress`) unit-tested directly, presenter wiring
in the wealth view. The Przegląd hero gains a "Wartość netto" summary card linking here.

---

## 7. Phased rollout (each phase independently shippable, PR-sized)

- **Phase 2a — Emergency-fund progress (smallest win).** `networth_accounts` +
  `networth_account_balances` (V12, accounts+balances only), `NetWorthStore` +
  `NetWorthService` (liquid + emergency progress only; no liabilities, invested=0),
  `GET/PUT/POST` for accounts/balances, Majątek progress bar + balance editor.
  *Delivers "you have X of your Y emergency-fund target".*
- **Phase 2b — Net worth.** Add `networth_liabilities`, fold in
  `fireSummary.currentPortfolioValue`, net-worth header + history sparkline.
  *Delivers the full assets − liabilities number and trend.*
- **Phase 2c — Derivation & reconciliation.** Per-account opening balance + cumulative
  transaction net flow to **pre-fill** the next snapshot and **flag drift** vs the last
  manual balance. Reuses the existing `account` string on transactions.
- **Phase 2d — Debt payoff.** Amortization from `Liability` (principal/rate/payment),
  avalanche vs snowball comparison, "overpay mortgage vs invest" — ties into the FIRE
  loan-overpayment stream the review flagged.

Goals-with-dates (the other big review gap) layer naturally on this once balances exist
(a goal = target amount + date + funding account), and become Phase 2e or its own Phase 3.

---

## 8. Open product decisions (need a human call)

1. **Sourcing for Phase 2a — manual snapshots first?** *Recommend yes* (above). The
   derive-from-flows assist is Phase 2c, not now.
2. **Account granularity.** Per real bank account, or a single "liquid cash" pot to start?
   *Recommend per-account* (small extra cost, and the `account` field already distinguishes
   them) but seed with the savings-account the app already detects (`OSZCZ/LOKAT`).
3. **Currency.** PLN-only for now (matches everything else)? *Recommend yes; defer multi-ccy.*
4. **Emergency-fund basis.** Keep `liquidAssets / emergencyFundComfort`, or count only
   `SAVINGS`/`CASH` (exclude everyday checking float)? *Recommend liquid-savings only* for a
   truer number, with a toggle.

## 9. Risks

- **Double counting** invested capital if a user enters brokerage balances also in MyFund —
  mitigated by `excludeFromNetWorth` + UI copy; net-worth service only sums `liquid`/
  `OTHER_ASSET` accounts plus the single injected `currentPortfolioValue`.
- **Stale manual balances** make net worth lag reality — mitigated by showing `asOf` and
  nudging updates; Phase 2c derivation closes the gap.
- **Contract test churn** — every endpoint/DTO must land with the YAML; budget the
  `OpenApiContractTest` update into each phase.
- **Scope creep** — liabilities/goals/payoff are tempting to build together; the phasing
  exists to resist that and keep PRs reviewable.
