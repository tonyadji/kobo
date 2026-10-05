\set users 1000000
\set transactions 5000000
\timing on

-- 1 M d'utilisateurs, répartis sur 10 pays, 2 % suspendus, ~1 % fermés
INSERT INTO users (email, full_name, country, status, created_at)
SELECT 'user' || g || '@kobo.test',
       'User ' || g,
       (ARRAY['FR','GB','PL','AE','NG','BJ','CM','DE','ES','PT'])[1 + (g % 10)],
       CASE WHEN g % 50 = 0 THEN 'SUSPENDED'
            WHEN g % 97 = 0 THEN 'CLOSED'
            ELSE 'ACTIVE' END,
       now() - random() * interval '3 years'
FROM generate_series(1, :users) AS g;

-- Un compte EUR par utilisateur (ids 1..N, donc id de compte = id d'utilisateur),
-- puis un compte USD pour 20 % des utilisateurs
INSERT INTO accounts (user_id, currency, balance_minor, created_at)
SELECT id, 'EUR', (random() * 500000)::bigint, created_at FROM users ORDER BY id;

INSERT INTO accounts (user_id, currency, balance_minor, created_at)
SELECT id, 'USD', (random() * 200000)::bigint, created_at FROM users WHERE id % 5 = 0;

-- 5 M de transactions sur 2 ans, entre comptes EUR.
-- power(random(), 3) concentre les débits sur les petits ids : quelques comptes
-- très sollicités (comme un compte marchand), et une longue traîne de comptes calmes.
INSERT INTO transactions (from_account_id, to_account_id, amount_minor, currency, status, created_at)
SELECT 1 + floor(:users * power(random(), 3))::bigint,
    1 + floor(:users * random())::bigint,
    1 + floor(random() * 50000)::bigint,
    'EUR',
       CASE WHEN random() < 0.97 THEN 'COMPLETED'
            WHEN random() < 0.5  THEN 'FAILED'
            ELSE 'PENDING' END,
       now() - random() * interval '2 years'
FROM generate_series(1, :transactions);

ANALYZE;