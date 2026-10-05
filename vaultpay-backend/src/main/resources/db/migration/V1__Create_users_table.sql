-- ============================================================
-- V1 — Create users table (PostgreSQL)
-- ============================================================
-- This is the first migration. It defines the users table
-- which is the foundation of VaultPay — all other tables
-- reference this one.
--
-- Key differences from MySQL version:
--   AUTO_INCREMENT     → GENERATED ALWAYS AS IDENTITY
--   ENUM('ADMIN','USER') → VARCHAR(20) + CHECK constraint
--   BIT                → BOOLEAN
--   DATETIME(6)        → TIMESTAMP(6)
--   ENGINE/CHARSET     → not needed in PostgreSQL
-- ============================================================

CREATE TABLE IF NOT EXISTS users (
    -- GENERATED ALWAYS AS IDENTITY = PostgreSQL's auto-increment
    -- 'ALWAYS' means the DB controls the value; you can't manually insert an ID
    -- (use 'BY DEFAULT' if you need to override sometimes)
    id              BIGINT          GENERATED ALWAYS AS IDENTITY,

    -- Core user fields
    first_name      VARCHAR(100)    NOT NULL,
    last_name       VARCHAR(100)    NOT NULL,
    email           VARCHAR(200)    NOT NULL,
    password_hash   VARCHAR(255)    NOT NULL,

    -- Role — CHECK constraint replaces MySQL ENUM
    -- PostgreSQL doesn't have a native ENUM type (well, it does via CREATE TYPE,
    -- but VARCHAR + CHECK is simpler, more portable, and easier to modify later)
    role            VARCHAR(20)     NOT NULL DEFAULT 'USER',

    -- BOOLEAN replaces MySQL's BIT type — stores true/false directly
    active          BOOLEAN         NOT NULL DEFAULT TRUE,

    -- Audit columns — TIMESTAMP(6) stores date+time with microsecond precision
    -- (same as MySQL DATETIME(6), just different name)
    created_at      TIMESTAMP(6)    NOT NULL,
    updated_at      TIMESTAMP(6)    NOT NULL,
    created_by      VARCHAR(200),
    last_modified_by VARCHAR(200),

    PRIMARY KEY (id),
    CONSTRAINT uk_users_email UNIQUE (email),
    CONSTRAINT ck_users_role CHECK (role IN ('ADMIN', 'USER'))
);

COMMENT ON TABLE users IS 'VaultPay user accounts';
