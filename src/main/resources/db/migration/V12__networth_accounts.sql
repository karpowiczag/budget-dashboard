CREATE TABLE networth_accounts (
    account_key            VARCHAR(190) PRIMARY KEY,
    name                   VARCHAR(190) NOT NULL,
    kind                   VARCHAR(32)  NOT NULL,
    liquid                 BOOLEAN      NOT NULL DEFAULT TRUE,
    exclude_from_net_worth BOOLEAN      NOT NULL DEFAULT FALSE,
    anchor_balance         NUMERIC(14,2),
    anchor_date            DATE,
    updated_at             TIMESTAMP WITH TIME ZONE NOT NULL
);
