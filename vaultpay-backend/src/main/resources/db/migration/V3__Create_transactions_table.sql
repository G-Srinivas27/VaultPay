-- ============================================================
-- V3 — Create transactions table (PostgreSQL)
-- ============================================================
-- Records every financial event in the system.
-- Transactions are IMMUTABLE — never updated after creation.
--
-- Double-entry bookkeeping:
-- A transfer creates TWO records:
--   1. DEBIT  on sender's wallet   (wallet_id = sender)
--   2. CREDIT on receiver's wallet (wallet_id = receiver,
--              counterparty_wallet_id = sender)
--
-- Depends on: V2 (wallets table must exist)
-- ============================================================

CREATE TABLE IF NOT EXISTS transactions (
    id          BIGINT          GENERATED ALWAYS AS IDENTITY,

    -- The amount of this transaction (always positive)
    amount      DECIMAL(19, 4)  NOT NULL,

    -- Snapshot of wallet balance AFTER this transaction
    balance_after DECIMAL(19, 4) NOT NULL,

    -- Transaction type (replaces MySQL ENUM)
    type        VARCHAR(20)     NOT NULL,

    -- Optional human-readable note (like UPI remarks)
    description VARCHAR(255),

    -- The primary wallet involved in this transaction
    wallet_id   BIGINT          NOT NULL,

    -- For TRANSFER type: the OTHER wallet involved
    -- NULL for simple deposits and withdrawals
    counterparty_wallet_id BIGINT,

    -- Transactions are read-only — no updated_at needed
    created_at  TIMESTAMP(6)    NOT NULL,
    created_by  VARCHAR(200),

    PRIMARY KEY (id),
    CONSTRAINT fk_transactions_wallet FOREIGN KEY (wallet_id) REFERENCES wallets (id),
    CONSTRAINT fk_transactions_counterparty FOREIGN KEY (counterparty_wallet_id) REFERENCES wallets (id),
    CONSTRAINT ck_transactions_type CHECK (type IN ('CREDIT', 'DEBIT', 'TRANSFER'))
);

COMMENT ON TABLE transactions IS 'Immutable financial transaction ledger';
