-- ============================================================================
-- LedgerFlow — Multi-Branch Banking & Settlement Platform
-- Database Definition & Advanced Features Script (DDL, Views, Triggers, Functions)
-- Engine: PostgreSQL 16+ (ACID Double-Entry Core Banking Engine)
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 1. DROP EXISTING OBJECTS (Idempotent cleanup for fresh migrations)
-- ----------------------------------------------------------------------------
DROP TRIGGER IF EXISTS trg_enforce_account_balance ON accounts;
DROP FUNCTION IF EXISTS fn_enforce_account_balance();
DROP FUNCTION IF EXISTS fn_calculate_account_balance(BIGINT);

DROP VIEW IF EXISTS v_branch_settlement_summary;
DROP VIEW IF EXISTS v_account_statement;
DROP VIEW IF EXISTS v_trial_balance;

DROP TABLE IF EXISTS ach_transfers CASCADE;
DROP TABLE IF EXISTS ach_batches CASCADE;
DROP TABLE IF EXISTS ledger_entries CASCADE;
DROP TABLE IF EXISTS transactions CASCADE;
DROP TABLE IF EXISTS accounts CASCADE;
DROP TABLE IF EXISTS branches CASCADE;

-- ----------------------------------------------------------------------------
-- 2. RELATIONAL TABLES (DDL with Primary Keys, Foreign Keys & Check Constraints)
-- ----------------------------------------------------------------------------

