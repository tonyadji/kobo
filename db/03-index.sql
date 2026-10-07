-- Account history: covering index (replaces idx_tx_from_created, see s1.md, Wednesday 7)
CREATE INDEX IF NOT EXISTS idx_tx_from_cover ON transactions (from_account_id, created_at DESC)
    INCLUDE (amount_minor, status);
CREATE INDEX IF NOT EXISTS idx_tx_to_created ON transactions (to_account_id, created_at);

-- Retry job: only the ~1.5 % pending transactions
CREATE INDEX IF NOT EXISTS idx_tx_pending ON transactions (created_at) WHERE status = 'PENDING';

-- Foreign key used to find a user's accounts
CREATE INDEX IF NOT EXISTS idx_accounts_user ON accounts (user_id);
