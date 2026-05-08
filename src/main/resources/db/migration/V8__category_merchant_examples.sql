ALTER TABLE report_category_summaries
    ADD COLUMN IF NOT EXISTS merchant_examples TEXT NOT NULL DEFAULT '';
