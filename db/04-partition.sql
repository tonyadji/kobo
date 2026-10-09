-- Monthly range-partitioned copy of transactions (see s1.md, Thursday 8)
-- The partition key must be part of the primary key: each partition has its own unique index.
CREATE TABLE transactions_p (
    id               bigint      NOT NULL,
    from_account_id  bigint      NOT NULL,
    to_account_id    bigint      NOT NULL,
    amount_minor     bigint      NOT NULL CHECK (amount_minor > 0),
    currency         char(3)     NOT NULL,
    status           text        NOT NULL,
    created_at       timestamptz NOT NULL,
    PRIMARY KEY (id, created_at)
) PARTITION BY RANGE (created_at);

-- One partition per month, from the oldest transaction to one month after the newest.
-- Bounds come from the data, not from now(), so the script still works if run later than the seed.
DO $$
DECLARE
    d    timestamptz;
    last timestamptz;
BEGIN
    SELECT date_trunc('month', min(created_at)), date_trunc('month', max(created_at)) + interval '1 month'
    INTO d, last
    FROM transactions;

    WHILE d <= last LOOP
        EXECUTE format(
            'CREATE TABLE %I PARTITION OF transactions_p FOR VALUES FROM (%L) TO (%L)',
            'transactions_p_' || to_char(d, 'YYYY_MM'), d, d + interval '1 month');
        d := d + interval '1 month';
    END LOOP;
END $$;

INSERT INTO transactions_p SELECT * FROM transactions;

-- Account history: same index shape as Tuesday, created on every partition
CREATE INDEX ON transactions_p (from_account_id, created_at DESC);
CREATE INDEX ON transactions_p (to_account_id, created_at DESC);

ANALYZE transactions_p;
