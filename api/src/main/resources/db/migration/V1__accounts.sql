CREATE TABLE accounts (
    id              BIGINT PRIMARY KEY,
    balance_minor   BIGINT NOT NULL CHECK ( balance_minor >= 0 )
);