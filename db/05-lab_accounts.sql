CREATE TABLE lab_accounts (
      id            bigint PRIMARY KEY,
      balance_minor bigint NOT NULL CHECK (balance_minor >= 0),
      version       bigint NOT NULL DEFAULT 0
);