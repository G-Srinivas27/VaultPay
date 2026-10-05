-- ============================================================
-- V1 — Create users table
-- ============================================================
-- This is the first migration. It defines the users table
-- which is the foundation of VaultPay — all other tables
-- reference this one.
--
-- Why separate files per table?
-- Each migration is atomic — if V2 fails, V1 stays intact.
-- You can see exactly WHEN each table was created and WHY.
-- ============================================================

CREATE TABLE IF NOT EXISTS users (
    id              BIGINT          NOT NULL AUTO_INCREMENT,

    -- Core user fields
    first_name      VARCHAR(100)    NOT NULL,
    last_name       VARCHAR(100)    NOT NULL,
    email           VARCHAR(200)    NOT NULL,
    password_hash   VARCHAR(255)    NOT NULL,

    -- Role — ENUM restricts to valid values at DB level
    -- Application layer also validates, but DB is the last line of defence
    role            ENUM('ADMIN', 'USER') NOT NULL DEFAULT 'USER',

    -- Soft delete flag — we never hard delete users (preserves audit trail)
    active          BIT             NOT NULL DEFAULT 1,

    -- Audit columns (managed by Spring Data JPA @EnableJpaAuditing)
    created_at      DATETIME(6)     NOT NULL,
    updated_at      DATETIME(6)     NOT NULL,
    created_by      VARCHAR(200),       -- email of who created this record
    last_modified_by VARCHAR(200),      -- email of who last modified this record

    PRIMARY KEY (id),

    -- Email uniqueness enforced at DB level — application checks first,
    -- but this is the safety net against race conditions
    CONSTRAINT uk_users_email UNIQUE (email)

) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COMMENT = 'VaultPay user accounts';
