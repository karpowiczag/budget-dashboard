-- Phase 3b: categories become user-owned. The runtime catalog moves from the static
-- BudgetTaxonomy code map into these tables (seeded from BudgetTaxonomy on first boot, see
-- CategoryCatalogSeeder). The bucket/area/fixedness/flow vocabularies stay fixed in code; only
-- the per-category rows are editable. Behaviour-keyed flags (daily_paced, protected_flag,
-- sinking_fund_eligible) are materialized as columns so renames/edits survive.
CREATE TABLE category_group (
    group_id   VARCHAR(64)  PRIMARY KEY,
    label      VARCHAR(160) NOT NULL,
    sort_order INTEGER      NOT NULL DEFAULT 0
);

CREATE TABLE category (
    category_id           VARCHAR(80)  PRIMARY KEY,            -- stable slug; budget_transactions already stores this (V10)
    label                 VARCHAR(160) NOT NULL,
    area                  VARCHAR(160) NOT NULL,
    analytics_group       VARCHAR(160) NOT NULL,
    group_id              VARCHAR(64)  REFERENCES category_group(group_id),
    budget_bucket_label   VARCHAR(160) NOT NULL,
    fixedness             VARCHAR(64)  NOT NULL,
    flow_type             VARCHAR(40)  NOT NULL,
    discretionary         BOOLEAN      NOT NULL DEFAULT FALSE,
    excluded              BOOLEAN      NOT NULL DEFAULT FALSE,
    real_income           BOOLEAN      NOT NULL DEFAULT FALSE,
    daily_paced           BOOLEAN      NOT NULL DEFAULT FALSE,
    protected_flag        BOOLEAN      NOT NULL DEFAULT FALSE, -- not `protected` (reserved word)
    sinking_fund_eligible BOOLEAN      NOT NULL DEFAULT FALSE,
    sort_order            INTEGER      NOT NULL DEFAULT 0,
    archived              BOOLEAN      NOT NULL DEFAULT FALSE,
    updated_at            TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_category_group_id ON category (group_id);
