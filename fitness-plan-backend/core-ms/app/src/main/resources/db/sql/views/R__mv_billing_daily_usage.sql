DROP MATERIALIZED VIEW IF EXISTS mv_billing_daily_usage;

CREATE MATERIALIZED VIEW mv_billing_daily_usage AS
SELECT
    t.merchant_id,
    DATE_TRUNC('day', t.created_at)::date AS usage_date,
    COUNT(DISTINCT CASE
        WHEN a.type IN ('CUSTOMER', 'BUSINESS') THEN te.account
    END) AS active_account_count,
    COUNT(DISTINCT t.id) AS transaction_count
FROM transaction t
JOIN transaction_entry te ON te.transaction = t.id
JOIN account a ON a.id = te.account
WHERE t.status = 'COMPLETED'
GROUP BY t.merchant_id, DATE_TRUNC('day', t.created_at)::date;

CREATE UNIQUE INDEX IF NOT EXISTS idx_mv_billing_usage_unique
    ON mv_billing_daily_usage (merchant_id, usage_date);
