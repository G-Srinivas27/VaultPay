-- ============================================================
-- V5 — Create password_reset_tokens table (PostgreSQL)
-- ============================================================
-- Stores temporary tokens for the "Forgot Password" flow.
-- Tokens expire after a set duration (checked by the app).
-- ============================================================

CREATE TABLE IF NOT EXISTS password_reset_tokens (
    id          BIGINT          GENERATED ALWAYS AS IDENTITY,
    token       VARCHAR(100)    NOT NULL UNIQUE,
    user_id     BIGINT          NOT NULL,
    expiry_date TIMESTAMP       NOT NULL,

    PRIMARY KEY (id),
    CONSTRAINT fk_reset_tokens_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
