CREATE TABLE financial_goals (
    goal_id        VARCHAR(190) PRIMARY KEY,
    name           VARCHAR(190)  NOT NULL,
    target_amount  NUMERIC(14,2) NOT NULL,
    current_amount NUMERIC(14,2) NOT NULL DEFAULT 0,
    target_date    DATE,
    note           TEXT,
    updated_at     TIMESTAMP WITH TIME ZONE NOT NULL
);
