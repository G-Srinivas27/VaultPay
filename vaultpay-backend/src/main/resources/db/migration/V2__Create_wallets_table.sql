-- ============================================================
-- V2 — Create wallets table (PostgreSQL)
-- ============================================================
-- Each user gets exactly ONE wallet (OneToOne relationship).
-- The UNIQUE constraint on user_id enforces this at the DB level.
--
-- Depends on: V1 (users table must exist before wallets)
-- Flyway guarantees migrations run in version order.
-- ============================================================

CREATE TABLE IF NOT EXISTS wallets (
    id          BIGINT          GENERATED ALWAYS AS IDENTITY,

    -- DECIMAL(19,4) works identically in PostgreSQL and MySQL
    -- 19 digits total, 4 decimal places → supports up to ₹999,999,999,999,999.9999
    balance     DECIMAL(19, 4)  NOT NULL DEFAULT 0.0000,

    -- 3-char ISO 4217 currency code (INR, USD, EUR, etc.)
    currency    VARCHAR(3)      NOT NULL DEFAULT 'INR',

    -- Wallet lifecycle status (replaces MySQL ENUM with CHECK constraint)
    status      VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE',

    -- Foreign key to users — UNIQUE enforces one wallet per user
    user_id     BIGINT          NOT NULL,

    -- Audit columns
    created_at      TIMESTAMP(6) NOT NULL,
    updated_at      TIMESTAMP(6) NOT NULL,
    created_by      VARCHAR(200),
    last_modified_by VARCHAR(200),

    PRIMARY KEY (id),
    CONSTRAINT uk_wallets_user_id UNIQUE (user_id),
    CONSTRAINT fk_wallets_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT ck_wallets_status CHECK (status IN ('ACTIVE', 'CLOSED', 'FROZEN'))
);

COMMENT ON TABLE wallets IS 'Digital wallets — one per user';
