-- ============================================================
-- V3 — Create transactions table
-- ============================================================
-- Records every financial event in the system.
-- Transactions are IMMUTABLE — never updated after creation.
-- This is a fundamental rule of financial systems.
--
-- Double-entry bookkeeping:
-- A transfer creates TWO records:
--   1. DEBIT  on sender's wallet   (wallet_id = sender)
--   2. CREDIT on receiver's wallet (wallet_id = receiver,
--              counterparty_wallet_id = sender)
-- This lets you reconstruct the full history from either side.
--
-- Depends on: V2 (wallets table must exist)
-- ============================================================

CREATE TABLE IF NOT EXISTS transactions (
    id          BIGINT          NOT NULL AUTO_INCREMENT,

    -- The amount of this transaction (always positive)
    -- The TYPE field (DEBIT/CREDIT) determines the direction
    amount      DECIMAL(19, 4)  NOT NULL,

    -- Snapshot of the wallet balance AFTER this transaction
    -- Critical for auditing — can reconstruct full history without
    -- summing all transactions (O(1) balance lookup vs O(n) scan)
    balance_after DECIMAL(19, 4) NOT NULL,

    -- Transaction type
    -- CREDIT   → money IN  to wallet (deposit, transfer received)
    -- DEBIT    → money OUT of wallet (withdrawal, transfer sent)
    -- TRANSFER → used on the sender side when recording a transfer
    type        ENUM('CREDIT', 'DEBIT', 'TRANSFER') NOT NULL,

    -- Optional human-readable note (like UPI remarks)
    description VARCHAR(255),

    -- The primary wallet involved in this transaction
    wallet_id   BIGINT NOT NULL,

    -- For TRANSFER type: the OTHER wallet involved
    -- NULL for simple deposits and withdrawals
    -- Sender record:   wallet_id=sender,   counterparty=receiver
    -- Receiver record: wallet_id=receiver, counterparty=sender
    counterparty_wallet_id BIGINT,

    -- Transactions are read-only after creation — no updated_at needed
    created_at  DATETIME(6)     NOT NULL,
    created_by  VARCHAR(200),   -- email of user who triggered this transaction

    PRIMARY KEY (id),

    -- Every transaction must belong to a real wallet
    CONSTRAINT fk_transactions_wallet
        FOREIGN KEY (wallet_id)
        REFERENCES wallets (id),

    -- Counterparty wallet must also be real (if present)
    CONSTRAINT fk_transactions_counterparty
        FOREIGN KEY (counterparty_wallet_id)
        REFERENCES wallets (id)

) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COMMENT = 'Immutable financial transaction ledger';
