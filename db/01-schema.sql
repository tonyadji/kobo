CREATE EXTENSION IF NOT EXISTS pg_stat_statements;

CREATE TABLE users (
                       id          bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                       email       text        NOT NULL UNIQUE,
                       full_name   text        NOT NULL,
                       country     char(2)     NOT NULL,
                       status      text        NOT NULL DEFAULT 'ACTIVE'
                           CHECK (status IN ('ACTIVE', 'SUSPENDED', 'CLOSED')),
                       created_at  timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE accounts (
                          id             bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                          user_id        bigint      NOT NULL REFERENCES users (id),
                          currency       char(3)     NOT NULL,
                          balance_minor  bigint      NOT NULL DEFAULT 0 CHECK (balance_minor >= 0),
                          version        bigint      NOT NULL DEFAULT 0,
                          created_at     timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE transactions (
                              id               bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                              from_account_id  bigint      NOT NULL REFERENCES accounts (id),
                              to_account_id    bigint      NOT NULL REFERENCES accounts (id),
                              amount_minor     bigint      NOT NULL CHECK (amount_minor > 0),
                              currency         char(3)     NOT NULL,
                              status           text        NOT NULL
                                  CHECK (status IN ('PENDING', 'COMPLETED', 'FAILED')),
                              created_at       timestamptz NOT NULL
);