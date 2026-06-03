# Layer 2 design — user-owned categories (add / edit / delete everything)

Status: **proposal / not yet implemented.** Follows the flexible *limits* manager (commit
`4f507f5`), which unlocked editing limits/buckets for every category but left categories
themselves a fixed code taxonomy. This doc designs making **categories fully user-owned** —
add, rename, delete, reorder, hide — the complete answer to "modify everything."

This is a real architectural change, so it is phased so each step ships and stays green.

---

## 1. What's fixed today (and why it blocks "add a category")

- Categories are a hardcoded, ordered `Map` — `BudgetTaxonomy.CATEGORIES` ([BudgetTaxonomy.java:50](src/main/java/com/budget/application/categorization/BudgetTaxonomy.java#L50)). Each `CategoryDefinition` carries `id, label, area, analyticsGroup, budgetGroupId, budgetBucketLabel, fixedness, flowType, discretionary, excluded, realIncome`.
- Per-category attributes are read statically: `CategoryClassifier.metadata(label)` → `BudgetTaxonomy.category(categoryIdByLabel(label))` ([CategoryClassifier.java:108](src/main/java/com/budget/application/categorization/CategoryClassifier.java#L108)). `BudgetAnalysisService` consumes those via `classifier.budgetArea/group/budgetBucket/fixedness/isDiscretionary/isExcluded/isRealIncome`.
- Classification (which transaction → which category) is a Chain-of-Responsibility of matchers ([CategoryClassifier.java:27](src/main/java/com/budget/application/categorization/CategoryClassifier.java#L27)): employer-income → bank-category rules → regex rules (incl. user `PersonalCategoryRules`) → positive-flow → fallback. Regex rules are `CategoryRule(pattern → categoryId)`.

**Two enablers already exist** that make this tractable:
1. **Stable identity is already persisted.** Transactions carry `category_id` (`BudgetTransactionEntity.category_id`) alongside the `corrected_category` label, and `NormalizedTransaction` has `categoryId`. So we can group/analyze by **id** and resolve the label at presentation → **renames are safe**.
2. **Rename indirection already exists** — `LEGACY_CATEGORY_LABEL_ALIASES` maps old labels → ids, and `wealthCategoryLabels()` is a "single source of truth so labels can't drift."

**The real blockers** are: the catalog is code not data; and several behaviours **hardcode category labels** (e.g. `DAILY_PACED_CATEGORIES = {"Żywność i chemia", "Jedzenie poza domem"}`, default sinking funds, frontend `PROTECTED_CATEGORIES`/`RECURRING_OBLIGATION_CATEGORIES`). Renaming/deleting a default category would silently break those.

## 2. Scope decision — user-owned categories over a *fixed* vocabulary

YNAB/Actual make the whole tree user-owned. Here, the **bucket vocabulary drives planning
semantics** (safe-to-spend, 50/30/20, FIRE wealth flows all key off the 8 buckets + fixedness +
flow types as named constants). Making buckets renamable would ripple through the entire engine.

**Decision:** keep the **bucket / fixedness / flow-type vocabularies fixed** (they're the
"type system"), and make **categories fully user-owned** — each category is assigned to one of
the fixed buckets (+ fixedness + flow + flags). This is the YNAB experience for categories
("add/edit/delete/move/hide any category") with bounded blast radius. Category **groups** (the
analytics grouping) become user-editable too; **areas** stay a fixed list.

Behaviours that currently hardcode labels move to **flags on the category** so they survive
rename/delete: `dailyPaced`, `protected` (never auto-cut), `sinkingFundEligible`,
plus the existing `discretionary/excluded/realIncome`.

## 3. Target architecture

```
domain/category/                 Category, CategoryGroup, ClassificationRule (records)
application/categorization/
  CategoryCatalog (PORT)         read categories/groups/flags by id+label  ← classifier & analysis read this
  CategoryCatalogService         CRUD + reorder + archive; seeds defaults from BudgetTaxonomy
  ClassificationRuleStore (PORT) regex/bankCategory/merchant → categoryId, ordered
infrastructure/persistence/jdbc/ JdbcCategoryCatalog, JdbcClassificationRuleStore
web/controller/CategoryApiController   CRUD + rules + recategorize
```

- `BudgetTaxonomy` stops being the runtime authority and becomes the **default seed** (its
  current map → the initial rows). `CategoryClassifier.metadata()`/`categoryIdByLabel()` and
  the matchers resolve through `CategoryCatalog`/`ClassificationRuleStore` instead of statics.
- **Grouping by id.** `BudgetAnalysisService` and the report builders group by `category_id`
  (already stored) and resolve labels/attributes from the catalog → rename never breaks a report.
- **Behaviour flags** (`dailyPaced`, `protected`, `sinkingFundEligible`) replace the hardcoded
  label sets in `BudgetAnalysisService` and the frontend selectors.

### Persistence — Flyway V18

```sql
CREATE TABLE category_group (
    group_id    VARCHAR(64) PRIMARY KEY,
    label       VARCHAR(160) NOT NULL,
    sort_order  INTEGER NOT NULL DEFAULT 0
);
CREATE TABLE category (
    category_id           VARCHAR(64) PRIMARY KEY,   -- stable slug; transactions already store this
    label                 VARCHAR(160) NOT NULL,
    area                  VARCHAR(64)  NOT NULL,      -- from the fixed area list
    group_id              VARCHAR(64)  REFERENCES category_group(group_id),
    budget_bucket         VARCHAR(64)  NOT NULL,      -- from the fixed 8-bucket vocabulary
    fixedness             VARCHAR(32)  NOT NULL,
    flow_type             VARCHAR(32)  NOT NULL,
    discretionary         BOOLEAN NOT NULL DEFAULT FALSE,
    excluded              BOOLEAN NOT NULL DEFAULT FALSE,
    real_income           BOOLEAN NOT NULL DEFAULT FALSE,
    daily_paced           BOOLEAN NOT NULL DEFAULT FALSE,
    protected             BOOLEAN NOT NULL DEFAULT FALSE,
    sinking_fund_eligible BOOLEAN NOT NULL DEFAULT FALSE,
    sort_order            INTEGER NOT NULL DEFAULT 0,
    archived              BOOLEAN NOT NULL DEFAULT FALSE,
    updated_at            TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE TABLE classification_rule (
    id          BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    match_type  VARCHAR(16) NOT NULL,                 -- title | bankCategory | merchant
    pattern     TEXT NOT NULL,
    category_id VARCHAR(64) NOT NULL REFERENCES category(category_id),
    priority    INTEGER NOT NULL DEFAULT 100,
    enabled     BOOLEAN NOT NULL DEFAULT TRUE
);
```
Seeded on first boot from `BudgetTaxonomy.CATEGORIES`, `BUDGET_GROUPS`, the built-in
`CategoryRule`s, and `LEGACY_CATEGORY_LABEL_ALIASES` — so behaviour is identical until the user
edits anything. (Seed via a Java bootstrap that runs when the tables are empty, not SQL inserts,
to reuse the existing definitions as the single default source.)

### Delete / rename semantics
- **Rename** → update `label` only; `category_id` and all transactions/reports are untouched.
- **Hide** → `archived = true`; excluded from pickers, retained in history.
- **Delete** → require a **reassignment target**; bulk-remap rules + (optionally) re-tag the
  category_id on historical transactions, or block delete if referenced and offer archive.
- **Recategorize a transaction** → set its `category_id` (manual override, highest precedence),
  with an optional "create a rule from this" to make it stick for similar future rows.

## 4. Contract (`web` + OpenAPI)

`GET /api/v1/categories` → `{ groups[], categories[], rules[] }`; `PUT/DELETE
/api/v1/categories/{id}`; `PUT/DELETE /api/v1/categories/groups/{id}`; `PUT/DELETE
/api/v1/categories/rules/{id}`; `POST /api/v1/categories/reorder`; and a recategorize hook on
the existing transactions endpoint. New `web.dto` records + mapper; `budget-api.yaml` +
`OpenApiContractTest` extended (register `CategoryApiController` like Fire/NetWorth/Goal).

## 5. Frontend

A new **Kategorie** manager (its own view under a settings/transactions area): groups →
categories tree with drag-reorder, add/edit/delete/hide, per-category bucket + fixedness + flow
+ flags editors; a **Reguły** (rules) editor (pattern → category, priority, enable); and on the
Transactions table, a per-row **recategorize** + **"utwórz regułę z tej transakcji"**. The
existing limits manager (`selectLimitManager`) then operates over the user's live categories.

## 6. Phased rollout (each independently shippable, behaviour-preserving first)

- **3a — Read-side catalog (pure refactor, no UI, no behaviour change).** Add the tables +
  `CategoryCatalog`/`ClassificationRuleStore` ports + JDBC adapters, seed from `BudgetTaxonomy`,
  and route `CategoryClassifier` + the hardcoded label sets through the catalog/flags. Assert via
  the existing categorization tests that classification is byte-identical. *This de-risks everything.*
- **3b — Category CRUD + manager UI.** Rename/add/hide/reorder/edit attributes; group analysis
  by `category_id`; Kategorie manager. (Add/rename/hide — no delete yet.)
- **3c — Rules engine + manual recategorize.** Rules to DB + Reguły manager + per-transaction
  recategorize + "create rule from transaction."
- **3d — Delete/merge + reassignment + polish** (drag-reorder groups, bulk move, archive view).

## 7. Open decisions
1. **Category id for new user categories** — slug from label (collision-suffixed) vs UUID. *Recommend slug* (readable, matches existing ids), with uniqueness enforced.
2. **Delete policy** — hard delete + mandatory reassignment, or archive-only. *Recommend archive-first*, hard delete only when unreferenced.
3. **Groups user-owned or fixed?** *Recommend user-owned groups, fixed areas/buckets/fixedness/flow.*
4. **Re-tag history on rename of a default category's behaviour** — moving `dailyPaced` etc. to flags removes the need; confirm no remaining label-keyed logic before 3b.

## 8. Risks
- **Label-keyed logic** scattered in `BudgetAnalysisService` (daily-paced, sinking defaults,
  obligatory buckets) and frontend selectors (`PROTECTED_CATEGORIES`, `RECURRING_OBLIGATION_CATEGORIES`)
  — must all move to flags/catalog in 3a or they break on edit. **This is the main risk; 3a addresses it.**
- **Classifier determinism** — rules become data; keep deterministic ordering (priority) and keep
  `OpenApiContractTest`/categorization tests green by seeding identical defaults.
- **Migration of existing reports** — they store `category_id`; re-grouping by id is safe, but
  cached report payloads (`reports.payload_json`) may hold labels — rebuild on first run.
- **Contract + test surface** — a new controller + several endpoints; budget the OpenAPI + contract
  updates per phase.
- **Scope creep** — keep buckets/areas/fixedness/flow fixed; resist making the whole type system editable.
