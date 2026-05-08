ALTER TABLE budget_transactions
    ADD COLUMN IF NOT EXISTS category_id VARCHAR(80) NOT NULL DEFAULT '';

ALTER TABLE budget_transactions
    ADD COLUMN IF NOT EXISTS subcategory_id VARCHAR(80) NOT NULL DEFAULT '';

ALTER TABLE budget_transactions
    ADD COLUMN IF NOT EXISTS flow_type VARCHAR(40) NOT NULL DEFAULT '';

ALTER TABLE budget_transactions
    ADD COLUMN IF NOT EXISTS budget_group_id VARCHAR(40) NOT NULL DEFAULT '';

ALTER TABLE budget_transactions
    ADD COLUMN IF NOT EXISTS budget_group_label VARCHAR(80) NOT NULL DEFAULT '';

ALTER TABLE budget_transactions
    ADD COLUMN IF NOT EXISTS review_status VARCHAR(40) NOT NULL DEFAULT 'ok';

ALTER TABLE budget_transactions
    ADD COLUMN IF NOT EXISTS review_reason VARCHAR(256) NOT NULL DEFAULT '';

-- Taxonomy v2 is a breaking cleanup: persisted snapshots are rebuilt from CSV imports.
DELETE FROM reports;

DELETE FROM budget_category_limit_settings
WHERE category = 'Oszczędności i inwestycje'
  AND EXISTS (
      SELECT 1
      FROM budget_category_limit_settings existing
      WHERE existing.settings_key = budget_category_limit_settings.settings_key
        AND existing.category = 'Inwestycje'
  );

UPDATE budget_category_limit_settings
SET category = 'Inwestycje',
    limit_name = CASE WHEN limit_scope = 'category' THEN 'Inwestycje' ELSE limit_name END
WHERE category = 'Oszczędności i inwestycje';

DELETE FROM budget_category_limit_settings
WHERE category = 'Zdrowie i uroda'
  AND EXISTS (
      SELECT 1
      FROM budget_category_limit_settings existing
      WHERE existing.settings_key = budget_category_limit_settings.settings_key
        AND existing.category = 'Lekarz i apteka'
  );

UPDATE budget_category_limit_settings
SET category = 'Lekarz i apteka',
    limit_name = CASE WHEN limit_scope = 'category' THEN 'Lekarz i apteka' ELSE limit_name END
WHERE category = 'Zdrowie i uroda';

CREATE INDEX IF NOT EXISTS idx_budget_transactions_year_flow_type
    ON budget_transactions (report_year, flow_type);

CREATE INDEX IF NOT EXISTS idx_budget_transactions_year_budget_group
    ON budget_transactions (report_year, budget_group_id);

CREATE INDEX IF NOT EXISTS idx_budget_transactions_year_review_status
    ON budget_transactions (report_year, review_status);

CREATE INDEX IF NOT EXISTS idx_budget_transactions_year_category_id
    ON budget_transactions (report_year, category_id);
