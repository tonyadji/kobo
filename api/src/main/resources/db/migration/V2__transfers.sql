CREATE TABLE transfers (
    id              BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    from_account_id BIGINT NOT NULL REFERENCES accounts(id),
    to_account_id   BIGINT NOT NULL REFERENCES accounts(id),
    amount_minor    BIGINT NOT NULL CHECK ( amount_minor > 0 ),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX transfers_from_account_id_idx ON transfers(from_account_id);
CREATE INDEX transfers_to_account_id_idx ON transfers(to_account_id);