CREATE INDEX IF NOT EXISTS idx_from_account ON transactions (from_account_id, created_at);
CREATE INDEX IF NOT EXISTS idx_to_account ON transactions (to_account_id, created_at);
CREATE INDEX IF NOT EXISTS idx_accounts_user ON accounts (user_id);