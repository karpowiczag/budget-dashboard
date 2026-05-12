CREATE TABLE fire_settings_profiles (
    settings_key VARCHAR(64) PRIMARY KEY,
    reports_path VARCHAR(500) NOT NULL,
    current_age INTEGER NOT NULL,
    target_age INTEGER NOT NULL,
    monthly_spend_override NUMERIC(14,2),
    monthly_contribution_override NUMERIC(14,2),
    safe_withdrawal_rate NUMERIC(8,6) NOT NULL,
    pessimistic_real_return NUMERIC(8,6) NOT NULL,
    expected_real_return NUMERIC(8,6) NOT NULL,
    optimistic_real_return NUMERIC(8,6) NOT NULL,
    target_equity_share NUMERIC(8,6) NOT NULL,
    target_bond_share NUMERIC(8,6) NOT NULL,
    target_cash_share NUMERIC(8,6) NOT NULL,
    target_alternative_share NUMERIC(8,6) NOT NULL,
    rebalance_band NUMERIC(8,6) NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);
