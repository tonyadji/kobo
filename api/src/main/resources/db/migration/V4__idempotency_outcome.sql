ALTER TABLE transfers_idempotency_keys
    DROP COLUMN response_body,
    DROP COLUMN http_status,
    DROP COLUMN status,
    ADD COLUMN outcome TEXT NOT NULL DEFAULT 'PROCESSING'
         CHECK (outcome IN ('PROCESSING', 'COMPLETED', 'INSUFFICIENT_FUNDS', 'UNKNOWN_ACCOUNT')),
    ADD COLUMN transfer_id BIGINT NULL,
    ADD COLUMN account_id BIGINT NULL,
    ADD CHECK ((outcome = 'COMPLETED') = (transfer_id IS NOT NULL));