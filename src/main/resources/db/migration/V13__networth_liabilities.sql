CREATE TABLE networth_liabilities (
    liability_key        VARCHAR(190) PRIMARY KEY,
    name                 VARCHAR(190)  NOT NULL,
    kind                 VARCHAR(32)   NOT NULL,
    current_principal    NUMERIC(14,2) NOT NULL,
    annual_interest_rate NUMERIC(8,6),
    monthly_payment      NUMERIC(14,2),
    as_of                DATE,
    updated_at           TIMESTAMP WITH TIME ZONE NOT NULL
);
