DROP MATERIALIZED VIEW IF EXISTS mv_daily_account_creation;

CREATE MATERIALIZED VIEW mv_daily_account_creation AS
SELECT
    a.merchant_id,
    a.type,
    DATE_TRUNC('day', a.created_at) AS stat_date,
    COUNT(*) AS account_count
FROM account a
WHERE a.parent_id IS NOT NULL
AND a.type NOT IN ('EQUITY', 'CASH', 'FEE', 'EXPENSE', 'REVENUE', 'LOCK', 'CONVERSION', 'POOL', 'BRIDGE_ASSETS', 'BRIDGE_LIABILITIES')
GROUP BY a.merchant_id, a.type, DATE_TRUNC('day', a.created_at);

CREATE UNIQUE INDEX IF NOT EXISTS idx_mv_daily_acct_creation_unique
    ON mv_daily_account_creation (merchant_id, type, stat_date);

CREATE INDEX IF NOT EXISTS idx_mv_daily_acct_creation_merchant_date
    ON mv_daily_account_creation (merchant_id, stat_date);

CREATE INDEX IF NOT EXISTS idx_mv_daily_acct_creation_type_date
    ON mv_daily_account_creation (merchant_id, type, stat_date);
