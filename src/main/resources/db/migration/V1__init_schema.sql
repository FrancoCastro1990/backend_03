CREATE TABLE IF NOT EXISTS account (
    id BIGSERIAL PRIMARY KEY,
    account_number VARCHAR(64) UNIQUE NOT NULL,
    owner_name VARCHAR(255),
    type VARCHAR(32),
    balance NUMERIC(19,2) DEFAULT 0
);

CREATE TABLE IF NOT EXISTS transaction_legacy (
    id BIGSERIAL PRIMARY KEY,
    tx_id VARCHAR(64),
    account_number VARCHAR(64),
    tx_date DATE,
    description VARCHAR(255),
    amount NUMERIC(19,2)
);

CREATE TABLE IF NOT EXISTS daily_transaction_report (
    id BIGSERIAL PRIMARY KEY,
    report_date DATE NOT NULL,
    account_number VARCHAR(64),
    tx_count INT,
    total_amount NUMERIC(19,2),
    anomalies INT
);

CREATE TABLE IF NOT EXISTS monthly_interest (
    id BIGSERIAL PRIMARY KEY,
    month_year VARCHAR(7),
    account_number VARCHAR(64),
    interest_applied NUMERIC(19,2),
    final_balance NUMERIC(19,2)
);

CREATE TABLE IF NOT EXISTS annual_statement (
    id BIGSERIAL PRIMARY KEY,
    year INT,
    account_number VARCHAR(64),
    opening_balance NUMERIC(19,2),
    total_deposits NUMERIC(19,2),
    total_withdrawals NUMERIC(19,2),
    closing_balance NUMERIC(19,2)
);
