CREATE TABLE transfers_idempotency_keys (
    key                     TEXT PRIMARY KEY,
    request_hash            TEXT NOT NULL,
    response_body           TEXT,
    status                  TEXT NOT NULL CHECK ( status IN ('FAILED', 'PROCESSING', 'SUCCESS')),
    http_status             INTEGER,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT now()
);