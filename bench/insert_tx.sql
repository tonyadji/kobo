\set from random(1, 1000000)
\set to random(1, 1000000)
\set amount random(1, 50000)
INSERT INTO transactions (from_account_id, to_account_id, amount_minor, currency, status, created_at)
VALUES (:from, :to, :amount, 'EUR', 'COMPLETED', now());
