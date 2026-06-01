ALTER TABLE budget_category_limit_settings
    ADD COLUMN IF NOT EXISTS budget_bucket_override VARCHAR(80) NOT NULL DEFAULT '';
