-- ============================================================================
-- SCHEMA SIMPLIFICADO SEGÚN REQUERIMIENTOS ORIGINALES
-- Solo las tablas necesarias para procesar los 3 archivos CSV independientes
-- ============================================================================

-- 1. TABLA DE CUENTAS (para intereses.csv)
-- El monthlyInterestJob actualiza el balance después de calcular intereses
CREATE TABLE IF NOT EXISTS account (
    id BIGSERIAL PRIMARY KEY,
    account_number VARCHAR(64) UNIQUE NOT NULL,
    owner_name VARCHAR(255),
    type VARCHAR(32),
    balance NUMERIC(19,2) DEFAULT 0,
    age INTEGER CHECK (age IS NULL OR (age >= 0 AND age <= 150))
);

-- 2. TABLA DE TRANSACCIONES LEGACY (para transacciones.csv)
-- El dailyReportJob detecta anomalías y genera resúmenes de estas transacciones
CREATE TABLE IF NOT EXISTS transaction_legacy (
    id BIGSERIAL PRIMARY KEY,
    tx_id VARCHAR(64),
    account_number VARCHAR(64),
    tx_date DATE,
    description VARCHAR(255),
    amount NUMERIC(19,2)
);

-- 3. TABLA DE DATOS ANUALES (para cuentas_anuales.csv)
-- El annualAccountsJob compila datos anuales para auditorías
CREATE TABLE IF NOT EXISTS annual_account_data (
    id BIGSERIAL PRIMARY KEY,
    year INT NOT NULL,
    account_number VARCHAR(64),
    opening_balance NUMERIC(19,2),
    total_deposits NUMERIC(19,2),
    total_withdrawals NUMERIC(19,2),
    closing_balance NUMERIC(19,2),
    audit_date DATE DEFAULT CURRENT_DATE
);

-- ============================================================================
-- ÍNDICES BÁSICOS PARA OPTIMIZACIÓN
-- ============================================================================

-- Índices para account (intereses.csv processing)
CREATE INDEX IF NOT EXISTS idx_account_number ON account(account_number);
CREATE INDEX IF NOT EXISTS idx_account_age ON account(age);
CREATE INDEX IF NOT EXISTS idx_account_type ON account(type);

-- Índices para transaction_legacy (transacciones.csv processing)
CREATE INDEX IF NOT EXISTS idx_transaction_date ON transaction_legacy(tx_date);
CREATE INDEX IF NOT EXISTS idx_transaction_account ON transaction_legacy(account_number);
CREATE INDEX IF NOT EXISTS idx_transaction_amount ON transaction_legacy(amount);

-- Índices para annual_account_data (cuentas_anuales.csv processing)
CREATE INDEX IF NOT EXISTS idx_annual_year ON annual_account_data(year);
CREATE INDEX IF NOT EXISTS idx_annual_account ON annual_account_data(account_number);

-- ============================================================================
-- COMENTARIOS EXPLICATIVOS
-- ============================================================================

COMMENT ON TABLE account IS 'Cuentas bancarias del archivo intereses.csv - balance actualizado por monthlyInterestJob';
COMMENT ON TABLE transaction_legacy IS 'Transacciones del archivo transacciones.csv - procesadas por dailyReportJob para detectar anomalías';
COMMENT ON TABLE annual_account_data IS 'Datos anuales del archivo cuentas_anuales.csv - compilados por annualAccountsJob para auditorías';