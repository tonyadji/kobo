-- learning CTE and advanced SQL patterns
WITH movements AS (
    SELECT id, from_account_id as account, (-1) * amount_minor as amount, created_at
    FROM transactions
    WHERE status = 'COMPLETED'
    UNION ALL (
        SELECT id, to_account_id as account, amount_minor as amount, created_at
        FROM transactions
        WHERE status = 'COMPLETED'
    )
    ORDER BY created_at DESC
), balance AS (
    select id, user_id, balance_minor from accounts
)

select account, SUM(amount) as balance_minor
FROM movements
         INNER JOIN balance on balance.id = movements.account
WHERE account = 42
GROUP BY account, balance.balance_minor;

WITH movements AS (
    SELECT id, from_account_id as account, (-1) * amount_minor as amount, created_at
    FROM transactions
    WHERE status = 'COMPLETED'
    UNION ALL (
        SELECT id, to_account_id as account, amount_minor as amount, created_at
        FROM transactions
        WHERE status = 'COMPLETED'
    )
    ORDER BY created_at DESC
)
SELECT *
FROM movements
WHERE account = 42
ORDER BY amount asc
    LIMIT 3;
