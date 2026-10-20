-- Flyway Migration V3: Add encrypted refresh token and reauth_required to users

ALTER TABLE users 
    ADD COLUMN IF NOT EXISTS encrypted_refresh_token TEXT,
    ADD COLUMN IF NOT EXISTS reauth_required BOOLEAN NOT NULL DEFAULT FALSE;

CREATE INDEX IF NOT EXISTS idx_users_reauth_required ON users(reauth_required);
