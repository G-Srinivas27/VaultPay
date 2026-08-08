-- ============================================================
-- V2 — Create wallets table
-- ============================================================
-- Each user gets exactly ONE wallet (OneToOne relationship).
-- The UNIQUE constraint on user_id enforces this at the DB level.
--
-- Depends on: V1 (users table must exist before wallets)
-- Flyway guarantees migrations run in version order, so
-- users will always exist before wallets are created.
-- ============================================================

CREATE TABLE IF NOT EXISTS wallets (
    id          BIGINT          NOT NULL AUTO_INCREMENT,

    -- Financial balance — DECIMAL(19,4) for precise monetary values
    -- Never use FLOAT or DOUBLE for money — floating point errors will cost you!
    -- 19 digits total, 4 decimal places → supports up to ₹999,999,999,999,999.9999
    balance     DECIMAL(19, 4)  NOT NULL DEFAULT 0.0000,

    -- 3-char ISO 4217 currency code (INR, USD, EUR, etc.)
    currency    VARCHAR(3)      NOT NULL DEFAULT 'INR',

    -- Wallet lifecycle status
    -- ACTIVE  → normal operations allowed
    -- FROZEN  → no withdrawals or transfers (admin action)
    -- CLOSED  → permanently disabled
    status      ENUM('ACTIVE', 'CLOSED', 'FROZEN') NOT NULL DEFAULT 'ACTIVE',

    -- Foreign key to users — UNIQUE enforces one wallet per user
    user_id     BIGINT          NOT NULL,

    -- Audit columns
    created_at      DATETIME(6) NOT NULL,
    updated_at      DATETIME(6) NOT NULL,
    created_by      VARCHAR(200),
    last_modified_by VARCHAR(200),

    PRIMARY KEY (id),

    -- OneToOne enforcement: each user can have only ONE wallet
    CONSTRAINT uk_wallets_user_id UNIQUE (user_id),

    -- Referential integrity: wallet must belong to a real user
    CONSTRAINT fk_wallets_user
        FOREIGN KEY (user_id)
        REFERENCES users (id)

) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COMMENT = 'Digital wallets — one per user';