-- Table 1: Branches (Settlement Hubs)
CREATE TABLE branches (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    branch_code VARCHAR(30) NOT NULL UNIQUE,
    location VARCHAR(255) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Table 2: Accounts (Customer & System Clearing Accounts)
CREATE TABLE accounts (
    id BIGSERIAL PRIMARY KEY,
    account_number VARCHAR(64) NOT NULL UNIQUE,
    account_holder_name VARCHAR(150) NOT NULL,
    account_type VARCHAR(32) NOT NULL,
    balance NUMERIC(19, 4) NOT NULL DEFAULT 0.0000,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    branch_id BIGINT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_accounts_branch FOREIGN KEY (branch_id) 
        REFERENCES branches(id) ON DELETE RESTRICT,
    CONSTRAINT chk_account_balance_positive CHECK (balance >= 0.0000)
);

-- Table 3: ACH Batches (Batch Settlement Groups)
CREATE TABLE ach_batches (
    id BIGSERIAL PRIMARY KEY,
    batch_number VARCHAR(64) NOT NULL UNIQUE,
    status VARCHAR(32) NOT NULL DEFAULT 'OPEN',
    total_amount NUMERIC(19, 4) NOT NULL DEFAULT 0.0000,
    transfer_count INT NOT NULL DEFAULT 0,
    settled_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Table 4: Transactions (Header Records)
CREATE TABLE transactions (
    id BIGSERIAL PRIMARY KEY,
    reference_number VARCHAR(64) NOT NULL UNIQUE,
    amount NUMERIC(19, 4) NOT NULL,
    transaction_type VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    currency VARCHAR(8) NOT NULL DEFAULT 'INR',
    description VARCHAR(255),
    ach_batch_id BIGINT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT fk_transactions_ach_batch FOREIGN KEY (ach_batch_id) 
        REFERENCES ach_batches(id) ON DELETE SET NULL,
    CONSTRAINT chk_transaction_amount_positive CHECK (amount > 0.0000)
);

-- Table 5: Ledger Entries (Immutable Double-Entry Postings)
CREATE TABLE ledger_entries (
    id BIGSERIAL PRIMARY KEY,
    account_id BIGINT NOT NULL,
    transaction_id BIGINT NOT NULL,
    entry_type VARCHAR(16) NOT NULL, -- 'DEBIT' or 'CREDIT'
    amount NUMERIC(19, 4) NOT NULL,
    running_balance NUMERIC(19, 4) NOT NULL,
    description VARCHAR(255),
    posted_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_ledger_account FOREIGN KEY (account_id) 
        REFERENCES accounts(id) ON DELETE RESTRICT,
    CONSTRAINT fk_ledger_transaction FOREIGN KEY (transaction_id) 
        REFERENCES transactions(id) ON DELETE RESTRICT,
    CONSTRAINT chk_ledger_entry_amount_positive CHECK (amount > 0.0000),
    CONSTRAINT chk_ledger_entry_type CHECK (entry_type IN ('DEBIT', 'CREDIT'))
);

-- Table 6: ACH Transfers (Individual items in a delayed batch)
CREATE TABLE ach_transfers (
    id BIGSERIAL PRIMARY KEY,
    batch_id BIGINT NOT NULL,
    source_account_id BIGINT NOT NULL,
    destination_account_id BIGINT NOT NULL,
    amount NUMERIC(19, 4) NOT NULL,
    status VARCHAR(32) NOT NULL,
    error_message VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_ach_transfers_batch FOREIGN KEY (batch_id) 
        REFERENCES ach_batches(id) ON DELETE CASCADE,
    CONSTRAINT fk_ach_source_account FOREIGN KEY (source_account_id) 
        REFERENCES accounts(id) ON DELETE RESTRICT,
    CONSTRAINT fk_ach_dest_account FOREIGN KEY (destination_account_id) 
        REFERENCES accounts(id) ON DELETE RESTRICT,
    CONSTRAINT chk_ach_transfer_amount CHECK (amount > 0.0000)
);

-- ----------------------------------------------------------------------------
-- 3. EXPLICIT DATABASE INDEXES (Advanced SQL Feature: Performance Optimization)
-- ----------------------------------------------------------------------------
CREATE INDEX idx_accounts_number ON accounts(account_number);
CREATE INDEX idx_accounts_branch_status ON accounts(branch_id, status);
CREATE INDEX idx_transactions_reference ON transactions(reference_number);
CREATE INDEX idx_transactions_type_status ON transactions(transaction_type, status);
CREATE INDEX idx_ledger_entries_account_posted ON ledger_entries(account_id, posted_at DESC);
CREATE INDEX idx_ledger_entries_transaction ON ledger_entries(transaction_id);
CREATE INDEX idx_ach_batches_status ON ach_batches(status);
CREATE INDEX idx_ach_transfers_batch_status ON ach_transfers(batch_id, status);

-- ----------------------------------------------------------------------------
-- 4. ADVANCED SQL VIEWS (Advanced SQL Feature: Reporting & Aggregations)
-- ----------------------------------------------------------------------------

-- View 1: v_trial_balance (Calculates debits, credits, and net ledger balances)
CREATE OR REPLACE VIEW v_trial_balance AS
SELECT 
    b.id AS branch_id,
    b.name AS branch_name,
    b.branch_code,
    a.id AS account_id,
    a.account_number,
    a.account_holder_name,
    a.account_type,
    COALESCE(SUM(CASE WHEN le.entry_type = 'CREDIT' THEN le.amount ELSE 0 END), 0.0000) AS total_credits,
    COALESCE(SUM(CASE WHEN le.entry_type = 'DEBIT' THEN le.amount ELSE 0 END), 0.0000) AS total_debits,
    COALESCE(SUM(CASE WHEN le.entry_type = 'CREDIT' THEN le.amount ELSE -le.amount END), 0.0000) AS net_ledger_balance,
    a.balance AS stored_balance,
    a.status AS account_status
FROM accounts a
JOIN branches b ON a.branch_id = b.id
LEFT JOIN ledger_entries le ON a.id = le.account_id
GROUP BY b.id, b.name, b.branch_code, a.id, a.account_number, a.account_holder_name, a.account_type, a.balance, a.status;

-- View 2: v_account_statement (Denormalized chronological ledger audit log)
CREATE OR REPLACE VIEW v_account_statement AS
SELECT 
    le.id AS ledger_entry_id,
    a.account_number,
    a.account_holder_name,
    b.name AS branch_name,
    t.reference_number,
    t.transaction_type,
    le.entry_type,
    le.amount,
    le.running_balance,
    le.description,
    le.posted_at
FROM ledger_entries le
JOIN accounts a ON le.account_id = a.id
JOIN branches b ON a.branch_id = b.id
JOIN transactions t ON le.transaction_id = t.id;

-- View 3: v_branch_settlement_summary (Branch-level deposit & activity aggregation)
CREATE OR REPLACE VIEW v_branch_settlement_summary AS
SELECT
    b.id AS branch_id,
    b.name AS branch_name,
    b.branch_code,
    b.location,
    COUNT(DISTINCT a.id) AS total_accounts,
    COALESCE(SUM(a.balance), 0.0000) AS total_deposits,
    COUNT(DISTINCT le.id) AS total_ledger_entries
FROM branches b
LEFT JOIN accounts a ON b.id = a.branch_id AND a.account_number <> 'ACH-CLEARING'
LEFT JOIN ledger_entries le ON a.id = le.account_id
GROUP BY b.id, b.name, b.branch_code, b.location;

-- ----------------------------------------------------------------------------
-- 5. ADVANCED STORED FUNCTIONS & TRIGGERS (Advanced SQL Feature)
-- ----------------------------------------------------------------------------

-- Stored Function 1: Recalculate true account balance from immutable ledger entries
CREATE OR REPLACE FUNCTION fn_calculate_account_balance(p_account_id BIGINT)
RETURNS NUMERIC AS $$
DECLARE
    v_calculated_balance NUMERIC(19, 4);
BEGIN
    SELECT COALESCE(SUM(CASE WHEN entry_type = 'CREDIT' THEN amount ELSE -amount END), 0.0000)
    INTO v_calculated_balance
    FROM ledger_entries
    WHERE account_id = p_account_id;
    
    RETURN v_calculated_balance;
END;
$$ LANGUAGE plpgsql;

-- Stored Function 2 & Trigger: Enforce balance non-negativity constraint at the database engine level
CREATE OR REPLACE FUNCTION fn_enforce_account_balance()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.balance < 0.0000 THEN
        RAISE EXCEPTION 'Database Integrity Constraint Violated: Account % balance cannot drop below 0.00 (Attempted Balance: %)',
            NEW.account_number, NEW.balance;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Trigger: Execute function BEFORE UPDATE of balance on accounts table
CREATE TRIGGER trg_enforce_account_balance
    BEFORE UPDATE OF balance ON accounts
    FOR EACH ROW
    EXECUTE FUNCTION fn_enforce_account_balance();
