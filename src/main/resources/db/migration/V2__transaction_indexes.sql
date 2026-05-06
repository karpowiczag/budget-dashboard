CREATE INDEX idx_import_runs_created_at
    ON import_runs (created_at DESC);

CREATE INDEX idx_import_runs_report_year_created_at
    ON import_runs (report_year, created_at DESC);

CREATE INDEX idx_budget_transactions_year_month
    ON budget_transactions (report_year, month_key);

CREATE INDEX idx_budget_transactions_year_posted_date
    ON budget_transactions (report_year, posted_date);

CREATE INDEX idx_budget_transactions_year_category
    ON budget_transactions (report_year, corrected_category);

CREATE INDEX idx_budget_transactions_year_subcategory
    ON budget_transactions (report_year, corrected_category, subcategory);

CREATE INDEX idx_budget_transactions_year_merchant
    ON budget_transactions (report_year, merchant);

CREATE INDEX idx_budget_transactions_year_analysis_spend
    ON budget_transactions (report_year, analysis_spend DESC);
