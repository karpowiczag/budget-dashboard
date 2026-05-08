ALTER TABLE budget_category_limit_settings
    ADD COLUMN IF NOT EXISTS limit_scope VARCHAR(32) NOT NULL DEFAULT 'category';

ALTER TABLE budget_category_limit_settings
    ADD COLUMN IF NOT EXISTS limit_name VARCHAR(160);

UPDATE budget_category_limit_settings
SET limit_name = category
WHERE limit_name IS NULL OR limit_name = '';

CREATE INDEX IF NOT EXISTS idx_budget_category_limit_settings_scope
    ON budget_category_limit_settings (settings_key, limit_scope, limit_name);

ALTER TABLE report_category_limits
    ADD COLUMN IF NOT EXISTS limit_scope VARCHAR(32) NOT NULL DEFAULT 'category';

ALTER TABLE report_category_limits
    ADD COLUMN IF NOT EXISTS limit_name VARCHAR(160);

ALTER TABLE report_category_limits
    ADD COLUMN IF NOT EXISTS is_parent BOOLEAN NOT NULL DEFAULT FALSE;

UPDATE report_category_limits
SET limit_name = category
WHERE limit_name IS NULL OR limit_name = '';

CREATE INDEX IF NOT EXISTS idx_report_category_limits_scope
    ON report_category_limits (report_year, limit_scope, limit_name, is_parent);
