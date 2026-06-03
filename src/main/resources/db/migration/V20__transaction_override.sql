-- Phase 3c-iii: manual per-transaction recategorize overrides. Keyed by the transaction content
-- fingerprint (date|account|amount|normalized description) so an override re-attaches after a
-- rebuild that deletes+recreates budget_transactions with fresh ids. This table is OUTSIDE the
-- reports cascade, so it is never deleted by re-import. No FK to category (categories are archived,
-- not deleted; an unknown id falls back to the stored label during normalization). The identity
-- columns are auditable only.
CREATE TABLE transaction_override (
    content_key VARCHAR(2048) PRIMARY KEY,
    category_id VARCHAR(80)   NOT NULL,
    posted_date DATE,
    account     VARCHAR(255),
    amount      NUMERIC(14,2),
    description VARCHAR(1024),
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL
);
